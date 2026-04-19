import cors from "cors";
import express from "express";
import { ZodError, z } from "zod";
import { env } from "./config.js";
import { pool } from "./db.js";
import { getInitialNextDueDate, getRecurringStatus, syncRecurringTransactions } from "./recurring.js";
import { parseTransactionFilters, parseTransactionPagination } from "./transactions.js";

const app = express();
const demoUserId = 1;

const monthQuerySchema = z.string().regex(/^\d{4}-\d{2}$/).optional();

const transactionSchema = z.object({
  title: z.string().trim().min(2, "Title must be at least 2 characters."),
  kind: z.enum(["income", "expense"]),
  amount: z.coerce.number().positive("Amount must be greater than zero."),
  notes: z.string().trim().max(300, "Notes must be 300 characters or less.").optional().nullable(),
  merchant: z.string().trim().max(120, "Merchant must be 120 characters or less.").optional().nullable(),
  transactionDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "Transaction date must be in YYYY-MM-DD format."),
  accountId: z.coerce.number().int().positive(),
  categoryId: z.coerce.number().int().positive()
});

const categorySchema = z.object({
  name: z.string().trim().min(2, "Category name must be at least 2 characters.").max(80, "Category name is too long."),
  type: z.enum(["income", "expense"]),
  color: z.string().regex(/^#[0-9a-fA-F]{6}$/, "Color must be a valid hex code."),
  icon: z.string().trim().min(1, "Icon label is required.").max(40, "Icon label is too long."),
  budgetMode: z.enum(["fixed", "flexible"]).default("flexible"),
  changeNote: z.string().trim().max(300, "Change note must be 300 characters or less.").optional().nullable()
});

const budgetSchema = z.object({
  categoryId: z.coerce.number().int().positive(),
  month: z.coerce.number().int().min(1).max(12),
  year: z.coerce.number().int().min(2000),
  allocatedAmount: z.coerce.number().positive("Allocated amount must be greater than zero.")
});

const monthlyBudgetSchema = z.object({
  month: z.coerce.number().int().min(1).max(12),
  year: z.coerce.number().int().min(2000),
  totalBudget: z.coerce.number().min(0, "Monthly budget cannot be negative.")
});

const recurringTransactionSchema = z.object({
  title: z.string().trim().min(2, "Title must be at least 2 characters."),
  kind: z.enum(["income", "expense"]),
  amount: z.coerce.number().positive("Amount must be greater than zero."),
  notes: z.string().trim().max(300, "Notes must be 300 characters or less.").optional().nullable(),
  merchant: z.string().trim().max(120, "Merchant must be 120 characters or less.").optional().nullable(),
  accountId: z.coerce.number().int().positive(),
  categoryId: z.coerce.number().int().positive(),
  dayOfMonth: z.coerce.number().int().min(1).max(31),
  startDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "Start date must be in YYYY-MM-DD format."),
  autoCreate: z.boolean().default(true),
  isActive: z.boolean().default(true)
});

const bulkDeleteTransactionsSchema = z.object({
  ids: z.array(z.coerce.number().int().positive()).min(1, "Select at least one transaction."),
  note: z.string().trim().max(300).optional().nullable()
});

const bulkRecategorizeTransactionsSchema = z.object({
  ids: z.array(z.coerce.number().int().positive()).min(1, "Select at least one transaction."),
  categoryId: z.coerce.number().int().positive(),
  note: z.string().trim().max(300).optional().nullable()
});

const archiveCategorySchema = z.object({
  isArchived: z.boolean(),
  changeNote: z.string().trim().max(300).optional().nullable()
});

const deleteCategorySchema = z.object({
  reassignmentCategoryId: z.coerce.number().int().positive().optional(),
  changeNote: z.string().trim().max(300).optional().nullable()
});

app.use(cors({ origin: [env.CLIENT_ORIGIN, "http://127.0.0.1:5173", "http://localhost:5173"] }));
app.use(express.json());

function getSelectedMonth(rawMonth?: string) {
  const parsed = monthQuerySchema.parse(rawMonth);
  if (parsed) {
    const [year, month] = parsed.split("-").map(Number);
    return { year, month, monthKey: parsed };
  }

  const now = new Date();
  return {
    year: now.getFullYear(),
    month: now.getMonth() + 1,
    monthKey: `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`
  };
}

function formatMonthLabel(year: number, month: number) {
  return new Intl.DateTimeFormat("en-IN", {
    month: "short",
    year: "numeric"
  }).format(new Date(year, month - 1, 1));
}

function buildTrailingMonths(monthKey: string, count: number) {
  const [year, month] = monthKey.split("-").map(Number);
  const months: Array<{ period: string; monthLabel: string }> = [];

  for (let index = count - 1; index >= 0; index -= 1) {
    const current = new Date(year, month - 1 - index, 1);
    const currentYear = current.getFullYear();
    const currentMonth = current.getMonth() + 1;
    const period = `${currentYear}-${String(currentMonth).padStart(2, "0")}`;
    months.push({
      period,
      monthLabel: formatMonthLabel(currentYear, currentMonth)
    });
  }

  return months;
}

function buildYearMonths(year: number) {
  return Array.from({ length: 12 }, (_, index) => ({
    period: `${year}-${String(index + 1).padStart(2, "0")}`,
    monthLabel: formatMonthLabel(year, index + 1)
  }));
}

function buildTrailingYears(year: number, count: number) {
  return Array.from({ length: count }, (_, index) => {
    const currentYear = year - (count - 1 - index);
    return {
      year: currentYear,
      yearLabel: String(currentYear)
    };
  });
}

function getDaysInMonth(year: number, month: number) {
  return new Date(year, month, 0).getDate();
}

