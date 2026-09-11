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
            Restoring your session
          </h1>

          <p className="mt-2 text-sm text-slate-400">
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

