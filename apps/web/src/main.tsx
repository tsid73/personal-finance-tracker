import React, { Suspense, lazy } from "react";
import ReactDOM from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { createBrowserRouter, Navigate, RouterProvider } from "react-router-dom";
import { AppShell } from "./shell/AppShell";
import { LoadingState } from "./components/PageState";
import { RouteErrorBoundary } from "./components/RouteErrorBoundary";
import "./styles.css";

const queryClient = new QueryClient();
const DashboardPage = lazy(() => import("./pages/DashboardPage").then((module) => ({ default: module.DashboardPage })));
const TransactionsPage = lazy(() => import("./pages/TransactionsPage").then((module) => ({ default: module.TransactionsPage })));
const BudgetsPage = lazy(() => import("./pages/BudgetsPage").then((module) => ({ default: module.BudgetsPage })));
const CategoriesPage = lazy(() => import("./pages/CategoriesPage").then((module) => ({ default: module.CategoriesPage })));
const ReportsPage = lazy(() => import("./pages/ReportsPage").then((module) => ({ default: module.ReportsPage })));

function withSuspense(element: React.ReactNode) {
  return <Suspense fallback={<LoadingState message="Loading page..." />}>{element}</Suspense>;
}

const router = createBrowserRouter([
  {
    path: "/",
    element: <AppShell />,
    errorElement: <RouteErrorBoundary />,
    children: [
      { index: true, element: <Navigate to="/dashboard" replace /> },
      { path: "dashboard", element: withSuspense(<DashboardPage />) },
      { path: "transactions", element: withSuspense(<TransactionsPage />) },
      { path: "budgets", element: withSuspense(<BudgetsPage />) },
      { path: "categories", element: withSuspense(<CategoriesPage />) },
      { path: "reports", element: withSuspense(<ReportsPage />) }
    ]
  }
]);

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  </React.StrictMode>
);
