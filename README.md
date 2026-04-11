# Personal Finance Tracker

Personal Finance Tracker is a local budget and finance tracker for managing monthly spending, category budgets, reports, and transaction history.

## Current Product Behavior

- The app is driven by a shared selected-month control at the top of the layout.
- Changing the month updates dashboard, transactions, budgets, and reports together.
- The selected month is preserved while navigating between pages.
- A floating quick-add transaction button is available on every page.
- Data is stored in MySQL and persists across app stop/start cycles.
- Stopping the app shuts down the frontend, API, and Docker services without deleting the database volume.

## Tech Stack

### Frontend

- React
- TypeScript
- Vite
- Tailwind CSS
- React Router
- TanStack Query
- Recharts
- Axios

### Backend

- Node.js
- Express
- TypeScript
- Zod
- mysql2
- dotenv

### Data and local infrastructure

- MySQL 8
- Docker Compose

### Shared code

- `packages/shared` for shared TypeScript types

## Project Structure

```text
personal-finance-tracker/
+-- apps/
¦   +-- api/
¦   ¦   +-- src/config.ts
¦   ¦   +-- src/db.ts
¦   ¦   +-- src/server.ts
¦   +-- web/
¦       +-- src/main.tsx
¦       +-- src/shell/AppShell.tsx
¦       +-- src/shell/month.ts
¦       +-- src/shell/useAppShellContext.ts
¦       +-- src/pages/
¦       +-- src/components/
¦       +-- src/lib/
+-- packages/
¦   +-- shared/src/index.ts
+-- database/
¦   +-- schema.sql
¦   +-- seed-summary.mjs
+-- scripts/
¦   +-- start-dev.sh
¦   +-- stop-dev.sh
+-- Plan.md
+-- README.md
+-- docker-compose.yml
+-- package.json
+-- tsconfig.base.json
```

## Architecture

This project uses a monorepo-style workspace.

- `apps/web` contains the React application and page-level UI.
- `apps/api` contains the Express server and MySQL-backed REST endpoints.
- `packages/shared` contains shared domain types.
- `database/migrations` contains the schema and demo seed migrations. `database/schema.sql` remains as a schema reference snapshot.
- `docker-compose.yml` starts the local MySQL instance.
- `Plan.md` is the machine-readable AI handoff and planning file.

## How It Works

1. The frontend loads through Vite and renders a shared shell with month navigation.
2. The selected month is stored in the URL query string and reused across pages.
3. The frontend calls the backend using relative `/api` requests through the Vite proxy in development.
4. The backend validates write payloads with Zod and queries MySQL using `mysql2/promise`.
5. Dashboard and reports use aggregated month-aware SQL queries.
6. Transactions are paginated to keep the page responsive as data grows.
7. TanStack Query manages cache invalidation after create, edit, delete, and undo operations.

## Current Features

### Shared month control

- Previous month, current month, next month buttons
- Month picker input
- Same selected month reused across dashboard, transactions, budgets, and reports

### Dashboard

- Income summary for selected month
- Expense summary for selected month
- Monthly total budget summary
- Budget allocated summary
- Remaining budget summary
- Income vs expense trend chart
- Budget split chart
- Recent transactions
- Budget status overview

### Transactions

- Add, edit, delete transactions
- Undo delete for the most recent transaction removal
- Delete confirmation before destructive actions
- Paginated transaction list
- Floating quick-add button visible on every page
- Generic payment type selection such as `Cash`, `Bank`, `Credit Card`
- Validation for title, amount, account, category, and month-aligned date
- Mobile card layout and desktop table layout

### Budgets

- Monthly total budget target
- Category-level monthly allocations
- Add, edit, delete category budgets
- Remaining-to-allocate summary
- Spent vs allocated visualization

### Categories

- Seeded default categories
- Add, edit, delete custom categories
- Type, color, and icon label fields
- Input validation and user-facing errors

### Reports

- Expense distribution for selected month
- Category totals for selected month
- Multi-month comparison ending at the selected month

### Error handling

- Route-level error boundary
- Page-level loading states with lightweight skeleton placeholders
- Page-level error states
- Backend validation error responses

## Entry Points

### Frontend

- `apps/web/src/main.tsx`
- `apps/web/src/shell/AppShell.tsx`

### Backend

- `apps/api/src/server.ts`
- `apps/api/src/config.ts`
- `apps/api/src/db.ts`

### Database

- `database/migrations`
- `database/schema.sql`

## Database Notes

The schema includes these main tables:

- `users`
- `accounts`
- `categories`
- `transactions`
- `budgets`
- `monthly_budget_targets`

Indexes are included for common user/month/date query patterns to keep reporting and month-filtered reads responsive.

## Installation

### Prerequisites

- Node.js 20 or newer
- npm 10 or newer
- Docker with Compose support
- WSL Ubuntu if you are using the same workspace layout

### Initial Setup

1. Open the project root.
2. Copy `.env.example` to `.env` if `.env` does not already exist.
3. Install dependencies:
   ```bash
   npm install
   ```
4. Start the app:
   ```bash
   bash scripts/start-dev.sh
   ```

## Startup and Stop

### Start

Use either command:

```bash
bash scripts/start-dev.sh
```

or

```bash
npm run start:app
```

The startup script will:

- create `.env` from `.env.example` if needed
- install dependencies if `node_modules` is missing
- start MySQL through Docker Compose
- wait for MySQL to become reachable
- run database migrations
- build shared and API packages
- start the API on port `4000`
- start the frontend on port `5173`
- try to open the frontend in the browser
- write logs and pid files to `logs/`

### Stop

Use either command:

```bash
bash scripts/stop-dev.sh
```

or

```bash
npm run stop:app
```

The stop script will:

- stop the API process
- stop the frontend process
- kill listeners on ports `4000` and `5173` if needed
- shut down Docker services for this project
- keep the MySQL volume intact so your data remains on the next start

## Testing

Run the automated checks:

```bash
npm test
npm run lint
npm run build
```

## Persistence

Transactions, budgets, categories, and monthly budget data remain available after stopping and starting the app again.

That persistence works because the Docker stop flow keeps the named MySQL volume instead of deleting it.

## API Surface

- `GET /api/health`
- `GET /api/dashboard?month=YYYY-MM`
- `GET /api/transactions?month=YYYY-MM`
- `POST /api/transactions`
- `PUT /api/transactions/:id`
- `DELETE /api/transactions/:id`
- `GET /api/categories`
- `POST /api/categories`
- `PUT /api/categories/:id`
- `DELETE /api/categories/:id`
- `GET /api/accounts`
- `GET /api/monthly-budget?month=YYYY-MM`
- `PUT /api/monthly-budget`
- `GET /api/budgets?month=YYYY-MM`
- `POST /api/budgets`
- `PUT /api/budgets/:id`
- `DELETE /api/budgets/:id`
- `GET /api/reports/overview?month=YYYY-MM`

## Notes For Future Work

- Barcode scanning is still deferred.
- Authentication is not implemented yet.
- Good next additions are recurring transactions, savings goals, CSV import/export, alerts, attachments, and account management UI.
- `Plan.md` should be updated before and after future feature work.
