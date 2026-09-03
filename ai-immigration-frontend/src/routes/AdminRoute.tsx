import React from "react";
import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../features/auth/hooks/useAuth";

interface AdminRouteProps {
  children?: React.ReactNode;
}

export default function AdminRoute({
  children,
}: AdminRouteProps) {
  const {
    user,
    token,
    loading,
    hasRole,
  } = useAuth();

  const location = useLocation();

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-950">
        <div className="text-center">
          <div className="mx-auto mb-4 h-10 w-10 animate-spin rounded-full border-4 border-slate-700 border-t-indigo-500" />
          <p className="font-semibold text-white">
            Checking permissions...
          </p>
        </div>
      </div>
    );
  }

  if (!token) {
    return (
      <Navigate
        to="/login"
        replace
        state={{ from: location }}
      />
    );
  }

  if (!user || !hasRole("ADMIN")) {
    return (
      <Navigate
        to="/dashboard"
        replace
      />
    );
  }

  if (children) {
    return <>{children}</>;
  }

  return <Outlet />;
}