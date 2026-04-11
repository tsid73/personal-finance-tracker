import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ErrorState, LoadingState } from "../components/PageState";
import { api } from "../lib/api";
import { getErrorMessage } from "../lib/errors";

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
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["categories"] }),
        queryClient.invalidateQueries({ queryKey: ["budgets"] }),
        queryClient.invalidateQueries({ queryKey: ["transactions"] }),
        queryClient.invalidateQueries({ queryKey: ["reports"] }),
        queryClient.invalidateQueries({ queryKey: ["dashboard"] })
      ]);
    },
    onError: (error) => setFormError(getErrorMessage(error, "Unable to save the category."))
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: number) => api.delete(`/categories/${id}`),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["categories"] }),
        queryClient.invalidateQueries({ queryKey: ["budgets"] }),
        queryClient.invalidateQueries({ queryKey: ["transactions"] }),
        queryClient.invalidateQueries({ queryKey: ["reports"] }),
        queryClient.invalidateQueries({ queryKey: ["dashboard"] })
      ]);
    },
    onError: (error) => setFormError(getErrorMessage(error, "Unable to delete the category."))
  });

  return (
    <div className="space-y-6">
      <div className="card p-5 sm:p-6">
        <div className="section-title">Categories</div>
        <p className="mt-1 text-sm text-slate-500">Create or manage reusable income and expense groups.</p>
        <div className="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <input className="rounded-2xl border border-slate-200 px-4 py-3" placeholder="Category name" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />
          <select className="rounded-2xl border border-slate-200 px-4 py-3" value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value })}>
            <option value="expense">Expense</option>
            <option value="income">Income</option>
          </select>
          <input className="h-12 rounded-2xl border border-slate-200 px-2 py-1" type="color" value={form.color} onChange={(event) => setForm({ ...form, color: event.target.value })} />
          <input className="rounded-2xl border border-slate-200 px-4 py-3" placeholder="Icon label" value={form.icon} onChange={(event) => setForm({ ...form, icon: event.target.value })} />
        </div>
        {formError ? <div className="mt-4 rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-600">{formError}</div> : null}
        <div className="mt-4 flex flex-wrap gap-3">
          <button className="rounded-2xl bg-ink px-4 py-3 text-sm font-medium text-white disabled:opacity-60" onClick={() => saveMutation.mutate()} disabled={saveMutation.isPending}>
            {editingId ? "Update category" : "Add category"}
          </button>
          {editingId ? (
            <button className="rounded-2xl border border-slate-200 px-4 py-3 text-sm font-medium" onClick={() => { setEditingId(null); setForm(initialCategory); setFormError(null); }}>
              Cancel
            </button>
          ) : null}
        </div>
      </div>

      <div className="card p-5 sm:p-6">
        {isLoading ? (
          <LoadingState message="Loading categories..." />
        ) : isError ? (
          <ErrorState message="Unable to load categories." />
        ) : (
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {data.map((item: any) => (
              <div key={item.id} className="rounded-3xl border border-slate-100 bg-mist p-5">
                <div className="flex items-center justify-between gap-3">
                  <div className="flex items-center gap-3">
                    <span className="h-4 w-4 rounded-full" style={{ backgroundColor: item.color }} />
                    <div>
                      <div className="font-medium text-ink">{item.name}</div>
                      <div className="text-xs uppercase tracking-[0.2em] text-slate-400">{item.type}</div>
                    </div>
                  </div>
                  {item.isDefault ? (
                    <span className="rounded-full bg-white px-3 py-1 text-xs font-medium text-slate-500">Default</span>
                  ) : (
                    <div className="inline-flex gap-2">
                      <button className="rounded-xl border border-slate-200 px-3 py-2 text-xs font-medium" onClick={() => { setEditingId(item.id); setForm({ name: item.name, type: item.type, color: item.color, icon: item.icon }); setFormError(null); }}>Edit</button>
                      <button className="rounded-xl border border-rose-200 px-3 py-2 text-xs font-medium text-rose-600" onClick={() => deleteMutation.mutate(item.id)}>Delete</button>
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
