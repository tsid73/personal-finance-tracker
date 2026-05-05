import test from "node:test";
import assert from "node:assert/strict";
import { parseTransactionFilters, parseTransactionPagination } from "./transactions.js";

test("parseTransactionPagination applies defaults", () => {
  assert.deepEqual(parseTransactionPagination({}), {
    page: 1,
    perPage: 10,
    offset: 0
  });
});

test("parseTransactionPagination computes offset", () => {
  assert.deepEqual(parseTransactionPagination({ page: "3", perPage: "15" }), {
    page: 3,
    perPage: 15,
    offset: 30
  });
});

test("parseTransactionPagination rejects invalid page sizes", () => {
  assert.throws(() => parseTransactionPagination({ perPage: "100" }));
});

test("parseTransactionFilters normalizes empty values", () => {
  assert.deepEqual(
    parseTransactionFilters({
      q: "   ",
      kind: "",
      accountId: "",
      categoryId: undefined
    }),
    {
      q: undefined,
      kind: undefined,
      accountId: undefined,
      categoryId: undefined
    }
  );
});

test("parseTransactionFilters parses valid filters", () => {
  assert.deepEqual(
    parseTransactionFilters({
      q: "rent",
      kind: "expense",
      accountId: "2",
      categoryId: "5"
    }),
    {
      q: "rent",
      kind: "expense",
      accountId: 2,
      categoryId: 5
    }
  );
});
