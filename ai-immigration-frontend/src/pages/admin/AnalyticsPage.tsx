
// ============================================================
// ANALYTICS PAGE
// Production-ready administrative analytics dashboard.
//
// File:
// src/pages/admin/AnalyticsPage.tsx
//
// Responsibilities:
// - Load analytics overview from the analytics service
// - Display document, AI, and processing metrics
// - Provide manual refresh functionality
// - Handle loading and API error states
// - Provide responsive production-ready UI
// - Match the LUCCO / MukondoGTech administration theme
// ============================================================

import {
  Activity,
  CheckCircle2,
  FileText,
  MessageCircle,
  RefreshCw,
  ShieldCheck,
  TrendingUp,
  Zap,
} from "lucide-react";

import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";

import {
  getAnalyticsOverview,
} from "../../features/analytics/analyticsService";

import type {
  AnalyticsOverview,
} from "../../types/analytics";

import Loader from "../../components/common/Loader";

import ErrorAlert from "../../components/common/ErrorAlert";

// ============================================================
// TYPES
// ============================================================

interface AnalyticsCardProps {
  icon: ReactNode;
  title: string;
  value: number | string | undefined;
  description: string;
  iconClassName?: string;
  accentClassName?: string;
}

interface MetricSummaryProps {
  label: string;
  value: string | number;
  icon: ReactNode;
}

// ============================================================
// CONSTANTS
// ============================================================

// ============================================================
// NUMBER FORMATTER
// ============================================================

function formatNumber(
  value: number | undefined,
): string {
  if (
    typeof value !== "number" ||
    Number.isNaN(value)
  ) {
    return "0";
  }

  return value.toLocaleString();
}

// ============================================================
// PERCENTAGE FORMATTER
// ============================================================

function formatPercentage(
  value: number | undefined,
): string {
  if (
    typeof value !== "number" ||
    Number.isNaN(value)
  ) {
    return "0%";
  }

  return `${value}%`;
}

// ============================================================
// COMPONENT
// ============================================================

