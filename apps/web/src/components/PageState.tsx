export function LoadingState({ message }: { message: string }) {
  return (
    <div className="card p-6 dark:border dark:border-slate-800">
      <div className="text-slate-500 dark:text-slate-400">{message}</div>
      <div className="mt-4 space-y-3">
        <div className="h-4 w-40 animate-pulse rounded bg-slate-100 dark:bg-slate-800" />
        <div className="h-4 w-full animate-pulse rounded bg-slate-100 dark:bg-slate-800" />
        <div className="h-4 w-5/6 animate-pulse rounded bg-slate-100 dark:bg-slate-800" />
      </div>
    </div>
  );
}

export function CardGridLoadingState({ count = 6 }: { count?: number }) {
  return (
    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
      {Array.from({ length: count }, (_, index) => (
        <div key={index} className="card p-5 dark:border dark:border-slate-800">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 animate-pulse rounded-full bg-slate-100 dark:bg-slate-800" />
            <div className="flex-1 space-y-2">
              <div className="h-4 w-2/3 animate-pulse rounded bg-slate-100 dark:bg-slate-800" />
              <div className="h-3 w-1/3 animate-pulse rounded bg-slate-100 dark:bg-slate-800" />
            </div>
          </div>
          <div className="mt-4 h-3 w-full animate-pulse rounded bg-slate-100 dark:bg-slate-800" />
          <div className="mt-3 h-3 w-5/6 animate-pulse rounded bg-slate-100 dark:bg-slate-800" />
        </div>
      ))}
    </div>
  );
}

export function TableLoadingState({ rows = 6 }: { rows?: number }) {
  return (
    <div className="card overflow-hidden dark:border dark:border-slate-800">
      <div className="overflow-x-auto">
        <table className="min-w-full border-collapse">
          <thead className="bg-mist dark:bg-slate-800">
            <tr>
              {Array.from({ length: 6 }, (_, index) => (
                <th key={index} className="px-6 py-4">
                  <div className="h-3 w-24 animate-pulse rounded bg-slate-200 dark:bg-slate-700" />
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {Array.from({ length: rows }, (_, rowIndex) => (
              <tr key={rowIndex} className="border-t border-slate-100 dark:border-slate-800">
                {Array.from({ length: 6 }, (_, colIndex) => (
                  <td key={colIndex} className="px-6 py-4">
                    <div className="h-4 w-full animate-pulse rounded bg-slate-100 dark:bg-slate-800" />
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export function ErrorState({ message }: { message: string }) {
  return <div className="card border border-rose-100 p-6 text-rose-600 dark:border-rose-900/60 dark:bg-slate-900" role="alert">{message}</div>;
}