function buildTransactionWhereClause(selected: { month: number; year: number }, filters: ReturnType<typeof parseTransactionFilters>) {
  const whereClauses = [
    "t.user_id = ?",
    "MONTH(t.transaction_date) = ?",
    "YEAR(t.transaction_date) = ?"
  ];
  const whereParams: Array<string | number> = [demoUserId, selected.month, selected.year];

  if (filters.q) {
    whereClauses.push("(t.title LIKE ? OR COALESCE(t.merchant, '') LIKE ? OR COALESCE(t.notes, '') LIKE ?)");
    const query = `%${filters.q}%`;
    whereParams.push(query, query, query);
  }

  if (filters.kind) {
    whereClauses.push("t.kind = ?");
    whereParams.push(filters.kind);
  }

  if (filters.accountId) {
    whereClauses.push("t.account_id = ?");
    whereParams.push(filters.accountId);
  }

  if (filters.categoryId) {
    whereClauses.push("t.category_id = ?");
    whereParams.push(filters.categoryId);
  }

  return {
    whereSql: whereClauses.join("\n        AND "),
    whereParams
  };
}

async function logActivity(entityType: string, entityId: number | null, action: string, title: string, note?: string | null) {
  await pool.execute(
    `
      INSERT INTO activity_logs (user_id, entity_type, entity_id, action, title, note)
      VALUES (?, ?, ?, ?, ?, ?)
    `,
    [demoUserId, entityType, entityId, action, title, note ?? null]
  );
}

async function ensureCategoryUnique(name: string, type: "income" | "expense", excludeId?: number) {
  const [rows] = await pool.query(
    `
      SELECT id
      FROM categories
      WHERE LOWER(name) = LOWER(?)
        AND type = ?
        AND is_archived = FALSE
        AND (user_id IS NULL OR user_id = ?)
        ${excludeId ? "AND id <> ?" : ""}
      LIMIT 1
    `,
    excludeId ? [name, type, demoUserId, excludeId] : [name, type, demoUserId]
  );

  if (Array.isArray(rows) && rows.length > 0) {
    throw new ZodError([
      {
        code: "custom",
        path: ["name"],
        message: "A category with this name already exists for the selected type."
      }
    ]);
  }
}

app.get("/api/health", async (_request, response) => {
  await pool.query("SELECT 1");
  response.json({ ok: true });
});

