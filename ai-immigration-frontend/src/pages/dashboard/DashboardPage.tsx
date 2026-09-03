import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

import { Link } from "react-router-dom";

import { motion } from "framer-motion";

import {
  Activity,
  AlertTriangle,
  ArrowRight,
  BarChart3,
  CheckCircle2,
  Clock3,
  FileCheck,
  Loader2,
  MessageCircle,
  RefreshCw,
  Server,
  ShieldCheck,
  Sparkles,
  TrendingUp,
  UploadCloud,
  type LucideIcon,
} from "lucide-react";

import API from "@/api/axios";
import env from "@/config/env";

/* ============================================================================
 * CONFIGURATION
 * ========================================================================== */

const DASHBOARD_ENDPOINT = "/dashboard/summary";
const DASHBOARD_ACTIVITY_ENDPOINT = "/dashboard/activity";
const DASHBOARD_STATUS_ENDPOINT = "/dashboard/status";

const DASHBOARD_POLL_INTERVAL =
  env.AI_MONITORING_AUTO_REFRESH
    ? env.AI_MONITORING_REFRESH_INTERVAL_MS
    : 0;

/* ============================================================================
 * TYPES
 * ========================================================================== */

type UserRole =
  | "USER"
  | "ADMIN"
  | "SUPER_ADMIN"
  | string;

type DashboardMetric = {
  id: string;
  title: string;
  value: string;
  description: string;
  icon: string;
  trend?: {
    value: number;
    label: string;
    positive: boolean;
  };
};

type DashboardActivity = {
  id: string;
  title: string;
  description?: string;
  timestamp: string;
  type?: string;
};

type DashboardStatus = {
  status:
    | "OPERATIONAL"
    | "DEGRADED"
    | "MAINTENANCE"
    | "UNAVAILABLE"
    | "UNKNOWN";

  label: string;

  description?: string;

  checkedAt?: string;
};

type DashboardResponse = {
  metrics?: unknown[];
  statistics?: unknown[];
  stats?: unknown[];
  activities?: unknown[];
  recentActivity?: unknown[];
  recent_activity?: unknown[];
  performance?: unknown[];
  status?: unknown;
  systemStatus?: unknown;
  system_status?: unknown;
};

type DashboardData = {
  metrics: DashboardMetric[];
  activities: DashboardActivity[];
  performance: DashboardMetric[];
  status: DashboardStatus;
  lastUpdated: string;
};

type ApiErrorResponse = {
  message?: string;
  error?: string;
  timestamp?: string;
  status?: number;
};

type QuickAction = {
  id: string;
  title: string;
  description: string;
  icon: LucideIcon;
  link: string;
  requiresAdmin?: boolean;
};

/* ============================================================================
 * ICONS
 * ========================================================================== */

const ICONS: Record<string, LucideIcon> = {
  activity: Activity,
  upload: UploadCloud,
  uploadCloud: UploadCloud,
  message: MessageCircle,
  messageCircle: MessageCircle,
  analytics: BarChart3,
  barChart: BarChart3,
  barChart3: BarChart3,
  security: ShieldCheck,
  shield: ShieldCheck,
  shieldCheck: ShieldCheck,
  documents: FileCheck,
  fileCheck: FileCheck,
  processing: Clock3,
  clock: Clock3,
  performance: TrendingUp,
  trending: TrendingUp,
  server: Server,
};

/* ============================================================================
 * HELPERS
 * ========================================================================== */

function getApiErrorMessage(error: unknown): string {
  if (
    typeof error === "object" &&
    error !== null
  ) {
    const axiosLikeError = error as {
      response?: {
        data?: ApiErrorResponse;
      };
      message?: string;
    };

    const responseData =
      axiosLikeError.response?.data;

    if (
      responseData &&
      typeof responseData.message === "string" &&
      responseData.message.trim()
    ) {
      return responseData.message;
    }

    if (
      responseData &&
      typeof responseData.error === "string" &&
      responseData.error.trim()
    ) {
      return responseData.error;
    }

    if (
      typeof axiosLikeError.message === "string" &&
      axiosLikeError.message.trim()
    ) {
      return axiosLikeError.message;
    }
  }

  if (error instanceof Error) {
    return error.message;
  }

  return "Unable to load dashboard data.";
}

function isAbortError(error: unknown): boolean {
  if (
    typeof error === "object" &&
    error !== null
  ) {
    const source = error as {
      code?: string;
      name?: string;
    };

    return (
      source.code === "ERR_CANCELED" ||
      source.name === "CanceledError" ||
      source.name === "AbortError"
    );
  }

  return false;
}

function normalizeMetric(
  value: unknown,
  index: number,
): DashboardMetric {
  const source =
    value &&
    typeof value === "object"
      ? (value as Record<string, unknown>)
      : {};

  const rawTrend = source.trend;

  const trend =
    rawTrend &&
    typeof rawTrend === "object"
      ? (() => {
          const trendSource =
            rawTrend as Record<
              string,
              unknown
            >;

          const trendValue =
            Number(trendSource.value);

          return {
            value: Number.isFinite(
              trendValue,
            )
              ? trendValue
              : 0,

            label:
              typeof trendSource.label ===
              "string"
                ? trendSource.label
                : "",

            positive:
              Boolean(
                trendSource.positive,
              ),
          };
        })()
      : undefined;

  return {
    id:
      typeof source.id === "string"
        ? source.id
        : `metric-${index}`,

    title:
      typeof source.title === "string"
        ? source.title
        : "Metric",

    value:
      typeof source.value === "string"
        ? source.value
        : source.value !== undefined
          ? String(source.value)
          : "—",

    description:
      typeof source.description ===
      "string"
        ? source.description
        : typeof source.subtitle ===
            "string"
          ? source.subtitle
          : "",

    icon:
      typeof source.icon === "string"
        ? source.icon
        : "activity",

    trend,
  };
}

