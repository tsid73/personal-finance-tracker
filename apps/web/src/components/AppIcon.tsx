import type { LucideIcon } from "lucide-react";
import {
  ArrowDownCircle,
  ArrowUpCircle,
  BadgeIndianRupee,
  Briefcase,
  Car,
  CreditCard,
  Gift,
  HandCoins,
  HeartPulse,
  House,
  Landmark,
  PiggyBank,
  Receipt,
  ShoppingBag,
  Tag
} from "lucide-react";

const iconMap: Record<string, LucideIcon> = {
  salary: BadgeIndianRupee,
  income: ArrowUpCircle,
  expense: ArrowDownCircle,
  groceries: ShoppingBag,
  shopping: ShoppingBag,
  transport: Car,
  travel: Car,
  rent: House,
  housing: House,
  savings: PiggyBank,
  health: HeartPulse,
  medical: HeartPulse,
  subscriptions: Receipt,
  bills: Receipt,
  utility: Landmark,
  utilities: Landmark,
  freelance: Briefcase,
  bonus: Gift,
  cash: HandCoins,
  card: CreditCard,
  tag: Tag
};

export function AppIcon({
  name,
  className = "h-4 w-4",
  strokeWidth = 2
}: {
  name?: string | null;
  className?: string;
  strokeWidth?: number;
}) {
  const normalized = name?.trim().toLowerCase() ?? "";
  const Icon = iconMap[normalized] ?? Tag;
  return <Icon className={className} strokeWidth={strokeWidth} aria-hidden="true" />;
}
