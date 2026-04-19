import type { Pool, PoolConnection } from "mysql2/promise";

type SqlRunner = Pool | PoolConnection;

function parseDate(value: string) {
  return new Date(`${value}T00:00:00`);
}

function formatDate(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function getDaysInMonth(year: number, monthIndex: number) {
  return new Date(year, monthIndex + 1, 0).getDate();
}

function getMonthlyDate(year: number, monthIndex: number, dayOfMonth: number) {
  const day = Math.min(dayOfMonth, getDaysInMonth(year, monthIndex));
  return new Date(year, monthIndex, day);
}

export function getInitialNextDueDate(startDate: string, dayOfMonth: number) {
  const start = parseDate(startDate);
  const currentMonthDate = getMonthlyDate(start.getFullYear(), start.getMonth(), dayOfMonth);
  if (currentMonthDate >= start) {
    return formatDate(currentMonthDate);
  }

  return formatDate(getMonthlyDate(start.getFullYear(), start.getMonth() + 1, dayOfMonth));
}

export function getNextMonthlyDueDate(dateValue: string, dayOfMonth: number) {
  const current = parseDate(dateValue);
  return formatDate(getMonthlyDate(current.getFullYear(), current.getMonth() + 1, dayOfMonth));
}

export function getRecurringStatus(nextDueDate: string, isActive: boolean, today = formatDate(new Date())) {
  if (!isActive) {
    return "inactive";
  }

  return nextDueDate <= today ? "due" : "upcoming";
}

export async function syncRecurringTransactions(runner: SqlRunner, userId: number, today = formatDate(new Date())) {
  const [rows] = await runner.query(
    `
      SELECT
        id,
        account_id AS accountId,
        category_id AS categoryId,
        kind,
        title,
        notes,
        merchant,
        amount,
        day_of_month AS dayOfMonth,
        DATE_FORMAT(next_due_date, '%Y-%m-%d') AS nextDueDate
      FROM recurring_transactions
      WHERE user_id = ?
        AND is_active = TRUE
        AND auto_create = TRUE
        AND next_due_date <= ?
      ORDER BY next_due_date ASC, id ASC
    `,
    [userId, today]
  );

  let createdCount = 0;
  let updatedSchedules = 0;

  for (const row of Array.isArray(rows) ? rows as any[] : []) {
    let nextDueDate = row.nextDueDate as string;

    while (nextDueDate <= today) {
      const [existingRows] = await runner.query(
        `
          SELECT id
          FROM transactions
          WHERE user_id = ?
            AND recurring_transaction_id = ?
            AND transaction_date = ?
          LIMIT 1
        `,
        [userId, row.id, nextDueDate]
      );

      if (!Array.isArray(existingRows) || existingRows.length === 0) {
        await runner.execute(
          `
            INSERT INTO transactions (
              user_id,
              account_id,
              category_id,
              recurring_transaction_id,
              kind,
              title,
              notes,
              merchant,
              amount,
              transaction_date
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
          `,
          [userId, row.accountId, row.categoryId, row.id, row.kind, row.title, row.notes ?? null, row.merchant ?? null, row.amount, nextDueDate]
        );
        createdCount += 1;
      }

      nextDueDate = getNextMonthlyDueDate(nextDueDate, Number(row.dayOfMonth));
    }

    await runner.execute(
      `
        UPDATE recurring_transactions
        SET next_due_date = ?
        WHERE id = ? AND user_id = ?
      `,
      [nextDueDate, row.id, userId]
    );
    updatedSchedules += 1;
  }

  return {
    createdCount,
    updatedSchedules
  };
}
