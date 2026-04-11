import type { LucideIcon } from "lucide-react";
import { formatCurrency } from "../lib/format";

interface SummaryCardProps {
  label: string;
  value: number;
  tone?: "default" | "success" | "danger";
  icon?: LucideIcon;
}

export function SummaryCard({ label, value, tone = "default", icon: Icon }: SummaryCardProps) {
  const toneClass =
    tone === "success" ? "text-emerald-600" : tone === "danger" ? "text-rose-600" : "text-ink";

  return (
    <div className="card p-5 dark:border dark:border-slate-800">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0 text-xs uppercase tracking-[0.2em] text-slate-400 dark:text-slate-500">{label}</div>
        {Icon ? <Icon className="h-5 w-5 shrink-0 text-slate-400 dark:text-slate-500" aria-hidden="true" /> : null}
      </div>
      <div className={`tabular-nums mt-3 text-3xl font-semibold dark:text-slate-100 ${toneClass}`}>{formatCurrency(value)}</div>
    </div>
  );
}
