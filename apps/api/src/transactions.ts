import { z } from "zod";

export const transactionQuerySchema = z.object({
  page: z.coerce.number().int().min(1).default(1),
  perPage: z.coerce.number().int().min(1).max(50).default(10)
});

export function parseTransactionPagination(input: { page?: unknown; perPage?: unknown }) {
  const parsed = transactionQuerySchema.parse(input);
  return {
    page: parsed.page,
    perPage: parsed.perPage,
    offset: (parsed.page - 1) * parsed.perPage
  };
}
