import { Link, NavLink, Outlet, useSearchParams } from "react-router-dom";
import { formatMonthLabel, getCurrentMonth, shiftMonth } from "./month";

const navigation = [
  { label: "Dashboard", to: "/dashboard" },
  { label: "Transactions", to: "/transactions" },
  { label: "Budgets", to: "/budgets" },
  { label: "Categories", to: "/categories" },
  { label: "Reports", to: "/reports" }
];

export function AppShell() {
  const [searchParams, setSearchParams] = useSearchParams();
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
    <div className="min-h-screen px-3 py-4 sm:px-4 sm:py-6 md:px-6">
      <div className="mx-auto grid max-w-7xl gap-4 lg:grid-cols-[260px_minmax(0,1fr)] lg:gap-6">
        <aside className="card flex flex-col gap-5 p-5 sm:p-6">
          <div>
            <div className="text-sm uppercase tracking-[0.3em] text-slate-400">Finance Flow</div>
            <h1 className="mt-2 text-2xl font-semibold text-ink">Personal Tracker</h1>
            <p className="mt-2 text-sm text-slate-500">
              Track spending, stay inside budget, and understand your money at a glance.
            </p>
          </div>

          <nav className="-mx-1 flex gap-2 overflow-x-auto px-1 lg:mx-0 lg:flex-col lg:overflow-visible lg:px-0">
            {navigation.map((item) => (
              <NavLink
                key={item.to}
                to={{ pathname: item.to, search: monthSearch }}
                className={({ isActive }) =>
                  `shrink-0 rounded-2xl px-4 py-3 text-sm font-medium transition ${
                    isActive ? "bg-ink text-white" : "text-slate-600 hover:bg-slate-100"
                  }`
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>

        </aside>

        <main className="space-y-6">
          <header className="card flex flex-col gap-5 p-5 sm:p-6">
            <div>
              <div className="text-sm uppercase tracking-[0.3em] text-slate-400">Overview</div>
              <h2 className="mt-2 text-3xl font-semibold text-ink">Budget and finance tracker</h2>
            </div>
            <div className="grid gap-3 md:grid-cols-[auto_minmax(0,1fr)] md:items-end">
              <div>
                <div className="text-xs uppercase tracking-[0.2em] text-slate-400">Selected month</div>
                <div className="mt-2 text-2xl font-semibold text-ink">{selectedMonthLabel}</div>
              </div>
              <div className="flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-center sm:justify-end">
                <div className="flex gap-2">
                  <button
                    className="rounded-2xl border border-slate-200 px-4 py-3 text-sm font-medium text-slate-700"
                    onClick={() => updateMonth(shiftMonth(selectedMonth, -1))}
                  >
                    Previous
                  </button>
                  <button
                    className="rounded-2xl border border-slate-200 px-4 py-3 text-sm font-medium text-slate-700"
                    onClick={() => updateMonth(getCurrentMonth())}
                  >
                    Current
                  </button>
                  <button
                    className="rounded-2xl border border-slate-200 px-4 py-3 text-sm font-medium text-slate-700"
                    onClick={() => updateMonth(shiftMonth(selectedMonth, 1))}
                  >
                    Next
                  </button>
                </div>
                <input
                  className="rounded-2xl border border-slate-200 px-4 py-3 text-sm text-slate-700"
                  type="month"
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
        className="fixed bottom-5 right-5 z-30 inline-flex h-14 items-center justify-center rounded-full bg-ink px-5 text-sm font-semibold text-white shadow-card transition hover:translate-y-[-1px] hover:bg-slate-900"
      >
        + Add transaction
      </Link>
    </div>
  );
}