app.get("/api/dashboard", async (request, response) => {
  const selected = getSelectedMonth(request.query.month as string | undefined);

  const [summaryRows] = await pool.query(
    `
      SELECT
        COALESCE(SUM(CASE WHEN t.kind = 'income' THEN t.amount ELSE 0 END), 0) AS monthlyIncome,
        COALESCE(SUM(CASE WHEN t.kind = 'expense' THEN t.amount ELSE 0 END), 0) AS monthlyExpense,
        COALESCE((SELECT SUM(allocated_amount) FROM budgets WHERE user_id = ? AND month = ? AND year = ?), 0) AS budgetAllocated,
        COALESCE((SELECT total_budget FROM monthly_budget_targets WHERE user_id = ? AND month = ? AND year = ? LIMIT 1), 0) AS totalBudget
      FROM transactions t
      WHERE t.user_id = ?
        AND MONTH(t.transaction_date) = ?
        AND YEAR(t.transaction_date) = ?
    `,
    [demoUserId, selected.month, selected.year, demoUserId, selected.month, selected.year, demoUserId, selected.month, selected.year]
  );

  const [recentRows] = await pool.query(
    `
      SELECT
        t.id,
        t.title,
        t.kind,
        t.amount,
        t.transaction_date AS transactionDate,
        c.name AS categoryName,
        c.color AS categoryColor
      FROM transactions t
      JOIN categories c ON c.id = t.category_id
      WHERE t.user_id = ?
        AND MONTH(t.transaction_date) = ?
        AND YEAR(t.transaction_date) = ?
      ORDER BY t.transaction_date DESC, t.id DESC
      LIMIT 6
    `,
    [demoUserId, selected.month, selected.year]
  );

  const [budgetRows] = await pool.query(
    `
      SELECT
        b.id,
        c.name AS categoryName,
        c.color,
        c.budget_mode AS budgetMode,
        b.allocated_amount AS allocatedAmount,
        COALESCE(SUM(t.amount), 0) AS spentAmount
      FROM budgets b
      JOIN categories c ON c.id = b.category_id
      LEFT JOIN transactions t
        ON t.category_id = b.category_id
        AND t.kind = 'expense'
        AND MONTH(t.transaction_date) = b.month
        AND YEAR(t.transaction_date) = b.year
        AND t.user_id = b.user_id
      WHERE b.user_id = ?
        AND b.month = ?
        AND b.year = ?
      GROUP BY b.id, c.name, c.color, c.budget_mode, b.allocated_amount
      ORDER BY spentAmount DESC, c.name
    `,
    [demoUserId, selected.month, selected.year]
  );

  const [trendRows] = await pool.query(
    `
      SELECT
        DATE_FORMAT(transaction_date, '%b %Y') AS monthLabel,
        DATE_FORMAT(transaction_date, '%Y-%m') AS period,
        SUM(CASE WHEN kind = 'income' THEN amount ELSE 0 END) AS income,
        SUM(CASE WHEN kind = 'expense' THEN amount ELSE 0 END) AS expense
      FROM transactions
      WHERE user_id = ?
        AND transaction_date >= DATE_SUB(STR_TO_DATE(CONCAT(?, '-01'), '%Y-%m-%d'), INTERVAL 11 MONTH)
        AND transaction_date < DATE_ADD(STR_TO_DATE(CONCAT(?, '-01'), '%Y-%m-%d'), INTERVAL 1 MONTH)
      GROUP BY YEAR(transaction_date), MONTH(transaction_date), DATE_FORMAT(transaction_date, '%b %Y'), DATE_FORMAT(transaction_date, '%Y-%m')
      ORDER BY YEAR(transaction_date), MONTH(transaction_date)
    `,
    [demoUserId, selected.monthKey, selected.monthKey]
  );

  const [merchantRows] = await pool.query(
    `
      SELECT
        COALESCE(NULLIF(TRIM(merchant), ''), 'Unknown merchant') AS merchant,
        SUM(amount) AS totalSpent,
        COUNT(*) AS transactionCount
      FROM transactions
      WHERE user_id = ?
        AND kind = 'expense'
        AND MONTH(transaction_date) = ?
        AND YEAR(transaction_date) = ?
      GROUP BY COALESCE(NULLIF(TRIM(merchant), ''), 'Unknown merchant')
      ORDER BY totalSpent DESC, transactionCount DESC
      LIMIT 5
    `,
    [demoUserId, selected.month, selected.year]
  );

  const [categoryRows] = await pool.query(
    `
      SELECT
        c.name AS category,
        c.color,
        SUM(t.amount) AS totalSpent
      FROM transactions t
      JOIN categories c ON c.id = t.category_id
      WHERE t.user_id = ?
        AND t.kind = 'expense'
        AND MONTH(t.transaction_date) = ?
        AND YEAR(t.transaction_date) = ?
      GROUP BY c.id, c.name, c.color
      ORDER BY totalSpent DESC
      LIMIT 5
    `,
    [demoUserId, selected.month, selected.year]
  );

  const [categoryHistoryRows] = await pool.query(
    `
      SELECT
        c.name AS category,
        c.color,
        DATE_FORMAT(t.transaction_date, '%Y-%m') AS period,
        SUM(t.amount) AS totalSpent
      FROM transactions t
      JOIN categories c ON c.id = t.category_id
      WHERE t.user_id = ?
        AND t.kind = 'expense'
        AND t.transaction_date >= DATE_SUB(STR_TO_DATE(CONCAT(?, '-01'), '%Y-%m-%d'), INTERVAL 3 MONTH)
        AND t.transaction_date < DATE_ADD(STR_TO_DATE(CONCAT(?, '-01'), '%Y-%m-%d'), INTERVAL 1 MONTH)
      GROUP BY c.id, c.name, c.color, DATE_FORMAT(t.transaction_date, '%Y-%m')
      ORDER BY c.name, period
    `,
    [demoUserId, selected.monthKey, selected.monthKey]
  );

  const summary = Array.isArray(summaryRows) ? (summaryRows[0] as Record<string, string | number>) : {};
  const monthlyIncome = Number(summary.monthlyIncome ?? 0);
  const monthlyExpense = Number(summary.monthlyExpense ?? 0);
  const budgetAllocated = Number(summary.budgetAllocated ?? 0);
  const totalBudget = Number(summary.totalBudget ?? 0);
  const budgetSpent = Array.isArray(budgetRows)
    ? budgetRows.reduce((sum, row) => sum + Number((row as Record<string, string | number>).spentAmount ?? 0), 0)
    : 0;
  const daysInMonth = getDaysInMonth(selected.year, selected.month);
  const today = new Date();
  const isCurrentMonth = today.getFullYear() === selected.year && today.getMonth() + 1 === selected.month;
  const elapsedDays = isCurrentMonth ? Math.max(1, today.getDate()) : daysInMonth;
  const remainingDays = isCurrentMonth ? Math.max(daysInMonth - today.getDate() + 1, 1) : 0;
  const safeToSpend = remainingDays > 0 ? (totalBudget - monthlyExpense) / remainingDays : 0;
  const fixedBudget = Array.isArray(budgetRows)
    ? budgetRows.reduce((sum, row: any) => sum + (row.budgetMode === "fixed" ? Number(row.allocatedAmount ?? 0) : 0), 0)
    : 0;
  const flexibleBudget = Math.max(totalBudget - fixedBudget, 0);
  const budgetRisk = Array.isArray(budgetRows)
    ? budgetRows
        .map((row: any) => {
          const allocated = Number(row.allocatedAmount ?? 0);
          const spent = Number(row.spentAmount ?? 0);
          const projected = elapsedDays > 0 ? (spent / elapsedDays) * daysInMonth : spent;
          const overrun = projected - allocated;
          return {
            categoryName: row.categoryName,
            color: row.color,
            allocatedAmount: allocated,
            spentAmount: spent,
            projectedSpent: projected,
            overrun
          };
        })
        .filter((row) => row.overrun > 0)
        .sort((a, b) => b.overrun - a.overrun)
        .slice(0, 5)
    : [];

  const categoryHistoryMap = new Map<string, Array<{ period: string; totalSpent: number; color: string }>>();
  (Array.isArray(categoryHistoryRows) ? categoryHistoryRows : []).forEach((row: any) => {
    const existing = categoryHistoryMap.get(row.category) ?? [];
    existing.push({
      period: row.period,
      totalSpent: Number(row.totalSpent ?? 0),
      color: row.color
    });
    categoryHistoryMap.set(row.category, existing);
  });

  const unusualSpendAlerts = Array.from(categoryHistoryMap.entries())
    .map(([category, entries]) => {
      const current = entries.find((entry) => entry.period === selected.monthKey)?.totalSpent ?? 0;
      const previous = entries.filter((entry) => entry.period !== selected.monthKey).map((entry) => entry.totalSpent);
      const average = previous.length > 0 ? previous.reduce((sum, value) => sum + value, 0) / previous.length : 0;
      return {
        category,
        color: entries[0]?.color ?? "#0f766e",
        currentSpent: current,
        averageSpent: average,
        increaseAmount: current - average
      };
    })
    .filter((row) => row.currentSpent > 0 && row.averageSpent > 0 && row.currentSpent >= row.averageSpent * 1.5)
    .sort((a, b) => b.increaseAmount - a.increaseAmount)
    .slice(0, 5);

  response.json({
    selectedMonth: selected.monthKey,
    summary: {
      monthlyIncome,
      monthlyExpense,
      totalBudget,
      budgetAllocated,
      budgetSpent,
      remainingBudget: totalBudget - monthlyExpense,
      availableToAllocate: totalBudget - budgetAllocated,
      savingsRate: monthlyIncome > 0 ? ((monthlyIncome - monthlyExpense) / monthlyIncome) * 100 : 0,
      safeToSpend,
      remainingDays,
      fixedBudget,
      flexibleBudget
    },
    recentTransactions: recentRows,
    budgetProgress: budgetRows,
    monthlyTrend: trendRows,
    topMerchants: merchantRows,
    topSpendingCategories: categoryRows,
    unusualSpendAlerts,
    budgetRisk
  });
});

