import { useEffect, useState } from "react";
import { CircleDollarSign, PencilLine, PiggyBank, Save, Tag, Trash2 } from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ErrorState, LoadingState } from "../components/PageState";
import { api } from "../lib/api";
import { confirmDestructiveAction, showErrorToast, showSuccessToast } from "../lib/alerts";
import { getErrorMessage } from "../lib/errors";
import { formatCurrency } from "../lib/format";
import { invalidateActiveQueries } from "../lib/query";
import { useAppShellContext } from "../shell/useAppShellContext";

export function BudgetsPage() {
  const { selectedMonth, selectedMonthLabel } = useAppShellContext();
  const [year, month] = selectedMonth.split("-").map(Number);
  const queryClient = useQueryClient();
  const [editingId, setEditingId] = useState<number | null>(null);
  const [allocationError, setAllocationError] = useState<string | null>(null);
  const [monthlyBudgetError, setMonthlyBudgetError] = useState<string | null>(null);
  const [form, setForm] = useState({ categoryId: "", allocatedAmount: "" });
  const [monthlyBudget, setMonthlyBudget] = useState("");

  const { data = [], isLoading, isError } = useQuery({
    queryKey: ["budgets", selectedMonth],
    queryFn: async () => (await api.get("/budgets", { params: { month: selectedMonth } })).data
  });

  const { data: monthlyBudgetData } = useQuery({
    queryKey: ["monthly-budget", selectedMonth],
    queryFn: async () => (await api.get("/monthly-budget", { params: { month: selectedMonth } })).data
  });

  useEffect(() => {
    setMonthlyBudget(String(monthlyBudgetData?.totalBudget ?? 0));
  }, [monthlyBudgetData]);

  const { data: categories = [] } = useQuery({
    queryKey: ["categories"],
    queryFn: async () => (await api.get("/categories")).data
  });

  const expenseCategories = categories.filter((category: any) => category.type === "expense");
  const allocatedTotal = data.reduce((sum: number, item: any) => sum + Number(item.allocatedAmount), 0);
  const monthlyTarget = Number(monthlyBudgetData?.totalBudget ?? 0);

  const refreshQueries = async () => {
    await invalidateActiveQueries(queryClient, [
      ["budgets", selectedMonth],
      ["monthly-budget", selectedMonth],
      ["dashboard", selectedMonth],
      ["reports", selectedMonth]
    ]);
  };

  const saveMonthlyBudgetMutation = useMutation({
    mutationFn: async () => {
      if (monthlyBudget === "" || Number(monthlyBudget) < 0) {
        throw new Error("Please enter a monthly budget amount of zero or more.");
      }

      await api.put("/monthly-budget", {
        month,
        year,
        totalBudget: Number(monthlyBudget)
      });
    },
    onSuccess: async () => {
      setMonthlyBudgetError(null);
      await refreshQueries();
      await showSuccessToast("Monthly budget saved");
    },
    onError: (error) => {
      const message = getErrorMessage(error, "Unable to save the monthly budget.");
      setMonthlyBudgetError(message);
      void showErrorToast(message);
    }
  });

  const saveMutation = useMutation({
    mutationFn: async () => {
      if (!form.categoryId) {
        throw new Error("Please select a category.");
      }

      if (!form.allocatedAmount || Number(form.allocatedAmount) <= 0) {
        throw new Error("Please enter an allocated amount greater than zero.");
      }

      const payload = {
        categoryId: Number(form.categoryId),
        month,
        year,
        allocatedAmount: Number(form.allocatedAmount)
      };

      if (editingId) {
        await api.put(`/budgets/${editingId}`, payload);
      } else {
        await api.post("/budgets", payload);
      }
    },
    onSuccess: async () => {
      setEditingId(null);
      setForm({ categoryId: "", allocatedAmount: "" });
      setAllocationError(null);
      await refreshQueries();
      await showSuccessToast(editingId ? "Category budget updated" : "Category budget created");
    },
    onError: (error) => {
      const message = getErrorMessage(error, "Unable to save the category budget.");
      setAllocationError(message);
      void showErrorToast(message);
    }
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: number) => api.delete(`/budgets/${id}`),
    onSuccess: async () => {
      await refreshQueries();
      await showSuccessToast("Category budget deleted");
    },
    onError: (error) => {
      const message = getErrorMessage(error, "Unable to delete the category budget.");
      setAllocationError(message);
      void showErrorToast(message);
    }
  });

  return (
    <div className="space-y-6">
      <div className="grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
        <div className="card p-5 sm:p-6 dark:border dark:border-slate-800">
          <div className="section-title">Monthly budget</div>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">Set the overall budget for {selectedMonthLabel}.</p>
          <div className="mt-6 flex flex-col gap-4 sm:flex-row">
            <label className="flex-1 text-sm text-slate-600 dark:text-slate-300">
              <span className="mb-2 inline-flex items-center gap-2 font-medium"><PiggyBank className="h-4 w-4" aria-hidden="true" />Monthly budget</span>
              <input
                className="w-full rounded-lg border border-slate-200 bg-white px-4 py-3 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
                type="number"
                name="monthlyBudget"
                inputMode="decimal"
                min="0"
                step="0.01"
                autoComplete="off"
                placeholder="25000.00"
                value={monthlyBudget}
                onChange={(event) => setMonthlyBudget(event.target.value)}
              />
            </label>
            <button
              className="inline-flex items-center justify-center gap-2 rounded-lg bg-ink px-4 py-3 text-sm font-medium text-white disabled:opacity-60 dark:bg-slate-100 dark:text-slate-950"
              onClick={() => saveMonthlyBudgetMutation.mutate()}
              disabled={saveMonthlyBudgetMutation.isPending}
            >
              <Save className="h-4 w-4" aria-hidden="true" />
              Save monthly budget
            </button>
          </div>
          {monthlyBudgetError ? <div className="mt-4 rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-600 dark:bg-rose-950/40" aria-live="polite">{monthlyBudgetError}</div> : null}
          <div className="mt-5 grid gap-3 sm:grid-cols-3">
            <div className="rounded-lg bg-mist px-4 py-4 dark:bg-slate-800">
              <div className="text-xs uppercase tracking-[0.2em] text-slate-400 dark:text-slate-500">Monthly target</div>
              <div className="tabular-nums mt-2 text-lg font-semibold text-ink dark:text-slate-100">{formatCurrency(monthlyTarget)}</div>
            </div>
            <div className="rounded-lg bg-mist px-4 py-4 dark:bg-slate-800">
              <div className="text-xs uppercase tracking-[0.2em] text-slate-400 dark:text-slate-500">Allocated</div>
              <div className="tabular-nums mt-2 text-lg font-semibold text-ink dark:text-slate-100">{formatCurrency(allocatedTotal)}</div>
            </div>
            <div className="rounded-lg bg-mist px-4 py-4 dark:bg-slate-800">
              <div className="text-xs uppercase tracking-[0.2em] text-slate-400 dark:text-slate-500">Left to allocate</div>
              <div className={`tabular-nums mt-2 text-lg font-semibold ${monthlyTarget - allocatedTotal < 0 ? "text-rose-600" : "text-emerald-600"}`}>
                {formatCurrency(monthlyTarget - allocatedTotal)}
              </div>
            </div>
          </div>
        </div>

        <div className="card p-5 sm:p-6 dark:border dark:border-slate-800">
          <div className="section-title">Category budgets</div>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">Allocate spending by category for {selectedMonthLabel}.</p>
          <div className="mt-6 grid gap-4 md:grid-cols-2">
            <label className="flex flex-col gap-2 text-sm text-slate-600 dark:text-slate-300">
              <span className="inline-flex items-center gap-2 font-medium"><Tag className="h-4 w-4" aria-hidden="true" />Category</span>
              <select className="rounded-lg border border-slate-200 bg-white px-4 py-3 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100" name="budgetCategoryId" value={form.categoryId} onChange={(event) => setForm({ ...form, categoryId: event.target.value })}>
                <option value="">Select category</option>
                {expenseCategories.map((category: any) => (
                  <option key={category.id} value={category.id}>{category.name}</option>
                ))}
              </select>
            </label>
            <label className="flex flex-col gap-2 text-sm text-slate-600 dark:text-slate-300">
              <span className="inline-flex items-center gap-2 font-medium"><CircleDollarSign className="h-4 w-4" aria-hidden="true" />Allocated amount</span>
              <input className="rounded-lg border border-slate-200 bg-white px-4 py-3 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100" type="number" name="allocatedAmount" inputMode="decimal" min="0" step="0.01" autoComplete="off" placeholder="5000.00" value={form.allocatedAmount} onChange={(event) => setForm({ ...form, allocatedAmount: event.target.value })} />
            </label>
          </div>
          {allocationError ? <div className="mt-4 rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-600 dark:bg-rose-950/40" aria-live="polite">{allocationError}</div> : null}
          <div className="mt-4 flex flex-wrap gap-3">
            <button className="inline-flex items-center gap-2 rounded-lg bg-accent px-4 py-3 text-sm font-medium text-white disabled:opacity-60" onClick={() => saveMutation.mutate()} disabled={saveMutation.isPending}>
              {editingId ? <PencilLine className="h-4 w-4" aria-hidden="true" /> : <Save className="h-4 w-4" aria-hidden="true" />}
              {editingId ? "Update category budget" : "Create category budget"}
            </button>
            {editingId ? (
              <button
                className="rounded-lg border border-slate-200 px-4 py-3 text-sm font-medium dark:border-slate-700 dark:text-slate-200"
                onClick={() => {
                  setEditingId(null);
                  setForm({ categoryId: "", allocatedAmount: "" });
                  setAllocationError(null);
                }}
              >
                Cancel
              </button>
            ) : null}
          </div>
        </div>
      </div>

      {isLoading ? (
        <LoadingState message="Loading budgets…" />
      ) : isError ? (
        <ErrorState message="Unable to load budgets for the selected month." />
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {data.length === 0 ? (
            <div className="card p-6 text-sm text-slate-500 dark:border dark:border-slate-800 dark:text-slate-400">No category budgets are set for {selectedMonthLabel} yet.</div>
          ) : (
            data.map((item: any) => {
              const spent = Number(item.spentAmount);
              const allocated = Number(item.allocatedAmount);
              const remaining = allocated - spent;
              const percent = allocated > 0 ? Math.min((spent / allocated) * 100, 100) : 0;
              const requestDelete = async () => {
                const confirmed = await confirmDestructiveAction("Delete category budget?", `Remove the budget for "${item.categoryName}"?`, "Delete");
                if (confirmed) {
                  deleteMutation.mutate(item.id);
                }
              };
              return (
                <div key={item.id} className="card p-5 dark:border dark:border-slate-800">
                  <div className="flex items-start justify-between gap-4">
                    <div className="min-w-0">
                      <div className="text-sm uppercase tracking-[0.2em] text-slate-400 dark:text-slate-500">{selectedMonthLabel}</div>
                      <div className="mt-2 truncate text-xl font-semibold text-ink dark:text-slate-100">{item.categoryName}</div>
                    </div>
                    <div className="inline-flex gap-2">
                      <button
                        className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 text-slate-700 transition hover:bg-white dark:border-slate-700 dark:text-slate-200 dark:hover:bg-slate-950"
                        aria-label={`Edit ${item.categoryName} budget`}
                        title={`Edit ${item.categoryName} budget`}
                        onClick={() => {
                          setEditingId(item.id);
                          setForm({ categoryId: String(item.categoryId), allocatedAmount: String(item.allocatedAmount) });
                          setAllocationError(null);
                        }}
                      >
                        <PencilLine className="h-4 w-4" aria-hidden="true" />
                      </button>
                      <button
                        className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-rose-200 text-rose-600 transition hover:bg-white dark:border-rose-900 dark:text-rose-300 dark:hover:bg-slate-950"
                        aria-label={`Delete ${item.categoryName} budget`}
                        title={`Delete ${item.categoryName} budget`}
                        onClick={() => void requestDelete()}
                      >
                        <Trash2 className="h-4 w-4" aria-hidden="true" />
                      </button>
                    </div>
                  </div>
                  <div className="mt-5 h-3 rounded-full bg-slate-100 dark:bg-slate-800">
                    <div className="h-3 rounded-full" style={{ width: `${percent}%`, backgroundColor: item.color }} />
                  </div>
                  <div className="mt-5 space-y-2 text-sm text-slate-500 dark:text-slate-400">
                    <div className="flex items-center justify-between"><span>Allocated</span><span className="tabular-nums font-medium text-ink dark:text-slate-100">{formatCurrency(allocated)}</span></div>
                    <div className="flex items-center justify-between"><span>Spent</span><span className="tabular-nums font-medium text-ink dark:text-slate-100">{formatCurrency(spent)}</span></div>
                    <div className="flex items-center justify-between"><span>Remaining</span><span className={`tabular-nums font-medium ${remaining < 0 ? "text-rose-600" : "text-emerald-600"}`}>{formatCurrency(remaining)}</span></div>
                  </div>
                </div>
              );
            })
          )}
        </div>
      )}
    </div>
  );
}
