import { useOutletContext } from "react-router-dom";

export interface AppShellContextValue {
  selectedMonth: string;
  selectedMonthLabel: string;
}

export function useAppShellContext() {
  return useOutletContext<AppShellContextValue>();
}
