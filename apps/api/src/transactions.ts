import { z } from "zod";

export const transactionQuerySchema = z.object({
  page: z.coerce.number().int().min(1).default(1),
  perPage: z.coerce.number().int().min(1).max(50).default(10)
});

const optionalPositiveInt = z.preprocess(
  (value) => (value === "" || value == null ? undefined : value),
  z.coerce.number().int().positive().optional()
);

export const transactionFilterSchema = z.object({
  q: z.preprocess(
    (value) => {
      if (typeof value !== "string") {
        return value;
      }

      const trimmed = value.trim();
      return trimmed === "" ? undefined : trimmed;
    },
    z.string().max(120).optional()
  ),
  kind: z.preprocess(
    (value) => (value === "" || value == null ? undefined : value),
    z.enum(["income", "expense"]).optional()
  ),
  accountId: optionalPositiveInt,
  categoryId: optionalPositiveInt
});

export function parseTransactionPagination(input: { page?: unknown; perPage?: unknown }) {
  const parsed = transactionQuerySchema.parse(input);
  return {
    page: parsed.page,
    perPage: parsed.perPage,
    offset: (parsed.page - 1) * parsed.perPage
  };
}

export function parseTransactionFilters(input: {
  q?: unknown;
  kind?: unknown;
  accountId?: unknown;
  categoryId?: unknown;
}) {
  return transactionFilterSchema.parse(input);
}
