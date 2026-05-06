import type { ErrorInfo, ReactNode } from "react";
import { Component } from "react";
import { House, RefreshCcw, ShieldAlert } from "lucide-react";

type CrashBoundaryProps = {
  children: ReactNode;
};

type CrashBoundaryState = {
  error: Error | null;
};

export class AppCrashBoundary extends Component<CrashBoundaryProps, CrashBoundaryState> {
  state: CrashBoundaryState = {
    error: null
  };

  private readonly handleWindowError = (event: ErrorEvent) => {
    this.setState({
      error: event.error instanceof Error ? event.error : new Error(event.message || "Unexpected application error.")
    });
  };

  private readonly handleUnhandledRejection = (event: PromiseRejectionEvent) => {
    const reason = event.reason;
    this.setState({
      error: reason instanceof Error ? reason : new Error(typeof reason === "string" ? reason : "Unexpected application error.")
    });
  };

  static getDerivedStateFromError(error: Error): CrashBoundaryState {
    return { error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error("Unhandled React render error", error, errorInfo);
  }

  componentDidMount() {
    window.addEventListener("error", this.handleWindowError);
    window.addEventListener("unhandledrejection", this.handleUnhandledRejection);
  }

  componentWillUnmount() {
    window.removeEventListener("error", this.handleWindowError);
    window.removeEventListener("unhandledrejection", this.handleUnhandledRejection);
  }

  render() {
    if (!this.state.error) {
      return this.props.children;
    }

    return (
      <div className="flex min-h-screen items-center justify-center px-4 py-10">
        <div className="card w-full max-w-xl p-8 dark:border dark:border-slate-800">
          <div className="flex items-center gap-3 text-sm uppercase tracking-[0.3em] text-slate-400 dark:text-slate-500">
            <ShieldAlert className="h-5 w-5" aria-hidden="true" />
            Application error
          </div>
          <h1 className="mt-3 text-3xl font-semibold text-ink dark:text-slate-100">The app hit an unrecoverable error.</h1>
          <p className="mt-3 text-sm text-slate-500 dark:text-slate-400">
            {this.state.error.message || "Something unexpected happened."}
          </p>
          <div className="mt-6 flex flex-wrap gap-3">
            <button
              type="button"
              className="inline-flex items-center gap-2 rounded-lg bg-ink px-4 py-3 text-sm font-medium text-white dark:bg-slate-100 dark:text-slate-900"
              onClick={() => window.location.reload()}
            >
              <RefreshCcw className="h-4 w-4" aria-hidden="true" />
              Reload
            </button>
            <a
              className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-4 py-3 text-sm font-medium text-slate-700 dark:border-slate-700 dark:text-slate-200"
              href="/dashboard"
            >
              <House className="h-4 w-4" aria-hidden="true" />
              Go to dashboard
            </a>
          </div>
        </div>
      </div>
    );
  }
}
