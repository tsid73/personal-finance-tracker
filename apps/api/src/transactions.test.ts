import test from "node:test";
import assert from "node:assert/strict";
import { parseTransactionPagination } from "./transactions.js";

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
