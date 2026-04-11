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

export function ErrorState({ message }: { message: string }) {
  return <div className="card border border-rose-100 p-6 text-rose-600 dark:border-rose-900/60 dark:bg-slate-900" role="alert">{message}</div>;
}