function normalizeActivity(
  value: unknown,
  index: number,
): DashboardActivity {
  const source =
    value &&
    typeof value === "object"
      ? (value as Record<string, unknown>)
      : {};

  return {
    id:
      typeof source.id === "string"
        ? source.id
        : `activity-${index}`,

    title:
      typeof source.title === "string"
        ? source.title
        : typeof source.name === "string"
          ? source.name
          : "Platform activity",

    description:
      typeof source.description ===
      "string"
        ? source.description
        : undefined,

    timestamp:
      typeof source.timestamp ===
      "string"
        ? source.timestamp
        : typeof source.createdAt ===
            "string"
          ? source.createdAt
          : typeof source.created_at ===
              "string"
            ? source.created_at
            : "",

    type:
      typeof source.type === "string"
        ? source.type
        : undefined,
  };
}

function normalizeStatus(
  value: unknown,
): DashboardStatus {
  const source =
    value &&
    typeof value === "object"
      ? (value as Record<string, unknown>)
      : {};

  const rawStatus =
    typeof source.status === "string"
      ? source.status.toUpperCase()
      : "UNKNOWN";

  const allowedStatuses = [
    "OPERATIONAL",
    "DEGRADED",
    "MAINTENANCE",
    "UNAVAILABLE",
    "UNKNOWN",
  ] as const;

  const status = allowedStatuses.includes(
    rawStatus as (typeof allowedStatuses)[number],
  )
    ? (rawStatus as DashboardStatus["status"])
    : "UNKNOWN";

  return {
    status,

    label:
      typeof source.label === "string"
        ? source.label
        : status === "OPERATIONAL"
          ? "System Operational"
          : status === "DEGRADED"
            ? "System Degraded"
            : status === "MAINTENANCE"
              ? "Maintenance Mode"
              : status === "UNAVAILABLE"
                ? "System Unavailable"
                : "System Status Unknown",

    description:
      typeof source.description ===
      "string"
        ? source.description
        : undefined,

    checkedAt:
      typeof source.checkedAt ===
      "string"
        ? source.checkedAt
        : typeof source.checked_at ===
            "string"
          ? source.checked_at
          : undefined,
  };
}

/* ============================================================================
 * RESPONSE NORMALIZATION
 * ========================================================================== */

function normalizeDashboardResponse(
  raw: unknown,
): DashboardData {
  const response =
    raw &&
    typeof raw === "object"
      ? (raw as DashboardResponse)
      : {};

  const rawMetrics =
    Array.isArray(response.metrics)
      ? response.metrics
      : Array.isArray(response.statistics)
        ? response.statistics
        : Array.isArray(response.stats)
          ? response.stats
          : [];

  const rawActivities =
    Array.isArray(response.activities)
      ? response.activities
      : Array.isArray(
          response.recentActivity,
        )
        ? response.recentActivity
        : Array.isArray(
            response.recent_activity,
          )
          ? response.recent_activity
          : [];

  const rawPerformance =
    Array.isArray(response.performance)
      ? response.performance
      : [];

  const rawStatus =
    response.status ??
    response.systemStatus ??
    response.system_status;

  return {
    metrics: rawMetrics.map(
      normalizeMetric,
    ),

    activities: rawActivities.map(
      normalizeActivity,
    ),

    performance: rawPerformance.map(
      normalizeMetric,
    ),

    status: normalizeStatus(
      rawStatus,
    ),

    lastUpdated:
      new Date().toISOString(),
  };
}

/* ============================================================================
 * API
 * ========================================================================== */

async function fetchDashboardData(
  signal?: AbortSignal,
): Promise<DashboardData> {
  const [
    summaryResponse,
    activityResponse,
    statusResponse,
  ] = await Promise.all([
    API.get(
      DASHBOARD_ENDPOINT,
      { signal },
    ),

    API.get(
      DASHBOARD_ACTIVITY_ENDPOINT,
      { signal },
    ).catch((error) => {
      if (isAbortError(error)) {
        throw error;
      }

      return {
        data: {
          activities: [],
        },
      };
    }),

    API.get(
      DASHBOARD_STATUS_ENDPOINT,
      { signal },
    ).catch((error) => {
      if (isAbortError(error)) {
        throw error;
      }

      return {
        data: {
          status: {
            status: "UNKNOWN",
          },
        },
      };
    }),
  ]);

  const summary =
    normalizeDashboardResponse(
      summaryResponse.data,
    );

  const activity =
    normalizeDashboardResponse(
      activityResponse.data,
    );

  const status =
    normalizeDashboardResponse(
      statusResponse.data,
    );

  return {
    metrics: summary.metrics,

    activities:
      activity.activities.length > 0
        ? activity.activities
        : summary.activities,

    performance:
      summary.performance,

    status:
      status.status.status !==
        "UNKNOWN"
        ? status.status
        : summary.status,

    lastUpdated:
      new Date().toISOString(),
  };
}

/* ============================================================================
 * DATE FORMATTER
 * ========================================================================== */

function formatDate(
  value?: string,
): string {
  if (!value) {
    return "Unknown";
  }

  const date = new Date(value);

  if (
    Number.isNaN(
      date.getTime(),
    )
  ) {
    return "Unknown";
  }

  return new Intl.DateTimeFormat(
    undefined,
    {
      dateStyle: "medium",
      timeStyle: "short",
    },
  ).format(date);
}

/* ============================================================================
 * RELATIVE TIME
 * ========================================================================== */

function formatRelativeTime(
  value?: string,
): string {
  if (!value) {
    return "Time unavailable";
  }

  const date = new Date(value);

  if (
    Number.isNaN(
      date.getTime(),
    )
  ) {
    return "Time unavailable";
  }

  const difference =
    Date.now() -
    date.getTime();

  const seconds =
    Math.floor(
      Math.abs(difference) /
        1000,
    );

  const minutes =
    Math.floor(
      seconds / 60,
    );

  const hours =
    Math.floor(
      minutes / 60,
    );

  const days =
    Math.floor(
      hours / 24,
    );

  if (seconds < 60) {
    return "Just now";
  }

  if (minutes < 60) {
    return `${minutes}m ago`;
  }

  if (hours < 24) {
    return `${hours}h ago`;
  }

  if (days < 7) {
    return `${days}d ago`;
  }

  return formatDate(value);
}