app.get("/api/transactions", async (request, response) => {
  const selected = getSelectedMonth(request.query.month as string | undefined);
  const pagination = parseTransactionPagination({
    page: request.query.page,
    perPage: request.query.perPage
  });
  const filters = parseTransactionFilters({
    q: request.query.q,
    kind: request.query.kind,
    accountId: request.query.accountId,
    categoryId: request.query.categoryId
  });
  const { whereSql, whereParams } = buildTransactionWhereClause(selected, filters);

  const [countRows] = await pool.query(
    `
      SELECT COUNT(*) AS total
      FROM transactions t
      WHERE ${whereSql}
    `,
    whereParams
  );

  const [rows] = await pool.query(
    `
      SELECT
        t.id,
        t.title,
        t.kind,
        t.amount,
        t.notes,
        t.merchant,
        DATE_FORMAT(t.transaction_date, '%Y-%m-%d') AS transactionDate,
        t.account_id AS accountId,
        a.name AS accountLabel,
        t.category_id AS categoryId,
        c.name AS categoryName,
        c.color AS categoryColor
      FROM transactions t
      JOIN accounts a ON a.id = t.account_id
      JOIN categories c ON c.id = t.category_id
      WHERE ${whereSql}
      ORDER BY t.transaction_date DESC, t.id DESC
      LIMIT ?
      OFFSET ?
    `,
    [...whereParams, pagination.perPage, pagination.offset]
  );

  const totalItems = Number(Array.isArray(countRows) ? (countRows[0] as Record<string, number>)?.total ?? 0 : 0);
  const totalPages = Math.max(1, Math.ceil(totalItems / pagination.perPage));

  response.json({
    items: rows,
    pagination: {
      page: pagination.page,
      perPage: pagination.perPage,
      totalItems,
      totalPages
    }
  });
});

app.get("/api/transactions/export", async (request, response) => {
  const selected = getSelectedMonth(request.query.month as string | undefined);
  const filters = parseTransactionFilters({
    q: request.query.q,
    kind: request.query.kind,
    accountId: request.query.accountId,
    categoryId: request.query.categoryId
  });
  const { whereSql, whereParams } = buildTransactionWhereClause(selected, filters);

  const [rows] = await pool.query(
    `
      SELECT
        t.id,
        t.title,
        t.kind,
        t.amount,
        t.notes,
        t.merchant,
        DATE_FORMAT(t.transaction_date, '%Y-%m-%d') AS transactionDate,
        a.name AS accountLabel,
        c.name AS categoryName
      FROM transactions t
      JOIN accounts a ON a.id = t.account_id
      JOIN categories c ON c.id = t.category_id
      WHERE ${whereSql}
      ORDER BY t.transaction_date DESC, t.id DESC
    `,
    whereParams
  );

  response.json(rows);
});

app.post("/api/transactions", async (request, response) => {
  const payload = transactionSchema.parse(request.body);
  const [result] = await pool.execute(
    `
      INSERT INTO transactions (user_id, account_id, category_id, kind, title, notes, merchant, amount, transaction_date)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    `,
    [demoUserId, payload.accountId, payload.categoryId, payload.kind, payload.title, payload.notes ?? null, payload.merchant ?? null, payload.amount, payload.transactionDate]
  );

  await logActivity("transaction", (result as { insertId: number }).insertId, "create", `Created transaction ${payload.title}`);
  response.status(201).json({ id: (result as { insertId: number }).insertId });
});

app.put("/api/transactions/:id", async (request, response) => {
  const payload = transactionSchema.parse(request.body);
  await pool.execute(
    `
      UPDATE transactions
      SET account_id = ?, category_id = ?, kind = ?, title = ?, notes = ?, merchant = ?, amount = ?, transaction_date = ?
      WHERE id = ? AND user_id = ?
    `,
    [payload.accountId, payload.categoryId, payload.kind, payload.title, payload.notes ?? null, payload.merchant ?? null, payload.amount, payload.transactionDate, Number(request.params.id), demoUserId]
  );

  await logActivity("transaction", Number(request.params.id), "update", `Updated transaction ${payload.title}`);
  response.json({ ok: true });
});

app.delete("/api/transactions/:id", async (request, response) => {
  const [rows] = await pool.query("SELECT title FROM transactions WHERE id = ? AND user_id = ? LIMIT 1", [Number(request.params.id), demoUserId]);
  await pool.execute("DELETE FROM transactions WHERE id = ? AND user_id = ?", [Number(request.params.id), demoUserId]);
  const title = Array.isArray(rows) && rows.length > 0 ? (rows[0] as any).title : `transaction ${request.params.id}`;
  await logActivity("transaction", Number(request.params.id), "delete", `Deleted ${title}`);
  response.status(204).send();
});

app.post("/api/transactions/bulk-delete", async (request, response) => {
  const payload = bulkDeleteTransactionsSchema.parse(request.body);

  await pool.query(
    `
      DELETE FROM transactions
      WHERE user_id = ?
        AND id IN (?)
    `,
    [demoUserId, payload.ids]
  );

  await logActivity("transaction", null, "bulk_delete", `Deleted ${payload.ids.length} transactions`, payload.note ?? null);
  response.json({ ok: true, deletedCount: payload.ids.length });
});

app.post("/api/transactions/bulk-recategorize", async (request, response) => {
  const payload = bulkRecategorizeTransactionsSchema.parse(request.body);

  await pool.query(
    `
      UPDATE transactions
      SET category_id = ?
      WHERE user_id = ?
        AND id IN (?)
    `,
    [payload.categoryId, demoUserId, payload.ids]
  );

  await logActivity("transaction", null, "bulk_recategorize", `Recategorized ${payload.ids.length} transactions`, payload.note ?? null);
  response.json({ ok: true, updatedCount: payload.ids.length });
});

