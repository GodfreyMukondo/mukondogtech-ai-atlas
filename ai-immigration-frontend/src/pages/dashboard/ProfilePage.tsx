import React, {
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";
import { Link, useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import {
  User,
  Mail,
  ShieldCheck,
  CreditCard,
  LogOut,
  Camera,
  ScanSearch,
  Sparkles,
  FileText,
  ChevronRight,
  Settings,
  Crown,
  BarChart3,
  Calendar,
  Edit3,
  Bell,
  AlertCircle,
  RefreshCcw,
  Loader2,
} from "lucide-react";

import api from "../../services/api";

/* ============================================================================
 * ENVIRONMENT CONFIGURATION
 * ========================================================================== */

const PROFILE_ENDPOINT =
  import.meta.env.VITE_PROFILE_ENDPOINT;

const LOGOUT_ENDPOINT =
  import.meta.env.VITE_LOGOUT_ENDPOINT;

const DASHBOARD_ROUTE =
  import.meta.env.VITE_DASHBOARD_ROUTE;

const LOGIN_ROUTE =
  import.meta.env.VITE_LOGIN_ROUTE;

const PROFILE_SETTINGS_ROUTE =
  import.meta.env.VITE_PROFILE_SETTINGS_ROUTE;

const SUBSCRIPTION_ROUTE =
  import.meta.env.VITE_SUBSCRIPTION_ROUTE;

const DOCUMENTS_ROUTE =
  import.meta.env.VITE_DOCUMENTS_ROUTE;

const REPORTS_ROUTE =
  import.meta.env.VITE_REPORTS_ROUTE;

const NOTIFICATION_SETTINGS_ROUTE =
  import.meta.env.VITE_NOTIFICATION_SETTINGS_ROUTE;

/**
 * The application intentionally fails clearly when required configuration
 * has not been supplied instead of silently calling an incorrect endpoint.
 */
const requiredConfiguration = {
  PROFILE_ENDPOINT,
  LOGOUT_ENDPOINT,
  DASHBOARD_ROUTE,
  LOGIN_ROUTE,
  PROFILE_SETTINGS_ROUTE,
  SUBSCRIPTION_ROUTE,
  DOCUMENTS_ROUTE,
  REPORTS_ROUTE,
  NOTIFICATION_SETTINGS_ROUTE,
};

const missingConfiguration = Object.entries(
  requiredConfiguration
)
  .filter(([, value]) => !value)
  .map(([key]) => key);

if (missingConfiguration.length > 0) {
  console.error(
    "ProfilePage configuration is incomplete.",
    {
      missingConfiguration,
    }
  );
}

/* ============================================================================
 * TYPES
 * ========================================================================== */

interface UserProfile {
  id?: string | number;

  name: string;

  email: string;

  role: string;

  plan: string;

  analysesUsed: number;

  analysesLimit: number;

  memberSince: string;

  verified: boolean;

  avatarUrl?: string | null;
}

interface ApiProfileResponse {
  id?: string | number;

  name?: string;

  fullName?: string;

  firstName?: string;

  lastName?: string;

  email?: string;

  role?: string;

  plan?: string;

  subscriptionPlan?: string;

  analysesUsed?: number;

  analysisCount?: number;

  analysesLimit?: number;

  analysisLimit?: number;

  memberSince?: string;

  createdAt?: string;

  verified?: boolean;

  emailVerified?: boolean;

  avatarUrl?: string | null;

  profileImageUrl?: string | null;
}

interface ApiErrorResponse {
  message?: string;

  error?: string;

  detail?: string;
}

/* ============================================================================
 * HELPERS
 * ========================================================================== */

function normalizeProfile(
  payload: ApiProfileResponse
): UserProfile {
  const fullName =
    payload.name ??
    payload.fullName ??
    [
      payload.firstName,
      payload.lastName,
    ]
      .filter(Boolean)
      .join(" ")
      .trim();

  return {
    id: payload.id,

    name: fullName || "User",

    email: payload.email ?? "",

    role: payload.role ?? "",

    plan:
      payload.plan ??
      payload.subscriptionPlan ??
      "",

    analysesUsed:
      Number(
        payload.analysesUsed ??
          payload.analysisCount ??
          0
      ),

    analysesLimit:
      Number(
        payload.analysesLimit ??
          payload.analysisLimit ??
          0
      ),

    memberSince:
      payload.memberSince ??
      payload.createdAt ??
      "",

    verified:
      Boolean(
        payload.verified ??
          payload.emailVerified ??
          false
      ),

    avatarUrl:
      payload.avatarUrl ??
      payload.profileImageUrl ??
      null,
  };
}

function getErrorMessage(
  error: unknown
): string {
  const candidate =
    error as {
      response?: {
        data?: ApiErrorResponse;
      };

      message?: string;
    };

  return (
    candidate?.response?.data?.message ??
    candidate?.response?.data?.error ??
    candidate?.response?.data?.detail ??
    candidate?.message ??
    "Unable to load your profile. Please try again."
  );
}

function formatMemberSince(
  value: string
): string {
  if (!value) {
    return "Not available";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat(
    undefined,
    {
      month: "long",
      year: "numeric",
    }
  ).format(date);
}

function getInitial(
  name: string
): string {
  const trimmed = name.trim();

  if (!trimmed) {
    return "?";
  }

  return trimmed
    .charAt(0)
    .toUpperCase();
}

/* ============================================================================
 * PAGE
 * ========================================================================== */

export default function ProfilePage() {
  const navigate = useNavigate();

  const [
    user,
    setUser,
  ] = useState<UserProfile | null>(null);

  const [
    loading,
    setLoading,
  ] = useState(true);

  const [
    refreshing,
    setRefreshing,
  ] = useState(false);

  const [
    error,
    setError,
  ] = useState<string | null>(null);

  const [
    loggingOut,
    setLoggingOut,
  ] = useState(false);

  /* --------------------------------------------------------------------------
   * FETCH PROFILE
   * ------------------------------------------------------------------------ */

  const loadProfile = useCallback(
    async (
      isRefresh = false
    ) => {
      if (!PROFILE_ENDPOINT) {
        setError(
          "Profile API configuration is missing."
        );

        setLoading(false);
        setRefreshing(false);

        return;
      }

      try {
        if (isRefresh) {
          setRefreshing(true);
        } else {
          setLoading(true);
        }

        setError(null);

        const response =
          await api.get(
            PROFILE_ENDPOINT
          );

        const payload =
          response?.data;

        const profile =
          normalizeProfile(
            payload?.data ??
              payload?.user ??
              payload
          );

        setUser(profile);
      } catch (requestError) {
        console.error(
          "Failed to load user profile:",
          requestError
        );

        setError(
          getErrorMessage(
            requestError
          )
        );
      } finally {
        setLoading(false);
        setRefreshing(false);
      }
    },
    []
  );

  useEffect(() => {
    void loadProfile();
  }, [loadProfile]);

  /* --------------------------------------------------------------------------
   * USAGE
   * ------------------------------------------------------------------------ */

  const usagePercentage =
    useMemo(() => {
      if (
        !user ||
        user.analysesLimit <= 0
      ) {
        return 0;
      }

      return Math.min(
        Math.max(
          (user.analysesUsed /
            user.analysesLimit) *
            100,
          0
        ),
        100
      );
    }, [user]);

  const remainingAnalyses =
    useMemo(() => {
      if (!user) {
        return 0;
      }

      return Math.max(
        user.analysesLimit -
          user.analysesUsed,
        0
      );
    }, [user]);

  /* --------------------------------------------------------------------------
   * LOGOUT
   * ------------------------------------------------------------------------ */

  const handleLogout =
    useCallback(async () => {
      if (loggingOut) {
        return;
      }

      try {
        setLoggingOut(true);

        if (LOGOUT_ENDPOINT) {
          await api.post(
            LOGOUT_ENDPOINT
          );
        }
      } catch (logoutError) {
        console.warn(
          "Server logout request failed:",
          logoutError
        );
      } finally {
        window.dispatchEvent(
          new Event(
            "auth:logout"
          )
        );

        if (LOGIN_ROUTE) {
          navigate(
            LOGIN_ROUTE,
            {
              replace: true,
            }
          );
        }
      }
    }, [
      loggingOut,
      navigate,
    ]);

  /* ==========================================================================
   * LOADING STATE
   * ======================================================================== */

  if (loading) {
    return (
      <main className="min-h-screen">
        <div className="mx-auto flex min-h-screen max-w-7xl items-center justify-center px-6">
          <div
            className="
              flex
              flex-col
              items-center
              justify-center
              rounded-3xl
              border
              border-white/10
              bg-white/5
              backdrop-blur-xl
              px-12
              py-12
              shadow-xl
            "
            role="status"
            aria-live="polite"
          >
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-[#C6A15B]/15">
              <Loader2
                size={28}
                className="animate-spin text-[#C6A15B]"
              />
            </div>

            <p className="mt-5 text-sm font-bold text-white">
              Loading your profile...
            </p>

            <p className="mt-1 text-xs text-slate-400">
              Please wait a moment.
            </p>
          </div>
        </div>
      </main>
    );
  }

  /* ==========================================================================
   * ERROR STATE
   * ======================================================================== */

  if (error && !user) {
    return (
      <main className="min-h-screen">
        <div className="mx-auto flex min-h-screen max-w-3xl items-center justify-center px-6">
          <div
            className="
              w-full
              rounded-[28px]
              border
              border-red-500/20
              bg-white/5
              backdrop-blur-xl
              p-10
              text-center
              shadow-xl
            "
            role="alert"
          >
            <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-red-500/10 text-red-300">
              <AlertCircle
                size={30}
              />
            </div>

            <h1 className="mt-6 text-2xl font-black text-white">
              Unable to load profile
            </h1>

            <p className="mx-auto mt-3 max-w-xl text-sm leading-6 text-slate-300">
              {error}
            </p>

            <button
              type="button"
              onClick={() =>
                void loadProfile()
              }
              className="
                mt-7
                inline-flex
                items-center
                gap-2
                rounded-xl
                bg-[#0B1F3A]
                px-6
                py-3
                text-sm
                font-bold
                text-white
                transition
                hover:bg-[#3C4C61]
                focus:outline-none
                focus:ring-2
                focus:ring-[#C6A15B]
                focus:ring-offset-2
              "
            >
              <RefreshCcw
                size={17}
              />

              Try Again
            </button>
          </div>
        </div>
      </main>
    );
  }

  if (!user) {
    return null;
  }

  /* ==========================================================================
   * PAGE
   * ======================================================================== */

  return (
    <main className="min-h-screen">
      <div className="w-full px-4 py-6 sm:px-6 lg:px-8 lg:py-8">

        {/* ====================================================================
            PAGE HEADER
        ===================================================================== */}

        <header className="mb-6">
          <div className="flex flex-col gap-5 xl:flex-row xl:items-center xl:justify-between">

            <div className="flex items-center gap-4">
              <Link
                to={
                  DASHBOARD_ROUTE ||
                  "/"
                }
                aria-label="Back to dashboard"
                className="
                  flex
                  h-11
                  w-11
                  shrink-0
                  items-center
                  justify-center
                  rounded-xl
                  border
                  border-white/15
                  bg-white/5
                  text-white
                  shadow-sm
                  transition
                  hover:border-[#C6A15B]/50
                  hover:bg-white/10
                "
              >
                <ScanSearch
                  size={19}
                  className="text-[#C6A15B]"
                />
              </Link>

              <div>
                <div className="flex items-center gap-2">
                  <h1 className="text-xl font-black tracking-tight text-white sm:text-2xl">
                    Profile
                  </h1>

                  <span className="hidden h-1 w-1 rounded-full bg-[#C6A15B] sm:block" />

                  <span className="hidden text-xs font-semibold text-slate-400 sm:block">
                    Account Management
                  </span>
                </div>

                <p className="mt-0.5 text-xs text-slate-400 sm:text-sm">
                  Manage your account, subscription,
                  and AI activity.
                </p>
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-2">

              <button
                type="button"
                onClick={() =>
                  void loadProfile(true)
                }
                disabled={refreshing}
                className="
                  inline-flex
                  h-10
                  items-center
                  gap-2
                  rounded-xl
                  border
                  border-white/15
                  bg-white/5
                  px-4
                  text-xs
                  font-bold
                  text-slate-200
                  shadow-sm
                  transition
                  hover:border-[#C6A15B]/50
                  hover:text-white
                  disabled:cursor-not-allowed
                  disabled:opacity-60
                "
              >
                <RefreshCcw
                  size={15}
                  className={
                    refreshing
                      ? "animate-spin"
                      : ""
                  }
                />

                Refresh
              </button>

              <Link
                to={
                  PROFILE_SETTINGS_ROUTE ||
                  "/"
                }
                className="
                  inline-flex
                  h-10
                  items-center
                  gap-2
                  rounded-xl
                  border
                  border-white/15
                  bg-white/5
                  px-4
                  text-xs
                  font-bold
                  text-slate-200
                  shadow-sm
                  transition
                  hover:border-[#C6A15B]/50
                  hover:bg-white/10
                  hover:text-white
                "
              >
                <Settings
                  size={15}
                />

                Settings
              </Link>

              <button
                type="button"
                onClick={
                  handleLogout
                }
                disabled={loggingOut}
                className="
                  inline-flex
                  h-10
                  items-center
                  gap-2
                  rounded-xl
                  bg-[#0B1F3A]
                  px-4
                  text-xs
                  font-bold
                  text-white
                  shadow-sm
                  transition
                  hover:bg-[#3C4C61]
                  disabled:cursor-not-allowed
                  disabled:opacity-60
                "
              >
                {loggingOut ? (
                  <Loader2
                    size={15}
                    className="animate-spin"
                  />
                ) : (
                  <LogOut
                    size={15}
                  />
                )}

                {loggingOut
                  ? "Signing Out..."
                  : "Sign Out"}
              </button>
            </div>
          </div>
        </header>

        {/* ====================================================================
            REFRESH ERROR
        ===================================================================== */}

        {error && user && (
          <div
            className="
              mb-5
              flex
              items-start
              justify-between
              gap-4
              rounded-xl
              border
              border-amber-500/30
              bg-amber-500/10
              px-4
              py-3
            "
            role="alert"
          >
            <div className="flex items-start gap-3">
              <AlertCircle
                size={17}
                className="mt-0.5 shrink-0 text-amber-400"
              />

              <p className="text-xs font-medium leading-5 text-amber-300">
                {error}
              </p>
            </div>

            <button
              type="button"
              onClick={() =>
                void loadProfile(
                  true
                )
              }
              className="
                shrink-0
                text-xs
                font-bold
                text-amber-300
                underline
                underline-offset-2
              "
            >
              Retry
            </button>
          </div>
        )}

        {/* ====================================================================
            PROFILE HERO
        ===================================================================== */}

        <motion.section
          initial={{
            opacity: 0,
            y: 12,
          }}
          animate={{
            opacity: 1,
            y: 0,
          }}
          transition={{
            duration: 0.3,
          }}
          className="
            relative
            overflow-hidden
            rounded-2xl
            bg-[#0B1F3A]
            shadow-xl
            shadow-[#0B1F3A]/10
          "
        >
          {/* Decorative background */}

          <div
            className="
              pointer-events-none
              absolute
              -right-24
              -top-32
              h-72
              w-72
              rounded-full
              bg-[#C6A15B]/10
              blur-3xl
            "
          />

          <div
            className="
              pointer-events-none
              absolute
              -bottom-32
              left-1/3
              h-64
              w-64
              rounded-full
              bg-blue-500/10
              blur-3xl
            "
          />

          <div className="relative p-5 sm:p-6 lg:p-7">
            <div className="flex flex-col gap-6 xl:flex-row xl:items-center xl:justify-between">

              {/* Identity */}

              <div className="flex min-w-0 items-center gap-4 sm:gap-5">

                {/* Avatar */}

                <div className="relative shrink-0">
                  {user.avatarUrl ? (
                    <img
                      src={
                        user.avatarUrl
                      }
                      alt={`${user.name} profile`}
                      className="
                        h-20
                        w-20
                        rounded-2xl
                        object-cover
                        ring-4
                        ring-white/10
                        sm:h-24
                        sm:w-24
                      "
                    />
                  ) : (
                    <div
                      className="
                        flex
                        h-20
                        w-20
                        items-center
                        justify-center
                        rounded-2xl
                        bg-white/10
                        text-2xl
                        font-black
                        text-[#C6A15B]
                        ring-4
                        ring-white/10
                        sm:h-24
                        sm:w-24
                        sm:text-3xl
                      "
                    >
                      {getInitial(
                        user.name
                      )}
                    </div>
                  )}

                  <button
                    type="button"
                    disabled
                    title="Profile photo management"
                    aria-label="Profile photo management"
                    className="
                      absolute
                      -bottom-1.5
                      -right-1.5
                      flex
                      h-8
                      w-8
                      items-center
                      justify-center
                      rounded-lg
                      border-2
                      border-[#0B1F3A]
                      bg-[#C6A15B]
                      text-[#0B1F3A]
                      shadow-lg
                      disabled:cursor-not-allowed
                    "
                  >
                    <Camera
                      size={14}
                    />
                  </button>
                </div>

                {/* User details */}

                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <h2 className="truncate text-xl font-black text-white sm:text-2xl">
                      {user.name}
                    </h2>

                    {user.verified && (
                      <span
                        className="
                          inline-flex
                          items-center
                          gap-1
                          rounded-full
                          bg-emerald-400/10
                          px-2.5
                          py-1
                          text-[9px]
                          font-extrabold
                          uppercase
                          tracking-wider
                          text-emerald-300
                        "
                      >
                        <ShieldCheck
                          size={11}
                        />

                        Verified
                      </span>
                    )}
                  </div>

                  {user.email && (
                    <div className="mt-1 flex items-center gap-2 text-sm text-slate-300">
                      <Mail
                        size={14}
                      />

                      <span className="truncate">
                        {user.email}
                      </span>
                    </div>
                  )}

                  <div className="mt-3 flex flex-wrap items-center gap-2">
                    {user.role && (
                      <span className="rounded-full bg-white/10 px-3 py-1 text-[9px] font-bold uppercase tracking-wider text-slate-200">
                        {user.role}
                      </span>
                    )}

                    {user.plan && (
                      <span className="inline-flex items-center gap-1.5 rounded-full bg-[#C6A15B]/15 px-3 py-1 text-[9px] font-bold uppercase tracking-wider text-[#F4C84A]">
                        <Crown
                          size={11}
                        />

                        {user.plan}
                      </span>
                    )}
                  </div>
                </div>
              </div>

              {/* Hero actions */}

              <div className="flex flex-col gap-3 sm:flex-row xl:flex-col 2xl:flex-row">
                <Link
                  to={
                    PROFILE_SETTINGS_ROUTE ||
                    "/"
                  }
                  className="
                    inline-flex
                    items-center
                    justify-center
                    gap-2
                    rounded-xl
                    bg-white
                    px-5
                    py-2.5
                    text-xs
                    font-extrabold
                    text-[#0B1F3A]
                    transition
                    hover:bg-[#FFF7DC]
                  "
                >
                  <Edit3
                    size={15}
                  />

                  Edit Profile
                </Link>

                <div className="flex items-center justify-center gap-2 rounded-xl border border-white/10 bg-white/5 px-5 py-2.5">
                  <Calendar
                    size={14}
                    className="text-[#C6A15B]"
                  />

                  <span className="text-[10px] font-semibold text-slate-300">
                    Member since{" "}
                    <span className="font-bold text-white">
                      {formatMemberSince(
                        user.memberSince
                      )}
                    </span>
                  </span>
                </div>
              </div>
            </div>
          </div>
        </motion.section>

        {/* ====================================================================
            MAIN CONTENT
        ===================================================================== */}

        <div className="mt-6 grid gap-6 lg:grid-cols-3">

          {/* ==================================================================
              LEFT COLUMN
          =================================================================== */}

          <div className="space-y-6 lg:col-span-2">

            {/* ================================================================
                ACCOUNT INFORMATION
            ================================================================ */}

            <ProfileSection
              title="Account Information"
              description="Your personal and account details."
              icon={
                <User
                  size={17}
                />
              }
            >
              <div className="grid gap-3 md:grid-cols-2">

                <InfoCard
                  icon={
                    <User
                      size={16}
                    />
                  }
                  label="Full Name"
                  value={
                    user.name ||
                    "Not available"
                  }
                />

                <InfoCard
                  icon={
                    <Mail
                      size={16}
                    />
                  }
                  label="Email Address"
                  value={
                    user.email ||
                    "Not available"
                  }
                  verified={
                    user.verified
                  }
                />

                <InfoCard
                  icon={
                    <ShieldCheck
                      size={16}
                    />
                  }
                  label="Account Role"
                  value={
                    user.role ||
                    "Not available"
                  }
                />

                <InfoCard
                  icon={
                    <Calendar
                      size={16}
                    />
                  }
                  label="Member Since"
                  value={formatMemberSince(
                    user.memberSince
                  )}
                />
              </div>
            </ProfileSection>

            {/* ================================================================
                AI USAGE
            ================================================================ */}

            <ProfileSection
              title="AI Usage"
              description="Monitor your document analysis activity."
              icon={
                <Sparkles
                  size={17}
                />
              }
            >
              <div className="grid gap-5 md:grid-cols-3">

                {/* Used */}

                <UsageMetric
                  label="Analyses Used"
                  value={user.analysesUsed.toLocaleString()}
                  icon={
                    <BarChart3
                      size={17}
                    />
                  }
                />

                {/* Remaining */}

                <UsageMetric
                  label="Remaining"
                  value={
                    user.analysesLimit > 0
                      ? remainingAnalyses.toLocaleString()
                      : "—"
                  }
                  icon={
                    <Sparkles
                      size={17}
                    />
                  }
                />

                {/* Plan */}

                <UsageMetric
                  label="Current Plan"
                  value={
                    user.plan ||
                    "Standard"
                  }
                  icon={
                    <Crown
                      size={17}
                    />
                  }
                />
              </div>

              {user.analysesLimit >
                0 && (
                <div className="mt-6 rounded-xl border border-white/10 bg-white/5 p-4">
                  <div className="mb-3 flex items-center justify-between gap-3">
                    <div>
                      <p className="text-xs font-extrabold text-white">
                        Analysis allowance
                      </p>

                      <p className="mt-0.5 text-[10px] text-slate-400">
                        Your current usage against
                        your plan limit.
                      </p>
                    </div>

                    <span className="text-sm font-black text-white">
                      {usagePercentage.toFixed(
                        0
                      )}
                      %
                    </span>
                  </div>

                  <div
                    className="h-2 overflow-hidden rounded-full bg-white/10"
                    role="progressbar"
                    aria-valuemin={0}
                    aria-valuemax={100}
                    aria-valuenow={
                      usagePercentage
                    }
                    aria-label="AI analysis usage"
                  >
                    <motion.div
                      initial={{
                        width: 0,
                      }}
                      animate={{
                        width: `${usagePercentage}%`,
                      }}
                      transition={{
                        duration: 0.7,
                        ease: "easeOut",
                      }}
                      className="h-full rounded-full bg-gradient-to-r from-[#D89D00] via-[#C6A15B] to-[#FFD76A]"
                    />
                  </div>

                  <div className="mt-2 flex items-center justify-between">
                    <span className="text-[10px] font-semibold text-slate-400">
                      {user.analysesUsed.toLocaleString()}{" "}
                      used
                    </span>

                    <span className="text-[10px] font-semibold text-slate-400">
                      {user.analysesLimit.toLocaleString()}{" "}
                      total
                    </span>
                  </div>
                </div>
              )}
            </ProfileSection>
          </div>

          {/* ==================================================================
              RIGHT COLUMN
          =================================================================== */}

          <aside className="space-y-4">

            {/* ================================================================
                SUBSCRIPTION
            ================================================================ */}

            <QuickActionCard
              icon={
                <CreditCard
                  size={18}
                />
              }
              title="Subscription"
              description={
                user.plan
                  ? `${user.plan} plan`
                  : "Manage your subscription"
              }
              action="Manage Plan"
              link={
                SUBSCRIPTION_ROUTE ||
                "/"
              }
              featured
            />

            {/* ================================================================
                DOCUMENTS
            ================================================================ */}

            <QuickActionCard
              icon={
                <FileText
                  size={18}
                />
              }
              title="Documents"
              description="View and manage your uploaded documents."
              action="View Documents"
              link={
                DOCUMENTS_ROUTE ||
                "/"
              }
            />

            {/* ================================================================
                REPORTS
            ================================================================ */}

            <QuickActionCard
              icon={
                <BarChart3
                  size={18}
                />
              }
              title="Reports"
              description="Review your AI analysis reports and results."
              action="View Reports"
              link={
                REPORTS_ROUTE ||
                "/"
              }
            />

            {/* ================================================================
                NOTIFICATIONS
            ================================================================ */}

            <QuickActionCard
              icon={
                <Bell
                  size={18}
                />
              }
              title="Notifications"
              description="Manage alerts and account notifications."
              action="Notification Settings"
              link={
                NOTIFICATION_SETTINGS_ROUTE ||
                "/"
              }
            />

            {/* ================================================================
                SECURITY CARD
            ================================================================ */}

            <div className="rounded-2xl border border-emerald-500/20 bg-emerald-500/10 p-5">
              <div className="flex items-start gap-3">
                <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-emerald-500/15 text-emerald-300">
                  <ShieldCheck
                    size={17}
                  />
                </div>

                <div>
                  <p className="text-xs font-extrabold text-emerald-300">
                    Account Security
                  </p>

                  <p className="mt-1 text-[10px] leading-5 text-emerald-300/80">
                    Your account information is
                    protected by the application's
                    authentication and security
                    controls.
                  </p>
                </div>
              </div>
            </div>
          </aside>
        </div>

        {/* ====================================================================
            FOOTER
        ===================================================================== */}

        <footer className="mt-8 border-t border-white/10 pt-5">
          <div className="flex flex-col gap-2 text-[10px] text-slate-500 sm:flex-row sm:items-center sm:justify-between">
            <span>
              Profile information is managed securely
              within your account.
            </span>

            {user.id !==
              undefined && (
              <span className="font-mono">
                Account ID:{" "}
                {String(user.id)}
              </span>
            )}
          </div>
        </footer>
      </div>
    </main>
  );
}

/* ============================================================================
 * PROFILE SECTION
 * ========================================================================== */

interface ProfileSectionProps {
  title: string;

  description: string;

  icon: React.ReactNode;

  children: React.ReactNode;
}

function ProfileSection({
  title,
  description,
  icon,
  children,
}: ProfileSectionProps) {
  return (
    <section
      className="
        overflow-hidden
        rounded-2xl
        border
        border-white/10
        bg-white/5
        backdrop-blur-xl
        shadow-sm
      "
    >
      <div className="border-b border-white/10 px-5 py-4 sm:px-6">
        <div className="flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-[#C6A15B]/15 text-[#C6A15B]">
            {icon}
          </div>

          <div>
            <h3 className="text-sm font-extrabold text-white">
              {title}
            </h3>

            <p className="mt-0.5 text-[10px] text-slate-400">
              {description}
            </p>
          </div>
        </div>
      </div>

      <div className="p-5 sm:p-6">
        {children}
      </div>
    </section>
  );
}

/* ============================================================================
 * INFORMATION CARD
 * ========================================================================== */

interface InfoCardProps {
  icon: React.ReactNode;

  label: string;

  value: string;

  verified?: boolean;
}

function InfoCard({
  icon,
  label,
  value,
  verified,
}: InfoCardProps) {
  return (
    <div
      className="
        group
        rounded-xl
        border
        border-white/10
        bg-white/5
        p-4
        transition-all
        duration-200
        hover:border-[#C6A15B]/30
        hover:bg-white/10
      "
    >
      <div className="flex items-center justify-between gap-3">
        <div className="flex min-w-0 items-center gap-2.5 text-slate-400">
          <div className="shrink-0">
            {icon}
          </div>

          <span className="truncate text-[10px] font-bold uppercase tracking-wider">
            {label}
          </span>
        </div>

        {verified && (
          <ShieldCheck
            size={13}
            className="shrink-0 text-emerald-500"
          />
        )}
      </div>

      <p className="mt-3 break-words text-sm font-bold text-white">
        {value}
      </p>
    </div>
  );
}

/* ============================================================================
 * USAGE METRIC
 * ========================================================================== */

interface UsageMetricProps {
  label: string;

  value: string;

  icon: React.ReactNode;
}

function UsageMetric({
  label,
  value,
  icon,
}: UsageMetricProps) {
  return (
    <div className="rounded-xl border border-white/10 bg-white/5 p-4">
      <div className="flex items-center gap-2 text-slate-400">
        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-white/10 text-[#C6A15B] shadow-sm">
          {icon}
        </div>

        <span className="text-[10px] font-bold uppercase tracking-wider">
          {label}
        </span>
      </div>

      <p className="mt-3 truncate text-xl font-black text-white">
        {value}
      </p>
    </div>
  );
}

/* ============================================================================
 * QUICK ACTION CARD
 * ========================================================================== */

interface QuickActionCardProps {
  icon: React.ReactNode;

  title: string;

  description: string;

  action: string;

  link: string;

  featured?: boolean;
}

function QuickActionCard({
  icon,
  title,
  description,
  action,
  link,
  featured = false,
}: QuickActionCardProps) {
  return (
    <Link
      to={link}
      className={`
        group
        block
        overflow-hidden
        rounded-2xl
        border
        p-5
        shadow-sm
        transition-all
        duration-200
        hover:-translate-y-0.5
        hover:shadow-md
        focus:outline-none
        focus:ring-2
        focus:ring-[#C6A15B]
        focus:ring-offset-2
        ${
          featured
            ? "border-[#C6A15B]/30 bg-gradient-to-br from-[#C6A15B]/15 to-[#C6A15B]/5"
            : "border-white/10 bg-white/5 hover:border-[#C6A15B]/40"
        }
      `}
    >
      <div className="flex items-start justify-between gap-4">
        <div
          className={`
            flex
            h-10
            w-10
            shrink-0
            items-center
            justify-center
            rounded-xl
            ${
              featured
                ? "bg-[#C6A15B]/20 text-[#C6A15B]"
                : "bg-white/10 text-white"
            }
          `}
        >
          {icon}
        </div>

        <ChevronRight
          size={16}
          className="
            mt-1
            shrink-0
            text-slate-500
            transition-transform
            duration-200
            group-hover:translate-x-1
            group-hover:text-white
          "
        />
      </div>

      <div className="mt-4">
        <h4 className="text-sm font-extrabold text-white">
          {title}
        </h4>

        <p className="mt-1 text-[11px] leading-5 text-slate-400">
          {description}
        </p>
      </div>

      <div className="mt-4 flex items-center gap-1.5 text-[10px] font-extrabold text-white">
        <span>
          {action}
        </span>

        <ChevronRight
          size={12}
          className="transition-transform group-hover:translate-x-0.5"
        />
      </div>
    </Link>
  );
}