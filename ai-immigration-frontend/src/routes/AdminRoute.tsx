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
      <div className="flex min-h-screen items-center justify-center px-6">
        <div className="w-full max-w-md rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl p-8 text-center shadow-2xl shadow-black/40">
          <div
            className="
              mx-auto
              mb-5
              h-10
              w-10
              animate-spin
              rounded-full
              border-4
              border-white/10
              border-t-[#C6A15B]
            "
            aria-hidden="true"
          />

          <h1 className="text-lg font-black text-white">
            Checking permissions
          </h1>

          <p className="mt-2 text-sm text-slate-400">
            Verifying your administrator access.
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