app.get("/api/recurring-transactions", async (_request, response) => {
  const [rows] = await pool.query(
    `
      SELECT
        r.id,
        r.title,
        r.kind,
        r.amount,
        r.notes,
        r.merchant,
        r.account_id AS accountId,
        a.name AS accountLabel,
        r.category_id AS categoryId,
        c.name AS categoryName,
        DATE_FORMAT(r.start_date, '%Y-%m-%d') AS startDate,
        DATE_FORMAT(r.next_due_date, '%Y-%m-%d') AS nextDueDate,
        r.day_of_month AS dayOfMonth,
        r.frequency,
        r.auto_create AS autoCreate,
        r.is_active AS isActive,
        MAX(DATE_FORMAT(t.transaction_date, '%Y-%m-%d')) AS lastGeneratedDate
      FROM recurring_transactions r
      JOIN accounts a ON a.id = r.account_id
      JOIN categories c ON c.id = r.category_id
      LEFT JOIN transactions t ON t.recurring_transaction_id = r.id
      WHERE r.user_id = ?
      GROUP BY
        r.id,
        r.title,
        r.kind,
        r.amount,
        r.notes,
        r.merchant,
        r.account_id,
        a.name,
        r.category_id,
        c.name,
        r.start_date,
        r.next_due_date,
        r.day_of_month,
        r.frequency,
        r.auto_create,
        r.is_active
      ORDER BY r.is_active DESC, r.next_due_date ASC, r.id DESC
    `,
    [demoUserId]
  );

  response.json(
    Array.isArray(rows)
      ? rows.map((row: any) => ({
          ...row,
          status: getRecurringStatus(row.nextDueDate, Boolean(row.isActive))
        }))
      : []
  );
});

app.post("/api/recurring-transactions/sync", async (_request, response) => {
  const connection = await pool.getConnection();

  try {
    await connection.beginTransaction();
    const result = await syncRecurringTransactions(connection, demoUserId);
    await connection.commit();
    response.json(result);
  } catch (error) {
    await connection.rollback();
    throw error;
  } finally {
    connection.release();
  }
});

app.post("/api/recurring-transactions", async (request, response) => {
  const payload = recurringTransactionSchema.parse(request.body);
  const nextDueDate = getInitialNextDueDate(payload.startDate, payload.dayOfMonth);

  const [result] = await pool.execute(
    `
      INSERT INTO recurring_transactions (
        user_id,
        account_id,
        category_id,
        kind,
        title,
        notes,
        merchant,
        amount,
        day_of_month,
        start_date,
        next_due_date,
        auto_create,
        is_active
      )
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `,
    [
      demoUserId,
      payload.accountId,
      payload.categoryId,
      payload.kind,
      payload.title,
      payload.notes ?? null,
      payload.merchant ?? null,
      payload.amount,
      payload.dayOfMonth,
      payload.startDate,
      nextDueDate,
      payload.autoCreate,
      payload.isActive
    ]
  );

  await logActivity("recurring_transaction", (result as { insertId: number }).insertId, "create", `Created recurring transaction ${payload.title}`);
  response.status(201).json({ id: (result as { insertId: number }).insertId });
});

app.put("/api/recurring-transactions/:id", async (request, response) => {
  const payload = recurringTransactionSchema.parse(request.body);
  const nextDueDate = getInitialNextDueDate(payload.startDate, payload.dayOfMonth);

  await pool.execute(
    `
      UPDATE recurring_transactions
      SET
        account_id = ?,
        category_id = ?,
        kind = ?,
        title = ?,
        notes = ?,
        merchant = ?,
        amount = ?,
        day_of_month = ?,
        start_date = ?,
        next_due_date = ?,
        auto_create = ?,
        is_active = ?
      WHERE id = ? AND user_id = ?
    `,
    [
      payload.accountId,
      payload.categoryId,
      payload.kind,
      payload.title,
      payload.notes ?? null,
      payload.merchant ?? null,
      payload.amount,
      payload.dayOfMonth,
      payload.startDate,
      nextDueDate,
      payload.autoCreate,
      payload.isActive,
      Number(request.params.id),
      demoUserId
    ]
  );

  await logActivity("recurring_transaction", Number(request.params.id), "update", `Updated recurring transaction ${payload.title}`);
  response.json({ ok: true });
});

app.delete("/api/recurring-transactions/:id", async (request, response) => {
  const [rows] = await pool.query("SELECT title FROM recurring_transactions WHERE id = ? AND user_id = ? LIMIT 1", [Number(request.params.id), demoUserId]);
  await pool.execute("DELETE FROM recurring_transactions WHERE id = ? AND user_id = ?", [Number(request.params.id), demoUserId]);
  const title = Array.isArray(rows) && rows.length > 0 ? (rows[0] as any).title : `recurring transaction ${request.params.id}`;
  await logActivity("recurring_transaction", Number(request.params.id), "delete", `Deleted ${title}`);
  response.status(204).send();
});

app.get("/api/categories", async (request, response) => {
  const includeArchived = request.query.includeArchived === "1";
  const [rows] = await pool.query(`
    SELECT
      c.id,
      c.name,
      c.type,
      c.color,
      c.icon,
      c.is_default AS isDefault,
      c.is_archived AS isArchived,
      c.budget_mode AS budgetMode,
      EXISTS (SELECT 1 FROM transactions t WHERE t.category_id = c.id LIMIT 1) AS hasTransactions,
      EXISTS (SELECT 1 FROM budgets b WHERE b.category_id = c.id LIMIT 1) AS hasBudgets,
      EXISTS (SELECT 1 FROM recurring_transactions r WHERE r.category_id = c.id LIMIT 1) AS hasRecurring
    FROM categories c
    WHERE (c.user_id IS NULL OR c.user_id = ?)
      AND (? = TRUE OR c.is_archived = FALSE)
    ORDER BY c.type, c.name
  `, [demoUserId, includeArchived]);

  response.json(rows);
});

app.post("/api/categories", async (request, response) => {
  const payload = categorySchema.parse(request.body);
  await ensureCategoryUnique(payload.name, payload.type);
  const [result] = await pool.execute(
    `
      INSERT INTO categories (user_id, name, type, color, icon, is_default, budget_mode)
      VALUES (?, ?, ?, ?, ?, FALSE, ?)
    `,
    [demoUserId, payload.name, payload.type, payload.color, payload.icon, payload.budgetMode]
  );

  await logActivity("category", (result as { insertId: number }).insertId, "create", `Created category ${payload.name}`, payload.changeNote ?? null);
  response.status(201).json({ id: (result as { insertId: number }).insertId });
});

