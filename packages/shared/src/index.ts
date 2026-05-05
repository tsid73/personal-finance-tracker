export type TransactionKind = "income" | "expense";
export type AccountType = "cash" | "bank" | "wallet" | "credit";

export interface Category {
  id: number;
  name: string;
  type: TransactionKind;
  color: string;
  icon: string;
  isDefault: boolean;
}

export interface Account {
  id: number;
  name: string;
  type: AccountType;
  balance: number;
}

export interface Transaction {
  id: number;
  title: string;
  amount: number;
  kind: TransactionKind;
  notes: string | null;
  merchant: string | null;
  transactionDate: string;
  categoryId: number;
  accountId: number;
}

export interface Budget {
  id: number;
  categoryId: number;
  month: number;
  year: number;
  allocatedAmount: number;
}

export interface DashboardSummary {
  balance: number;
  monthlyIncome: number;
  monthlyExpense: number;
  budgetAllocated: number;
  budgetSpent: number;
  savingsRate: number;
}

export const defaultCategorySeeds: Omit<Category, "id">[] = [
  { name: "Groceries", type: "expense", color: "#0f766e", icon: "shopping-bag", isDefault: true },
  { name: "Rent", type: "expense", color: "#b45309", icon: "home", isDefault: true },
  { name: "Utilities", type: "expense", color: "#2563eb", icon: "bolt", isDefault: true },
  { name: "Transport", type: "expense", color: "#7c3aed", icon: "car", isDefault: true },
  { name: "Dining", type: "expense", color: "#dc2626", icon: "utensils", isDefault: true },
  { name: "Healthcare", type: "expense", color: "#db2777", icon: "heart-pulse", isDefault: true },
  { name: "Entertainment", type: "expense", color: "#0891b2", icon: "film", isDefault: true },
  { name: "Shopping", type: "expense", color: "#ea580c", icon: "shirt", isDefault: true },
  { name: "Salary", type: "income", color: "#15803d", icon: "briefcase", isDefault: true },
  { name: "Freelance", type: "income", color: "#4338ca", icon: "laptop", isDefault: true }
];
