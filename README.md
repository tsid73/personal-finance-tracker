# Personal Finance Tracker

Personal Finance Tracker is a local-first finance app for one user. It manages transactions, recurring schedules, budgets, categories, reports, and month-aware dashboard insights on top of a MySQL database that persists locally between runs.

## Security Model

This app is intentionally local-first and designed for one user.

It does not include authentication. Run it on `localhost` or a trusted private network only. Do not expose it directly to the public internet unless you add authentication, authorization, CSRF protection, rate limiting, and deployment hardening.

Anyone who can access the running app can view, create, edit, delete, export, and modify finance data.

The optional Tailscale access flow is intended for trusted tailnet-only access. Any device or user that can reach the exposed Tailscale URL should be considered trusted.

## Current Product Behavior

- A shared selected-month control in the shell drives dashboard, transactions, budgets, reports, and monthly drill-downs together.
- The selected month is preserved while navigating between pages.
- A floating quick-add transaction button is available on every page.
- A built-in theme toggle switches between light and dark modes and persists locally.
- Data is stored in MySQL and persists across app stop/start cycles.
- Stopping the app shuts down the frontend, API, and Docker services without deleting the database volume.
- The app now includes a dedicated recurring-transactions page instead of mixing recurring management into the transactions page.

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
|   +-- api/
|   |   +-- src/config.ts
|   |   +-- src/db.ts
|   |   +-- src/server.ts
|   +-- web/
|       +-- src/main.tsx
|       +-- src/shell/AppShell.tsx
|       +-- src/shell/month.ts
|       +-- src/shell/useAppShellContext.ts
|       +-- src/pages/
|       +-- src/components/
|       +-- src/lib/
+-- packages/
|   +-- shared/src/index.ts
+-- database/
|   +-- schema.sql
|   +-- seed-summary.mjs
+-- scripts/
|   +-- start-dev.sh
|   +-- stop-dev.sh
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
- `.private/Plan.md` is the private rebuild and planning file kept out of git.

## How It Works

1. The frontend loads through Vite and renders a shared shell with month navigation.
2. The selected month is stored in the URL query string and reused across pages.
3. The frontend calls the backend using relative `/api` requests through the Vite proxy in development.
4. The backend validates write payloads with Zod and queries MySQL using `mysql2/promise`.
5. Dashboard and reports use aggregated month-aware SQL queries.
6. Transactions are paginated and filterable to keep the page responsive as data grows.
7. Recurring schedules can auto-create due transactions through the API.
8. TanStack Query manages cache invalidation after create, edit, delete, archive, bulk actions, and undo operations.

## Current Features

### Shared month control

- Previous month, current month, next month buttons
- Month picker input
- Same selected month reused across dashboard, transactions, recurring, budgets, and reports

### Dashboard

- Income summary for selected month
- Expense summary for selected month
- Monthly total budget summary
- Budget allocated summary
- Remaining budget summary
- Daily safe-to-spend summary
- Remaining-days summary
- Fixed-budget summary
- Flexible-budget summary
- Income vs expense trend chart
- Recent transactions
- Budget status overview
- Top merchants
- Top spending categories
- Unusual spend alerts
- Budget risk panel for categories projected to overshoot

### Transactions

- Add, edit, delete transactions
- Undo delete for the most recent transaction removal
- Undo for the most recent bulk delete
- Delete confirmation before destructive actions
- Paginated transaction list
- Search and filters for title, merchant, notes, type, account, and category
- Export filtered transactions to CSV
- Bulk delete
- Bulk recategorize
- Sticky filter panel
- SweetAlert delete confirmation and undo restore flow
- Floating quick-add button visible on every page
- Account selection includes `Bank`, `Cash`, `Credit Card`, `UPI`, `UPI-Lite`, and `NEFT`
- Validation for title, amount, account, category, and month-aligned date
- Mobile card layout and desktop table layout
- Mobile cards use direct icon actions instead of overflow menus