app.put("/api/categories/:id", async (request, response) => {
  const payload = categorySchema.parse(request.body);
  await ensureCategoryUnique(payload.name, payload.type, Number(request.params.id));
  await pool.execute(
    `
      UPDATE categories
      SET name = ?, type = ?, color = ?, icon = ?, budget_mode = ?
      WHERE id = ? AND user_id = ?
    `,
    [payload.name, payload.type, payload.color, payload.icon, payload.budgetMode, Number(request.params.id), demoUserId]
  );

  await logActivity("category", Number(request.params.id), "update", `Updated category ${payload.name}`, payload.changeNote ?? null);
  response.json({ ok: true });
});

app.delete("/api/categories/:id", async (request, response) => {
  const payload = deleteCategorySchema.parse(request.body ?? {});
  const categoryId = Number(request.params.id);

  const [rows] = await pool.query(
    `
      SELECT name, type
      FROM categories
      WHERE id = ? AND user_id = ?
      LIMIT 1
    `,
    [categoryId, demoUserId]
  );

  const category = Array.isArray(rows) && rows.length > 0 ? rows[0] as any : null;
  if (!category) {
    response.status(404).json({ message: "Category not found." });
    return;
  }

  const [usageRows] = await pool.query(
    `
      SELECT
        (SELECT COUNT(*) FROM transactions WHERE category_id = ?) AS transactionCount,
        (SELECT COUNT(*) FROM budgets WHERE category_id = ?) AS budgetCount,
        (SELECT COUNT(*) FROM recurring_transactions WHERE category_id = ?) AS recurringCount
    `,
    [categoryId, categoryId, categoryId]
  );

  const usage = Array.isArray(usageRows) && usageRows.length > 0 ? usageRows[0] as any : { transactionCount: 0, budgetCount: 0, recurringCount: 0 };
  const hasUsage = Number(usage.transactionCount) > 0 || Number(usage.budgetCount) > 0 || Number(usage.recurringCount) > 0;

  if (hasUsage && !payload.reassignmentCategoryId) {
    response.status(400).json({ message: "This category is already in use. Reassign it before deleting." });
    return;
  }

  if (payload.reassignmentCategoryId) {
    const [targetRows] = await pool.query(
      `
        SELECT id
        FROM categories
        WHERE id = ?
          AND type = ?
          AND is_archived = FALSE
          AND (user_id IS NULL OR user_id = ?)
        LIMIT 1
      `,
      [payload.reassignmentCategoryId, category.type, demoUserId]
    );

    if (!Array.isArray(targetRows) || targetRows.length === 0) {
      response.status(400).json({ message: "Select a valid replacement category with the same type." });
      return;
    }

    await pool.execute("UPDATE transactions SET category_id = ? WHERE category_id = ?", [payload.reassignmentCategoryId, categoryId]);
    await pool.execute("UPDATE budgets SET category_id = ? WHERE category_id = ?", [payload.reassignmentCategoryId, categoryId]);
    await pool.execute("UPDATE recurring_transactions SET category_id = ? WHERE category_id = ?", [payload.reassignmentCategoryId, categoryId]);
  }

  await pool.execute("DELETE FROM categories WHERE id = ? AND user_id = ?", [categoryId, demoUserId]);
  await logActivity("category", categoryId, "delete", `Deleted category ${category.name}`, payload.changeNote ?? null);
  response.status(204).send();
});

app.put("/api/categories/:id/archive", async (request, response) => {
  const payload = archiveCategorySchema.parse(request.body);
  const categoryId = Number(request.params.id);

  await pool.execute(
    `
      UPDATE categories
      SET is_archived = ?
      WHERE id = ? AND user_id = ?
    `,
    [payload.isArchived, categoryId, demoUserId]
  );

  await logActivity("category", categoryId, payload.isArchived ? "archive" : "restore", `${payload.isArchived ? "Archived" : "Restored"} category ${categoryId}`, payload.changeNote ?? null);
  response.json({ ok: true });
});

app.get("/api/activity", async (_request, response) => {
  const [rows] = await pool.query(
    `
      SELECT
        id,
        entity_type AS entityType,
        entity_id AS entityId,
        action,
        title,
        note,
        DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS createdAt
      FROM activity_logs
      WHERE user_id = ?
      ORDER BY created_at DESC, id DESC
      LIMIT 20
    `,
    [demoUserId]
  );

  response.json(rows);
});

app.get("/api/accounts", async (_request, response) => {
  const [rows] = await pool.query(
    `
      SELECT id, name, type
      FROM accounts
      WHERE user_id = ?
      ORDER BY
        CASE
          WHEN name = 'Bank' THEN 1
          WHEN name = 'Cash' THEN 2
          WHEN name = 'Credit Card' THEN 3
          WHEN name = 'UPI' THEN 4
          WHEN name = 'UPI-Lite' THEN 5
          WHEN name = 'NEFT' THEN 6
          ELSE 99
        END,
        id
    `,
    [demoUserId]
  );

  const normalizedRows = Array.isArray(rows)
    ? rows.map((row: any) => ({
        id: row.id,
        type: row.type,
        label: row.name
      }))
    : [];

  response.json(normalizedRows);
});

app.get("/api/monthly-budget", async (request, response) => {
  const selected = getSelectedMonth(request.query.month as string | undefined);
  const [rows] = await pool.query(
    `
      SELECT total_budget AS totalBudget
      FROM monthly_budget_targets
      WHERE user_id = ? AND month = ? AND year = ?
      LIMIT 1
    `,
    [demoUserId, selected.month, selected.year]
  );

  const row = Array.isArray(rows) && rows.length > 0 ? (rows[0] as Record<string, string | number>) : null;
  response.json({ month: selected.monthKey, totalBudget: Number(row?.totalBudget ?? 0) });
});

app.put("/api/monthly-budget", async (request, response) => {
  const payload = monthlyBudgetSchema.parse(request.body);
  await pool.execute(
    `
      INSERT INTO monthly_budget_targets (user_id, month, year, total_budget)
      VALUES (?, ?, ?, ?)
      ON DUPLICATE KEY UPDATE total_budget = VALUES(total_budget)
    `,
    [demoUserId, payload.month, payload.year, payload.totalBudget]
  );

  response.json({ ok: true });
});

