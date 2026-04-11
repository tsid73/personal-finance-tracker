import { useEffect, useMemo, useRef, useState } from "react";
import { CalendarDays, CircleDollarSign, ClipboardPen, CreditCard, Edit3, FileText, RotateCcw, Save, Tag, Trash2, UserRound } from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import { ErrorState, LoadingState } from "../components/PageState";
import { api } from "../lib/api";
import { confirmDestructiveAction, showErrorToast, showSuccessToast } from "../lib/alerts";
import { getErrorMessage } from "../lib/errors";
import { formatCurrency } from "../lib/format";
import { getTotalPages, getVisibleRange } from "../lib/pagination";
import { invalidateActiveQueries } from "../lib/query";
import { useAppShellContext } from "../shell/useAppShellContext";

type TransactionForm = {
  title: string;
  kind: "income" | "expense";
  amount: string;
  notes: string;
  merchant: string;
  transactionDate: string;
  accountId: string;
  categoryId: string;
};

export function TransactionsPage() {
  const perPage = 10;
  const { selectedMonth, selectedMonthLabel } = useAppShellContext();
  const [searchParams, setSearchParams] = useSearchParams();
  const queryClient = useQueryClient();
  const titleInputRef = useRef<HTMLInputElement | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [form, setForm] = useState<TransactionForm>({
    title: "",
    kind: "expense",
    amount: "",
    notes: "",
    merchant: "",
    transactionDate: `${selectedMonth}-01`,
    accountId: "",
    categoryId: ""
  });

  useEffect(() => {
    if (!editingId) {
      setForm((current) => ({ ...current, transactionDate: `${selectedMonth}-01` }));
    }
  }, [selectedMonth, editingId]);

  useEffect(() => {
    if (searchParams.get("addTxn") === "1") {
      titleInputRef.current?.focus();
      const nextParams = new URLSearchParams(searchParams);
      nextParams.delete("addTxn");
      setSearchParams(nextParams, { replace: true });
    }
  }, [searchParams, setSearchParams]);

  const parsedPage = Number(searchParams.get("page") ?? "1");
  const currentPage = Number.isFinite(parsedPage) && parsedPage > 0 ? parsedPage : 1;

  const { data, isLoading, isError } = useQuery({
    queryKey: ["transactions", selectedMonth, currentPage, perPage],
    queryFn: async () => (await api.get("/transactions", { params: { month: selectedMonth, page: currentPage, perPage } })).data
  });

  const { data: categories = [] } = useQuery({
    queryKey: ["categories"],
    queryFn: async () => (await api.get("/categories")).data
  });

  const { data: accounts = [] } = useQuery({
    queryKey: ["accounts"],
    queryFn: async () => (await api.get("/accounts")).data
  });

  const filteredCategories = useMemo(
    () => categories.filter((category: any) => category.type === form.kind),
    [categories, form.kind]
  );

  const validateForm = () => {
    if (form.title.trim().length < 2) return "Please enter a transaction title with at least 2 characters.";
    if (!form.accountId) return "Please select an account.";
    if (!form.categoryId) return "Please select a category.";
    if (!form.amount || Number(form.amount) <= 0) return "Please enter an amount greater than zero.";
    if (!form.transactionDate.startsWith(selectedMonth)) return `Transaction date must fall within ${selectedMonthLabel}.`;
    return null;
  };

  const resetForm = () => {
    setEditingId(null);
    setFormError(null);
    setForm({
      title: "",
      kind: "expense",
      amount: "",
      notes: "",
      merchant: "",
      transactionDate: `${selectedMonth}-01`,
      accountId: "",
      categoryId: ""
    });
  };

  const refreshQueries = async () => {
    await invalidateActiveQueries(queryClient, [
      ["transactions", selectedMonth],
      ["dashboard", selectedMonth],
      ["budgets", selectedMonth],
      ["reports", selectedMonth]
    ]);
  };

  const saveMutation = useMutation({
    mutationFn: async () => {
      const validationMessage = validateForm();
      if (validationMessage) {
        throw new Error(validationMessage);
      }

      const payload = {
        ...form,
        title: form.title.trim(),
        notes: form.notes.trim() || null,
        merchant: form.merchant.trim() || null,
        amount: Number(form.amount),
        accountId: Number(form.accountId),
        categoryId: Number(form.categoryId)
      };

      if (editingId) {
        await api.put(`/transactions/${editingId}`, payload);
      } else {
        await api.post("/transactions", payload);
      }
    },
    onSuccess: async () => {
      resetForm();
      await refreshQueries();
      await showSuccessToast(editingId ? "Transaction updated" : "Transaction added");
    },
    onError: (error) => {
      const message = getErrorMessage(error, "Unable to save the transaction.");
      setFormError(message);
      void showErrorToast(message);
    }
  });

  const deleteMutation = useMutation({
    mutationFn: async (item: any) => {
      await api.delete(`/transactions/${item.id}`);
      return item;
    },
    onSuccess: async (item) => {
      setRecentlyDeleted({
        title: item.title,
        kind: item.kind,
        amount: Number(item.amount),
        notes: item.notes ?? null,
        merchant: item.merchant ?? null,
        transactionDate: item.transactionDate,
        accountId: Number(item.accountId),
        categoryId: Number(item.categoryId)
      });
      await refreshQueries();
      await showSuccessToast("Transaction deleted");
    },
    onError: (error) => {
      const message = getErrorMessage(error, "Unable to delete the transaction.");
      setFormError(message);
      void showErrorToast(message);
    }
  });

  const undoDeleteMutation = useMutation({
    mutationFn: async (payload: any) => api.post("/transactions", payload),
    onSuccess: async () => {
      setRecentlyDeleted(null);
      await refreshQueries();
      await showSuccessToast("Transaction restored");
    },
    onError: (error) => {
      const message = getErrorMessage(error, "Unable to restore the deleted transaction.");
      setFormError(message);
      void showErrorToast(message);
    }
  });

  const [recentlyDeleted, setRecentlyDeleted] = useState<any | null>(null);

  useEffect(() => {
    if (!recentlyDeleted) {
      return undefined;
    }

    const timeoutId = window.setTimeout(() => {
      setRecentlyDeleted(null);
    }, 8000);

    return () => window.clearTimeout(timeoutId);
  }, [recentlyDeleted]);

  const startEdit = (item: any) => {
    setEditingId(item.id);
    setFormError(null);
    setForm({
      title: item.title,
      kind: item.kind,
      amount: String(item.amount),
      notes: item.notes ?? "",
      merchant: item.merchant ?? "",
      transactionDate: item.transactionDate,
      accountId: String(item.accountId),
      categoryId: String(item.categoryId)
    });
  };

  const transactions = data?.items ?? [];
  const pagination = data?.pagination ?? { page: currentPage, perPage, totalItems: 0, totalPages: 1 };
  const totalPages = getTotalPages(pagination.totalItems, pagination.perPage);

  useEffect(() => {
    if (!data) {
      return;
    }

    if (currentPage > data.pagination.totalPages) {
      const nextParams = new URLSearchParams(searchParams);
      nextParams.set("page", String(data.pagination.totalPages));
      nextParams.set("month", selectedMonth);
      setSearchParams(nextParams, { replace: true });
    }
  }, [currentPage, data, searchParams, selectedMonth, setSearchParams]);

  const setPage = (page: number) => {
    const nextParams = new URLSearchParams(searchParams);
    nextParams.set("page", String(page));
    nextParams.set("month", selectedMonth);
    setSearchParams(nextParams, { replace: true });
  };

  const requestDelete = async (item: any) => {
    const confirmed = await confirmDestructiveAction("Delete transaction?", `Delete "${item.title}" from ${selectedMonthLabel}?`, "Delete");
    if (!confirmed) {
      return;
    }

    deleteMutation.mutate(item);
  };

  return (
    <div className="space-y-6">
      <div className="card p-5 sm:p-6 dark:border dark:border-slate-800">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <div className="section-title">Transactions</div>
            <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">Add, edit, and delete entries for {selectedMonthLabel}.</p>
          </div>
          {editingId ? (
            <button className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-4 py-3 text-sm font-medium dark:border-slate-700 dark:text-slate-200" onClick={resetForm}>
              <RotateCcw className="h-4 w-4" aria-hidden="true" />
              Cancel edit
            </button>
          ) : null}
        </div>

        <div className="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <label className="flex flex-col gap-2 text-sm text-slate-600 dark:text-slate-300">
            <span className="inline-flex items-center gap-2 font-medium"><ClipboardPen className="h-4 w-4" aria-hidden="true" />Title</span>
            <input ref={titleInputRef} className="rounded-lg border border-slate-200 bg-white px-4 py-3 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100" placeholder="Salary payment…" name="title" autoComplete="off" value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} />
          </label>
          <label className="flex flex-col gap-2 text-sm text-slate-600 dark:text-slate-300">
            <span className="inline-flex items-center gap-2 font-medium"><Tag className="h-4 w-4" aria-hidden="true" />Type</span>
            <select className="rounded-lg border border-slate-200 bg-white px-4 py-3 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100" name="kind" value={form.kind} onChange={(event) => setForm({ ...form, kind: event.target.value as "income" | "expense", categoryId: "" })}>
              <option value="expense">Expense</option>
              <option value="income">Income</option>
            </select>
          </label>
          <label className="flex flex-col gap-2 text-sm text-slate-600 dark:text-slate-300">
            <span className="inline-flex items-center gap-2 font-medium"><CircleDollarSign className="h-4 w-4" aria-hidden="true" />Amount</span>
            <input className="rounded-lg border border-slate-200 bg-white px-4 py-3 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100" placeholder="1200.00" name="amount" type="number" inputMode="decimal" autoComplete="off" min="0" step="0.01" value={form.amount} onChange={(event) => setForm({ ...form, amount: event.target.value })} />
          </label>
          <label className="flex flex-col gap-2 text-sm text-slate-600 dark:text-slate-300">
            <span className="inline-flex items-center gap-2 font-medium"><CalendarDays className="h-4 w-4" aria-hidden="true" />Date</span>
            <input className="rounded-lg border border-slate-200 bg-white px-4 py-3 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100" name="transactionDate" type="date" value={form.transactionDate} onChange={(event) => setForm({ ...form, transactionDate: event.target.value })} />
          </label>
          <label className="flex flex-col gap-2 text-sm text-slate-600 dark:text-slate-300">
            <span className="inline-flex items-center gap-2 font-medium"><CreditCard className="h-4 w-4" aria-hidden="true" />Account</span>
            <select className="rounded-lg border border-slate-200 bg-white px-4 py-3 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100" name="accountId" value={form.accountId} onChange={(event) => setForm({ ...form, accountId: event.target.value })}>
              <option value="">Select account</option>
              {accounts.map((account: any) => (
                <option key={account.id} value={account.id}>{account.label}</option>
              ))}
            </select>
          </label>
          <label className="flex flex-col gap-2 text-sm text-slate-600 dark:text-slate-300">
            <span className="inline-flex items-center gap-2 font-medium"><Tag className="h-4 w-4" aria-hidden="true" />Category</span>
            <select className="rounded-lg border border-slate-200 bg-white px-4 py-3 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100" name="categoryId" value={form.categoryId} onChange={(event) => setForm({ ...form, categoryId: event.target.value })}>
              <option value="">Select category</option>
              {filteredCategories.map((category: any) => (
                <option key={category.id} value={category.id}>{category.name}</option>
              ))}
            </select>
          </label>
          <label className="flex flex-col gap-2 text-sm text-slate-600 dark:text-slate-300">
            <span className="inline-flex items-center gap-2 font-medium"><UserRound className="h-4 w-4" aria-hidden="true" />Merchant</span>
            <input className="rounded-lg border border-slate-200 bg-white px-4 py-3 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100" placeholder="Corner market…" name="merchant" autoComplete="off" value={form.merchant} onChange={(event) => setForm({ ...form, merchant: event.target.value })} />
          </label>
          <label className="flex flex-col gap-2 text-sm text-slate-600 dark:text-slate-300">
            <span className="inline-flex items-center gap-2 font-medium"><FileText className="h-4 w-4" aria-hidden="true" />Notes</span>
            <input className="rounded-lg border border-slate-200 bg-white px-4 py-3 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100" placeholder="Optional details…" name="notes" autoComplete="off" value={form.notes} onChange={(event) => setForm({ ...form, notes: event.target.value })} />
          </label>
        </div>

        {formError ? <div className="mt-4 rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-600 dark:bg-rose-950/40" aria-live="polite">{formError}</div> : null}
        {recentlyDeleted ? (
          <div className="mt-4 flex flex-col gap-3 rounded-lg bg-amber-50 px-4 py-3 text-sm text-amber-700 dark:bg-amber-950/30 dark:text-amber-200 sm:flex-row sm:items-center sm:justify-between" aria-live="polite">
            <span>Transaction deleted.</span>
            <button
              className="inline-flex items-center gap-2 rounded-lg border border-amber-200 px-3 py-2 text-xs font-medium dark:border-amber-800"
              onClick={() => undoDeleteMutation.mutate(recentlyDeleted)}
              disabled={undoDeleteMutation.isPending}
            >
              <RotateCcw className="h-4 w-4" aria-hidden="true" />
              Undo delete
            </button>
          </div>
        ) : null}

        <button className="mt-4 inline-flex items-center gap-2 rounded-lg bg-ink px-4 py-3 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-60 dark:bg-slate-100 dark:text-slate-950" onClick={() => saveMutation.mutate()} disabled={saveMutation.isPending}>
          {editingId ? <Save className="h-4 w-4" aria-hidden="true" /> : <ClipboardPen className="h-4 w-4" aria-hidden="true" />}
          {editingId ? "Update transaction" : "Add transaction"}
        </button>
      </div>

      {isLoading ? (
        <LoadingState message="Loading transactions…" />
      ) : isError ? (
        <ErrorState message="Unable to load transactions for the selected month." />
      ) : (
        <div>
          <div className="space-y-3 lg:hidden">
            {transactions.length === 0 ? (
              <div className="card p-6 text-sm text-slate-500 dark:border dark:border-slate-800 dark:text-slate-400">No transactions found for this month.</div>
            ) : (
              transactions.map((item: any) => (
                <div key={item.id} className="card p-4 dark:border dark:border-slate-800">
                  <div className="flex items-start justify-between gap-4">
                    <div className="min-w-0">
                      <div className="truncate font-medium text-ink dark:text-slate-100">{item.title}</div>
                      <div className="truncate mt-1 text-sm text-slate-500 dark:text-slate-400">{item.categoryName} • {item.accountLabel}</div>
                      <div className="truncate mt-1 text-xs text-slate-400 dark:text-slate-500">{item.transactionDate} • {item.merchant ?? "No merchant"}</div>
                    </div>
                    <div className={`tabular-nums text-right font-semibold ${item.kind === "income" ? "text-emerald-600" : "text-rose-600"}`}>
                      {item.kind === "income" ? "+" : "-"}{formatCurrency(Number(item.amount))}
                    </div>
                  </div>
                  <div className="mt-4 flex gap-2">
                    <button className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-2 text-xs font-medium dark:border-slate-700 dark:text-slate-200" onClick={() => startEdit(item)}><Edit3 className="h-4 w-4" aria-hidden="true" />Edit</button>
                    <button className="inline-flex items-center gap-2 rounded-lg border border-rose-200 px-3 py-2 text-xs font-medium text-rose-600 dark:border-rose-900 dark:text-rose-300" onClick={() => void requestDelete(item)}><Trash2 className="h-4 w-4" aria-hidden="true" />Delete</button>
                  </div>
                </div>
              ))
            )}
          </div>

          <div className="card hidden overflow-hidden dark:border dark:border-slate-800 lg:block">
            <div className="overflow-x-auto">
              <table className="min-w-full border-collapse">
                <thead className="bg-mist text-left text-xs uppercase tracking-[0.2em] text-slate-400 dark:bg-slate-800 dark:text-slate-500">
                  <tr>
                    <th className="px-6 py-4">Title</th>
                    <th className="px-6 py-4">Category</th>
                    <th className="px-6 py-4">Payment type</th>
                    <th className="px-6 py-4">Date</th>
                    <th className="px-6 py-4">Merchant</th>
                    <th className="px-6 py-4 text-right">Amount</th>
                    <th className="px-6 py-4 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {transactions.map((item: any) => (
                    <tr key={item.id} className="border-t border-slate-100 text-sm text-slate-600 dark:border-slate-800 dark:text-slate-300">
                      <td className="px-6 py-4"><div className="max-w-[220px] truncate font-medium text-ink dark:text-slate-100">{item.title}</div><div className="max-w-[220px] break-words text-xs text-slate-400 dark:text-slate-500">{item.notes}</div></td>
                      <td className="px-6 py-4"><span className="inline-flex max-w-[180px] truncate rounded-full px-3 py-1 text-xs font-medium" style={{ backgroundColor: `${item.categoryColor}20`, color: item.categoryColor }}>{item.categoryName}</span></td>
                      <td className="px-6 py-4"><span className="max-w-[120px] truncate inline-block align-bottom">{item.accountLabel}</span></td>
                      <td className="px-6 py-4 tabular-nums">{item.transactionDate}</td>
                      <td className="px-6 py-4"><span className="max-w-[180px] truncate inline-block align-bottom">{item.merchant ?? "-"}</span></td>
                      <td className={`tabular-nums px-6 py-4 text-right font-semibold ${item.kind === "income" ? "text-emerald-600" : "text-rose-600"}`}>{item.kind === "income" ? "+" : "-"}{formatCurrency(Number(item.amount))}</td>
                      <td className="px-6 py-4 text-right"><div className="inline-flex gap-2"><button className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-2 text-xs font-medium dark:border-slate-700 dark:text-slate-200" onClick={() => startEdit(item)}><Edit3 className="h-4 w-4" aria-hidden="true" />Edit</button><button className="inline-flex items-center gap-2 rounded-lg border border-rose-200 px-3 py-2 text-xs font-medium text-rose-600 dark:border-rose-900 dark:text-rose-300" onClick={() => void requestDelete(item)}><Trash2 className="h-4 w-4" aria-hidden="true" />Delete</button></div></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="mt-4 flex flex-col gap-3 rounded-lg border border-slate-200 px-4 py-3 text-sm text-slate-600 dark:border-slate-700 dark:text-slate-300 sm:flex-row sm:items-center sm:justify-between">
            <div className="tabular-nums">{getVisibleRange(pagination.page, pagination.perPage, pagination.totalItems)}</div>
            <div className="flex gap-2">
              <button
                className="rounded-lg border border-slate-200 px-3 py-2 text-xs font-medium disabled:opacity-50 dark:border-slate-700 dark:text-slate-200"
                onClick={() => setPage(Math.max(1, pagination.page - 1))}
                disabled={pagination.page <= 1}
              >
                Previous page
              </button>
              <div className="tabular-nums flex items-center px-2 text-xs text-slate-500 dark:text-slate-400">
                Page {pagination.page} of {totalPages}
              </div>
              <button
                className="rounded-lg border border-slate-200 px-3 py-2 text-xs font-medium disabled:opacity-50 dark:border-slate-700 dark:text-slate-200"
                onClick={() => setPage(Math.min(totalPages, pagination.page + 1))}
                disabled={pagination.page >= totalPages}
              >
                Next page
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
