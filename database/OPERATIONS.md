# Database Operations

## Migrations

Run migrations manually:

```bash
npm run db:migrate
```

The start flow runs migrations before the API starts. If Docker is installed but the daemon is down, the scripts fail with a clear message instead of a generic startup error.

Do not commit live or personal finance imports into `database/migrations`. Keep backups under `backups/`, and run one-off private restore or import SQL outside the tracked migration set.

## Backup

Create a timestamped compressed backup:

```bash
npm run db:backup
```

Write to a specific file:

```bash
bash scripts/db-backup.sh backups/manual.sql.gz
```

## Testing

Run:

```bash
npm test
```

This covers the pagination helpers in the API and web layers.

## Restore

Restore from a compressed backup:

```bash
bash scripts/db-restore.sh backups/manual.sql.gz
```

Restore from a plain SQL file:

```bash
bash scripts/db-restore.sh backups/manual.sql
```
