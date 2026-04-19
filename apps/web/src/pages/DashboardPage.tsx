import { ArrowDownCircle, ArrowUpCircle, AlertTriangle, PiggyBank, Scale, Wallet } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { ErrorState, LoadingState } from "../components/PageState";
import { SummaryCard } from "../components/SummaryCard";
import { api } from "../lib/api";
import { formatCurrency } from "../lib/format";
import { useAppShellContext } from "../shell/useAppShellContext";

export function DashboardPage() {
  const { selectedMonth, selectedMonthLabel } = useAppShellContext();
  const { data, isLoading, isError } = useQuery({
    queryKey: ["dashboard", selectedMonth],
    queryFn: async () => (await api.get("/dashboard", { params: { month: selectedMonth } })).data
  });

  if (isLoading) {
    return <LoadingState message="Loading dashboard…" />;
  }

  if (isError || !data) {
    return <ErrorState message="Unable to load dashboard data for the selected month." />;
  }

  const summary = data.summary;

  return (
    <div className="space-y-6">
      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-5">
        <SummaryCard label={`${selectedMonthLabel} income`} value={summary.monthlyIncome} tone="success" icon={ArrowUpCircle} />
        <SummaryCard label={`${selectedMonthLabel} expenses`} value={summary.monthlyExpense} tone="danger" icon={ArrowDownCircle} />
        <SummaryCard label="Monthly budget" value={summary.totalBudget} icon={Wallet} />
        <SummaryCard label="Budget allocated" value={summary.budgetAllocated} icon={PiggyBank} />
        <SummaryCard
          label="Remaining budget"
          value={summary.remainingBudget}
          tone={summary.remainingBudget < 0 ? "danger" : "success"}
          icon={Scale}
        />
      </section>

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <SummaryCard label="Daily safe to spend" value={summary.safeToSpend} tone={summary.safeToSpend < 0 ? "danger" : "success"} icon={Wallet} />
        <SummaryCard label="Days remaining" value={summary.remainingDays} icon={Scale} format="number" />
        <SummaryCard label="Fixed budget" value={summary.fixedBudget} icon={PiggyBank} />
        <SummaryCard label="Flexible budget" value={summary.flexibleBudget} icon={PiggyBank} />
      </section>

      <section className="grid gap-6 xl:grid-cols-[1.35fr_1fr]">
        <div className="card p-5 sm:p-6 dark:border dark:border-slate-800">
          <div className="section-title">Income vs expense trend</div>
          <div className="mt-1 text-sm text-slate-500 dark:text-slate-400">Rolling 12-month view ending in {selectedMonthLabel}.</div>
          <div className="mt-4 h-72 sm:h-80">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={data.monthlyTrend}>
                <CartesianGrid vertical={false} strokeDasharray="3 3" />
                <XAxis dataKey="monthLabel" tick={{ fill: "#94a3b8" }} />
                <YAxis />
                <Tooltip />
                <Bar dataKey="income" fill="#0f766e" radius={[8, 8, 0, 0]} />
                <Bar dataKey="expense" fill="#e76f51" radius={[8, 8, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="card p-5 sm:p-6 dark:border dark:border-slate-800">
          <div className="section-title">Budget status</div>
          <div className="mt-1 text-sm text-slate-500 dark:text-slate-400">Track spending against the selected month's category budgets.</div>
          <div className="mt-4 space-y-4">
            {data.budgetProgress.length === 0 ? (
              <div className="rounded-lg bg-mist px-4 py-6 text-sm text-slate-500 dark:bg-slate-800 dark:text-slate-400">
                No category budgets are set for this month.
                <div className="mt-3">
                  <Link className="inline-flex rounded-lg border border-slate-200 px-3 py-2 text-xs font-medium text-slate-700 dark:border-slate-700 dark:text-slate-200" to={`/budgets?month=${selectedMonth}`}>Create budgets</Link>
                </div>
              </div>
            ) : (
              data.budgetProgress.map((item: any) => {
                const spent = Number(item.spentAmount);
                const allocated = Number(item.allocatedAmount);
                const percent = allocated > 0 ? Math.min((spent / allocated) * 100, 100) : 0;
                return (
                  <div key={item.id} className="min-w-0">
                    <div className="mb-2 flex items-center justify-between gap-4 text-sm">
                      <span className="min-w-0 truncate font-medium text-ink dark:text-slate-100">{item.categoryName}</span>
                      <span className="tabular-nums shrink-0 text-right text-slate-500 dark:text-slate-400">
                        {formatCurrency(spent)} / {formatCurrency(allocated)}
                      </span>
                    </div>
                    <div className="h-3 rounded-full bg-slate-100 dark:bg-slate-800">
                      <div className="h-3 rounded-full" style={{ width: `${percent}%`, backgroundColor: item.color }} />
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>
      </section>

      <section className="card p-5 sm:p-6 dark:border dark:border-slate-800">
        <div className="section-title">Recent transactions</div>
        <div className="mt-1 text-sm text-slate-500 dark:text-slate-400">Entries recorded in {selectedMonthLabel}.</div>
        <div className="mt-4 space-y-3">
          {data.recentTransactions.length === 0 ? (
            <div className="rounded-lg bg-mist px-4 py-6 text-sm text-slate-500 dark:bg-slate-800 dark:text-slate-400">
              No transactions found for this month.
              <div className="mt-3">
                <Link className="inline-flex rounded-lg border border-slate-200 px-3 py-2 text-xs font-medium text-slate-700 dark:border-slate-700 dark:text-slate-200" to={`/transactions?month=${selectedMonth}&addTxn=1`}>Add transaction</Link>
              </div>
            </div>
          ) : (
            data.recentTransactions.map((item: any) => (
              <div key={item.id} className="flex items-center justify-between gap-4 rounded-lg bg-mist px-4 py-3 dark:bg-slate-800">
                <div className="min-w-0">
                  <div className="truncate font-medium text-ink dark:text-slate-100">{item.title}</div>
                  <div className="truncate text-sm text-slate-500 dark:text-slate-400">{item.categoryName}</div>
                </div>
                <div className={`tabular-nums shrink-0 ${item.kind === "income" ? "text-emerald-600" : "text-rose-600"}`}>
                  {item.kind === "income" ? "+" : "-"}
                  {formatCurrency(Number(item.amount))}
                </div>
              </div>
            ))
          )}
        </div>
      </section>

      <section className="grid gap-6 xl:grid-cols-3">
        <div className="card p-5 sm:p-6 dark:border dark:border-slate-800">
          <div className="section-title">Top merchants</div>
          <div className="mt-4 space-y-3">
            {data.topMerchants.length === 0 ? (
              <div className="rounded-lg bg-mist px-4 py-6 text-sm text-slate-500 dark:bg-slate-800 dark:text-slate-400">No merchant data available for this month.</div>
            ) : (
              data.topMerchants.map((item: any) => (
                <div key={item.merchant} className="flex items-center justify-between gap-4 rounded-lg bg-mist px-4 py-3 dark:bg-slate-800">
                  <div className="truncate font-medium text-ink dark:text-slate-100">{item.merchant}</div>
                  <div className="tabular-nums text-sm text-slate-500 dark:text-slate-400">{formatCurrency(Number(item.totalSpent))}</div>
                </div>
              ))
            )}
          </div>
        </div>

        <div className="card p-5 sm:p-6 dark:border dark:border-slate-800">
          <div className="section-title">Top spending categories</div>
          <div className="mt-4 space-y-3">
            {data.topSpendingCategories.length === 0 ? (
              <div className="rounded-lg bg-mist px-4 py-6 text-sm text-slate-500 dark:bg-slate-800 dark:text-slate-400">No spending categories available for this month.</div>
            ) : (
              data.topSpendingCategories.map((item: any) => (
                <div key={item.category} className="flex items-center justify-between gap-4 rounded-lg bg-mist px-4 py-3 dark:bg-slate-800">
                  <div className="flex min-w-0 items-center gap-3">
                    <span className="h-3 w-3 rounded-full" style={{ backgroundColor: item.color }} />
                    <div className="truncate font-medium text-ink dark:text-slate-100">{item.category}</div>
                  </div>
                  <div className="tabular-nums text-sm text-slate-500 dark:text-slate-400">{formatCurrency(Number(item.totalSpent))}</div>
                </div>
              ))
            )}
          </div>
        </div>

        <div className="card p-5 sm:p-6 dark:border dark:border-slate-800">
          <div className="section-title inline-flex items-center gap-2"><AlertTriangle className="h-5 w-5" aria-hidden="true" />Unusual spend alerts</div>
          <div className="mt-4 space-y-3">
            {data.unusualSpendAlerts.length === 0 ? (
              <div className="rounded-lg bg-mist px-4 py-6 text-sm text-slate-500 dark:bg-slate-800 dark:text-slate-400">No unusual spend spikes detected.</div>
            ) : (
              data.unusualSpendAlerts.map((item: any) => (
                <div key={item.category} className="rounded-lg bg-mist px-4 py-3 dark:bg-slate-800">
                  <div className="font-medium text-ink dark:text-slate-100">{item.category}</div>
                  <div className="mt-1 text-sm text-slate-500 dark:text-slate-400">
                    {formatCurrency(Number(item.currentSpent))} this month vs {formatCurrency(Number(item.averageSpent))} average
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </section>

      <section className="card p-5 sm:p-6 dark:border dark:border-slate-800">
        <div className="section-title">Budget risk</div>
        <div className="mt-1 text-sm text-slate-500 dark:text-slate-400">Categories currently projected to overshoot this month.</div>
        <div className="mt-4 space-y-3">
          {data.budgetRisk.length === 0 ? (
            <div className="rounded-lg bg-mist px-4 py-6 text-sm text-slate-500 dark:bg-slate-800 dark:text-slate-400">No overspend risks detected for the selected month.</div>
          ) : (
            data.budgetRisk.map((item: any) => (
              <div key={item.categoryName} className="rounded-lg bg-mist px-4 py-3 dark:bg-slate-800">
                <div className="flex items-center justify-between gap-4">
                  <div className="flex min-w-0 items-center gap-3">
                    <span className="h-3 w-3 rounded-full" style={{ backgroundColor: item.color }} />
                    <div className="truncate font-medium text-ink dark:text-slate-100">{item.categoryName}</div>
                  </div>
                  <div className="tabular-nums text-sm text-rose-600">+{formatCurrency(Number(item.overrun))}</div>
                </div>
                <div className="mt-1 text-sm text-slate-500 dark:text-slate-400">Projected {formatCurrency(Number(item.projectedSpent))} against {formatCurrency(Number(item.allocatedAmount))} allocated</div>
              </div>
            ))
          )}
        </div>
      </section>
    </div>
  );
}
