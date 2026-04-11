export function LoadingState({ message }: { message: string }) {
  return (
    <div className="card p-6">
      <div className="text-slate-500">{message}</div>
      <div className="mt-4 space-y-3">
        <div className="h-4 w-40 animate-pulse rounded bg-slate-100" />
        <div className="h-4 w-full animate-pulse rounded bg-slate-100" />
        <div className="h-4 w-5/6 animate-pulse rounded bg-slate-100" />
      </div>
    </div>
  );
}

export function ErrorState({ message }: { message: string }) {
  return <div className="card border border-rose-100 p-6 text-rose-600">{message}</div>;
}
