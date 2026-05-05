import test from "node:test";
import assert from "node:assert/strict";
import { getTotalPages, getVisibleRange } from "./pagination.js";

test("getTotalPages returns at least one page", () => {
  assert.equal(getTotalPages(0, 10), 1);
});

test("getTotalPages rounds up", () => {
  assert.equal(getTotalPages(21, 10), 3);
});

test("getVisibleRange formats empty state", () => {
  assert.equal(getVisibleRange(1, 10, 0), "0 of 0");
});

test("getVisibleRange formats current slice", () => {
  assert.equal(getVisibleRange(2, 10, 26), "11-20 of 26");
});
