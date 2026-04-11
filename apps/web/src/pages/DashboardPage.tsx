import { useQuery } from "@tanstack/react-query";
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
    return <LoadingState message="Loading dashboard..." />;
  }

  if (isError || !data) {
    return <ErrorState message="Unable to load dashboard data for the selected month." />;
  }

  const summary = data.summary;

  return (
    <div className="space-y-6">
      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-5">
        <SummaryCard label={`${selectedMonthLabel} income`} value={summary.monthlyIncome} tone="success" />
        <SummaryCard label={`${selectedMonthLabel} expenses`} value={summary.monthlyExpense} tone="danger" />
        <SummaryCard label="Monthly budget" value={summary.totalBudget} />
        <SummaryCard label="Budget allocated" value={summary.budgetAllocated} />
        <SummaryCard
          label="Remaining budget"
          value={summary.remainingBudget}
          tone={summary.remainingBudget < 0 ? "danger" : "success"}
        />
      </section>

      <section className="grid gap-6 xl:grid-cols-[1.35fr_1fr]">
        <div className="card p-5 sm:p-6">
          <div className="section-title">Income vs expense trend</div>
          <div className="mt-1 text-sm text-slate-500">Six-month view ending in {selectedMonthLabel}.</div>
          <div className="mt-4 h-72 sm:h-80">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={data.monthlyTrend}>
                <CartesianGrid vertical={false} strokeDasharray="3 3" />
                <XAxis dataKey="monthLabel" />
                <YAxis />
                <Tooltip />
                <Bar dataKey="income" fill="#0f766e" radius={[8, 8, 0, 0]} />
                <Bar dataKey="expense" fill="#e76f51" radius={[8, 8, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="card p-5 sm:p-6">
          <div className="section-title">Budget status</div>
          <div className="mt-1 text-sm text-slate-500">Track spending against the selected month's category budgets.</div>
          <div className="mt-4 space-y-4">
            {data.budgetProgress.length === 0 ? (
              <div className="rounded-2xl bg-mist px-4 py-6 text-sm text-slate-500">No category budgets are set for this month.</div>
            ) : (
              data.budgetProgress.map((item: any) => {
                const spent = Number(item.spentAmount);
                const allocated = Number(item.allocatedAmount);
                const percent = allocated > 0 ? Math.min((spent / allocated) * 100, 100) : 0;
                return (
                  <div key={item.id}>
                    <div className="mb-2 flex items-center justify-between gap-4 text-sm">
                      <span className="font-medium text-ink">{item.categoryName}</span>
                      <span className="text-right text-slate-500">
                        {formatCurrency(spent)} / {formatCurrency(allocated)}
                      </span>
                    </div>
                    <div className="h-3 rounded-full bg-slate-100">
                      <div className="h-3 rounded-full" style={{ width: `${percent}%`, backgroundColor: item.color }} />
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>
      </section>

      <section className="card p-5 sm:p-6">
        <div className="section-title">Recent transactions</div>
        <div className="mt-1 text-sm text-slate-500">Entries recorded in {selectedMonthLabel}.</div>
        <div className="mt-4 space-y-3">
          {data.recentTransactions.length === 0 ? (
            <div className="rounded-2xl bg-mist px-4 py-6 text-sm text-slate-500">No transactions found for this month.</div>
          ) : (
            data.recentTransactions.map((item: any) => (
              <div key={item.id} className="flex items-center justify-between gap-4 rounded-2xl bg-mist px-4 py-3">
                <div className="min-w-0">
                  <div className="truncate font-medium text-ink">{item.title}</div>
                  <div className="text-sm text-slate-500">{item.categoryName}</div>
                </div>
                <div className={`shrink-0 ${item.kind === "income" ? "text-emerald-600" : "text-rose-600"}`}>
                  {item.kind === "income" ? "+" : "-"}
                  {formatCurrency(Number(item.amount))}
                </div>
              </div>
            ))
          )}
        </div>
      </section>
    </div>
  );
}
