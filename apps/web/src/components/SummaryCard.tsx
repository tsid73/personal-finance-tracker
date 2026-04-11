import { formatCurrency } from "../lib/format";

interface SummaryCardProps {
  label: string;
  value: number;
  tone?: "default" | "success" | "danger";
}

export function SummaryCard({ label, value, tone = "default" }: SummaryCardProps) {
  const toneClass =
    tone === "success" ? "text-emerald-600" : tone === "danger" ? "text-rose-600" : "text-ink";

  return (
    <div className="card p-5">
      <div className="text-xs uppercase tracking-[0.2em] text-slate-400">{label}</div>
      <div className={`mt-3 text-3xl font-semibold ${toneClass}`}>{formatCurrency(value)}</div>
    </div>
  );
}