app.get("/api/budgets", async (request, response) => {
  const selected = getSelectedMonth(request.query.month as string | undefined);
  const [rows] = await pool.query(
    `
      SELECT
        b.id,
        b.month,
        b.year,
        b.category_id AS categoryId,
        c.name AS categoryName,
        c.color,
        c.budget_mode AS budgetMode,
        b.allocated_amount AS allocatedAmount,
        COALESCE(SUM(t.amount), 0) AS spentAmount
      FROM budgets b
      JOIN categories c ON c.id = b.category_id
      LEFT JOIN transactions t
        ON t.category_id = b.category_id
        AND t.kind = 'expense'
        AND MONTH(t.transaction_date) = b.month
        AND YEAR(t.transaction_date) = b.year
        AND t.user_id = b.user_id
      WHERE b.user_id = ?
        AND b.month = ?
        AND b.year = ?
      GROUP BY b.id, b.month, b.year, b.category_id, c.name, c.color, c.budget_mode, b.allocated_amount
      ORDER BY c.name
    `,
    [demoUserId, selected.month, selected.year]
  );

  response.json(rows);
});

app.post("/api/budgets", async (request, response) => {
  const payload = budgetSchema.parse(request.body);
  const [result] = await pool.execute(
    `
      INSERT INTO budgets (user_id, category_id, month, year, allocated_amount)
      VALUES (?, ?, ?, ?, ?)
      ON DUPLICATE KEY UPDATE allocated_amount = VALUES(allocated_amount)
    `,
    [demoUserId, payload.categoryId, payload.month, payload.year, payload.allocatedAmount]
  );

  response.status(201).json({ id: (result as { insertId: number }).insertId });
});

app.put("/api/budgets/:id", async (request, response) => {
  const payload = budgetSchema.parse(request.body);
  await pool.execute(
    `
      UPDATE budgets
      SET category_id = ?, month = ?, year = ?, allocated_amount = ?
      WHERE id = ? AND user_id = ?
    `,
    [payload.categoryId, payload.month, payload.year, payload.allocatedAmount, Number(request.params.id), demoUserId]
  );

  response.json({ ok: true });
});

app.delete("/api/budgets/:id", async (request, response) => {
  await pool.execute("DELETE FROM budgets WHERE id = ? AND user_id = ?", [Number(request.params.id), demoUserId]);
  response.status(204).send();
});

