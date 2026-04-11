import { useState } from "react";
import { Edit3, Palette, Save, Shapes, Tag, Trash2 } from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AppIcon } from "../components/AppIcon";
import { ErrorState, LoadingState } from "../components/PageState";
import { api } from "../lib/api";
import { confirmDestructiveAction, showErrorToast, showSuccessToast } from "../lib/alerts";
import { getErrorMessage } from "../lib/errors";
import { invalidateActiveQueries } from "../lib/query";

const initialCategory = {
  name: "",
  type: "expense",
  color: "#0f766e",
  icon: "tag"
};

export function CategoriesPage() {
  const queryClient = useQueryClient();
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState(initialCategory);
  const [formError, setFormError] = useState<string | null>(null);
  const { data = [], isLoading, isError } = useQuery({
    queryKey: ["categories"],
    queryFn: async () => (await api.get("/categories")).data
  });

  const validateCategory = () => {
    if (form.name.trim().length < 2) return "Category name must be at least 2 characters.";
    if (!/^#[0-9a-fA-F]{6}$/.test(form.color)) return "Please choose a valid color.";
    if (!form.icon.trim()) return "Icon label is required.";
    return null;
  };

  const saveMutation = useMutation({
    mutationFn: async () => {
      const validationMessage = validateCategory();
      if (validationMessage) throw new Error(validationMessage);
      const payload = { ...form, name: form.name.trim(), icon: form.icon.trim() };
      if (editingId) {
        await api.put(`/categories/${editingId}`, payload);
      } else {
        await api.post("/categories", payload);
      }
    },
    onSuccess: async () => {
      setEditingId(null);
      setForm(initialCategory);
      setFormError(null);
      await invalidateActiveQueries(queryClient, [["categories"], ["budgets"], ["transactions"], ["reports"], ["dashboard"]]);
      await showSuccessToast(editingId ? "Category updated" : "Category added");
    },
    onError: (error) => {
      const message = getErrorMessage(error, "Unable to save the category.");
      setFormError(message);
      void showErrorToast(message);
    }
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: number) => api.delete(`/categories/${id}`),
    onSuccess: async () => {
      await invalidateActiveQueries(queryClient, [["categories"], ["budgets"], ["transactions"], ["reports"], ["dashboard"]]);
      await showSuccessToast("Category deleted");
    },
    onError: (error) => {
      const message = getErrorMessage(error, "Unable to delete the category.");
      setFormError(message);
      void showErrorToast(message);
    }
  });

  return (
    <div className="space-y-6">
      <div className="card p-5 sm:p-6 dark:border dark:border-slate-800">
        <div className="section-title">Categories</div>
        <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">Create or manage reusable income and expense groups.</p>
        <div className="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <label className="flex flex-col gap-2 text-sm text-slate-600 dark:text-slate-300">
            <span className="inline-flex items-center gap-2 font-medium"><Tag className="h-4 w-4" aria-hidden="true" />Category name</span>
            <input className="rounded-lg border border-slate-200 bg-white px-4 py-3 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100" placeholder="Groceries…" name="categoryName" autoComplete="off" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />
          </label>
          <label className="flex flex-col gap-2 text-sm text-slate-600 dark:text-slate-300">
            <span className="inline-flex items-center gap-2 font-medium"><Shapes className="h-4 w-4" aria-hidden="true" />Type</span>
            <select className="rounded-lg border border-slate-200 bg-white px-4 py-3 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100" name="categoryType" value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value })}>
              <option value="expense">Expense</option>
              <option value="income">Income</option>
            </select>
          </label>
          <label className="flex flex-col gap-2 text-sm text-slate-600 dark:text-slate-300">
            <span className="inline-flex items-center gap-2 font-medium"><Palette className="h-4 w-4" aria-hidden="true" />Color</span>
            <input className="h-12 rounded-lg border border-slate-200 px-2 py-1 dark:border-slate-700 dark:bg-slate-950" type="color" name="categoryColor" aria-label="Category color" value={form.color} onChange={(event) => setForm({ ...form, color: event.target.value })} />
          </label>
          <label className="flex flex-col gap-2 text-sm text-slate-600 dark:text-slate-300">
            <span className="inline-flex items-center gap-2 font-medium"><Shapes className="h-4 w-4" aria-hidden="true" />Icon label</span>
            <input className="rounded-lg border border-slate-200 bg-white px-4 py-3 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100" placeholder="shopping…" name="categoryIcon" autoComplete="off" value={form.icon} onChange={(event) => setForm({ ...form, icon: event.target.value })} />
          </label>
        </div>
        {formError ? <div className="mt-4 rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-600 dark:bg-rose-950/40" aria-live="polite">{formError}</div> : null}
        <div className="mt-4 flex flex-wrap gap-3">
          <button className="inline-flex items-center gap-2 rounded-lg bg-ink px-4 py-3 text-sm font-medium text-white disabled:opacity-60 dark:bg-slate-100 dark:text-slate-950" onClick={() => saveMutation.mutate()} disabled={saveMutation.isPending}>
            {editingId ? <Edit3 className="h-4 w-4" aria-hidden="true" /> : <Save className="h-4 w-4" aria-hidden="true" />}
            {editingId ? "Update category" : "Add category"}
          </button>
          {editingId ? (
            <button className="rounded-lg border border-slate-200 px-4 py-3 text-sm font-medium dark:border-slate-700 dark:text-slate-200" onClick={() => { setEditingId(null); setForm(initialCategory); setFormError(null); }}>
              Cancel
            </button>
          ) : null}
        </div>
      </div>

      <div className="card p-5 sm:p-6 dark:border dark:border-slate-800">
        {isLoading ? (
          <LoadingState message="Loading categories…" />
        ) : isError ? (
          <ErrorState message="Unable to load categories." />
        ) : (
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {data.map((item: any) => (
              <div key={item.id} className="rounded-lg border border-slate-100 bg-mist p-5 dark:border-slate-800 dark:bg-slate-800">
                <div className="flex items-center justify-between gap-3">
                  <div className="flex items-center gap-3">
                    <span className="inline-flex h-10 w-10 items-center justify-center rounded-full" style={{ backgroundColor: `${item.color}22`, color: item.color }}>
                      <AppIcon name={item.icon} className="h-5 w-5" />
                    </span>
                    <div className="min-w-0">
                      <div className="truncate font-medium text-ink dark:text-slate-100">{item.name}</div>
                      <div className="truncate text-xs uppercase tracking-[0.2em] text-slate-400 dark:text-slate-500">{item.type}</div>
                    </div>
                  </div>
                  {item.isDefault ? (
                    <span className="rounded-full bg-white px-3 py-1 text-xs font-medium text-slate-500 dark:bg-slate-950 dark:text-slate-400">Default</span>
                  ) : (
                    <div className="inline-flex gap-2">
                      <button className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-2 text-xs font-medium dark:border-slate-700 dark:text-slate-200" onClick={() => { setEditingId(item.id); setForm({ name: item.name, type: item.type, color: item.color, icon: item.icon }); setFormError(null); }}><Edit3 className="h-4 w-4" aria-hidden="true" />Edit</button>
                      <button className="inline-flex items-center gap-2 rounded-lg border border-rose-200 px-3 py-2 text-xs font-medium text-rose-600 dark:border-rose-900 dark:text-rose-300" onClick={async () => {
                        const confirmed = await confirmDestructiveAction("Delete category?", `Delete "${item.name}"?`, "Delete");
                        if (confirmed) {
                          deleteMutation.mutate(item.id);
                        }
                      }}><Trash2 className="h-4 w-4" aria-hidden="true" />Delete</button>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
