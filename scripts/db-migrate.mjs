import dotenv from "dotenv";
import { readdir, readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import mysql from "mysql2/promise";

dotenv.config();

const requiredEnv = [
  "MYSQL_HOST",
  "MYSQL_PORT",
  "MYSQL_USER",
  "MYSQL_PASSWORD",
  "MYSQL_DATABASE"
];

for (const key of requiredEnv) {
  if (!process.env[key]) {
    throw new Error(`${key} is required for migrations.`);
  }
}

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const rootDir = path.resolve(__dirname, "..");
const migrationsDir = path.join(rootDir, "database", "migrations");

let connection;

try {
  connection = await mysql.createConnection({
    host: process.env.MYSQL_HOST,
    port: Number(process.env.MYSQL_PORT),
    user: process.env.MYSQL_USER,
    password: process.env.MYSQL_PASSWORD,
    database: process.env.MYSQL_DATABASE,
    multipleStatements: true
  });
} catch (error) {
  if (error && typeof error === "object" && "code" in error && error.code === "ECONNREFUSED") {
    throw new Error("MySQL is not reachable on the configured host and port. Start the database service first.");
  }

  throw error;
}

await connection.query(`
  CREATE TABLE IF NOT EXISTS schema_migrations (
    id INT PRIMARY KEY AUTO_INCREMENT,
    migration_name VARCHAR(255) NOT NULL UNIQUE,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  )
`);

const [appliedRows] = await connection.query("SELECT migration_name FROM schema_migrations");
const applied = new Set(appliedRows.map((row) => row.migration_name));

const migrationFiles = (await readdir(migrationsDir))
  .filter((file) => file.endsWith(".sql"))
  .sort();

for (const file of migrationFiles) {
  if (applied.has(file)) {
    continue;
  }

  const sql = await readFile(path.join(migrationsDir, file), "utf8");
  const statements = sql
    .split(/;\s*\n/)
    .map((statement) => statement.trim())
    .filter(Boolean);

  await connection.beginTransaction();
  try {
    for (const statement of statements) {
      try {
        await connection.query(statement);
      } catch (error) {
        if (
          error &&
          typeof error === "object" &&
          "code" in error &&
          (error.code === "ER_DUP_KEYNAME" || error.code === "ER_TABLE_EXISTS_ERROR")
        ) {
          continue;
        }

        throw error;
      }
    }
    await connection.execute(
      "INSERT INTO schema_migrations (migration_name) VALUES (?)",
      [file]
    );
    await connection.commit();
    console.log(`Applied migration ${file}`);
  } catch (error) {
    await connection.rollback();
    throw error;
  }
}

await connection.end();
