import cors from "cors";
import express from "express";
import { ZodError, z } from "zod";
import { env } from "./config.js";
import { pool } from "./db.js";
import { parseTransactionPagination } from "./transactions.js";

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
  icon: z.string().trim().min(1, "Icon label is required.").max(40, "Icon label is too long.")
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

  const summary = Array.isArray(summaryRows) ? (summaryRows[0] as Record<string, string | number>) : {};
  const monthlyIncome = Number(summary.monthlyIncome ?? 0);
  const monthlyExpense = Number(summary.monthlyExpense ?? 0);
  const budgetAllocated = Number(summary.budgetAllocated ?? 0);
  const totalBudget = Number(summary.totalBudget ?? 0);
  const budgetSpent = Array.isArray(budgetRows)
    ? budgetRows.reduce((sum, row) => sum + Number((row as Record<string, string | number>).spentAmount ?? 0), 0)
    : 0;

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
      savingsRate: monthlyIncome > 0 ? ((monthlyIncome - monthlyExpense) / monthlyIncome) * 100 : 0
    },
    recentTransactions: recentRows,
    budgetProgress: budgetRows,
    monthlyTrend: trendRows
  });
});

app.get("/api/transactions", async (request, response) => {
  const selected = getSelectedMonth(request.query.month as string | undefined);
  const pagination = parseTransactionPagination({
    page: request.query.page,
    perPage: request.query.perPage
  });

  const [countRows] = await pool.query(
    `
      SELECT COUNT(*) AS total
      FROM transactions t
      WHERE t.user_id = ?
        AND MONTH(t.transaction_date) = ?
        AND YEAR(t.transaction_date) = ?
    `,
    [demoUserId, selected.month, selected.year]
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
        CASE
          WHEN a.type = 'bank' THEN 'Bank'
          WHEN a.type = 'cash' THEN 'Cash'
          WHEN a.type = 'credit' THEN 'Credit Card'
          WHEN a.type = 'wallet' THEN 'Wallet'
          ELSE a.name
        END AS accountLabel,
        t.category_id AS categoryId,
        c.name AS categoryName,
        c.color AS categoryColor
      FROM transactions t
      JOIN accounts a ON a.id = t.account_id
      JOIN categories c ON c.id = t.category_id
      WHERE t.user_id = ?
        AND MONTH(t.transaction_date) = ?
        AND YEAR(t.transaction_date) = ?
      ORDER BY t.transaction_date DESC, t.id DESC
      LIMIT ?
      OFFSET ?
    `,
    [demoUserId, selected.month, selected.year, pagination.perPage, pagination.offset]
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

app.post("/api/transactions", async (request, response) => {
  const payload = transactionSchema.parse(request.body);
  const [result] = await pool.execute(
    `
      INSERT INTO transactions (user_id, account_id, category_id, kind, title, notes, merchant, amount, transaction_date)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    `,
    [demoUserId, payload.accountId, payload.categoryId, payload.kind, payload.title, payload.notes ?? null, payload.merchant ?? null, payload.amount, payload.transactionDate]
  );

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

  response.json({ ok: true });
});

app.delete("/api/transactions/:id", async (request, response) => {
  await pool.execute("DELETE FROM transactions WHERE id = ? AND user_id = ?", [Number(request.params.id), demoUserId]);
  response.status(204).send();
});

app.get("/api/categories", async (_request, response) => {
  const [rows] = await pool.query(`
    SELECT
      id,
      name,
      type,
      color,
      icon,
      is_default AS isDefault
    FROM categories
    WHERE user_id IS NULL OR user_id = ?
    ORDER BY type, name
  `, [demoUserId]);

  response.json(rows);
});

app.post("/api/categories", async (request, response) => {
  const payload = categorySchema.parse(request.body);
  const [result] = await pool.execute(
    `
      INSERT INTO categories (user_id, name, type, color, icon, is_default)
      VALUES (?, ?, ?, ?, ?, FALSE)
    `,
    [demoUserId, payload.name, payload.type, payload.color, payload.icon]
  );

  response.status(201).json({ id: (result as { insertId: number }).insertId });
});

app.put("/api/categories/:id", async (request, response) => {
  const payload = categorySchema.parse(request.body);
  await pool.execute(
    `
      UPDATE categories
      SET name = ?, type = ?, color = ?, icon = ?
      WHERE id = ? AND user_id = ?
    `,
    [payload.name, payload.type, payload.color, payload.icon, Number(request.params.id), demoUserId]
  );

  response.json({ ok: true });
});

app.delete("/api/categories/:id", async (request, response) => {
  await pool.execute("DELETE FROM categories WHERE id = ? AND user_id = ?", [Number(request.params.id), demoUserId]);
  response.status(204).send();
});

app.get("/api/accounts", async (_request, response) => {
  const [rows] = await pool.query(
    `
      SELECT id, type
      FROM accounts
      WHERE user_id = ?
      ORDER BY FIELD(type, 'cash', 'bank', 'credit', 'wallet'), id
    `,
    [demoUserId]
  );

  const normalizedRows = Array.isArray(rows)
    ? rows.map((row: any) => ({
        id: row.id,
        type: row.type,
        label:
          row.type === "cash"
            ? "Cash"
            : row.type === "bank"
              ? "Bank"
              : row.type === "credit"
                ? "Credit Card"
                : "Wallet"
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
      GROUP BY b.id, b.month, b.year, b.category_id, c.name, c.color, b.allocated_amount
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
        c.name AS category,
        c.color,
        SUM(t.amount) AS total
      FROM transactions t
      JOIN categories c ON c.id = t.category_id
      WHERE t.user_id = ?
        AND t.kind = 'expense'
        AND MONTH(t.transaction_date) = ?
        AND YEAR(t.transaction_date) = ?
      GROUP BY c.name, c.color
      ORDER BY total DESC
    `,
    [demoUserId, selected.month, selected.year]
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
        c.name AS category,
        c.color,
        SUM(t.amount) AS total
      FROM transactions t
      JOIN categories c ON c.id = t.category_id
      WHERE t.user_id = ?
        AND t.kind = 'expense'
        AND YEAR(t.transaction_date) = ?
      GROUP BY c.name, c.color
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
