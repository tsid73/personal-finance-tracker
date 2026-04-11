import { isRouteErrorResponse, Link, useRouteError } from "react-router-dom";

export function RouteErrorBoundary() {
  const error = useRouteError();

  const message = isRouteErrorResponse(error)
    ? `${error.status} ${error.statusText}`
    : error instanceof Error
      ? error.message
      : "Something unexpected happened.";

  return (
    <div className="flex min-h-screen items-center justify-center px-4 py-10">
      <div className="card w-full max-w-xl p-8">
        <div className="text-sm uppercase tracking-[0.3em] text-slate-400">Error</div>
        <h1 className="mt-3 text-3xl font-semibold text-ink">We hit a problem loading this page.</h1>
        <p className="mt-3 text-sm text-slate-500">{message}</p>
        <div className="mt-6 flex flex-wrap gap-3">
          <button
            className="rounded-2xl bg-ink px-4 py-3 text-sm font-medium text-white"
            onClick={() => window.location.reload()}
          >
            Reload
          </button>
          <Link className="rounded-2xl border border-slate-200 px-4 py-3 text-sm font-medium text-slate-700" to="/dashboard">
            Go to dashboard
          </Link>
        </div>
      </div>
    </div>
  );
}
