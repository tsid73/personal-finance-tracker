import type { QueryClient, QueryKey } from "@tanstack/react-query";

export async function invalidateActiveQueries(queryClient: QueryClient, queryKeys: QueryKey[]) {
  await Promise.all(
    queryKeys.map((queryKey) =>
      queryClient.invalidateQueries({
        queryKey,
        refetchType: "active"
      })
    )
  );
}
