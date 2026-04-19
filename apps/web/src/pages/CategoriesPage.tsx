import { useMemo, useState } from "react";
import { Archive, Edit3, Palette, RotateCcw, Save, Shapes, Tag, Trash2 } from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AppIcon, iconOptions } from "../components/AppIcon";
import { CardGridLoadingState, ErrorState } from "../components/PageState";
import { api } from "../lib/api";
import { confirmDestructiveAction, confirmImpactAction, showErrorToast, showSuccessToast } from "../lib/alerts";
import { getErrorMessage } from "../lib/errors";
import { invalidateActiveQueries } from "../lib/query";

const initialCategory = {
  name: "",
  type: "expense",
  color: "#0f766e",
  icon: "tag",
  budgetMode: "flexible" as "fixed" | "flexible",
  changeNote: ""
};

type DeleteState = {
  categoryId: number;
  replacementCategoryId: string;
  changeNote: string;
};

export function CategoriesPage() {
  const queryClient = useQueryClient();
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState(initialCategory);
  const [formError, setFormError] = useState<string | null>(null);
  const [deleteState, setDeleteState] = useState<DeleteState | null>(null);
  const { data = [], isLoading, isError } = useQuery({
    queryKey: ["categories", "all"],
    queryFn: async () => (await api.get("/categories", { params: { includeArchived: 1 } })).data
  });
  const { data: activity = [] } = useQuery({
    queryKey: ["activity"],
    queryFn: async () => (await api.get("/activity")).data
  });

  const activeCategories = useMemo(() => data.filter((item: any) => !item.isArchived), [data]);
  const archivedCategories = useMemo(() => data.filter((item: any) => item.isArchived), [data]);
  const editingCategory = useMemo(() => data.find((item: any) => item.id === editingId) ?? null, [data, editingId]);

  const validateCategory = () => {
    if (form.name.trim().length < 2) return "Category name must be at least 2 characters.";
    if (!/^#[0-9a-fA-F]{6}$/.test(form.color)) return "Please choose a valid color.";
    if (!form.icon.trim()) return "Please choose an icon.";
    return null;
  };

  const refreshQueries = async () => {
    await invalidateActiveQueries(queryClient, [["categories"], ["categories", "all"], ["budgets"], ["transactions"], ["reports"], ["dashboard"], ["activity"]]);
  };

  const saveMutation = useMutation({
    mutationFn: async () => {
      const validationMessage = validateCategory();
      if (validationMessage) throw new Error(validationMessage);
      const payload = {
        ...form,
        name: form.name.trim(),
        icon: form.icon.trim(),
        changeNote: form.changeNote.trim() || null
      };
      if (editingId) {
        await api.put(`/categories/${editingId}`, payload);
      } else {
        await api.post("/categories", payload);
      }
    },
    onSuccess: async () => {
      const currentAction = editingId ? "Category updated" : "Category added";
      setEditingId(null);
      setForm(initialCategory);
      setFormError(null);
      await refreshQueries();
      await showSuccessToast(currentAction);
    },
    onError: (error) => {
      const message = getErrorMessage(error, "Unable to save the category.");
      setFormError(message);
      void showErrorToast(message);
    }
  });

  const handleSaveCategory = async () => {
    const validationMessage = validateCategory();
    if (validationMessage) {
      setFormError(validationMessage);
      return;
    }

    if (
      editingCategory &&
      form.name.trim().toLowerCase() !== String(editingCategory.name ?? "").trim().toLowerCase() &&
      (editingCategory.hasTransactions || editingCategory.hasBudgets || editingCategory.hasRecurring)
    ) {
      const confirmed = await confirmImpactAction(
        "Update category name?",
        "This category is already used by existing records. Renaming it will update every transaction, budget, report, and recurring schedule that uses this category.",
        "Update category"
      );

      if (!confirmed) {
        return;
      }
    }

    setFormError(null);
    saveMutation.mutate();
  };

  const archiveMutation = useMutation({
    mutationFn: async ({ id, isArchived, changeNote }: { id: number; isArchived: boolean; changeNote?: string | null }) =>
      api.put(`/categories/${id}/archive`, { isArchived, changeNote: changeNote ?? null }),
    onSuccess: async (_result, variables) => {
      await refreshQueries();
      await showSuccessToast(variables.isArchived ? "Category archived" : "Category restored");
    },
    onError: (error) => {
      const message = getErrorMessage(error, "Unable to update the category archive state.");
      setFormError(message);
      void showErrorToast(message);
    }
  });

  const deleteMutation = useMutation({
    mutationFn: async ({ id, reassignmentCategoryId, changeNote }: { id: number; reassignmentCategoryId?: number; changeNote?: string | null }) =>
      api.delete(`/categories/${id}`, { data: { reassignmentCategoryId, changeNote: changeNote ?? null } }),
    onSuccess: async () => {
      setDeleteState(null);
      await refreshQueries();
      await showSuccessToast("Category deleted");
    },
    onError: (error) => {
      const message = getErrorMessage(error, "Unable to delete the category.");
      setFormError(message);
      void showErrorToast(message);
    }
  });

  const requestDelete = async (item: any) => {
    const inUse = item.hasTransactions || item.hasBudgets || item.hasRecurring;

    if (inUse) {
      setDeleteState({
        categoryId: item.id,
        replacementCategoryId: "",
        changeNote: ""
      });
      setFormError(null);
      return;
    }

    const confirmed = await confirmDestructiveAction("Delete category?", `Delete "${item.name}"?`, "Delete");
    if (!confirmed) {
      return;
    }

    deleteMutation.mutate({
      id: item.id,
      changeNote: null
    });
  };

  const confirmDeleteWithReplacement = (item: any) => {
    if (!deleteState || deleteState.categoryId !== item.id) {
      return;
    }

    if (!deleteState.replacementCategoryId) {
      setFormError("Select a replacement category before deleting a category that is already in use.");
      return;
    }

    deleteMutation.mutate({
      id: item.id,
      reassignmentCategoryId: Number(deleteState.replacementCategoryId),
      changeNote: deleteState.changeNote.trim() || null
    });
  };

  const startEdit = (item: any) => {
    setDeleteState(null);
    setEditingId(item.id);
    setFormError(null);
    setForm({
      name: item.name,
      type: item.type,
      color: item.color,
      icon: item.icon,
      budgetMode: item.budgetMode ?? "flexible",
      changeNote: ""
    });
  };

  const resetCategoryForm = () => {
    setEditingId(null);
    setDeleteState(null);
    setForm(initialCategory);
    setFormError(null);
  };

  const renderCategoryCard = (item: any, archived = false) => {
    const replacementOptions = activeCategories.filter((category: any) => category.id !== item.id && category.type === item.type);
    const isDeleteTarget = deleteState?.categoryId === item.id;

    return (
      <div key={item.id} className="rounded-lg border border-slate-100 bg-mist p-5 dark:border-slate-800 dark:bg-slate-800">
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0 flex items-center gap-3">
            <span className="inline-flex h-10 w-10 items-center justify-center rounded-full" style={{ backgroundColor: `${item.color}22`, color: item.color }}>
              <AppIcon name={item.icon} className="h-5 w-5" />
            </span>
            <div className="min-w-0">
              <div className="truncate font-medium text-ink dark:text-slate-100">{item.name}</div>
              <div className="mt-1 flex flex-wrap gap-2 text-xs uppercase tracking-[0.2em] text-slate-400 dark:text-slate-500">
                <span>{item.type}</span>
                <span>{item.budgetMode}</span>
                {item.isDefault ? <span>Default</span> : null}
                {archived ? <span>Archived</span> : null}
              </div>
            </div>
          </div>
          {!item.isDefault ? (
            <div className="flex flex-wrap justify-end gap-2">
              {!archived ? (
                <>
                  <button
                    className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 text-slate-700 transition hover:bg-white dark:border-slate-700 dark:text-slate-200 dark:hover:bg-slate-950"
                    aria-label={`Edit ${item.name}`}
                    title={`Edit ${item.name}`}
                    onClick={() => startEdit(item)}
                  >
                    <Edit3 className="h-4 w-4" aria-hidden="true" />
                  </button>
                  <button
                    className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 text-slate-700 transition hover:bg-white dark:border-slate-700 dark:text-slate-200 dark:hover:bg-slate-950"
                    aria-label={`Archive ${item.name}`}
                    title={`Archive ${item.name}`}
                    onClick={() => archiveMutation.mutate({ id: item.id, isArchived: true, changeNote: form.changeNote.trim() || null })}
                  >
                    <Archive className="h-4 w-4" aria-hidden="true" />
                  </button>
                  <button
                    className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-rose-200 text-rose-600 transition hover:bg-white dark:border-rose-900 dark:text-rose-300 dark:hover:bg-slate-950"
                    aria-label={`Delete ${item.name}`}
                    title={`Delete ${item.name}`}
                    onClick={() => void requestDelete(item)}
                  >
                    <Trash2 className="h-4 w-4" aria-hidden="true" />
                  </button>
                </>
              ) : (
                <button
                  className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 text-slate-700 transition hover:bg-white dark:border-slate-700 dark:text-slate-200 dark:hover:bg-slate-950"
                  aria-label={`Restore ${item.name}`}
                  title={`Restore ${item.name}`}
                  onClick={() => archiveMutation.mutate({ id: item.id, isArchived: false, changeNote: form.changeNote.trim() || null })}
                >
                  <RotateCcw className="h-4 w-4" aria-hidden="true" />
                </button>
              )}
            </div>
          ) : (
            <span className="rounded-full bg-white px-3 py-1 text-xs font-medium text-slate-500 dark:bg-slate-950 dark:text-slate-400">Protected</span>
          )}
        </div>

        {!archived && !item.isDefault && isDeleteTarget ? (
          <div className="mt-4 space-y-2">
            <div className="text-xs text-amber-600 dark:text-amber-300">This category is in use. Choose a replacement category before delete.</div>
            <select
              className="w-full rounded-lg border border-slate-200 bg-white px-4 py-3 text-sm dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
              value={deleteState?.replacementCategoryId ?? ""}
              onChange={(event) => setDeleteState((current) => current ? { ...current, replacementCategoryId: event.target.value } : current)}
            >
              <option value="">Select replacement category</option>
              {replacementOptions.map((category: any) => (
                <option key={category.id} value={category.id}>{category.name}</option>
              ))}
            </select>
            <input
              className="w-full rounded-lg border border-slate-200 bg-white px-4 py-3 text-sm dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
              placeholder="Optional delete note"
              value={deleteState?.changeNote ?? ""}
              onChange={(event) => setDeleteState((current) => current ? { ...current, changeNote: event.target.value } : current)}
            />
            <div className="flex flex-wrap gap-2">
              <button className="inline-flex items-center gap-2 rounded-lg border border-rose-200 px-4 py-3 text-sm font-medium text-rose-600 dark:border-rose-900 dark:text-rose-300" onClick={() => confirmDeleteWithReplacement(item)}>
                <Trash2 className="h-4 w-4" aria-hidden="true" />
                Reassign and delete
              </button>
              <button className="rounded-lg border border-slate-200 px-4 py-3 text-sm font-medium dark:border-slate-700 dark:text-slate-200" onClick={() => setDeleteState(null)}>
                Cancel
              </button>
            </div>
          </div>
        ) : null}
      </div>
    );
  };

  return (
    <div className="space-y-6">
      <div className="card p-5 sm:p-6 dark:border dark:border-slate-800">
        <div className="section-title">Categories</div>
        <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">Create, archive, and manage reusable income and expense groups.</p>
        <div className="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <label className="flex flex-col gap-2 text-sm text-slate-600 dark:text-slate-300">
            <span className="inline-flex items-center gap-2 font-medium"><Tag className="h-4 w-4" aria-hidden="true" />Category name</span>
            <input className="rounded-lg border border-slate-200 bg-white px-4 py-3 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100" placeholder="Groceries…" autoComplete="off" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />
          </label>
          <label className="flex flex-col gap-2 text-sm text-slate-600 dark:text-slate-300">
            <span className="inline-flex items-center gap-2 font-medium"><Shapes className="h-4 w-4" aria-hidden="true" />Type</span>
            <select className="rounded-lg border border-slate-200 bg-white px-4 py-3 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100" value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value as "income" | "expense" })}>
              <option value="expense">Expense</option>
              <option value="income">Income</option>
            </select>
          </label>
          <label className="flex flex-col gap-2 text-sm text-slate-600 dark:text-slate-300">
            <span className="inline-flex items-center gap-2 font-medium"><Palette className="h-4 w-4" aria-hidden="true" />Color</span>
            <input className="h-12 rounded-lg border border-slate-200 px-2 py-1 dark:border-slate-700 dark:bg-slate-950" type="color" aria-label="Category color" value={form.color} onChange={(event) => setForm({ ...form, color: event.target.value })} />
          </label>
          <label className="flex flex-col gap-2 text-sm text-slate-600 dark:text-slate-300">
            <span className="inline-flex items-center gap-2 font-medium"><Shapes className="h-4 w-4" aria-hidden="true" />Budget mode</span>
            <select className="rounded-lg border border-slate-200 bg-white px-4 py-3 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100" value={form.budgetMode} onChange={(event) => setForm({ ...form, budgetMode: event.target.value as "fixed" | "flexible" })}>
              <option value="flexible">Flexible</option>
              <option value="fixed">Fixed</option>
            </select>
          </label>
        </div>

        <div className="mt-6">
          <div className="mb-3 text-sm font-medium text-slate-600 dark:text-slate-300">Icon picker</div>
          <div className="grid gap-3 sm:grid-cols-4 lg:grid-cols-8">
            {iconOptions.map((iconName) => (
              <button
                key={iconName}
                type="button"
                className={`flex flex-col items-center gap-2 rounded-lg border px-3 py-3 text-xs font-medium capitalize transition ${
                  form.icon === iconName
                    ? "border-slate-900 bg-slate-100 text-slate-900 dark:border-slate-100 dark:bg-slate-900 dark:text-slate-100"
                    : "border-slate-200 text-slate-600 hover:bg-slate-100 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-900"
                }`}
                onClick={() => setForm({ ...form, icon: iconName })}
              >
                <AppIcon name={iconName} className="h-5 w-5" />
                {iconName}
              </button>
            ))}
          </div>
        </div>

        <label className="mt-6 flex flex-col gap-2 text-sm text-slate-600 dark:text-slate-300">
          <span className="inline-flex items-center gap-2 font-medium"><Edit3 className="h-4 w-4" aria-hidden="true" />Optional change note</span>
          <input className="rounded-lg border border-slate-200 bg-white px-4 py-3 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100" placeholder="Reason for this change" value={form.changeNote} onChange={(event) => setForm({ ...form, changeNote: event.target.value })} />
        </label>

        {formError ? <div className="mt-4 rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-600 dark:bg-rose-950/40" aria-live="polite">{formError}</div> : null}
        <div className="mt-4 flex flex-wrap gap-3">
          <button className="inline-flex items-center gap-2 rounded-lg bg-ink px-4 py-3 text-sm font-medium text-white disabled:opacity-60 dark:bg-slate-100 dark:text-slate-950" onClick={() => void handleSaveCategory()} disabled={saveMutation.isPending}>
            {editingId ? <Edit3 className="h-4 w-4" aria-hidden="true" /> : <Save className="h-4 w-4" aria-hidden="true" />}
            {editingId ? "Update category" : "Add category"}
          </button>
          {editingId ? (
            <button className="rounded-lg border border-slate-200 px-4 py-3 text-sm font-medium dark:border-slate-700 dark:text-slate-200" onClick={resetCategoryForm}>
              Cancel
            </button>
          ) : null}
        </div>
      </div>

      <div className="grid gap-6 xl:grid-cols-[1.4fr_0.9fr]">
        <div className="card p-5 sm:p-6 dark:border dark:border-slate-800">
          <div className="section-title">Active categories</div>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">Default categories are protected. Custom categories can be archived or deleted.</p>
          {isLoading ? (
            <div className="mt-6"><CardGridLoadingState count={4} /></div>
          ) : isError ? (
            <ErrorState message="Unable to load categories." />
          ) : activeCategories.length === 0 ? (
            <div className="mt-4 rounded-lg bg-mist px-4 py-6 text-sm text-slate-500 dark:bg-slate-800 dark:text-slate-400">No active categories. Create one from the form above.</div>
          ) : (
            <div className="mt-6 grid gap-4 md:grid-cols-2">
              {activeCategories.map((item: any) => renderCategoryCard(item))}
            </div>
          )}
        </div>

        <div className="space-y-6">
          <div className="card p-5 sm:p-6 dark:border dark:border-slate-800">
            <div className="section-title">Archived categories</div>
            <div className="mt-4 space-y-3">
              {archivedCategories.length === 0 ? (
                <div className="rounded-lg bg-mist px-4 py-6 text-sm text-slate-500 dark:bg-slate-800 dark:text-slate-400">No archived categories.</div>
              ) : (
                archivedCategories.map((item: any) => renderCategoryCard(item, true))
              )}
            </div>
          </div>

          <div className="card p-5 sm:p-6 dark:border dark:border-slate-800">
            <div className="section-title">Recent activity</div>
            <div className="mt-4 space-y-3">
              {activity.filter((item: any) => item.entityType === "category").length === 0 ? (
                <div className="rounded-lg bg-mist px-4 py-6 text-sm text-slate-500 dark:bg-slate-800 dark:text-slate-400">Category history will appear here after updates, archives, and deletes.</div>
              ) : (
                activity
                  .filter((item: any) => item.entityType === "category")
                  .slice(0, 8)
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
      </div>
    </div>
  );
}