### Recurring

- Dedicated recurring page in the main navigation
- Add, edit, delete recurring schedules
- Turn recurring schedules on and off
- Manual `Create due now` action
- Auto-sync due schedules through the API
- Recurring form is collapsible
- Schedule list shows due, upcoming, and inactive state
- Start date, next due date, and last generated transaction date are shown per schedule

### Budgets

- Monthly total budget target
- Category-level monthly allocations
- Add, edit, delete category budgets
- Remaining-to-allocate summary
- Spent vs allocated visualization
- Fixed vs flexible category budget modes are shown
- Direct icon actions instead of overflow menus

### Categories

- Seeded default categories
- Add, edit, archive, restore, and delete custom categories
- Type, color, icon picker, and budget mode fields
- Input validation and user-facing errors
- Duplicate category names are blocked per type
- Delete is prevented for categories in use unless a same-type replacement category is chosen
- Replacement selection only appears when delete is initiated on an in-use category
- Renaming an in-use category warns that existing records will be affected
- Category activity history is shown in the UI

### Reports

- Expense distribution for selected month
- Category totals for selected month
- Multi-month comparison ending at the selected month
- Category totals include allocated, spent, and remaining for monthly reports
- Clicking a monthly category total drills into filtered transactions for that category and month
- Monthly category CSV export includes budget, spent, and remaining

### Error handling

- Route-level error boundary
- Page-level loading states with lightweight skeleton placeholders
- Page-level error states
- Backend validation error responses
- Recent activity panels for category, transaction, and recurring changes

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
- `recurring_transactions`
- `budgets`
- `monthly_budget_targets`
- `activity_logs`

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

## Optional Tailscale Access

The repo includes a tailnet-only remote access layer for phone usage without rebinding the app itself to the network.

Start it with:

```bash
npm run start:tailscale
```

Stop it with:

```bash
npm run stop:tailscale
```

This keeps the main app on `127.0.0.1` and exposes only a separate proxy bound to the machine's Tailscale IP.

## Testing

Run the automated checks:

```bash
npm test
npm run lint
npm run build
```

## Persistence

Transactions, recurring schedules, budgets, categories, activity history, and monthly budget data remain available after stopping and starting the app again.

That persistence works because the Docker stop flow keeps the named MySQL volume instead of deleting it.

## API Surface

- `GET /api/health`
- `GET /api/dashboard?month=YYYY-MM`
- `GET /api/transactions?month=YYYY-MM&page=1&perPage=10&q=&kind=&accountId=&categoryId=`
- `GET /api/transactions/export?month=YYYY-MM&q=&kind=&accountId=&categoryId=`
- `POST /api/transactions`
- `PUT /api/transactions/:id`
- `DELETE /api/transactions/:id`
- `POST /api/transactions/bulk-delete`
- `POST /api/transactions/bulk-recategorize`
- `GET /api/recurring-transactions`
- `POST /api/recurring-transactions`
- `PUT /api/recurring-transactions/:id`
- `DELETE /api/recurring-transactions/:id`
- `POST /api/recurring-transactions/sync`
- `GET /api/categories`
- `POST /api/categories`
- `PUT /api/categories/:id`
- `DELETE /api/categories/:id`
- `PUT /api/categories/:id/archive`
- `GET /api/accounts`
- `GET /api/monthly-budget?month=YYYY-MM`
- `PUT /api/monthly-budget`
- `GET /api/budgets?month=YYYY-MM`
- `POST /api/budgets`
- `PUT /api/budgets/:id`
- `DELETE /api/budgets/:id`
- `GET /api/reports/overview?month=YYYY-MM`
- `GET /api/activity`

## Notes For Future Work

- Barcode scanning is still deferred.
- Authentication is not included by design. Use only on localhost or a trusted private network unless you add auth and hardening.
- Good next additions are savings goals, CSV import, attachments, and account management UI.
- `.private/Plan.md` should be updated before and after future feature work.