app.get("/api/reports/overview", async (request, response) => {
  const selected = getSelectedMonth(request.query.month as string | undefined);
  const selectedYear = selected.year;

  const [summaryRows] = await pool.query(
    `
      SELECT
        COALESCE(SUM(CASE
          WHEN YEAR(transaction_date) = ? AND MONTH(transaction_date) = ? AND kind = 'income' THEN amount
          ELSE 0
        END), 0) AS monthlyIncome,
        COALESCE(SUM(CASE
          WHEN YEAR(transaction_date) = ? AND MONTH(transaction_date) = ? AND kind = 'expense' THEN amount
          ELSE 0
        END), 0) AS monthlyExpense,
        COALESCE(SUM(CASE WHEN YEAR(transaction_date) = ? AND kind = 'income' THEN amount ELSE 0 END), 0) AS yearlyIncome,
        COALESCE(SUM(CASE WHEN YEAR(transaction_date) = ? AND kind = 'expense' THEN amount ELSE 0 END), 0) AS yearlyExpense
      FROM transactions
      WHERE user_id = ?
        AND YEAR(transaction_date) BETWEEN ? AND ?
    `,
    [
      selected.year,
      selected.month,
      selected.year,
      selected.month,
      selected.year,
      selected.year,
      demoUserId,
      selected.year - 3,
      selected.year
    ]
  );

  const [categoryRows] = await pool.query(
    `
      SELECT
        c.id AS categoryId,
        c.name AS category,
        c.color,
        COALESCE(b.allocated_amount, 0) AS allocatedAmount,
        COALESCE(tx.spentAmount, 0) AS spentAmount
      FROM categories c
      LEFT JOIN budgets b
        ON b.category_id = c.id
        AND b.user_id = ?
        AND b.month = ?
        AND b.year = ?
      LEFT JOIN (
        SELECT
          category_id,
          SUM(amount) AS spentAmount
        FROM transactions
        WHERE user_id = ?
          AND kind = 'expense'
          AND MONTH(transaction_date) = ?
          AND YEAR(transaction_date) = ?
        GROUP BY category_id
      ) tx ON tx.category_id = c.id
      WHERE (c.user_id IS NULL OR c.user_id = ?)
        AND c.type = 'expense'
        AND (b.id IS NOT NULL OR tx.spentAmount IS NOT NULL)
      ORDER BY spentAmount DESC, allocatedAmount DESC, c.name
    `,
    [demoUserId, selected.month, selected.year, demoUserId, selected.month, selected.year, demoUserId]
  );

  const [monthlyRows] = await pool.query(
    `
      SELECT
        DATE_FORMAT(transaction_date, '%Y-%m') AS period,
        DATE_FORMAT(transaction_date, '%b %Y') AS monthLabel,
        SUM(CASE WHEN kind = 'income' THEN amount ELSE 0 END) AS income,
        SUM(CASE WHEN kind = 'expense' THEN amount ELSE 0 END) AS expense
      FROM transactions
      WHERE user_id = ?
        AND transaction_date >= DATE_SUB(STR_TO_DATE(CONCAT(?, '-01'), '%Y-%m-%d'), INTERVAL 5 MONTH)
        AND transaction_date < DATE_ADD(STR_TO_DATE(CONCAT(?, '-01'), '%Y-%m-%d'), INTERVAL 1 MONTH)
      GROUP BY DATE_FORMAT(transaction_date, '%Y-%m'), DATE_FORMAT(transaction_date, '%b %Y'), YEAR(transaction_date), MONTH(transaction_date)
      ORDER BY period ASC
    `,
    [demoUserId, selected.monthKey, selected.monthKey]
  );

  const [monthlyBreakdownRows] = await pool.query(
    `
      SELECT
        DATE_FORMAT(transaction_date, '%Y-%m') AS period,
        DATE_FORMAT(transaction_date, '%b %Y') AS monthLabel,
        SUM(CASE WHEN kind = 'income' THEN amount ELSE 0 END) AS income,
        SUM(CASE WHEN kind = 'expense' THEN amount ELSE 0 END) AS expense
      FROM transactions
      WHERE user_id = ?
        AND YEAR(transaction_date) = ?
      GROUP BY DATE_FORMAT(transaction_date, '%Y-%m'), DATE_FORMAT(transaction_date, '%b %Y'), YEAR(transaction_date), MONTH(transaction_date)
      ORDER BY period ASC
    `,
    [demoUserId, selectedYear]
  );

  const [yearlyRows] = await pool.query(
    `
      SELECT
        YEAR(transaction_date) AS year,
        SUM(CASE WHEN kind = 'income' THEN amount ELSE 0 END) AS income,
        SUM(CASE WHEN kind = 'expense' THEN amount ELSE 0 END) AS expense
      FROM transactions
      WHERE user_id = ?
        AND YEAR(transaction_date) BETWEEN ? AND ?
      GROUP BY YEAR(transaction_date)
      ORDER BY YEAR(transaction_date) ASC
    `,
    [demoUserId, selectedYear - 3, selectedYear]
  );

  const [yearlyCategoryRows] = await pool.query(
    `
      SELECT
        c.id AS categoryId,
        c.name AS category,
        c.color,
        SUM(t.amount) AS total
      FROM transactions t
      JOIN categories c ON c.id = t.category_id
      WHERE t.user_id = ?
        AND t.kind = 'expense'
        AND YEAR(t.transaction_date) = ?
      GROUP BY c.id, c.name, c.color
      ORDER BY total DESC
    `,
    [demoUserId, selectedYear]
  );

  const [budgetRows] = await pool.query(
    `
      SELECT
        b.id,
        c.name AS category,
        c.color,
        b.allocated_amount AS allocatedAmount,
        COALESCE(SUM(t.amount), 0) AS spentAmount
      FROM budgets b
      JOIN categories c ON c.id = b.category_id
      LEFT JOIN transactions t
        ON t.category_id = b.category_id
        AND t.kind = 'expense'
        AND MONTH(t.transaction_date) = b.month
        AND YEAR(t.transaction_date) = b.year
        AND t.user_id = b.user_id
      WHERE b.user_id = ?
        AND b.month = ?
        AND b.year = ?
      GROUP BY b.id, c.name, c.color, b.allocated_amount
      ORDER BY spentAmount DESC, c.name
    `,
    [demoUserId, selected.month, selected.year]
  );

  const monthlyLookup = new Map(
    (Array.isArray(monthlyRows) ? monthlyRows : []).map((row: any) => [
      row.period,
      {
        income: Number(row.income ?? 0),
        expense: Number(row.expense ?? 0)
      }
    ])
  );

  const monthlyComparison = buildTrailingMonths(selected.monthKey, 12).map(({ period, monthLabel }) => {
    const row = monthlyLookup.get(period);
    const income = row?.income ?? 0;
    const expense = row?.expense ?? 0;
    return {
      period,
      monthLabel,
      income,
      expense,
      net: income - expense
    };
  });

  const monthlyBreakdownLookup = new Map(
    (Array.isArray(monthlyBreakdownRows) ? monthlyBreakdownRows : []).map((row: any) => [
      row.period,
      {
        income: Number(row.income ?? 0),
        expense: Number(row.expense ?? 0)
      }
    ])
  );

  const monthlyBreakdown = buildYearMonths(selectedYear).map(({ period, monthLabel }) => {
    const row = monthlyBreakdownLookup.get(period);
    const income = row?.income ?? 0;
    const expense = row?.expense ?? 0;
    return {
      period,
      monthLabel,
      income,
      expense,
      net: income - expense
    };
  });

  const yearlyLookup = new Map(
    (Array.isArray(yearlyRows) ? yearlyRows : []).map((row: any) => [
      Number(row.year),
      {
        income: Number(row.income ?? 0),
        expense: Number(row.expense ?? 0)
      }
    ])
  );

  const yearlyComparison = buildTrailingYears(selectedYear, 4).map(({ year, yearLabel }) => {
    const row = yearlyLookup.get(year);
    const income = row?.income ?? 0;
    const expense = row?.expense ?? 0;
    return {
      year,
      yearLabel,
      income,
      expense,
      net: income - expense
    };
  });

  const summary = Array.isArray(summaryRows) && summaryRows.length > 0 ? (summaryRows[0] as Record<string, string | number>) : {};
  const monthlyIncome = Number(summary.monthlyIncome ?? 0);
  const monthlyExpense = Number(summary.monthlyExpense ?? 0);
  const yearlyIncome = Number(summary.yearlyIncome ?? 0);
  const yearlyExpense = Number(summary.yearlyExpense ?? 0);

  response.json({
    selectedMonth: selected.monthKey,
    selectedYear,
    summary: {
      monthlyIncome,
      monthlyExpense,
      monthlyNet: monthlyIncome - monthlyExpense,
      yearlyIncome,
      yearlyExpense,
      yearlyNet: yearlyIncome - yearlyExpense,
      averageMonthlyExpense:
        monthlyBreakdown.reduce((sum, item) => sum + Number(item.expense), 0) / Math.max(monthlyBreakdown.length, 1),
      topExpenseCategory: Array.isArray(categoryRows) && categoryRows.length > 0 ? (categoryRows[0] as any).category : null,
      topYearCategory: Array.isArray(yearlyCategoryRows) && yearlyCategoryRows.length > 0 ? (yearlyCategoryRows[0] as any).category : null
    },
    categoryTotals: categoryRows,
    monthlyComparison,
    monthlyBreakdown,
    yearlyComparison,
    yearlyCategoryTotals: yearlyCategoryRows,
    budgetVsActual: budgetRows
  });
});

app.use((error: unknown, _request: express.Request, response: express.Response, _next: express.NextFunction) => {
  if (error instanceof ZodError) {
    response.status(400).json({
      message: error.issues[0]?.message ?? "Validation failed.",
      issues: error.issues
    });
    return;
  }

  console.error(error);
  response.status(500).json({ message: "Something went wrong on the server." });
});

(async () => {
  app.listen(env.PORT, "127.0.0.1", () => {
    console.log(`API running on http://127.0.0.1:${env.PORT}`);
  });
})().catch((error) => {
  console.error("Failed to start API", error);
  process.exit(1);
});
