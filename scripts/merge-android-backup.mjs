import { readFile, writeFile } from "node:fs/promises";
import path from "node:path";

const [, , backupPath, outputPath, ...csvPaths] = process.argv;

if (!backupPath || !outputPath || csvPaths.length === 0) {
  console.error(
    "Usage: node scripts/merge-android-backup.mjs <backup.json> <output.json> <transactions-1.csv> [transactions-2.csv ...]"
  );
  process.exit(1);
}

const backup = JSON.parse(await readFile(backupPath, "utf8"));
const accountIdByName = new Map((backup.e ?? []).map((account) => [normalizeName(account.c), account.a]));
const categoryIdByName = new Map((backup.f ?? []).map((category) => [normalizeName(category.c), category.a]));

const existingTransactions = backup.g ?? [];
const exportedAt = Date.now();
const createdAt = exportedAt;
const nextTransactionId = existingTransactions.reduce((maxId, transaction) => Math.max(maxId, transaction.a ?? 0), 0) + 1;

const importedRows = [];
for (const csvPath of csvPaths) {
  const csvText = await readFile(csvPath, "utf8");
  importedRows.push(...parseCsv(csvText).map((row) => ({ row, csvPath })));
}

const mergedTransactions = [
  ...existingTransactions,
  ...importedRows.map(({ row, csvPath }, index) => {
    const accountName = normalizeName(row.Account);
    const categoryName = normalizeName(row.Category);
    const accountId = accountIdByName.get(accountName);
    const categoryId = categoryIdByName.get(categoryName);

    if (!accountId) {
      throw new Error(`Unknown account '${row.Account}' in ${path.basename(csvPath)}.`);
    }
    if (!categoryId) {
      throw new Error(`Unknown category '${row.Category}' in ${path.basename(csvPath)}.`);
    }

    return {
      a: nextTransactionId + index,
      b: 1,
      c: accountId,
      d: categoryId,
      e: null,
      f: normalizeKind(row.Kind),
      g: row.Title.trim(),
      h: normalizeOptional(row.Notes),
      i: normalizeOptional(row.Merchant),
      j: parseAmountToCents(row.Amount),
      k: stripQuotes(row.Date),
      l: createdAt
    };
  })
];

backup.b = exportedAt;
if (backup.c && typeof backup.c === "object") {
  backup.c.b = "2026-06";
}
backup.g = mergedTransactions;

validateBackupShape(backup);

await writeFile(outputPath, `${JSON.stringify(backup, null, 2)}\n`, "utf8");

console.log(
  JSON.stringify(
    {
      backupPath,
      outputPath,
      existingTransactionCount: existingTransactions.length,
      importedTransactionCount: importedRows.length,
      mergedTransactionCount: mergedTransactions.length,
      selectedMonth: backup.c?.b ?? null
    },
    null,
    2
  )
);

function parseCsv(text) {
  const lines = text.trim().split(/\r?\n/);
  if (lines.length < 2) {
    return [];
  }

  const headers = parseCsvLine(lines[0]);
  return lines.slice(1).filter(Boolean).map((line) => {
    const values = parseCsvLine(line);
    return headers.reduce((row, header, index) => {
      row[header] = values[index] ?? "";
      return row;
    }, {});
  });
}

function parseCsvLine(line) {
  const values = [];
  let current = "";
  let inQuotes = false;

  for (let index = 0; index < line.length; index += 1) {
    const character = line[index];
    if (character === "\"") {
      if (inQuotes && line[index + 1] === "\"") {
        current += "\"";
        index += 1;
      } else {
        inQuotes = !inQuotes;
      }
      continue;
    }
    if (character === "," && !inQuotes) {
      values.push(current);
      current = "";
      continue;
    }
    current += character;
  }

  values.push(current);
  return values.map((value) => stripQuotes(value).trim());
}

function stripQuotes(value) {
  return String(value).replace(/^"(.*)"$/s, "$1");
}

function normalizeName(value) {
  return stripQuotes(value).trim().toLowerCase();
}

function normalizeOptional(value) {
  const normalized = stripQuotes(value).trim();
  return normalized ? normalized : null;
}

function normalizeKind(value) {
  const kind = stripQuotes(value).trim().toUpperCase();
  if (kind !== "EXPENSE" && kind !== "INCOME") {
    throw new Error(`Unsupported transaction kind '${value}'.`);
  }
  return kind;
}

function parseAmountToCents(value) {
  const normalized = stripQuotes(value).replace(/[^0-9,.-]/g, "").replace(/,/g, "");
  const parsed = Number.parseFloat(normalized);
  if (!Number.isFinite(parsed)) {
    throw new Error(`Invalid amount '${value}'.`);
  }
  return Math.round(parsed * 100);
}

function validateBackupShape(document) {
  if (document.a !== 1) {
    throw new Error(`Unsupported backup format version '${document.a}'.`);
  }
  if (!Array.isArray(document.d) || document.d.length !== 1 || document.d[0]?.a !== 1) {
    throw new Error("Backup must contain exactly one user with id 1.");
  }
  if (!document.c?.b || !/^\d{4}-\d{2}$/.test(document.c.b)) {
    throw new Error(`Selected month is invalid: ${document.c?.b}`);
  }

  const accountIds = new Set((document.e ?? []).map((account) => account.a));
  const categoryIds = new Set((document.f ?? []).map((category) => category.a));
  const recurringIds = new Set((document.h ?? []).map((recurring) => recurring.a));
  const transactionIds = new Set();

  for (const transaction of document.g ?? []) {
    if (transactionIds.has(transaction.a)) {
      throw new Error(`Duplicate transaction id '${transaction.a}'.`);
    }
    transactionIds.add(transaction.a);

    if (transaction.b !== 1) {
      throw new Error(`Transaction '${transaction.g}' has invalid user id '${transaction.b}'.`);
    }
    if (!accountIds.has(transaction.c)) {
      throw new Error(`Transaction '${transaction.g}' references missing account id '${transaction.c}'.`);
    }
    if (!categoryIds.has(transaction.d)) {
      throw new Error(`Transaction '${transaction.g}' references missing category id '${transaction.d}'.`);
    }
    if (transaction.e != null && !recurringIds.has(transaction.e)) {
      throw new Error(`Transaction '${transaction.g}' references missing recurring id '${transaction.e}'.`);
    }
    if (!/^\d{4}-\d{2}-\d{2}$/.test(transaction.k)) {
      throw new Error(`Transaction '${transaction.g}' has invalid date '${transaction.k}'.`);
    }
    if (transaction.f !== "EXPENSE" && transaction.f !== "INCOME") {
      throw new Error(`Transaction '${transaction.g}' has invalid kind '${transaction.f}'.`);
    }
  }
}
