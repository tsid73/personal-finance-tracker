import test from "node:test";
import assert from "node:assert/strict";
import { getInitialNextDueDate, getNextMonthlyDueDate, getRecurringStatus } from "./recurring.js";

test("getInitialNextDueDate uses same month when occurrence is not in the past", () => {
  assert.equal(getInitialNextDueDate("2026-04-03", 5), "2026-04-05");
});

test("getInitialNextDueDate rolls to next month when current month date has passed", () => {
  assert.equal(getInitialNextDueDate("2026-04-28", 5), "2026-05-05");
});

test("getNextMonthlyDueDate clamps to end of shorter months", () => {
  assert.equal(getNextMonthlyDueDate("2026-01-31", 31), "2026-02-28");
});

test("getRecurringStatus returns due and inactive states correctly", () => {
  assert.equal(getRecurringStatus("2026-04-10", true, "2026-04-19"), "due");
  assert.equal(getRecurringStatus("2026-04-20", true, "2026-04-19"), "upcoming");
  assert.equal(getRecurringStatus("2026-04-10", false, "2026-04-19"), "inactive");
});
