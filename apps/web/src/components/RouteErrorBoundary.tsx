import { House, RefreshCcw, TriangleAlert } from "lucide-react";
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
      <div className="card w-full max-w-xl p-8 dark:border dark:border-slate-800">
        <div className="flex items-center gap-3 text-sm uppercase tracking-[0.3em] text-slate-400 dark:text-slate-500">
          <TriangleAlert className="h-5 w-5" aria-hidden="true" />
          Error
        </div>
        <h1 className="mt-3 text-3xl font-semibold text-ink dark:text-slate-100">We hit a problem loading this page.</h1>
        <p className="mt-3 text-sm text-slate-500 dark:text-slate-400">{message}</p>
        <div className="mt-6 flex flex-wrap gap-3">
          <button
            className="inline-flex items-center gap-2 rounded-lg bg-ink px-4 py-3 text-sm font-medium text-white dark:bg-slate-100 dark:text-slate-900"
            onClick={() => window.location.reload()}
          >
            <RefreshCcw className="h-4 w-4" aria-hidden="true" />
            Reload
          </button>
          <Link className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-4 py-3 text-sm font-medium text-slate-700 dark:border-slate-700 dark:text-slate-200" to="/dashboard">
            <House className="h-4 w-4" aria-hidden="true" />
            Go to dashboard
          </Link>
        </div>
      </div>
    </div>
  );
}
