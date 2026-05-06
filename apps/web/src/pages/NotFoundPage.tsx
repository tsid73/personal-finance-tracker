import { Compass, House, SearchX } from "lucide-react";
import { Link } from "react-router-dom";

export function NotFoundPage() {
  return (
    <div className="card p-8 dark:border dark:border-slate-800">
      <div className="flex items-center gap-3 text-sm uppercase tracking-[0.3em] text-slate-400 dark:text-slate-500">
        <SearchX className="h-5 w-5" aria-hidden="true" />
        404
      </div>
      <h1 className="mt-3 text-3xl font-semibold text-ink dark:text-slate-100">Page not found</h1>
      <p className="mt-3 max-w-2xl text-sm text-slate-500 dark:text-slate-400">
        The page you requested does not exist or the link is stale.
      </p>
      <div className="mt-6 flex flex-wrap gap-3">
        <Link
          to="/dashboard"
          className="inline-flex items-center gap-2 rounded-lg bg-ink px-4 py-3 text-sm font-medium text-white dark:bg-slate-100 dark:text-slate-900"
        >
          <House className="h-4 w-4" aria-hidden="true" />
          Go to dashboard
        </Link>
        <Link
          to="/security"
          className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-4 py-3 text-sm font-medium text-slate-700 dark:border-slate-700 dark:text-slate-200"
        >
          <Compass className="h-4 w-4" aria-hidden="true" />
          View security policy
        </Link>
      </div>
    </div>
  );
}
