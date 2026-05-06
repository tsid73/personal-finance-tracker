export function SecurityPolicyPage() {
  return (
    <div className="card p-8 dark:border dark:border-slate-800">
      <div className="text-sm uppercase tracking-[0.3em] text-slate-400 dark:text-slate-500">Security policy</div>
      <h1 className="mt-3 text-3xl font-semibold text-ink dark:text-slate-100">Private deployment only</h1>
      <div className="mt-4 space-y-4 text-sm leading-6 text-slate-600 dark:text-slate-300">
        <p>
          This finance app is designed for localhost or a trusted private network. It does not include authentication and should not be exposed directly to the public internet in its current form.
        </p>
        <p>
          There is no public bug bounty or internet-facing disclosure program for this deployment. If you are using this repository in a team or internal environment, route security reports through your normal maintainer channel before deploying the web app publicly.
        </p>
      </div>
    </div>
  );
}