/* ============================================================================
 * QUICK ACTIONS
 * ========================================================================== */

const QUICK_ACTIONS: QuickAction[] = [
  {
    id: "upload-documents",
    title: "Upload Documents",
    description:
      "Securely upload passports, visas, permits, certificates, and supporting immigration documents.",
    icon: UploadCloud,
    link: "/dashboard/upload",
  },

  {
    id: "ai-assistant",
    title: "AI Assistant",
    description:
      "Get intelligent immigration guidance, document explanations, and compliance assistance.",
    icon: MessageCircle,
    link: "/dashboard/chat",
  },

  {
    id: "reports",
    title: "Reports & Analytics",
    description:
      "Review verification activity, document trends, AI performance, and operational insights.",
    icon: BarChart3,
    link: "/dashboard/reports",
  },

  {
    id: "administration",
    title: "Administration",
    description:
      "Manage platform settings, permissions, security controls, and administrative operations.",
    icon: ShieldCheck,
    link: "/admin",
    requiresAdmin: true,
  },
];

/* ============================================================================
 * SECTION HEADER
 * ========================================================================== */

function SectionHeader({
  eyebrow,
  title,
  description,
  icon: Icon,
  action,
}: {
  eyebrow?: string;
  title: string;
  description?: string;
  icon?: LucideIcon;
  action?: React.ReactNode;
}) {
  return (
    <div
      className="
        flex
        flex-col
        gap-5
        sm:flex-row
        sm:items-end
        sm:justify-between
      "
    >
      <div className="min-w-0">
        {eyebrow && (
          <p
            className="
              text-xs
              font-bold
              uppercase
              tracking-[0.18em]
              text-[#F4B81A]
            "
          >
            {eyebrow}
          </p>
        )}

        <div
          className="
            mt-1
            flex
            items-center
            gap-3
          "
        >
          {Icon && (
            <div
              className="
                flex
                h-10
                w-10
                shrink-0
                items-center
                justify-center
                rounded-xl
                bg-[#071330]
                text-[#F4B81A]
              "
            >
              <Icon size={20} />
            </div>
          )}

          <h2
            className="
              text-2xl
              font-black
              tracking-tight
              text-[#071330]
              sm:text-3xl
            "
          >
            {title}
          </h2>
        </div>

        {description && (
          <p
            className="
              mt-2
              max-w-2xl
              text-sm
              leading-6
              text-slate-500
            "
          >
            {description}
          </p>
        )}
      </div>

      {action}
    </div>
  );
}

/* ============================================================================
 * METRIC CARD
 * ========================================================================== */

function StatCard({
  metric,
}: {
  metric: DashboardMetric;
}) {
  const Icon =
    ICONS[metric.icon] ??
    Activity;

  return (
    <motion.article
      whileHover={{
        y: -4,
      }}
      transition={{
        duration: 0.2,
      }}
      className="
        group
        relative
        h-full
        overflow-hidden
        rounded-[26px]
        border
        border-slate-200/80
        bg-white
        p-6
        shadow-sm
        transition-shadow
        hover:shadow-xl
      "
    >
      <div
        className="
          absolute
          -right-10
          -top-10
          h-32
          w-32
          rounded-full
          bg-[#F4B81A]/10
          blur-3xl
        "
      />

      <div className="relative">
        <div
          className="
            flex
            items-center
            justify-between
          "
        >
          <div
            className="
              flex
              h-12
              w-12
              items-center
              justify-center
              rounded-2xl
              bg-[#071330]
              text-[#F4B81A]
              shadow-md
              transition-transform
              duration-300
              group-hover:scale-105
            "
            aria-hidden="true"
          >
            <Icon size={23} />
          </div>

          {metric.trend && (
            <div
              className={`
                inline-flex
                items-center
                gap-1
                rounded-full
                px-2.5
                py-1
                text-[11px]
                font-bold
                ${
                  metric.trend.positive
                    ? "bg-emerald-50 text-emerald-700"
                    : "bg-amber-50 text-amber-700"
                }
              `}
            >
              <TrendingUp
                size={12}
              />

              {metric.trend.value > 0
                ? "+"
                : ""}
              {metric.trend.value}%
            </div>
          )}
        </div>

        <p
          className="
            mt-6
            text-3xl
            font-black
            tracking-tight
            text-[#071330]
          "
        >
          {metric.value}
        </p>

        <h3
          className="
            mt-2
            text-sm
            font-bold
            text-slate-800
          "
        >
          {metric.title}
        </h3>

        {metric.description && (
          <p
            className="
              mt-1.5
              text-xs
              leading-5
              text-slate-500
            "
          >
            {metric.description}
          </p>
        )}

        {metric.trend?.label && (
          <p
            className="
              mt-3
              text-[11px]
              font-medium
              text-slate-400
            "
          >
            {metric.trend.label}
          </p>
        )}
      </div>
    </motion.article>
  );
}

/* ============================================================================
 * PERFORMANCE CARD
 * ========================================================================== */

function PerformanceCard({
  metric,
}: {
  metric: DashboardMetric;
}) {
  const Icon =
    ICONS[metric.icon] ??
    TrendingUp;

  return (
    <div
      className="
        group
        rounded-2xl
        border
        border-slate-200
        bg-slate-50/70
        p-5
        transition-all
        duration-200
        hover:border-slate-300
        hover:bg-white
        hover:shadow-sm
      "
    >
      <div
        className="
          flex
          items-start
          justify-between
          gap-4
        "
      >
        <div
          className="
            flex
            h-10
            w-10
            items-center
            justify-center
            rounded-xl
            bg-white
            text-[#071330]
            shadow-sm
          "
        >
          <Icon size={18} />
        </div>

        {metric.trend && (
          <span
            className={`
              text-xs
              font-bold
              ${
                metric.trend.positive
                  ? "text-emerald-600"
                  : "text-amber-600"
              }
            `}
          >
            {metric.trend.value > 0
              ? "+"
              : ""}
            {metric.trend.value}%
          </span>
        )}
      </div>

      <p
        className="
          mt-5
          text-2xl
          font-black
          tracking-tight
          text-[#071330]
        "
      >
        {metric.value}
      </p>

      <p
        className="
          mt-1
          text-sm
          font-semibold
          text-slate-700
        "
      >
        {metric.title}
      </p>

      {metric.description && (
        <p
          className="
            mt-2
            text-xs
            leading-5
            text-slate-400
          "
        >
          {metric.description}
        </p>
      )}
    </div>
  );
}

/* ============================================================================
 * ACTIVITY ITEM
 * ========================================================================== */

function ActivityItem({
  activity,
}: {
  activity: DashboardActivity;
}) {
  return (
    <div
      className="
        group
        flex
        items-start
        gap-4
      "
    >
      <div
        className="
          relative
          mt-0.5
          flex
          h-10
          w-10
          shrink-0
          items-center
          justify-center
          rounded-xl
          bg-blue-50
          text-blue-600
          transition-colors
          group-hover:bg-[#071330]
          group-hover:text-[#F4B81A]
        "
        aria-hidden="true"
      >
        <Activity size={17} />
      </div>

      <div className="min-w-0 flex-1">
        <p
          className="
            text-sm
            font-bold
            text-[#071330]
          "
        >
          {activity.title}
        </p>

        {activity.description && (
          <p
            className="
              mt-1
              text-xs
              leading-5
              text-slate-500
            "
          >
            {activity.description}
          </p>
        )}

        <time
          dateTime={
            activity.timestamp ||
            undefined
          }
          className="
            mt-1.5
            block
            text-[11px]
            font-medium
            text-slate-400
          "
        >
          {formatRelativeTime(
            activity.timestamp,
          )}
        </time>
      </div>
    </div>
  );
}

/* ============================================================================
 * EMPTY STATE
 * ========================================================================== */

function EmptyState({
  title,
  description,
  icon: Icon = Activity,
}: {
  title: string;
  description: string;
  icon?: LucideIcon;
}) {
  return (
    <div
      className="
        rounded-2xl
        border
        border-dashed
        border-slate-200
        bg-slate-50/60
        px-6
        py-10
        text-center
      "
    >
      <div
        className="
          mx-auto
          flex
          h-12
          w-12
          items-center
          justify-center
          rounded-2xl
          bg-slate-100
          text-slate-400
        "
      >
        <Icon size={22} />
      </div>

      <h3
        className="
          mt-4
          text-sm
          font-bold
          text-slate-800
        "
      >
        {title}
      </h3>

      <p
        className="
          mx-auto
          mt-2
          max-w-md
          text-xs
          leading-6
          text-slate-500
        "
      >
        {description}
      </p>
    </div>
  );
}

/* ============================================================================
 * LOADING SKELETON
 * ========================================================================== */

function DashboardSkeleton() {
  return (
    <div
      className="
        animate-pulse
        space-y-8
      "
      aria-label="Loading dashboard"
    >
      <div
        className="
          h-14
          rounded-2xl
          bg-slate-200
        "
      />

      <div
        className="
          h-[360px]
          rounded-[32px]
          bg-slate-200
        "
      />

      <div
        className="
          grid
          gap-5
          sm:grid-cols-2
          xl:grid-cols-4
        "
      >
        {Array.from({
          length: 4,
        }).map((_, index) => (
          <div
            key={index}
            className="
              h-48
              rounded-[26px]
              bg-slate-200
            "
          />
        ))}
      </div>

      <div
        className="
          h-10
          w-48
          rounded-xl
          bg-slate-200
        "
      />

      <div
        className="
          grid
          gap-5
          sm:grid-cols-2
          xl:grid-cols-4
        "
      >
        {Array.from({
          length: 4,
        }).map((_, index) => (
          <div
            key={index}
            className="
              h-60
              rounded-[26px]
              bg-slate-200
            "
          />
        ))}
      </div>

      <div
        className="
          grid
          gap-5
          xl:grid-cols-3
        "
      >
        <div
          className="
            h-[430px]
            rounded-[28px]
            bg-slate-200
            xl:col-span-2
          "
        />

        <div
          className="
            h-[430px]
            rounded-[28px]
            bg-slate-200
          "
        />
      </div>
    </div>
  );
}

/* ============================================================================
 * STATUS BADGE
 * ========================================================================== */

function SystemStatusBadge({
  status,
}: {
  status: DashboardStatus;
}) {
  const styles = {
    OPERATIONAL:
      "bg-emerald-50 text-emerald-700 border-emerald-100",
    DEGRADED:
      "bg-amber-50 text-amber-700 border-amber-100",
    MAINTENANCE:
      "bg-blue-50 text-blue-700 border-blue-100",
    UNAVAILABLE:
      "bg-red-50 text-red-700 border-red-100",
    UNKNOWN:
      "bg-slate-100 text-slate-600 border-slate-200",
  };

  const style =
    styles[status.status] ??
    styles.UNKNOWN;

  return (
    <div
      className={`
        inline-flex
        items-center
        gap-2
        rounded-full
        border
        px-3.5
        py-2
        text-xs
        font-bold
        ${style}
      `}
      role="status"
    >
      <span
        className={`
          h-2
          w-2
          rounded-full
          ${
            status.status ===
            "OPERATIONAL"
              ? "bg-emerald-500"
              : status.status ===
                  "DEGRADED"
                ? "bg-amber-500"
                : status.status ===
                    "UNAVAILABLE"
                  ? "bg-red-500"
                  : "bg-slate-400"
          }
        `}
      />

      {status.label}
    </div>
  );
}

/* ============================================================================
 * QUICK ACTION CARD
 * ========================================================================== */

function QuickActionCard({
  action,
  index,
}: {
  action: QuickAction;
  index: number;
}) {
  const Icon = action.icon;

  return (
    <motion.div
      initial={{
        opacity: 0,
        y: 15,
      }}
      animate={{
        opacity: 1,
        y: 0,
      }}
      transition={{
        delay: index * 0.07,
      }}
      className="h-full"
    >
      <Link
        to={action.link}
        className="
          group
          relative
          flex
          h-full
          min-h-[235px]
          flex-col
          overflow-hidden
          rounded-[26px]
          border
          border-slate-200
          bg-white
          p-6
          shadow-sm
          transition-all
          duration-300
          hover:-translate-y-1
          hover:border-slate-300
          hover:shadow-xl
          focus:outline-none
          focus:ring-2
          focus:ring-[#F4B81A]
          focus:ring-offset-2
        "
      >
        <div
          className="
            absolute
            inset-0
            bg-gradient-to-br
            from-[#F4B81A]/5
            via-transparent
            to-blue-500/5
            opacity-0
            transition-opacity
            duration-300
            group-hover:opacity-100
          "
        />

        <div
          className="
            relative
            flex
            items-center
            justify-between
          "
        >
          <div
            className="
              flex
              h-14
              w-14
              items-center
              justify-center
              rounded-2xl
              bg-[#071330]
              text-[#F4B81A]
              shadow-md
              transition-transform
              duration-300
              group-hover:scale-105
            "
          >
            <Icon size={25} />
          </div>

          <div
            className="
              flex
              h-9
              w-9
              items-center
              justify-center
              rounded-full
              bg-slate-50
              text-slate-400
              transition-all
              group-hover:bg-[#F4B81A]
              group-hover:text-[#071330]
            "
          >
            <ArrowRight size={16} />
          </div>
        </div>

        <div className="relative mt-6">
          <h3
            className="
              text-lg
              font-black
              text-[#071330]
            "
          >
            {action.title}
          </h3>

          <p
            className="
              mt-2
              text-sm
              leading-6
              text-slate-500
            "
          >
            {action.description}
          </p>
        </div>

        <div
          className="
            relative
            mt-auto
            pt-6
            text-xs
            font-bold
            uppercase
            tracking-wider
            text-[#071330]
          "
        >
          Open workspace
        </div>
      </Link>
    </motion.div>
  );
}

/* ============================================================================
 * MAIN DASHBOARD
 * ========================================================================== */

export default function DashboardPage() {
  const [
    data,
    setData,
  ] = useState<DashboardData | null>(
    null,
  );

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
  ] = useState<string | null>(
    null,
  );

  const [
    userRole,
    setUserRole,
  ] = useState<UserRole | null>(
    null,
  );

  const abortControllerRef =
    useRef<AbortController | null>(
      null,
    );

  /* ------------------------------------------------------------------------
   * ROLE
   * ---------------------------------------------------------------------- */

  useEffect(() => {
    try {
      const storedUser =
        localStorage.getItem(
          "authUser",
        );

      if (!storedUser) {
        return;
      }

      const parsed =
        JSON.parse(storedUser) as {
          role?: UserRole;
        };

      if (
        typeof parsed.role ===
        "string"
      ) {
        setUserRole(
          parsed.role,
        );
      }
    } catch {
      /*
       * Invalid local authentication metadata must not crash the dashboard.
       * The backend remains the authority for authorization.
       */
    }
  }, []);

  /* ------------------------------------------------------------------------
   * LOAD DASHBOARD
   * ---------------------------------------------------------------------- */

  const loadDashboard =
    useCallback(
      async (
        isRefresh = false,
      ) => {
        abortControllerRef.current?.abort();

        const controller =
          new AbortController();

        abortControllerRef.current =
          controller;

        if (isRefresh) {
          setRefreshing(true);
        } else {
          setLoading(true);
        }

        setError(null);

        try {
          const dashboard =
            await fetchDashboardData(
              controller.signal,
            );

          if (
            controller.signal.aborted
          ) {
            return;
          }

          setData(dashboard);
        } catch (requestError) {
          if (
            controller.signal.aborted ||
            isAbortError(
              requestError,
            )
          ) {
            return;
          }

          setError(
            getApiErrorMessage(
              requestError,
            ),
          );
        } finally {
          if (
            !controller.signal.aborted
          ) {
            setLoading(false);
            setRefreshing(false);
          }
        }
      },
      [],
    );

  /* ------------------------------------------------------------------------
   * INITIAL LOAD
   * ---------------------------------------------------------------------- */

  useEffect(() => {
    void loadDashboard();

    return () => {
      abortControllerRef.current?.abort();
    };
  }, [loadDashboard]);

  /* ------------------------------------------------------------------------
   * AUTOMATIC REFRESH
   * ---------------------------------------------------------------------- */

  useEffect(() => {
    if (
      !Number.isFinite(
        DASHBOARD_POLL_INTERVAL,
      ) ||
      DASHBOARD_POLL_INTERVAL <= 0
    ) {
      return;
    }

    const interval =
      window.setInterval(() => {
        if (
          document.visibilityState ===
          "visible"
        ) {
          void loadDashboard(
            true,
          );
        }
      }, DASHBOARD_POLL_INTERVAL);

    const handleVisibilityChange =
      () => {
        if (
          document.visibilityState ===
          "visible"
        ) {
          void loadDashboard(
            true,
          );
        }
      };

    document.addEventListener(
      "visibilitychange",
      handleVisibilityChange,
    );

    return () => {
      window.clearInterval(
        interval,
      );

      document.removeEventListener(
        "visibilitychange",
        handleVisibilityChange,
      );
    };
  }, [loadDashboard]);

  /* ------------------------------------------------------------------------
   * SAFE DATA
   * ---------------------------------------------------------------------- */

  const safeData =
    useMemo<DashboardData>(
      () =>
        data ?? {
          metrics: [],
          activities: [],
          performance: [],
          status: {
            status: "UNKNOWN",
            label:
              "System Status Unknown",
          },
          lastUpdated:
            new Date().toISOString(),
        },
      [data],
    );

  const isAdmin =
    userRole === "ADMIN" ||
    userRole === "SUPER_ADMIN";

  const visibleQuickActions =
    useMemo(
      () =>
        QUICK_ACTIONS.filter(
          (action) =>
            !action.requiresAdmin ||
            isAdmin,
        ),
      [isAdmin],
    );

  /* ------------------------------------------------------------------------
   * INITIAL LOADING
   * ---------------------------------------------------------------------- */

  if (
    loading &&
    !data
  ) {
    return (
      <main
        className="
          min-h-screen
          bg-[#F8F6F1]
          px-4
          py-6
          sm:px-6
          lg:px-8
        "
      >
        <div
          className="
            mx-auto
            max-w-[1600px]
          "
        >
          <DashboardSkeleton />
        </div>
      </main>
    );
  }

  /* ------------------------------------------------------------------------
   * COMPLETE FAILURE
   * ---------------------------------------------------------------------- */

  if (
    error &&
    !data
  ) {
    return (
      <main
        className="
          flex
          min-h-screen
          items-center
          justify-center
          bg-[#F8F6F1]
          px-6
        "
      >
        <section
          className="
            w-full
            max-w-lg
            rounded-[32px]
            border
            border-red-100
            bg-white
            p-8
            text-center
            shadow-xl
          "
          role="alert"
        >
          <div
            className="
              mx-auto
              flex
              h-14
              w-14
              items-center
              justify-center
              rounded-2xl
              bg-red-50
              text-red-600
            "
          >
            <AlertTriangle
              size={26}
            />
          </div>

          <h1
            className="
              mt-5
              text-2xl
              font-black
              text-slate-900
            "
          >
            Unable to load dashboard
          </h1>

          <p
            className="
              mt-3
              text-sm
              leading-6
              text-slate-500
            "
          >
            {error}
          </p>

          <button
            type="button"
            onClick={() =>
              void loadDashboard()
            }
            className="
              mt-6
              inline-flex
              items-center
              gap-2
              rounded-2xl
              bg-[#071330]
              px-5
              py-3
              font-bold
              text-white
              transition
              hover:bg-[#183B6B]
              focus:outline-none
              focus:ring-2
              focus:ring-[#F4B81A]
              focus:ring-offset-2
            "
          >
            <RefreshCw size={17} />

            Retry
          </button>
        </section>
      </main>
    );
  }

  /* ------------------------------------------------------------------------
   * RENDER
   * ---------------------------------------------------------------------- */

  return (
    <main
      className="
        min-h-screen
        bg-[#F8F6F1]
        px-4
        py-5
        sm:px-6
        lg:px-8
        lg:py-7
      "
    >
      <div
        className="
          mx-auto
          max-w-[1600px]
        "
      >
        {/* ================================================================
            TOP TOOLBAR
        ================================================================ */}

        <header
          className="
            mb-7
            flex
            flex-col
            gap-4
            rounded-2xl
            border
            border-slate-200/80
            bg-white/90
            px-5
            py-4
            shadow-sm
            backdrop-blur-xl
            sm:flex-row
            sm:items-center
            sm:justify-between
          "
        >
          <div className="min-w-0">
            <div
              className="
                flex
                items-center
                gap-2
              "
            >
              <div
                className="
                  h-2
                  w-2
                  rounded-full
                  bg-[#F4B81A]
                "
              />

              <p
                className="
                  text-xs
                  font-bold
                  uppercase
                  tracking-[0.18em]
                  text-slate-500
                "
              >
                Workspace
              </p>
            </div>

            <div
              className="
                mt-1
                flex
                flex-wrap
                items-center
                gap-x-3
                gap-y-1
              "
            >
              <h1
                className="
                  text-xl
                  font-black
                  tracking-tight
                  text-[#071330]
                "
              >
                Dashboard
              </h1>

              <span
                className="
                  hidden
                  h-4
                  w-px
                  bg-slate-200
                  sm:block
                "
              />

              <p
                className="
                  text-xs
                  text-slate-400
                "
              >
                Updated{" "}
                {formatRelativeTime(
                  safeData.lastUpdated,
                )}
              </p>
            </div>
          </div>

          <div
            className="
              flex
              flex-wrap
              items-center
              gap-3
            "
          >
            <SystemStatusBadge
              status={
                safeData.status
              }
            />

            <button
              type="button"
              onClick={() =>
                void loadDashboard(
                  true,
                )
              }
              disabled={refreshing}
              className="
                inline-flex
                items-center
                gap-2
                rounded-xl
                border
                border-slate-200
                bg-white
                px-4
                py-2.5
                text-xs
                font-bold
                text-slate-700
                shadow-sm
                transition
                hover:border-slate-300
                hover:bg-slate-50
                disabled:cursor-not-allowed
                disabled:opacity-60
                focus:outline-none
                focus:ring-2
                focus:ring-[#F4B81A]
                focus:ring-offset-2
              "
              aria-label="Refresh dashboard"
            >
              <RefreshCw
                size={15}
                className={
                  refreshing
                    ? "animate-spin"
                    : ""
                }
              />

              Refresh
            </button>
          </div>
        </header>

        {/* ================================================================
            REFRESH ERROR
        ================================================================ */}

        {error && data && (
          <div
            role="alert"
            className="
              mb-7
              flex
              items-start
              gap-3
              rounded-2xl
              border
              border-amber-200
              bg-amber-50
              px-5
              py-4
              text-sm
              text-amber-800
            "
          >
            <AlertTriangle
              size={18}
              className="mt-0.5 shrink-0"
            />

            <div>
              <p className="font-bold">
                Dashboard refresh failed
              </p>

              <p className="mt-1">
                {error}
              </p>
            </div>
          </div>
        )}

        {/* ================================================================
            HERO
        ================================================================ */}

        <motion.section
          initial={{
            opacity: 0,
            y: 20,
          }}
          animate={{
            opacity: 1,
            y: 0,
          }}
          transition={{
            duration: 0.45,
          }}
          className="
            relative
            overflow-hidden
            rounded-[32px]
            bg-gradient-to-br
            from-[#071330]
            via-[#0B1736]
            to-[#183B6B]
            shadow-2xl
          "
        >
          <div
            className="
              absolute
              -right-24
              -top-24
              h-96
              w-96
              rounded-full
              bg-[#F4B81A]/10
              blur-3xl
            "
          />

          <div
            className="
              absolute
              -bottom-32
              -left-20
              h-96
              w-96
              rounded-full
              bg-blue-500/10
              blur-3xl
            "
          />

          <div
            className="
              relative
              z-10
              grid
              gap-10
              px-7
              py-9
              lg:grid-cols-[1fr_auto]
              lg:items-center
              lg:px-12
              lg:py-12
            "
          >
            <div className="max-w-3xl">
              <div
                className="
                  inline-flex
                  items-center
                  gap-2
                  rounded-full
                  border
                  border-[#F4B81A]/20
                  bg-[#F4B81A]/10
                  px-3.5
                  py-2
                  text-[#F4B81A]
                "
              >
                <Sparkles size={15} />

                <span
                  className="
                    text-[10px]
                    font-bold
                    uppercase
                    tracking-[0.22em]
                  "
                >
                  AI Immigration Intelligence
                </span>
              </div>

              <h2
                className="
                  mt-5
                  text-4xl
                  font-black
                  leading-[1.05]
                  tracking-tight
                  text-white
                  sm:text-5xl
                  lg:text-6xl
                "
              >
                Welcome Back
              </h2>

              <p
                className="
                  mt-5
                  max-w-2xl
                  text-sm
                  leading-7
                  text-slate-300
                  sm:text-base
                "
              >
                Manage immigration
                workflows, verify
                documents, interact
                with intelligent AI
                assistance, and monitor
                platform activity from
                one secure workspace.
              </p>

              <div
                className="
                  mt-7
                  flex
                  flex-wrap
                  gap-3
                "
              >
                <Link
                  to="/dashboard/upload"
                  className="
                    inline-flex
                    items-center
                    gap-2
                    rounded-xl
                    bg-gradient-to-r
                    from-[#F4B81A]
                    to-[#FFD96A]
                    px-5
                    py-3
                    text-sm
                    font-black
                    text-[#071330]
                    shadow-lg
                    transition
                    hover:-translate-y-0.5
                    hover:shadow-xl
                    focus:outline-none
                    focus:ring-2
                    focus:ring-[#F4B81A]
                    focus:ring-offset-2
                    focus:ring-offset-[#071330]
                  "
                >
                  Upload Documents

                  <ArrowRight
                    size={17}
                  />
                </Link>

                <Link
                  to="/dashboard/chat"
                  className="
                    inline-flex
                    items-center
                    gap-2
                    rounded-xl
                    border
                    border-white/15
                    bg-white/10
                    px-5
                    py-3
                    text-sm
                    font-bold
                    text-white
                    backdrop-blur-sm
                    transition
                    hover:bg-white/15
                    focus:outline-none
                    focus:ring-2
                    focus:ring-white/50
                  "
                >
                  <MessageCircle
                    size={17}
                  />

                  Open AI Assistant
                </Link>
              </div>
            </div>

            {/* HERO STATUS PANEL */}

            <div
              className="
                hidden
                min-w-[230px]
                rounded-2xl
                border
                border-white/10
                bg-white/5
                p-5
                backdrop-blur-md
                lg:block
              "
            >
              <div
                className="
                  flex
                  items-center
                  justify-between
                  gap-4
                "
              >
                <div>
                  <p
                    className="
                      text-[10px]
                      font-bold
                      uppercase
                      tracking-wider
                      text-slate-400
                    "
                  >
                    Platform status
                  </p>

                  <p
                    className="
                      mt-2
                      text-sm
                      font-bold
                      text-white
                    "
                  >
                    {safeData.status.label}
                  </p>
                </div>

                <div
                  className="
                    flex
                    h-11
                    w-11
                    items-center
                    justify-center
                    rounded-xl
                    bg-emerald-500/10
                    text-emerald-400
                  "
                >
                  <Server size={20} />
                </div>
              </div>

              <div
                className="
                  mt-5
                  h-px
                  bg-white/10
                "
              />

              <p
                className="
                  mt-4
                  text-xs
                  leading-5
                  text-slate-400
                "
              >
                {safeData.status.description ??
                  "Platform monitoring is active."}
              </p>
            </div>
          </div>
        </motion.section>

        {/* ================================================================
            STATISTICS
        ================================================================ */}

        <section
          className="mt-10"
          aria-label="Dashboard statistics"
        >
          <SectionHeader
            eyebrow="Overview"
            title="Platform Summary"
            description="Key indicators and activity metrics from your immigration workspace."
            icon={BarChart3}
          />

          <div className="mt-5">
            {safeData.metrics.length ===
            0 ? (
              <EmptyState
                title="No dashboard metrics available"
                description="The platform has not returned any dashboard statistics yet."
                icon={BarChart3}
              />
            ) : (
              <div
                className="
                  grid
                  grid-cols-1
                  gap-5
                  sm:grid-cols-2
                  xl:grid-cols-4
                "
              >
                {safeData.metrics.map(
                  (metric) => (
                    <StatCard
                      key={metric.id}
                      metric={metric}
                    />
                  ),
                )}
              </div>
            )}
          </div>
        </section>

        {/* ================================================================
            QUICK ACTIONS
        ================================================================ */}

        <section className="mt-12">
          <SectionHeader
            eyebrow="Workspace"
            title="Quick Actions"
            description="Access the most important tools and workflows available to your account."
          />

          <div
            className="
              mt-5
              grid
              grid-cols-1
              gap-5
              sm:grid-cols-2
              xl:grid-cols-4
            "
          >
            {visibleQuickActions.map(
              (
                item,
                index,
              ) => (
                <QuickActionCard
                  key={item.id}
                  action={item}
                  index={index}
                />
              ),
            )}
          </div>
        </section>

        {/* ================================================================
            MONITORING
        ================================================================ */}

        <section className="mt-12">
          <SectionHeader
            eyebrow="Monitoring"
            title="Platform Monitoring"
            description="Review operational performance and recent activity across the platform."
            icon={Activity}
            action={
              <SystemStatusBadge
                status={
                  safeData.status
                }
              />
            }
          />

          <div
            className="
              mt-5
              grid
              gap-5
              xl:grid-cols-3
            "
          >
            {/* PERFORMANCE */}

            <section
              className="
                rounded-[28px]
                border
                border-slate-200
                bg-white
                p-6
                shadow-sm
                xl:col-span-2
              "
              aria-labelledby="performance-heading"
            >
              <div
                className="
                  flex
                  items-start
                  justify-between
                  gap-4
                "
              >
                <div>
                  <div
                    className="
                      flex
                      items-center
                      gap-2.5
                    "
                  >
                    <div
                      className="
                        flex
                        h-9
                        w-9
                        items-center
                        justify-center
                        rounded-xl
                        bg-[#071330]
                        text-[#F4B81A]
                      "
                    >
                      <TrendingUp
                        size={17}
                      />
                    </div>

                    <h3
                      id="performance-heading"
                      className="
                        text-lg
                        font-black
                        text-[#071330]
                      "
                    >
                      Performance Overview
                    </h3>
                  </div>

                  <p
                    className="
                      mt-2
                      text-xs
                      leading-5
                      text-slate-500
                    "
                  >
                    Live performance
                    indicators returned
                    by the platform
                    monitoring service.
                  </p>
                </div>

                <div
                  className="
                    hidden
                    rounded-lg
                    bg-slate-50
                    px-2.5
                    py-1.5
                    text-[10px]
                    font-bold
                    uppercase
                    tracking-wider
                    text-slate-400
                    sm:block
                  "
                >
                  Live
                </div>
              </div>

              <div
                className="
                  mt-6
                  grid
                  grid-cols-1
                  gap-4
                  sm:grid-cols-2
                "
              >
                {safeData.performance
                  .length === 0 ? (
                  <div className="sm:col-span-2">
                    <EmptyState
                      title="No performance data"
                      description="Performance indicators are currently unavailable."
                      icon={
                        TrendingUp
                      }
                    />
                  </div>
                ) : (
                  safeData.performance.map(
                    (metric) => (
                      <PerformanceCard
                        key={
                          metric.id
                        }
                        metric={
                          metric
                        }
                      />
                    ),
                  )
                )}
              </div>
            </section>

            {/* RECENT ACTIVITY */}

            <section
              className="
                rounded-[28px]
                border
                border-slate-200
                bg-white
                p-6
                shadow-sm
              "
              aria-labelledby="activity-heading"
            >
              <div
                className="
                  flex
                  items-start
                  justify-between
                  gap-4
                "
              >
                <div>
                  <div
                    className="
                      flex
                      items-center
                      gap-2.5
                    "
                  >
                    <div
                      className="
                        flex
                        h-9
                        w-9
                        items-center
                        justify-center
                        rounded-xl
                        bg-blue-50
                        text-blue-600
                      "
                    >
                      <Activity
                        size={17}
                      />
                    </div>

                    <h3
                      id="activity-heading"
                      className="
                        text-lg
                        font-black
                        text-[#071330]
                      "
                    >
                      Recent Activity
                    </h3>
                  </div>

                  <p
                    className="
                      mt-2
                      text-xs
                      leading-5
                      text-slate-500
                    "
                  >
                    Latest activity
                    recorded in your
                    workspace.
                  </p>
                </div>

                <span
                  className="
                    rounded-full
                    bg-slate-50
                    px-2.5
                    py-1
                    text-[10px]
                    font-bold
                    text-slate-400
                  "
                >
                  Recent
                </span>
              </div>

              <div className="mt-6">
                {safeData.activities
                  .length === 0 ? (
                  <EmptyState
                    title="No recent activity"
                    description="Your recent platform activity will appear here."
                    icon={
                      Activity
                    }
                  />
                ) : (
                  <div
                    className="
                      divide-y
                      divide-slate-100
                    "
                  >
                    {safeData.activities.map(
                      (
                        activity,
                        index,
                      ) => (
                        <div
                          key={
                            activity.id
                          }
                          className={
                            index === 0
                              ? "pb-5"
                              : "py-5"
                          }
                        >
                          <ActivityItem
                            activity={
                              activity
                            }
                          />
                        </div>
                      ),
                    )}
                  </div>
                )}
              </div>
            </section>
          </div>
        </section>

        {/* ================================================================
            SYSTEM FOOTER
        ================================================================ */}

        <footer
          className="
            mt-8
            flex
            flex-col
            gap-3
            rounded-2xl
            border
            border-slate-200
            bg-white
            px-5
            py-4
            text-xs
            text-slate-500
            shadow-sm
            sm:flex-row
            sm:items-center
            sm:justify-between
          "
        >
          <div
            className="
              flex
              items-center
              gap-2
            "
          >
            {refreshing ? (
              <Loader2
                size={14}
                className="animate-spin"
              />
            ) : safeData.status
                .status ===
              "OPERATIONAL" ? (
              <CheckCircle2
                size={14}
                className="text-emerald-500"
              />
            ) : (
              <Activity size={14} />
            )}

            <span>
              {refreshing
                ? "Refreshing dashboard..."
                : safeData.status
                    .description ??
                  safeData.status
                    .label}
            </span>
          </div>

          <div
            className="
              flex
              items-center
              gap-2
            "
          >
            <span className="text-slate-400">
              Last synchronization
            </span>

            <span
              className="
                font-semibold
                text-slate-600
              "
            >
              {formatDate(
                safeData.lastUpdated,
              )}
            </span>
          </div>
        </footer>
      </div>
    </main>
  );
}