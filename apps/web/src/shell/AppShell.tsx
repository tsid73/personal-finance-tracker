import { BarChart3, CalendarDays, ChevronLeft, ChevronRight, LayoutDashboard, ListTodo, Moon, Plus, Shapes, SunMedium, Wallet } from "lucide-react";
import { Link, NavLink, Outlet, useSearchParams } from "react-router-dom";
import { useTheme } from "../lib/theme";
import { formatMonthLabel, getCurrentMonth, shiftMonth } from "./month";

const navigation = [
  { label: "Dashboard", to: "/dashboard", icon: LayoutDashboard },
  { label: "Transactions", to: "/transactions", icon: ListTodo },
  { label: "Budgets", to: "/budgets", icon: Wallet },
  { label: "Categories", to: "/categories", icon: Shapes },
  { label: "Reports", to: "/reports", icon: BarChart3 }
];

export function AppShell() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { theme, toggleTheme } = useTheme();
  const selectedMonth = searchParams.get("month") ?? getCurrentMonth();
  const selectedMonthLabel = formatMonthLabel(selectedMonth);

  const updateMonth = (month: string) => {
    const nextParams = new URLSearchParams(searchParams);
    nextParams.set("month", month);
    setSearchParams(nextParams, { replace: true });
  };
  const monthSearch = `?month=${selectedMonth}`;
  const quickAddSearch = `?month=${selectedMonth}&addTxn=1`;

  return (
    <div className="min-h-screen px-3 py-4 sm:px-4 sm:py-6 md:px-6 dark:bg-transparent">
      <div className="mx-auto grid max-w-7xl gap-4 lg:grid-cols-[260px_minmax(0,1fr)] lg:gap-6">
        <aside className="card flex flex-col gap-5 p-5 sm:p-6 dark:border dark:border-slate-800">
          <div>
            <div className="text-sm uppercase tracking-[0.3em] text-slate-400 dark:text-slate-500">Finance Flow</div>
            <h1 className="mt-2 text-2xl font-semibold text-ink dark:text-slate-100">Personal Tracker</h1>
            <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">
              Track spending, stay inside budget, and understand your money at a glance.
            </p>
          </div>

          <nav className="-mx-1 flex gap-2 overflow-x-auto px-1 lg:mx-0 lg:flex-col lg:overflow-visible lg:px-0">
            {navigation.map((item) => (
              <NavLink
                key={item.to}
                to={{ pathname: item.to, search: monthSearch }}
                className={({ isActive }) =>
                  `shrink-0 rounded-lg px-4 py-3 text-sm font-medium transition ${
                    isActive ? "bg-ink text-white dark:bg-slate-100 dark:text-slate-950" : "text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800"
                  }`
                }
              >
                <span className="inline-flex items-center gap-2">
                  <item.icon className="h-4 w-4" aria-hidden="true" />
                  {item.label}
                </span>
              </NavLink>
            ))}
          </nav>
          <button
            type="button"
            className="inline-flex items-center justify-center gap-2 rounded-lg border border-slate-200 px-4 py-3 text-sm font-medium text-slate-700 dark:border-slate-700 dark:text-slate-200"
            onClick={toggleTheme}
            aria-label={`Switch to ${theme === "light" ? "dark" : "light"} theme`}
          >
            {theme === "light" ? <Moon className="h-4 w-4" aria-hidden="true" /> : <SunMedium className="h-4 w-4" aria-hidden="true" />}
            {theme === "light" ? "Dark Theme" : "Light Theme"}
          </button>
        </aside>

        <main className="space-y-6">
          <header className="card flex flex-col gap-5 p-5 sm:p-6 dark:border dark:border-slate-800">
            <div>
              <div className="text-sm uppercase tracking-[0.3em] text-slate-400 dark:text-slate-500">Overview</div>
              <h2 className="mt-2 text-3xl font-semibold text-ink dark:text-slate-100">Budget and finance tracker</h2>
            </div>
            <div className="grid gap-3 md:grid-cols-[auto_minmax(0,1fr)] md:items-end">
              <div>
                <div className="inline-flex items-center gap-2 text-xs uppercase tracking-[0.2em] text-slate-400 dark:text-slate-500">
                  <CalendarDays className="h-4 w-4" aria-hidden="true" />
                  Selected month
                </div>
                <div className="mt-2 text-2xl font-semibold text-ink dark:text-slate-100">{selectedMonthLabel}</div>
              </div>
              <div className="flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-center sm:justify-end">
                <div className="flex gap-2">
                  <button
                    className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-4 py-3 text-sm font-medium text-slate-700 dark:border-slate-700 dark:text-slate-200"
                    onClick={() => updateMonth(shiftMonth(selectedMonth, -1))}
                    aria-label="Go to previous month"
                  >
                    <ChevronLeft className="h-4 w-4" aria-hidden="true" />
                    Previous
                  </button>
                  <button
                    className="rounded-lg border border-slate-200 px-4 py-3 text-sm font-medium text-slate-700 dark:border-slate-700 dark:text-slate-200"
                    onClick={() => updateMonth(getCurrentMonth())}
                  >
                    Current
                  </button>
                  <button
                    className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-4 py-3 text-sm font-medium text-slate-700 dark:border-slate-700 dark:text-slate-200"
                    onClick={() => updateMonth(shiftMonth(selectedMonth, 1))}
                    aria-label="Go to next month"
                  >
                    Next
                    <ChevronRight className="h-4 w-4" aria-hidden="true" />
                  </button>
                </div>
                <input
                  className="rounded-lg border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
                  type="month"
                  name="selectedMonth"
                  aria-label="Selected month"
                  value={selectedMonth}
                  onChange={(event) => updateMonth(event.target.value)}
                />
              </div>
            </div>
          </header>

          <Outlet context={{ selectedMonth, selectedMonthLabel }} />
        </main>
      </div>
      <Link
        to={{ pathname: "/transactions", search: quickAddSearch }}
        className="fixed bottom-5 right-5 z-30 inline-flex items-center justify-center gap-2 rounded-lg bg-ink px-5 py-4 text-sm font-semibold text-white shadow-card transition hover:translate-y-[-1px] hover:bg-slate-900 dark:bg-slate-100 dark:text-slate-950 dark:hover:bg-slate-200"
      >
        <Plus className="h-4 w-4" aria-hidden="true" />
        Add transaction
      </Link>
    </div>
  );
}
