export function getTotalPages(totalItems: number, perPage: number) {
  return Math.max(1, Math.ceil(totalItems / perPage));
}

export function getVisibleRange(page: number, perPage: number, totalItems: number) {
  if (totalItems === 0) {
    return "0 of 0";
  }

  const start = (page - 1) * perPage + 1;
  const end = Math.min(page * perPage, totalItems);
  return `${start}-${end} of ${totalItems}`;
}
