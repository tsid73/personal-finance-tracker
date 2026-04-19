import { useEffect, useMemo, useState } from "react";
import { CalendarDays, ChevronDown, ChevronUp, CircleDollarSign, ClipboardPen, CreditCard, Edit3, FileText, RefreshCw, RotateCcw, Save, Tag, Trash2, UserRound } from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CardGridLoadingState, ErrorState } from "../components/PageState";
import { api } from "../lib/api";
import { confirmDestructiveAction, showErrorToast, showSuccessToast } from "../lib/alerts";
import { getErrorMessage } from "../lib/errors";
import { formatCurrency, formatHumanReadableDate, getTodayDate } from "../lib/format";
import { invalidateActiveQueries } from "../lib/query";
import { useAppShellContext } from "../shell/useAppShellContext";

type RecurringForm = {
  title: string;
  kind: "income" | "expense";
  amount: string;
  notes: string;
  merchant: string;
  accountId: string;
  categoryId: string;
  dayOfMonth: string;
  startDate: string;
  autoCreate: boolean;
  isActive: boolean;
};

export function RecurringPage() {
  const { selectedMonth } = useAppShellContext();
  const queryClient = useQueryClient();
  const todayDate = getTodayDate();
  const [editingRecurringId, setEditingRecurringId] = useState<number | null>(null);
  const [recurringFormError, setRecurringFormError] = useState<string | null>(null);
  const [isFormExpanded, setIsFormExpanded] = useState(true);
  const [recurringForm, setRecurringForm] = useState<RecurringForm>({
    title: "",
    kind: "expense",
    amount: "",
    notes: "",
    merchant: "",
    accountId: "",
    categoryId: "",
    dayOfMonth: String(new Date(`${todayDate}T00:00:00`).getDate()),
    startDate: todayDate,
    autoCreate: true,
    isActive: true
  });

  const { data: categories = [] } = useQuery({
    queryKey: ["categories"],
    queryFn: async () => (await api.get("/categories")).data
  });

  const { data: accounts = [] } = useQuery({
    queryKey: ["accounts"],
    queryFn: async () => (await api.get("/accounts")).data
  });

  const { data: recurringTransactions = [], isLoading, isError } = useQuery({
    queryKey: ["recurring-transactions"],
    queryFn: async () => (await api.get("/recurring-transactions")).data
  });

  const { data: activity = [] } = useQuery({
    queryKey: ["activity"],
    queryFn: async () => (await api.get("/activity")).data
  });

  const filteredRecurringCategories = useMemo(
    () => categories.filter((category: any) => category.type === recurringForm.kind),
    [categories, recurringForm.kind]
  );

  const resetRecurringForm = () => {
    setEditingRecurringId(null);
    setRecurringFormError(null);
    setRecurringForm({
      title: "",
      kind: "expense",
      amount: "",
      notes: "",
      merchant: "",
      accountId: "",
      categoryId: "",
      dayOfMonth: String(new Date(`${todayDate}T00:00:00`).getDate()),
      startDate: todayDate,
      autoCreate: true,
      isActive: true
    });
  };

  const refreshQueries = async () => {
    await invalidateActiveQueries(queryClient, [
      ["transactions", selectedMonth],
      ["dashboard", selectedMonth],
      ["budgets", selectedMonth],
      ["reports", selectedMonth],
      ["recurring-transactions"],
      ["activity"]
    ]);
  };

  const validateRecurringForm = () => {
    if (recurringForm.title.trim().length < 2) return "Please enter a recurring title with at least 2 characters.";
    if (!recurringForm.accountId) return "Please select an account for the recurring transaction.";
    if (!recurringForm.categoryId) return "Please select a category for the recurring transaction.";
    if (!recurringForm.amount || Number(recurringForm.amount) <= 0) return "Please enter a recurring amount greater than zero.";
    if (!recurringForm.dayOfMonth || Number(recurringForm.dayOfMonth) < 1 || Number(recurringForm.dayOfMonth) > 31) return "Day of month must be between 1 and 31.";
    if (!recurringForm.startDate) return "Please select a start date for the recurring transaction.";
    return null;
  };

  const syncRecurringMutation = useMutation({
    mutationFn: async (_options?: { silent?: boolean }) => (await api.post("/recurring-transactions/sync")).data,
    onSuccess: async (result, variables) => {
      await refreshQueries();
      if (!variables?.silent) {
        await showSuccessToast(
          result.createdCount > 0
            ? `Created ${result.createdCount} due recurring transaction${result.createdCount === 1 ? "" : "s"}`
            : "No due recurring transactions to create"
        );
      }
    },
    onError: (error) => {
      const message = getErrorMessage(error, "Unable to sync recurring transactions.");
      setRecurringFormError(message);
      void showErrorToast(message);
    }
  });

  useEffect(() => {
    void syncRecurringMutation.mutateAsync({ silent: true });
  }, []);

  const saveRecurringMutation = useMutation({
    mutationFn: async () => {
      const validationMessage = validateRecurringForm();
      if (validationMessage) {
        throw new Error(validationMessage);
      }

      const payload = {
        ...recurringForm,
        title: recurringForm.title.trim(),
        notes: recurringForm.notes.trim() || null,
        merchant: recurringForm.merchant.trim() || null,
        amount: Number(recurringForm.amount),
        accountId: Number(recurringForm.accountId),
        categoryId: Number(recurringForm.categoryId),
        dayOfMonth: Number(recurringForm.dayOfMonth)
      };

      if (editingRecurringId) {
        await api.put(`/recurring-transactions/${editingRecurringId}`, payload);
      } else {
        await api.post("/recurring-transactions", payload);
      }
    },
    onSuccess: async () => {
      const shouldAutoSync = recurringForm.autoCreate && recurringForm.isActive;
      const wasEditing = editingRecurringId;
      resetRecurringForm();
      await refreshQueries();
      if (shouldAutoSync) {
        await syncRecurringMutation.mutateAsync({ silent: true });
      }
      await showSuccessToast(wasEditing ? "Recurring transaction updated" : "Recurring transaction added");
    },
    onError: (error) => {
      const message = getErrorMessage(error, "Unable to save the recurring transaction.");
      setRecurringFormError(message);
      void showErrorToast(message);
    }
  });

  const deleteRecurringMutation = useMutation({
    mutationFn: async (id: number) => api.delete(`/recurring-transactions/${id}`),
    onSuccess: async () => {
      resetRecurringForm();
      await refreshQueries();
      await showSuccessToast("Recurring transaction deleted");
    },
    onError: (error) => {
      const message = getErrorMessage(error, "Unable to delete the recurring transaction.");
      setRecurringFormError(message);
      void showErrorToast(message);
    }
  });

  const startRecurringEdit = (item: any) => {
    setIsFormExpanded(true);
    setEditingRecurringId(item.id);
    setRecurringFormError(null);
    setRecurringForm({
      title: item.title,
      kind: item.kind,
      amount: String(item.amount),
      notes: item.notes ?? "",
      merchant: item.merchant ?? "",
      accountId: String(item.accountId),
      categoryId: String(item.categoryId),
      dayOfMonth: String(item.dayOfMonth),
      startDate: item.startDate,
      autoCreate: Boolean(item.autoCreate),
      isActive: Boolean(item.isActive)
    });
  };

  const requestDeleteRecurring = async (item: any) => {
    const confirmed = await confirmDestructiveAction("Delete recurring transaction?", `Delete recurring schedule "${item.title}"?`, "Delete");
    if (!confirmed) {
      return;
    }

    deleteRecurringMutation.mutate(item.id);
  };

  const toggleRecurringActive = async (item: any) => {
    try {
      await api.put(`/recurring-transactions/${item.id}`, {
        title: item.title,
        kind: item.kind,
        amount: Number(item.amount),
        notes: item.notes ?? null,
        merchant: item.merchant ?? null,
        accountId: Number(item.accountId),
        categoryId: Number(item.categoryId),
        dayOfMonth: Number(item.dayOfMonth),
        startDate: item.startDate,
        autoCreate: Boolean(item.autoCreate),
        isActive: !item.isActive
      });
      await refreshQueries();
      await showSuccessToast(!item.isActive ? "Recurring transaction resumed" : "Recurring transaction paused");
    } catch (error) {
      const message = getErrorMessage(error, "Unable to update the recurring transaction.");
      setRecurringFormError(message);
      void showErrorToast(message);
    }
  };

  return (
    <div className="space-y-6">
      <div className="card p-5 sm:p-6 dark:border dark:border-slate-800">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <div className="section-title">Recurring transactions</div>
            <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
              Manage monthly salary, rent, EMI, and subscription schedules. Due schedules can auto-create transactions or stay marked due.
            </p>
          </div>
          <div className="flex flex-wrap gap-3">
            <button
              className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-4 py-3 text-sm font-medium dark:border-slate-700 dark:text-slate-200"
              onClick={() => syncRecurringMutation.mutate({ silent: false })}
              disabled={syncRecurringMutation.isPending}
            >
              <RefreshCw className={`h-4 w-4 ${syncRecurringMutation.isPending ? "animate-spin" : ""}`} aria-hidden="true" />
              Create due now
            </button>
            <button
              className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-4 py-3 text-sm font-medium dark:border-slate-700 dark:text-slate-200"
              onClick={() => setIsFormExpanded((current) => !current)}
            >
              {isFormExpanded ? <ChevronUp className="h-4 w-4" aria-hidden="true" /> : <ChevronDown className="h-4 w-4" aria-hidden="true" />}
              {isFormExpanded ? "Hide form" : "Show form"}
            </button>
            {editingRecurringId ? (
              <button className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-4 py-3 text-sm font-medium dark:border-slate-700 dark:text-slate-200" onClick={resetRecurringForm}>
                <RotateCcw className="h-4 w-4" aria-hidden="true" />
                Cancel edit
              </button>
            ) : null}
          </div>
        </div>

        {isFormExpanded ? (
          <>
            <div className="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
              <label className="flex flex-col gap-2 text-sm text-slate-600 dark:text-slate-300">
                <span className="inline-flex items-center gap-2 font-medium"><ClipboardPen className="h-4 w-4" aria-hidden="true" />Title</span>
                <input className="rounded-lg border border-slate-200 bg-white px-4 py-3 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100" placeholder="Rent, salary, EMI…" value={recurringForm.title} onChange={(event) => setRecurringForm({ ...recurringForm, title: event.target.value })} />
              </label>
              <label className="flex flex-col gap-2 text-sm text-slate-600 dark:text-slate-300">
                <span className="inline-flex items-center gap-2 font-medium"><Tag className="h-4 w-4" aria-hidden="true" />Type</span>
                <select className="rounded-lg border border-slate-200 bg-white px-4 py-3 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100" value={recurringForm.kind} onChange={(event) => setRecurringForm({ ...recurringForm, kind: event.target.value as "income" | "expense", categoryId: "" })}>
                  <option value="expense">Expense</option>
                  <option value="income">Income</option>
                </select>
              </label>
              <label className="flex flex-col gap-2 text-sm text-slate-600 dark:text-slate-300">
                <span className="inline-flex items-center gap-2 font-medium"><CircleDollarSign className="h-4 w-4" aria-hidden="true" />Amount</span>
                <input className="rounded-lg border border-slate-200 bg-white px-4 py-3 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100" placeholder="25000.00" type="number" min="0" step="0.01" value={recurringForm.amount} onChange={(event) => setRecurringForm({ ...recurringForm, amount: event.target.value })} />
              </label>
              <label className="flex flex-col gap-2 text-sm text-slate-600 dark:text-slate-300">
                <span className="inline-flex items-center gap-2 font-medium"><CalendarDays className="h-4 w-4" aria-hidden="true" />Day of month</span>
                <input className="rounded-lg border border-slate-200 bg-white px-4 py-3 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100" type="number" min="1" max="31" value={recurringForm.dayOfMonth} onChange={(event) => setRecurringForm({ ...recurringForm, dayOfMonth: event.target.value })} />
              </label>
              <label className="flex flex-col gap-2 text-sm text-slate-600 dark:text-slate-300">
                <span className="inline-flex items-center gap-2 font-medium"><CalendarDays className="h-4 w-4" aria-hidden="true" />Start date</span>
                <input className="rounded-lg border border-slate-200 bg-white px-4 py-3 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100" type="date" value={recurringForm.startDate} onChange={(event) => setRecurringForm({ ...recurringForm, startDate: event.target.value })} />
              </label>
              <label className="flex flex-col gap-2 text-sm text-slate-600 dark:text-slate-300">
                <span className="inline-flex items-center gap-2 font-medium"><CreditCard className="h-4 w-4" aria-hidden="true" />Account</span>
                <select className="rounded-lg border border-slate-200 bg-white px-4 py-3 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100" value={recurringForm.accountId} onChange={(event) => setRecurringForm({ ...recurringForm, accountId: event.target.value })}>
                  <option value="">Select account</option>
                  {accounts.map((account: any) => (
                    <option key={account.id} value={account.id}>{account.label}</option>
                  ))}
                </select>
              </label>
              <label className="flex flex-col gap-2 text-sm text-slate-600 dark:text-slate-300">
                <span className="inline-flex items-center gap-2 font-medium"><Tag className="h-4 w-4" aria-hidden="true" />Category</span>
                <select className="rounded-lg border border-slate-200 bg-white px-4 py-3 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100" value={recurringForm.categoryId} onChange={(event) => setRecurringForm({ ...recurringForm, categoryId: event.target.value })}>
                  <option value="">Select category</option>
                  {filteredRecurringCategories.map((category: any) => (
                    <option key={category.id} value={category.id}>{category.name}</option>
                  ))}
                </select>
              </label>
              <label className="flex flex-col gap-2 text-sm text-slate-600 dark:text-slate-300">
                <span className="inline-flex items-center gap-2 font-medium"><UserRound className="h-4 w-4" aria-hidden="true" />Merchant</span>
                <input className="rounded-lg border border-slate-200 bg-white px-4 py-3 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100" placeholder="Optional merchant" value={recurringForm.merchant} onChange={(event) => setRecurringForm({ ...recurringForm, merchant: event.target.value })} />
              </label>
              <label className="flex flex-col gap-2 text-sm text-slate-600 dark:text-slate-300 md:col-span-2 xl:col-span-4">
                <span className="inline-flex items-center gap-2 font-medium"><FileText className="h-4 w-4" aria-hidden="true" />Notes</span>
                <input className="rounded-lg border border-slate-200 bg-white px-4 py-3 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100" placeholder="Optional notes" value={recurringForm.notes} onChange={(event) => setRecurringForm({ ...recurringForm, notes: event.target.value })} />
              </label>
            </div>

            <div className="mt-4 flex flex-wrap gap-6 text-sm text-slate-600 dark:text-slate-300">
              <label className="inline-flex items-center gap-3">
                <input type="checkbox" checked={recurringForm.autoCreate} onChange={(event) => setRecurringForm({ ...recurringForm, autoCreate: event.target.checked })} />
                Auto-create when due
              </label>
              <label className="inline-flex items-center gap-3">
                <input type="checkbox" checked={recurringForm.isActive} onChange={(event) => setRecurringForm({ ...recurringForm, isActive: event.target.checked })} />
                Schedule is active
              </label>
            </div>

            {recurringFormError ? <div className="mt-4 rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-600 dark:bg-rose-950/40" aria-live="polite">{recurringFormError}</div> : null}

            <button className="mt-4 inline-flex items-center gap-2 rounded-lg bg-ink px-4 py-3 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-60 dark:bg-slate-100 dark:text-slate-950" onClick={() => saveRecurringMutation.mutate()} disabled={saveRecurringMutation.isPending}>
              {editingRecurringId ? <Save className="h-4 w-4" aria-hidden="true" /> : <ClipboardPen className="h-4 w-4" aria-hidden="true" />}
              {editingRecurringId ? "Update recurring transaction" : "Add recurring transaction"}
            </button>
          </>
        ) : null}
      </div>

      <div className="card p-5 sm:p-6 dark:border dark:border-slate-800">
        <div className="section-title">Schedules</div>
        <div className="mt-6 grid gap-4 xl:grid-cols-2">
          {isLoading ? (
            <CardGridLoadingState count={4} />
          ) : isError ? (
            <ErrorState message="Unable to load recurring transactions." />
          ) : recurringTransactions.length === 0 ? (
            <div className="rounded-lg bg-mist px-4 py-6 text-sm text-slate-500 dark:bg-slate-800 dark:text-slate-400">
              No recurring transactions yet. Add monthly salary, rent, EMI, or subscription schedules here.
            </div>
          ) : (
            recurringTransactions.map((item: any) => (
              <div key={item.id} className="rounded-lg border border-slate-100 bg-mist p-4 dark:border-slate-800 dark:bg-slate-800">
                <div className="flex items-start justify-between gap-4">
                  <div className="min-w-0">
                    <div className="truncate font-medium text-ink dark:text-slate-100">{item.title}</div>
                    <div className="mt-1 text-sm text-slate-500 dark:text-slate-400">{item.categoryName} • {item.accountLabel}</div>
                    <div className="mt-2 flex flex-wrap gap-2 text-xs">
                      <span className={`rounded-full px-3 py-1 font-medium ${item.status === "due" ? "bg-amber-100 text-amber-700 dark:bg-amber-950/40 dark:text-amber-200" : item.status === "inactive" ? "bg-slate-200 text-slate-600 dark:bg-slate-900 dark:text-slate-300" : "bg-emerald-100 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-200"}`}>
                        {item.status === "due" ? "Due" : item.status === "inactive" ? "Inactive" : "Upcoming"}
                      </span>
                      <span className="rounded-full bg-white px-3 py-1 font-medium text-slate-500 dark:bg-slate-950 dark:text-slate-300">Every month on day {item.dayOfMonth}</span>
                      <span className="rounded-full bg-white px-3 py-1 font-medium text-slate-500 dark:bg-slate-950 dark:text-slate-300">{item.autoCreate ? "Auto-create" : "Mark due only"}</span>
                    </div>
                  </div>
                  <div className={`tabular-nums shrink-0 font-semibold ${item.kind === "income" ? "text-emerald-600" : "text-rose-600"}`}>
                    {item.kind === "income" ? "+" : "-"}{formatCurrency(Number(item.amount))}
                  </div>
                </div>
                <div className="mt-4 grid gap-3 text-sm text-slate-500 dark:text-slate-400 sm:grid-cols-3">
                  <div>
                    <div className="text-xs uppercase tracking-[0.2em]">Start</div>
                    <div className="mt-1 tabular-nums text-ink dark:text-slate-100">{formatHumanReadableDate(item.startDate)}</div>
                  </div>
                  <div>
                    <div className="text-xs uppercase tracking-[0.2em]">Next due</div>
                    <div className="mt-1 tabular-nums text-ink dark:text-slate-100">{formatHumanReadableDate(item.nextDueDate)}</div>
                  </div>
                  <div>
                    <div className="text-xs uppercase tracking-[0.2em]">Last created</div>
                    <div className="mt-1 tabular-nums text-ink dark:text-slate-100">{item.lastGeneratedDate ? formatHumanReadableDate(item.lastGeneratedDate) : "Not created yet"}</div>
                  </div>
                </div>
                <div className="mt-4 flex flex-wrap items-center gap-2">
                  <button className="rounded-lg border border-slate-200 px-3 py-2 text-xs font-medium dark:border-slate-700 dark:text-slate-200" onClick={() => toggleRecurringActive(item)}>
                    {item.isActive ? "Turn off" : "Turn on"}
                  </button>
                  <button
                    className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 text-slate-700 transition hover:bg-white dark:border-slate-700 dark:text-slate-200 dark:hover:bg-slate-950"
                    aria-label={`Edit ${item.title}`}
                    title={`Edit ${item.title}`}
                    onClick={() => startRecurringEdit(item)}
                  >
                    <Edit3 className="h-4 w-4" aria-hidden="true" />
                  </button>
                  <button
                    className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-rose-200 text-rose-600 transition hover:bg-white dark:border-rose-900 dark:text-rose-300 dark:hover:bg-slate-950"
                    aria-label={`Delete ${item.title}`}
                    title={`Delete ${item.title}`}
                    onClick={() => void requestDeleteRecurring(item)}
                  >
                    <Trash2 className="h-4 w-4" aria-hidden="true" />
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      <div className="card p-5 sm:p-6 dark:border dark:border-slate-800">
        <div className="section-title">Recent activity</div>
        <div className="mt-4 space-y-3">
          {activity.filter((item: any) => item.entityType === "recurring_transaction").length === 0 ? (
            <div className="rounded-lg bg-mist px-4 py-6 text-sm text-slate-500 dark:bg-slate-800 dark:text-slate-400">Recurring transaction history will appear here after updates.</div>
          ) : (
            activity
              .filter((item: any) => item.entityType === "recurring_transaction")
              .slice(0, 10)
              .map((item: any) => (
                <div key={item.id} className="rounded-lg bg-mist px-4 py-3 dark:bg-slate-800">
                  <div className="text-sm font-medium text-ink dark:text-slate-100">{item.title}</div>
                  <div className="mt-1 text-xs text-slate-500 dark:text-slate-400">{item.createdAt}</div>
                  {item.note ? <div className="mt-2 text-sm text-slate-600 dark:text-slate-300">{item.note}</div> : null}
                </div>
              ))
          )}
        </div>
      </div>
    </div>
  );
}
