import React from "react";

import {
  Navigate,
  Outlet,
  useLocation,
} from "react-router-dom";

import { useAuth } from "../features/auth/hooks/useAuth";

/* ============================================================
   TYPES
   ============================================================ */

interface ProtectedRouteProps {
  children?: React.ReactNode;
}

/* ============================================================
   PROTECTED ROUTE
   ============================================================ */

export default function ProtectedRoute({
  children,
}: ProtectedRouteProps) {
  const {
    token,
    loading,
  } = useAuth();

  const location = useLocation();

  /* ==========================================================
     SESSION RESTORATION
     ========================================================== */

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-50 px-6">
        <div className="w-full max-w-md rounded-3xl border border-slate-200 bg-white p-8 text-center shadow-xl">

          <div
            className="
              mx-auto
              mb-5
              h-10
              w-10
              animate-spin
              rounded-full
              border-4
              border-slate-200
              border-t-[#F4B81A]
            "
            aria-hidden="true"
          />

          <h1 className="text-lg font-black text-[#0B1736]">
            Restoring your session
          </h1>

          <p className="mt-2 text-sm text-slate-500">
            Please wait while we verify your authentication.
          </p>

        </div>
      </div>
    );
  }

  /* ==========================================================
     AUTHENTICATION CHECK
     ========================================================== */

  if (!token) {
    if (import.meta.env.DEV) {
      console.warn(
        "[ProtectedRoute] Authentication required:",
        location.pathname
      );
    }

    return (
      <Navigate
        to="/login"
        replace
        state={{
          from: location,
          reason: "authentication_required",
        }}
      />
    );
  }

  /* ==========================================================
     CHILDREN COMPATIBILITY
     ========================================================== */

  if (children) {
    return <>{children}</>;
  }

  /* ==========================================================
     NESTED ROUTES
     ========================================================== */

  return <Outlet />;
}

