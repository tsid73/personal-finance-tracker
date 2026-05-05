export function formatMonthLabel(month: string) {
  const date = new Date(`${month}-01T00:00:00`);
  return new Intl.DateTimeFormat("en-IN", {
    month: "long",
    year: "numeric"
  }).format(date);
}

export function shiftMonth(month: string, delta: number) {
  const date = new Date(`${month}-01T00:00:00`);
  date.setMonth(date.getMonth() + delta);
  const year = date.getFullYear();
  const nextMonth = String(date.getMonth() + 1).padStart(2, "0");
  return `${year}-${nextMonth}`;
}

export function getCurrentMonth() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
}

export function monthBounds(month: string) {
  return {
    start: `${month}-01`,
    display: formatMonthLabel(month)
  };
}
