import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ErrorState, LoadingState } from "../components/PageState";
import { api } from "../lib/api";
import { getErrorMessage } from "../lib/errors";
import { formatCurrency } from "../lib/format";
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
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["budgets", selectedMonth] }),
      queryClient.invalidateQueries({ queryKey: ["monthly-budget", selectedMonth] }),
      queryClient.invalidateQueries({ queryKey: ["dashboard", selectedMonth] }),
      queryClient.invalidateQueries({ queryKey: ["reports", selectedMonth] })
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
    },
    onError: (error) => setMonthlyBudgetError(getErrorMessage(error, "Unable to save the monthly budget."))
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
    },
    onError: (error) => setAllocationError(getErrorMessage(error, "Unable to save the category budget."))
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: number) => api.delete(`/budgets/${id}`),
    onSuccess: refreshQueries,
    onError: (error) => setAllocationError(getErrorMessage(error, "Unable to delete the category budget."))
  });

  return (
    <div className="space-y-6">
      <div className="grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
        <div className="card p-5 sm:p-6">
          <div className="section-title">Monthly budget</div>
          <p className="mt-1 text-sm text-slate-500">Set the overall budget for {selectedMonthLabel}.</p>
          <div className="mt-6 flex flex-col gap-4 sm:flex-row">
            <input
              className="rounded-2xl border border-slate-200 px-4 py-3"
              type="number"
              min="0"
              step="0.01"
              placeholder="Monthly budget"
              value={monthlyBudget}
              onChange={(event) => setMonthlyBudget(event.target.value)}
            />
            <button
              className="rounded-2xl bg-ink px-4 py-3 text-sm font-medium text-white disabled:opacity-60"
              onClick={() => saveMonthlyBudgetMutation.mutate()}
              disabled={saveMonthlyBudgetMutation.isPending}
            >
              Save monthly budget
            </button>
          </div>
          {monthlyBudgetError ? <div className="mt-4 rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-600">{monthlyBudgetError}</div> : null}
          <div className="mt-5 grid gap-3 sm:grid-cols-3">
            <div className="rounded-2xl bg-mist px-4 py-4">
              <div className="text-xs uppercase tracking-[0.2em] text-slate-400">Monthly target</div>
              <div className="mt-2 text-lg font-semibold text-ink">{formatCurrency(monthlyTarget)}</div>
            </div>
            <div className="rounded-2xl bg-mist px-4 py-4">
              <div className="text-xs uppercase tracking-[0.2em] text-slate-400">Allocated</div>
              <div className="mt-2 text-lg font-semibold text-ink">{formatCurrency(allocatedTotal)}</div>
            </div>
            <div className="rounded-2xl bg-mist px-4 py-4">
              <div className="text-xs uppercase tracking-[0.2em] text-slate-400">Left to allocate</div>
              <div className={`mt-2 text-lg font-semibold ${monthlyTarget - allocatedTotal < 0 ? "text-rose-600" : "text-emerald-600"}`}>
                {formatCurrency(monthlyTarget - allocatedTotal)}
              </div>
            </div>
          </div>
        </div>

        <div className="card p-5 sm:p-6">
          <div className="section-title">Category budgets</div>
          <p className="mt-1 text-sm text-slate-500">Allocate spending by category for {selectedMonthLabel}.</p>
          <div className="mt-6 grid gap-4 md:grid-cols-2">
            <select className="rounded-2xl border border-slate-200 px-4 py-3" value={form.categoryId} onChange={(event) => setForm({ ...form, categoryId: event.target.value })}>
              <option value="">Select category</option>
              {expenseCategories.map((category: any) => (
                <option key={category.id} value={category.id}>{category.name}</option>
              ))}
            </select>
            <input className="rounded-2xl border border-slate-200 px-4 py-3" type="number" min="0" step="0.01" placeholder="Allocated amount" value={form.allocatedAmount} onChange={(event) => setForm({ ...form, allocatedAmount: event.target.value })} />
          </div>
          {allocationError ? <div className="mt-4 rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-600">{allocationError}</div> : null}
          <div className="mt-4 flex flex-wrap gap-3">
            <button className="rounded-2xl bg-accent px-4 py-3 text-sm font-medium text-white disabled:opacity-60" onClick={() => saveMutation.mutate()} disabled={saveMutation.isPending}>
              {editingId ? "Update category budget" : "Create category budget"}
            </button>
            {editingId ? (
              <button
                className="rounded-2xl border border-slate-200 px-4 py-3 text-sm font-medium"
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
        <LoadingState message="Loading budgets..." />
      ) : isError ? (
        <ErrorState message="Unable to load budgets for the selected month." />
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {data.length === 0 ? (
            <div className="card p-6 text-sm text-slate-500">No category budgets are set for {selectedMonthLabel} yet.</div>
          ) : (
            data.map((item: any) => {
              const spent = Number(item.spentAmount);
              const allocated = Number(item.allocatedAmount);
              const remaining = allocated - spent;
              const percent = allocated > 0 ? Math.min((spent / allocated) * 100, 100) : 0;
              return (
                <div key={item.id} className="card p-5">
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <div className="text-sm uppercase tracking-[0.2em] text-slate-400">{selectedMonthLabel}</div>
                      <div className="mt-2 text-xl font-semibold text-ink">{item.categoryName}</div>
                    </div>
                    <div className="inline-flex gap-2">
                      <button
                        className="rounded-xl border border-slate-200 px-3 py-2 text-xs font-medium"
                        onClick={() => {
                          setEditingId(item.id);
                          setForm({ categoryId: String(item.categoryId), allocatedAmount: String(item.allocatedAmount) });
                          setAllocationError(null);
                        }}
                      >
                        Edit
                      </button>
                      <button className="rounded-xl border border-rose-200 px-3 py-2 text-xs font-medium text-rose-600" onClick={() => deleteMutation.mutate(item.id)}>
                        Delete
                      </button>
                    </div>
                  </div>
                  <div className="mt-5 h-3 rounded-full bg-slate-100">
                    <div className="h-3 rounded-full" style={{ width: `${percent}%`, backgroundColor: item.color }} />
                  </div>
                  <div className="mt-5 space-y-2 text-sm text-slate-500">
                    <div className="flex items-center justify-between"><span>Allocated</span><span className="font-medium text-ink">{formatCurrency(allocated)}</span></div>
                    <div className="flex items-center justify-between"><span>Spent</span><span className="font-medium text-ink">{formatCurrency(spent)}</span></div>
                    <div className="flex items-center justify-between"><span>Remaining</span><span className={`font-medium ${remaining < 0 ? "text-rose-600" : "text-emerald-600"}`}>{formatCurrency(remaining)}</span></div>
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
