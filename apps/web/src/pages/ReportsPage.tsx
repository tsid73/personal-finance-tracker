import { useMemo, useState } from "react";
import { Download, PieChart as PieChartIcon, TrendingUp } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { Area, AreaChart, Bar, BarChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { ErrorState, LoadingState } from "../components/PageState";
import { api } from "../lib/api";
import { formatCurrency } from "../lib/format";
import { useAppShellContext } from "../shell/useAppShellContext";

type ReportScope = "monthly" | "yearly";

export function ReportsPage() {
  const { selectedMonth, selectedMonthLabel } = useAppShellContext();
  const [reportScope, setReportScope] = useState<ReportScope>("monthly");
  const { data, isLoading, isError } = useQuery({
    queryKey: ["reports", selectedMonth],
    queryFn: async () => (await api.get("/reports/overview", { params: { month: selectedMonth } })).data
  });

  if (isLoading) {
    return <LoadingState message="Loading reports…" />;
  }

  if (isError || !data) {
    return <ErrorState message="Unable to load reports for the selected month." />;
  }

  const isMonthly = reportScope === "monthly";
  const trendData = useMemo(
    () =>
      (isMonthly ? data.monthlyComparison : data.yearlyComparison).map((item: any) => ({
        ...item,
        income: Number(item.income ?? 0),
        expense: Number(item.expense ?? 0),
        net: Number(item.net ?? 0)
      })),
    [data.monthlyComparison, data.yearlyComparison, isMonthly]
  );
  const categoryData = useMemo(
    () =>
      (isMonthly ? data.categoryTotals : data.yearlyCategoryTotals).map((item: any) => ({
        ...item,
        total: Number(item.total ?? 0)
      })),
    [data.categoryTotals, data.yearlyCategoryTotals, isMonthly]
  );
  const barData = useMemo(
    () =>
      (isMonthly ? data.budgetVsActual : data.monthlyBreakdown).map((item: any) => ({
        ...item,
        allocatedAmount: Number(item.allocatedAmount ?? 0),
        spentAmount: Number(item.spentAmount ?? 0),
        income: Number(item.income ?? 0),
        expense: Number(item.expense ?? 0),
        net: Number(item.net ?? 0)
      })),
    [data.budgetVsActual, data.monthlyBreakdown, isMonthly]
  );

  const summaryCards = isMonthly
    ? [
        { label: "Monthly income", value: data.summary.monthlyIncome },
        { label: "Monthly expense", value: data.summary.monthlyExpense },
        { label: "Net cash flow", value: data.summary.monthlyNet },
        { label: "Top expense category", value: data.summary.topExpenseCategory ?? "No data" }
      ]
    : [
        { label: `Income in ${data.selectedYear}`, value: data.summary.yearlyIncome },
        { label: `Expense in ${data.selectedYear}`, value: data.summary.yearlyExpense },
        { label: "Yearly net", value: data.summary.yearlyNet },
        { label: "Avg monthly expense", value: data.summary.averageMonthlyExpense }
      ];

  const downloadCsv = (filename: string, rows: Array<Record<string, string | number>>) => {
    if (rows.length === 0) {
      return;
    }

    const headers = Object.keys(rows[0]);
    const csv = [
      headers.join(","),
      ...rows.map((row) =>
        headers
          .map((header) => {
            const value = row[header] ?? "";
            const escaped = String(value).replace(/"/g, "\"\"");
            return `"${escaped}"`;
          })
          .join(",")
      )
    ].join("\n");

    const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = filename;
    link.click();
    window.URL.revokeObjectURL(url);
  };

  return (
    <div className="space-y-6">
      <div className="card p-5 sm:p-6 dark:border dark:border-slate-800">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <div className="section-title">Reports</div>
            <div className="mt-1 text-sm text-slate-500 dark:text-slate-400">
              Review monthly and yearly finance trends anchored to {selectedMonthLabel}.
            </div>
          </div>
          <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
            <label className="flex flex-col gap-2 text-sm text-slate-500 dark:text-slate-400">
              Report scope
              <select
                className="rounded-lg border border-slate-200 bg-white px-4 py-3 text-ink dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
                name="reportScope"
                value={reportScope}
                onChange={(event) => setReportScope(event.target.value as ReportScope)}
              >
                <option value="monthly">Monthly</option>
                <option value="yearly">Yearly</option>
              </select>
            </label>
            <div className="flex flex-wrap gap-3">
              <button
                className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-4 py-3 text-sm font-medium text-ink dark:border-slate-700 dark:text-slate-100"
                onClick={() =>
                  downloadCsv(
                    `${reportScope}-trend-${selectedMonth}.csv`,
                    trendData.map((item: any) => ({
                      period: item.period ?? item.yearLabel,
                      income: Number(item.income ?? 0),
                      expense: Number(item.expense ?? 0),
                      net: Number(item.net ?? 0)
                    }))
                  )
                }
              >
                <Download className="h-4 w-4" aria-hidden="true" />
                Export trend CSV
              </button>
              <button
                className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-4 py-3 text-sm font-medium text-ink dark:border-slate-700 dark:text-slate-100"
                onClick={() =>
                  downloadCsv(
                    `${reportScope}-categories-${selectedMonth}.csv`,
                    categoryData.map((item: any) => ({
                      category: item.category,
                      total: Number(item.total ?? 0)
                    }))
                  )
                }
              >
                <Download className="h-4 w-4" aria-hidden="true" />
                Export category CSV
              </button>
            </div>
          </div>
        </div>

        <div className="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {summaryCards.map((item) => (
            <div key={item.label} className="rounded-lg bg-mist px-4 py-4 dark:bg-slate-800">
              <div className="text-xs uppercase tracking-[0.2em] text-slate-400 dark:text-slate-500">{item.label}</div>
              <div className="tabular-nums mt-2 text-lg font-semibold text-ink dark:text-slate-100">
                {typeof item.value === "number" ? formatCurrency(item.value) : item.value}
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="card p-5 sm:p-6 dark:border dark:border-slate-800">
        <div className="inline-flex items-center gap-2 section-title"><TrendingUp className="h-5 w-5" aria-hidden="true" />{isMonthly ? "Monthly trend" : "Yearly trend"}</div>
        <div className="mt-1 text-sm text-slate-500 dark:text-slate-400">
          {isMonthly
            ? `Rolling 12-month comparison ending in ${selectedMonthLabel}.`
            : `Year-over-year comparison through ${data.selectedYear}.`}
        </div>
        <div className="mt-4 h-72 sm:h-80">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={trendData}>
              <CartesianGrid vertical={false} strokeDasharray="3 3" />
              <XAxis dataKey={isMonthly ? "monthLabel" : "yearLabel"} />
              <YAxis />
              <Tooltip formatter={(value: number) => formatCurrency(value)} />
              <Area type="monotone" dataKey="income" stroke="#0f766e" fill="#0f766e33" />
              <Area type="monotone" dataKey="expense" stroke="#e76f51" fill="#e76f5133" />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
        <div className="card p-5 sm:p-6 dark:border dark:border-slate-800">
          <div className="inline-flex items-center gap-2 section-title"><PieChartIcon className="h-5 w-5" aria-hidden="true" />{isMonthly ? "Expense distribution" : "Yearly category mix"}</div>
          <div className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            {isMonthly
              ? `Expense categories for ${selectedMonthLabel}.`
              : `Expense categories across ${data.selectedYear}.`}
          </div>
          <div className="mt-4 h-72 sm:h-80">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={categoryData} dataKey="total" nameKey="category" innerRadius={60} outerRadius={104}>
                  {categoryData.map((item: any) => (
                    <Cell key={`${item.category}-${item.color}`} fill={item.color} />
                  ))}
                </Pie>
                <Tooltip formatter={(value: number) => formatCurrency(value)} />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="card p-5 sm:p-6 dark:border dark:border-slate-800">
          <div className="section-title">{isMonthly ? "Budget vs actual" : "Monthly breakdown"}</div>
          <div className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            {isMonthly
              ? `Allocated versus spent amounts for ${selectedMonthLabel}.`
              : `Income and expense totals across ${data.selectedYear}.`}
          </div>
          <div className="mt-4 h-72 sm:h-80">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={barData}>
                <CartesianGrid vertical={false} strokeDasharray="3 3" />
                <XAxis
                  dataKey={isMonthly ? "category" : "monthLabel"}
                  interval={0}
                  angle={isMonthly ? -18 : 0}
                  textAnchor={isMonthly ? "end" : "middle"}
                  height={isMonthly ? 72 : 36}
                />
                <YAxis />
                <Tooltip formatter={(value: number) => formatCurrency(value)} />
                {isMonthly ? (
                  <>
                    <Bar dataKey="allocatedAmount" fill="#1d4ed8" radius={[8, 8, 0, 0]} />
                    <Bar dataKey="spentAmount" fill="#e76f51" radius={[8, 8, 0, 0]} />
                  </>
                ) : (
                  <>
                    <Bar dataKey="income" fill="#0f766e" radius={[8, 8, 0, 0]} />
                    <Bar dataKey="expense" fill="#e76f51" radius={[8, 8, 0, 0]} />
                  </>
                )}
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      <div className="card p-5 sm:p-6 dark:border dark:border-slate-800">
        <div className="section-title">{isMonthly ? "Category totals" : "Yearly category totals"}</div>
        <div className="mt-4 space-y-3">
          {categoryData.length === 0 ? (
            <div className="rounded-lg bg-mist px-4 py-6 text-sm text-slate-500 dark:bg-slate-800 dark:text-slate-400">
              {isMonthly ? "No expense transactions found for this month." : "No expense transactions found for this year."}
            </div>
          ) : (
            categoryData.map((item: any) => (
              <div key={item.category} className="flex items-center justify-between gap-4 rounded-lg bg-mist px-4 py-3 dark:bg-slate-800">
                <div className="min-w-0 flex items-center gap-3">
                  <span className="h-3 w-3 rounded-full" style={{ backgroundColor: item.color }} />
                  <span className="truncate font-medium text-ink dark:text-slate-100">{item.category}</span>
                </div>
                <span className="tabular-nums shrink-0 font-medium text-ink dark:text-slate-100">{formatCurrency(Number(item.total))}</span>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