export default function AnalyticsPage() {
  // ==========================================================
  // STATE
  // ==========================================================

  const [
    analytics,
    setAnalytics,
  ] = useState<AnalyticsOverview | null>(null);

  const [
    loading,
    setLoading,
  ] = useState(true);

  const [
    error,
    setError,
  ] = useState("");

  const [
    lastUpdated,
    setLastUpdated,
  ] = useState<Date | null>(null);

  // ==========================================================
  // LOAD ANALYTICS
  // ==========================================================

  const loadAnalytics = useCallback(
    async () => {
      try {
        setLoading(true);
        setError("");

        const data =
          await getAnalyticsOverview();

        setAnalytics(data);
        setLastUpdated(new Date());
      } catch (err: unknown) {
        console.error(
          "Failed to load analytics:",
          err,
        );

        const message =
          err instanceof Error
            ? err.message
            : "Unable to load analytics. Please try again.";

        setError(message);
      } finally {
        setLoading(false);
      }
    },
    [],
  );

  // ==========================================================
  // INITIAL LOAD
  // ==========================================================

  useEffect(() => {
    let mounted = true;

    const initialize = async () => {
      try {
        setLoading(true);
        setError("");

        const data =
          await getAnalyticsOverview();

        if (!mounted) {
          return;
        }

        setAnalytics(data);
        setLastUpdated(new Date());
      } catch (err: unknown) {
        if (!mounted) {
          return;
        }

        console.error(
          "Analytics initialization failed:",
          err,
        );

        const message =
          err instanceof Error
            ? err.message
            : "Unable to load analytics. Please try again.";

        setError(message);
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };

    initialize();

    return () => {
      mounted = false;
    };
  }, []);

  // ==========================================================
  // DERIVED METRICS
  // ==========================================================

  const documentTotal = useMemo(
    () =>
      formatNumber(
        analytics?.documents?.totalDocuments,
      ),
    [analytics],
  );

  const queryTotal = useMemo(
    () =>
      formatNumber(
        analytics?.chat?.totalQueries,
      ),
    [analytics],
  );

  const accuracyRate = useMemo(
    () =>
      formatPercentage(
        analytics?.processing?.accuracyRate,
      ),
    [analytics],
  );

  // ==========================================================
  // LAST UPDATED LABEL
  // ==========================================================

  const lastUpdatedLabel = useMemo(() => {
    if (!lastUpdated) {
      return "Not updated yet";
    }

    return lastUpdated.toLocaleTimeString(
      [],
      {
        hour: "2-digit",
        minute: "2-digit",
      },
    );
  }, [lastUpdated]);

  // ==========================================================
  // LOADING STATE
  // ==========================================================

  if (loading && !analytics) {
    return (
      <div
        className="
          flex
          min-h-[60vh]
          items-center
          justify-center
          rounded-3xl
          bg-white/5
          px-6
        "
      >
        <Loader
          text="Loading analytics..."
        />
      </div>
    );
  }

  // ==========================================================
  // RENDER
  // ==========================================================

  return (
    <div
      className="
        min-h-screen
      "
    >
      <div
        className="
          w-full
          px-4
          py-6
          sm:px-6
          sm:py-8
          lg:px-8
          lg:py-10
        "
      >
        {/* ==================================================
            PAGE HEADER
        ================================================== */}

        <section
          className="
            relative
            overflow-hidden
            rounded-[2rem]
            border
            border-white/10
            bg-white/5
            backdrop-blur-xl
            shadow-sm
          "
        >
          {/* Decorative background */}

          <div
            className="
              pointer-events-none
              absolute
              -right-24
              -top-24
              h-72
              w-72
              rounded-full
              bg-[#C6A15B]/10
              blur-3xl
            "
            aria-hidden="true"
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
              bg-blue-500/5
              blur-3xl
            "
            aria-hidden="true"
          />

          <div
            className="
              relative
              flex
              flex-col
              gap-6
              p-6
              sm:p-8
              lg:flex-row
              lg:items-center
              lg:justify-between
            "
          >
            {/* Header information */}

            <div className="min-w-0">
              <div
                className="
                  mb-3
                  inline-flex
                  items-center
                  gap-2
                  rounded-full
                  border
                  border-[#C6A15B]/30
                  bg-[#C6A15B]/10
                  px-3
                  py-1.5
                  text-xs
                  font-bold
                  uppercase
                  tracking-wider
                  text-[#C6A15B]
                "
              >
                <Activity
                  size={14}
                  aria-hidden="true"
                />

                <span>
                  Platform Intelligence
                </span>
              </div>

              <h1
                className="
                  text-3xl
                  font-black
                  tracking-tight
                  text-white
                  sm:text-4xl
                  lg:text-5xl
                "
              >
                Analytics
              </h1>

              <p
                className="
                  mt-3
                  max-w-2xl
                  text-sm
                  leading-6
                  text-slate-300
                  sm:text-base
                "
              >
                Monitor platform usage, AI activity,
                document processing, and operational
                performance from one central dashboard.
              </p>

              {/* Last updated */}

              <div
                className="
                  mt-4
                  flex
                  flex-wrap
                  items-center
                  gap-2
                  text-xs
                  font-medium
                  text-slate-400
                "
              >
                <span
                  className="
                    h-2
                    w-2
                    rounded-full
                    bg-emerald-500
                  "
                  aria-hidden="true"
                />

                <span>
                  Last updated {lastUpdatedLabel}
                </span>
              </div>
            </div>

            {/* Refresh */}

            <button
              type="button"
              onClick={loadAnalytics}
              disabled={loading}
              aria-label="Refresh analytics"
              className="
                inline-flex
                min-h-11
                shrink-0
                items-center
                justify-center
                gap-2
                rounded-2xl
                bg-[#0B1F3A]
                px-5
                py-3
                text-sm
                font-bold
                text-white
                shadow-lg
                shadow-[#0B1F3A]/10
                transition-all
                duration-200
                hover:-translate-y-0.5
                hover:bg-[#13244D]
                hover:shadow-xl
                focus:outline-none
                focus:ring-2
                focus:ring-[#C6A15B]
                focus:ring-offset-2
                disabled:cursor-not-allowed
                disabled:opacity-60
                disabled:hover:translate-y-0
              "
            >
              <RefreshCw
                size={17}
                className={
                  loading
                    ? "animate-spin"
                    : ""
                }
                aria-hidden="true"
              />

              <span>
                {loading
                  ? "Refreshing..."
                  : "Refresh Data"}
              </span>
            </button>
          </div>
        </section>

        {/* ==================================================
            ERROR
        ================================================== */}

        {error && (
          <div className="mt-6">
            <ErrorAlert
              message={error}
            />
          </div>
        )}

        {/* ==================================================
            PRIMARY ANALYTICS CARDS
        ================================================== */}

        <section
          className="
            mt-8
            grid
            gap-5
            md:grid-cols-2
            xl:grid-cols-3
          "
          aria-label="Analytics overview"
        >
          {/* Documents */}

          <AnalyticsCard
            icon={
              <FileText
                size={25}
                aria-hidden="true"
              />
            }
            title="Document Uploads"
            value={documentTotal}
            description="Total documents uploaded to the platform."
            iconClassName="
              bg-blue-500/10
              text-blue-300
              ring-blue-500/20
            "
            accentClassName="
              from-blue-500/10
              via-transparent
              to-transparent
            "
          />

          {/* AI Queries */}

          <AnalyticsCard
            icon={
              <MessageCircle
                size={25}
                aria-hidden="true"
              />
            }
            title="AI Query Usage"
            value={queryTotal}
            description="Total conversations handled by the AI assistant."
            iconClassName="
              bg-violet-500/10
              text-violet-300
              ring-violet-500/20
            "
            accentClassName="
              from-violet-500/10
              via-transparent
              to-transparent
            "
          />

          {/* Accuracy */}

          <AnalyticsCard
            icon={
              <TrendingUp
                size={25}
                aria-hidden="true"
              />
            }
            title="AI Accuracy"
            value={accuracyRate}
            description="Current AI document analysis accuracy rate."
            iconClassName="
              bg-emerald-500/10
              text-emerald-300
              ring-emerald-500/20
            "
            accentClassName="
              from-emerald-500/10
              via-transparent
              to-transparent
            "
          />
        </section>

        {/* ==================================================
            SECONDARY PLATFORM SUMMARY
        ================================================== */}

        <section
          className="
            mt-8
            grid
            gap-5
            md:grid-cols-2
            lg:grid-cols-3
          "
          aria-label="Platform health summary"
        >
          <MetricSummary
            label="Document Intelligence"
            value="Active"
            icon={
              <FileText
                size={18}
                aria-hidden="true"
              />
            }
          />

          <MetricSummary
            label="AI Assistant"
            value="Operational"
            icon={
              <Zap
                size={18}
                aria-hidden="true"
              />
            }
          />

          <MetricSummary
            label="Platform Security"
            value="Protected"
            icon={
              <ShieldCheck
                size={18}
                aria-hidden="true"
              />
            }
          />
        </section>

        {/* ==================================================
            PERFORMANCE INSIGHT
        ================================================== */}

        <section
          className="
            relative
            mt-8
            overflow-hidden
            rounded-[2rem]
            border
            border-[#0B1F3A]/10
            bg-[#0B1F3A]
            shadow-xl
            shadow-[#0B1F3A]/10
          "
        >
          {/* Decorative gold glow */}

          <div
            className="
              pointer-events-none
              absolute
              -right-20
              -top-20
              h-56
              w-56
              rounded-full
              bg-[#C6A15B]/20
              blur-3xl
            "
            aria-hidden="true"
          />

          <div
            className="
              relative
              flex
              flex-col
              gap-5
              p-6
              sm:p-8
              lg:flex-row
              lg:items-center
              lg:justify-between
            "
          >
            <div>
              <div
                className="
                  flex
                  items-center
                  gap-2
                  text-[#C6A15B]
                "
              >
                <CheckCircle2
                  size={20}
                  aria-hidden="true"
                />

                <span
                  className="
                    text-xs
                    font-black
                    uppercase
                    tracking-widest
                  "
                >
                  Performance Insight
                </span>
              </div>

              <h2
                className="
                  mt-3
                  text-xl
                  font-black
                  text-white
                  sm:text-2xl
                "
              >
                AI processing performance
              </h2>

              <p
                className="
                  mt-2
                  max-w-2xl
                  text-sm
                  leading-6
                  text-slate-300
                "
              >
                The current platform accuracy rate is{" "}
                <span
                  className="
                    font-black
                    text-[#C6A15B]
                  "
                >
                  {accuracyRate}
                </span>
                . Continue monitoring document
                processing activity and AI query
                usage to identify operational trends.
              </p>
            </div>

            <div
              className="
                flex
                h-20
                w-20
                shrink-0
                items-center
                justify-center
                rounded-3xl
                border
                border-[#C6A15B]/30
                bg-[#C6A15B]/10
                text-[#C6A15B]
              "
            >
              <TrendingUp
                size={34}
                aria-hidden="true"
              />
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}

// ============================================================
// ANALYTICS CARD
// ============================================================

function AnalyticsCard({
  icon,
  title,
  value,
  description,
  iconClassName = "",
  accentClassName = "",
}: AnalyticsCardProps) {
  return (
    <article
      className="
        group
        relative
        overflow-hidden
        rounded-[2rem]
        border
        border-white/10
        bg-white/5
        backdrop-blur-xl
        p-6
        shadow-sm
        transition-all
        duration-300
        hover:-translate-y-1
        hover:border-white/20
        hover:shadow-xl
      "
    >
      {/* Accent background */}

      <div
        className={`
          pointer-events-none
          absolute
          inset-x-0
          top-0
          h-32
          bg-gradient-to-br
          ${accentClassName}
        `}
        aria-hidden="true"
      />

      <div className="relative">
        {/* Icon */}

        <div
          className={`
            inline-flex
            h-12
            w-12
            items-center
            justify-center
            rounded-2xl
            ring-1
            transition-transform
            duration-300
            group-hover:scale-105
            ${iconClassName}
          `}
        >
          {icon}
        </div>

        {/* Title */}

        <h2
          className="
            mt-6
            text-base
            font-black
            text-white
          "
        >
          {title}
        </h2>

        {/* Value */}

        <p
          className="
            mt-4
            text-4xl
            font-black
            tracking-tight
            text-white
            sm:text-5xl
          "
        >
          {value ?? "0"}
        </p>

        {/* Description */}

        <p
          className="
            mt-3
            max-w-sm
            text-sm
            leading-6
            text-slate-400
          "
        >
          {description}
        </p>

        {/* Bottom indicator */}

        <div
          className="
            mt-6
            flex
            items-center
            gap-2
            border-t
            border-white/10
            pt-4
            text-xs
            font-bold
            text-slate-400
          "
        >
          <span
            className="
              h-2
              w-2
              rounded-full
              bg-emerald-500
            "
            aria-hidden="true"
          />

          <span>
            Live platform metric
          </span>
        </div>
      </div>
    </article>
  );
}

// ============================================================
// SECONDARY METRIC
// ============================================================

function MetricSummary({
  label,
  value,
  icon,
}: MetricSummaryProps) {
  return (
    <div
      className="
        flex
        items-center
        justify-between
        gap-4
        rounded-3xl
        border
        border-white/10
        bg-white/5
        backdrop-blur-xl
        p-5
        shadow-sm
        transition
        duration-200
        hover:border-white/20
        hover:shadow-md
      "
    >
      <div
        className="
          flex
          min-w-0
          items-center
          gap-3
        "
      >
        <div
          className="
            flex
            h-10
            w-10
            shrink-0
            items-center
            justify-center
            rounded-xl
            bg-[#0B1F3A]
            text-[#C6A15B]
          "
        >
          {icon}
        </div>

        <div className="min-w-0">
          <p
            className="
              truncate
              text-sm
              font-bold
              text-white
            "
          >
            {label}
          </p>

          <p
            className="
              mt-0.5
              text-xs
              text-slate-400
            "
          >
            Platform status
          </p>
        </div>
      </div>

      <span
        className="
          shrink-0
          rounded-full
          bg-emerald-500/10
          px-3
          py-1.5
          text-xs
          font-black
          text-emerald-300
          ring-1
          ring-emerald-500/20
        "
      >
        {value}
      </span>
    </div>
  );
}

