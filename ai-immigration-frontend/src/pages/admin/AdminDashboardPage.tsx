import {
  useCallback,
  useMemo,
  useState,
} from "react";

import axios from "axios";

import {
  useNavigate,
} from "react-router-dom";

import {
  toast,
} from "sonner";

import {
  motion,
} from "framer-motion";

import type {
  LucideIcon,
} from "lucide-react";

import {
  Activity,
  AlertTriangle,
  Bot,
  Briefcase,
  CheckCircle2,
  ChevronRight,
  Clock3,
  CreditCard,
  FileCheck2,
  FileSearch,
  Fingerprint,
  Globe,
  MapPinned,
  RefreshCw,
  ServerCog,
  ShieldAlert,
  ShieldCheck,
  Sparkles,
  TrendingUp,
  Users,
  Zap,
} from "lucide-react";

import {
  appConfig,
} from "../../config/appConfig";

import {
  useAdminDashboard,
} from "../../hooks/useAdminDashboard";

import {
  KPICard,
} from "./KPICard";

import {
  SectionCard,
} from "./SectionCard";

import {
  StatusBadge,
} from "./StatusBadge";

import {
  generateAdminReport,
  runAIAudit,
} from "../../api/adminApi";

/* ============================================================================
 * TYPES
 * ========================================================================== */

type Primitive =
  | string
  | number
  | boolean
  | null;

interface ExecutiveMetrics {
  activeUsers?: number | string;
  activeUsersChange?: number | string;

  activeCases?: number | string;
  activeCasesChange?: number | string;

  monthlyRevenue?: number | string;
  revenueChange?: number | string;

  aiAccuracy?: number | string;
  aiAccuracyChange?: number | string;
}

interface PlatformMetrics {
  status?: string;
  aiModels?: number | string;
  uptime?: number | string;
}

interface DocumentMetricsSummary {
  ocrAccuracy?: number | string;
  fraudAlerts?: number | string;
  averageProcessingTime?: Primitive;
  successRate?: number | string;
}

interface DashboardMetric {
  label: string;
  value: Primitive;
}

interface RegionMetric {
  id: string;
  region: string;
  value: Primitive;
  trend?: string;
}

interface HealthItem {
  title: string;
  value: Primitive;
  status: string;
}

interface AuditLog {
  id: string | number;
  message: string;
  time: string;
}

interface DashboardAlert {
  id: string | number;
  message: string;
  time?: string;
}

interface DocumentMetric {
  title: string;
  value: Primitive;
}

interface DashboardData {
  executive?: ExecutiveMetrics;
  platform?: PlatformMetrics;
  documents?: DocumentMetricsSummary;

  health?: HealthItem[];

  operations?:
    | DashboardMetric[]
    | Record<string, unknown>;

  aiMetrics?:
    | DashboardMetric[]
    | Record<string, unknown>;

  security?:
    | DashboardMetric[]
    | Record<string, unknown>;

  auditLogs?: AuditLog[];

  alerts?: DashboardAlert[];

  documentMetrics?: DocumentMetric[];

  regions?: RegionMetric[];
}

interface KPIData {
  title: string;
  value: string;
  change?: string;
  icon: LucideIcon;
}

interface HealthMetric {
  title: string;
  value: string;
  status: string;
  icon: LucideIcon;
}

/* ============================================================================
 * DESIGN SYSTEM
 * ========================================================================== */

const UI = {
  page:
    "w-full space-y-8 p-4 sm:p-6 lg:p-8 xl:p-10",

  panel:
    "rounded-[2rem] border border-slate-200/80 bg-white shadow-[0_20px_60px_-35px_rgba(15,23,42,0.35)]",

  darkPanel:
    "rounded-[2rem] border border-slate-800/80 bg-slate-950 shadow-[0_25px_80px_-35px_rgba(15,23,42,0.8)]",

  metricCard:
    "group rounded-2xl border border-slate-200 bg-white p-5 transition-all duration-300 hover:-translate-y-1 hover:border-indigo-200 hover:shadow-xl hover:shadow-indigo-100/50",

  primaryButton:
    "inline-flex items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-indigo-600 via-violet-600 to-fuchsia-600 px-5 py-3 font-bold text-white shadow-lg shadow-indigo-500/25 transition-all duration-200 hover:-translate-y-0.5 hover:shadow-xl hover:shadow-violet-500/30 focus:outline-none focus:ring-2 focus:ring-violet-500 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50",

  secondaryButton:
    "inline-flex items-center justify-center gap-2 rounded-2xl border border-slate-200 bg-white px-5 py-3 font-bold text-slate-700 shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:border-indigo-200 hover:bg-indigo-50 hover:text-indigo-700 hover:shadow-md focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50",

  darkButton:
    "inline-flex items-center justify-center gap-2 rounded-2xl border border-slate-700 bg-slate-900 px-5 py-3 font-bold text-white transition-all duration-200 hover:-translate-y-0.5 hover:border-violet-500/60 hover:bg-slate-800 hover:shadow-lg hover:shadow-violet-950/30 focus:outline-none focus:ring-2 focus:ring-violet-500 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50",
} as const;

/* ============================================================================
 * ICON CONFIGURATION
 * ========================================================================== */

const healthIcons: Record<string, LucideIcon> = {
  "API Availability": Activity,
  "AI Infrastructure": Bot,
  "Global Coverage": Globe,
  "Security Status": ShieldCheck,
};

/* ============================================================================
 * SAFE VALUE HELPERS
 * ========================================================================== */

function toNumber(
  value: unknown,
  fallback = 0,
): number {
  if (
    typeof value === "number" &&
    Number.isFinite(value)
  ) {
    return value;
  }

  if (
    typeof value === "string" &&
    value.trim() !== ""
  ) {
    const parsed = Number(value);

    if (Number.isFinite(parsed)) {
      return parsed;
    }
  }

  return fallback;
}

function toStringValue(
  value: unknown,
  fallback = "N/A",
): string {
  if (
    value === null ||
    value === undefined
  ) {
    return fallback;
  }

  return String(value);
}

function formatNumber(
  value: unknown,
): string {
  return new Intl.NumberFormat(
    undefined,
    {
      maximumFractionDigits: 0,
    },
  ).format(
    toNumber(value),
  );
}

function formatCurrency(
  value: unknown,
): string {
  return new Intl.NumberFormat(
    undefined,
    {
      style: "currency",
      currency: "USD",
      maximumFractionDigits: 0,
    },
  ).format(
    toNumber(value),
  );
}

function formatPercentage(
  value: unknown,
): string {
  return `${toNumber(value)}%`;
}

function formatChange(
  value: unknown,
): string | undefined {
  const number = toNumber(
    value,
    NaN,
  );

  if (
    !Number.isFinite(number) ||
    number === 0
  ) {
    return undefined;
  }

  return `${
    number > 0
      ? "+"
      : ""
  }${number}%`;
}

function isObject(
  value: unknown,
): value is Record<string, unknown> {
  return (
    typeof value === "object" &&
    value !== null &&
    !Array.isArray(value)
  );
}

function formatLabel(
  key: string,
): string {
  return key
    .replace(
      /([A-Z])/g,
      " $1",
    )
    .replace(
      /[_-]/g,
      " ",
    )
    .replace(
      /^./,
      (char) =>
        char.toUpperCase(),
    )
    .trim();
}

function formatMetricValue(
  value: unknown,
): string {
  if (
    value === null ||
    value === undefined
  ) {
    return "N/A";
  }

  if (
    typeof value === "boolean"
  ) {
    return value
      ? "Yes"
      : "No";
  }

  if (
    Array.isArray(value)
  ) {
    return String(
      value.length,
    );
  }

  if (
    typeof value === "object"
  ) {
    try {
      return JSON.stringify(
        value,
      );
    } catch {
      return "N/A";
    }
  }

  return String(value);
}

/* ============================================================================
 * ERROR HELPERS
 * ========================================================================== */

function getApiErrorMessage(
  error: unknown,
  fallback: string,
): string {
  if (
    axios.isAxiosError(error)
  ) {
    const responseData =
      error.response?.data;

    if (
      isObject(responseData)
    ) {
      const message =
        responseData.message ??
        responseData.error ??
        responseData.detail;

      if (
        typeof message ===
        "string" &&
        message.trim()
      ) {
        return message;
      }
    }

    if (
      error.message &&
      error.message.trim()
    ) {
      return error.message;
    }

    if (
      error.response?.status
    ) {
      return `${fallback} (HTTP ${error.response.status})`;
    }
  }

  if (
    error instanceof Error &&
    error.message.trim()
  ) {
    return error.message;
  }

  return fallback;
}

function isForbiddenError(
  error: unknown,
): boolean {
  if (
    axios.isAxiosError(error)
  ) {
    return (
      error.response?.status ===
      401 ||
      error.response?.status ===
      403
    );
  }

  const message =
    getApiErrorMessage(
      error,
      "",
    );

  return /403|401|forbidden|unauthorized|access denied/i.test(
    message,
  );
}

/* ============================================================================
 * API RESPONSE NORMALIZATION
 * ========================================================================== */

function objectToMetrics(
  source: unknown,
): DashboardMetric[] {
  if (
    !isObject(source)
  ) {
    return [];
  }

  return Object.entries(
    source,
  )
    .filter(
      ([, value]) =>
        !Array.isArray(value) &&
        !isObject(value),
    )
    .map(
      ([key, value]) => ({
        label:
          formatLabel(key),

        value:
          value as Primitive,
      }),
    );
}

function normalizeMetrics(
  source:
    | DashboardMetric[]
    | Record<string, unknown>
    | undefined,
): DashboardMetric[] {
  if (
    Array.isArray(source)
  ) {
    return source
      .filter(
        (item) =>
          isObject(item) &&
          typeof item.label ===
            "string",
      )
      .map(
        (item) => ({
          label:
            String(
              item.label,
            ),

          value:
            (item.value ??
              null) as Primitive,
        }),
      );
  }

  return objectToMetrics(
    source,
  );
}

function normalizeHealth(
  source: unknown,
): HealthItem[] {
  if (
    !Array.isArray(source)
  ) {
    return [];
  }

  return source
    .filter(isObject)
    .map(
      (item) => ({
        title:
          String(
            item.title ??
              "System",
          ),

        value:
          (item.value ??
            null) as Primitive,

        status:
          String(
            item.status ??
              "UNKNOWN",
          ),
      }),
    );
}

function normalizeAuditLogs(
  source: unknown,
): AuditLog[] {
  if (
    !Array.isArray(source)
  ) {
    return [];
  }

  return source
    .filter(isObject)
    .map(
      (
        item,
        index,
      ) => ({
        id:
          String(
            item.id ??
              index,
          ),

        message:
          String(
            item.message ??
              item.action ??
              "System event",
          ),

        time:
          String(
            item.time ??
              item.timestamp ??
              "",
          ),
      }),
    );
}

function normalizeAlerts(
  source: unknown,
): DashboardAlert[] {
  if (
    !Array.isArray(source)
  ) {
    return [];
  }

  return source
    .filter(isObject)
    .map(
      (
        item,
        index,
      ) => {
        const rawTime =
          item.time ??
          item.timestamp;

        return {
          id:
            String(
              item.id ??
                index,
            ),

          message:
            String(
              item.message ??
                item.description ??
                "System alert",
            ),

          time:
            rawTime !==
              undefined &&
            rawTime !== null
              ? String(
                  rawTime,
                )
              : undefined,
        };
      },
    );
}

function normalizeRegions(
  source: unknown,
): RegionMetric[] {
  if (
    !Array.isArray(source)
  ) {
    return [];
  }

  return source
    .filter(isObject)
    .map(
      (
        item,
        index,
      ) => ({
        id:
          String(
            item.id ??
              item.region ??
              index,
          ),

        region:
          String(
            item.region ??
              item.name ??
              "Unknown",
          ),

        value:
          (item.value ??
            item.count ??
            item.applications ??
            null) as Primitive,

        trend:
          item.trend !==
            undefined &&
          item.trend !== null
            ? String(
                item.trend,
              )
            : undefined,
      }),
    );
}

function normalizeDashboardData(
  source: unknown,
): DashboardData | null {
  if (
    !isObject(source)
  ) {
    return null;
  }

  const payload =
    isObject(
      source.data,
    )
      ? source.data
      : source;

  return {
    executive:
      isObject(
        payload.executive,
      )
        ? (
            payload.executive as
              ExecutiveMetrics
          )
        : undefined,

    platform:
      isObject(
        payload.platform,
      )
        ? (
            payload.platform as
              PlatformMetrics
          )
        : undefined,

    documents:
      isObject(
        payload.documents,
      )
        ? (
            payload.documents as
              DocumentMetricsSummary
          )
        : undefined,

    health:
      normalizeHealth(
        payload.health,
      ),

    operations:
      Array.isArray(
        payload.operations,
      )
        ? (
            payload.operations as
              DashboardMetric[]
          )
        : isObject(
            payload.operations,
          )
          ? payload.operations
          : undefined,

    aiMetrics:
      Array.isArray(
        payload.aiMetrics,
      )
        ? (
            payload.aiMetrics as
              DashboardMetric[]
          )
        : isObject(
            payload.aiMetrics,
          )
          ? payload.aiMetrics
          : undefined,

    security:
      Array.isArray(
        payload.security,
      )
        ? (
            payload.security as
              DashboardMetric[]
          )
        : isObject(
            payload.security,
          )
          ? payload.security
          : undefined,

    auditLogs:
      normalizeAuditLogs(
        payload.auditLogs,
      ),

    alerts:
      normalizeAlerts(
        payload.alerts,
      ),

    documentMetrics:
      Array.isArray(
        payload.documentMetrics,
      )
        ? (
            payload.documentMetrics as
              DocumentMetric[]
          )
        : [],

    regions:
      normalizeRegions(
        payload.regions,
      ),
  };
}

/* ============================================================================
 * DOWNLOAD URL SECURITY
 * ========================================================================== */

function getAllowedDownloadOrigins(): Set<string> {
  const origins =
    new Set<string>();

  if (
    typeof window !==
    "undefined"
  ) {
    origins.add(
      window.location.origin,
    );
  }

  try {
    const apiBaseURL =
      appConfig.api.baseURL;

    if (apiBaseURL) {
      origins.add(
        new URL(
          apiBaseURL,
          window.location.origin,
        ).origin,
      );
    }
  } catch {
    // Ignore invalid configuration.
  }

  return origins;
}

function getSafeDownloadURL(
  downloadUrl: unknown,
): string | null {
  if (
    typeof downloadUrl !==
      "string" ||
    !downloadUrl.trim()
  ) {
    return null;
  }

  try {
    const parsedURL =
      new URL(
        downloadUrl,
        window.location.origin,
      );

    if (
      parsedURL.protocol !==
        "http:" &&
      parsedURL.protocol !==
        "https:"
    ) {
      return null;
    }

    const allowedOrigins =
      getAllowedDownloadOrigins();

    if (
      !allowedOrigins.has(
        parsedURL.origin,
      )
    ) {
      return null;
    }

    return parsedURL.toString();
  } catch {
    return null;
  }
}

/* ============================================================================
 * SMALL PRESENTATIONAL HELPERS
 * ========================================================================== */

function EmptyState({
  icon: Icon,
  title,
  description,
}: {
  icon: LucideIcon;
  title: string;
  description: string;
}) {
  return (
    <div className="flex min-h-56 flex-col items-center justify-center rounded-3xl border border-dashed border-slate-200 bg-slate-50/70 p-8 text-center">
      <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-white text-slate-400 shadow-sm">
        <Icon
          size={30}
        />
      </div>

      <h3 className="mt-4 font-bold text-slate-700">
        {title}
      </h3>

      <p className="mt-2 max-w-md text-sm leading-6 text-slate-500">
        {description}
      </p>
    </div>
  );
}

function MetricList({
  metrics,
  accent,
}: {
  metrics: DashboardMetric[];
  accent:
    | "indigo"
    | "violet"
    | "emerald";
}) {
  const accentClasses = {
    indigo:
      "text-indigo-700 bg-indigo-50 border-indigo-100",

    violet:
      "text-violet-700 bg-violet-50 border-violet-100",

    emerald:
      "text-emerald-700 bg-emerald-50 border-emerald-100",
  };

  if (
    metrics.length ===
    0
  ) {
    return (
      <EmptyState
        icon={
          Activity
        }
        title="No metrics available"
        description="Live platform metrics will appear here when the backend provides them."
      />
    );
  }

  return (
    <div className="space-y-3">
      {metrics.map(
        (metric) => (
          <div
            key={
              metric.label
            }
            className={`${UI.metricCard} flex items-center justify-between gap-4`}
          >
            <div className="min-w-0">
              <p className="truncate font-bold text-slate-800">
                {
                  metric.label
                }
              </p>

              <p className="mt-1 text-xs text-slate-400">
                Live platform metric
              </p>
            </div>

            <span
              className={`shrink-0 rounded-xl border px-3 py-2 text-sm font-black ${accentClasses[accent]}`}
            >
              {formatMetricValue(
                metric.value,
              )}
            </span>
          </div>
        ),
      )}
    </div>
  );
}

/* ============================================================================
 * COMPONENT
 * ========================================================================== */

export default function AdminDashboardPage() {
  const navigate =
    useNavigate();

  const {
    data,
    loading,
    error,
    refresh,
  } =
    useAdminDashboard();

  const [
    creatingCase,
    setCreatingCase,
  ] = useState(false);

  const [
    generatingReport,
    setGeneratingReport,
  ] = useState(false);

  const [
    runningAudit,
    setRunningAudit,
  ] = useState(false);

  /* ==========================================================================
   * NORMALIZED DATA
   * ======================================================================== */

  const dashboard =
    useMemo(
      () =>
        normalizeDashboardData(
          data,
        ),
      [data],
    );

  const executive =
    dashboard?.executive;

  const platform =
    dashboard?.platform;

  const documents =
    dashboard?.documents;

  const operations =
    useMemo(
      () =>
        normalizeMetrics(
          dashboard?.operations,
        ),
      [dashboard?.operations],
    );

  const aiMetrics =
    useMemo(
      () =>
        normalizeMetrics(
          dashboard?.aiMetrics,
        ),
      [dashboard?.aiMetrics],
    );

  const security =
    useMemo(
      () =>
        normalizeMetrics(
          dashboard?.security,
        ),
      [dashboard?.security],
    );

  const health =
    dashboard?.health ??
    [];

  const auditLogs =
    dashboard?.auditLogs ??
    [];

  const alerts =
    dashboard?.alerts ??
    [];

  const regions =
    dashboard?.regions ??
    [];

  /* ==========================================================================
   * ACTIONS
   * ======================================================================== */

  const handleCreateCase =
    useCallback(
      () => {
        if (
          creatingCase
        ) {
          return;
        }

        setCreatingCase(
          true,
        );

        try {
          navigate(
            "/admin/cases/create",
          );
        } catch (
          navigationError
        ) {
          console.error(
            "[AdminDashboard] Case navigation failed:",
            navigationError,
          );

          toast.error(
            "Unable to open case creation.",
          );

          setCreatingCase(
            false,
          );
        }
      },
      [
        creatingCase,
        navigate,
      ],
    );

  const handleGenerateReport =
    useCallback(
      async () => {
        if (
          generatingReport
        ) {
          return;
        }

        setGeneratingReport(
          true,
        );

        const toastId =
          toast.loading(
            "Generating enterprise report...",
          );

        try {
          const result =
            await generateAdminReport();

          const message =
            result?.message ??
            "Report generated successfully.";

          const downloadURL =
            getSafeDownloadURL(
              result?.downloadUrl,
            );

          if (
            result?.downloadUrl &&
            !downloadURL
          ) {
            toast.error(
              "The report was generated, but its download URL was rejected for security reasons.",
              {
                id: toastId,
              },
            );

            return;
          }

          toast.success(
            message,
            {
              id: toastId,
            },
          );

          if (
            downloadURL
          ) {
            window.open(
              downloadURL,
              "_blank",
              "noopener,noreferrer",
            );
          }
        } catch (
          reportError: unknown
        ) {
          console.error(
            "[AdminDashboard] Report generation failed:",
            reportError,
          );

          toast.error(
            getApiErrorMessage(
              reportError,
              "Failed to generate enterprise report.",
            ),
            {
              id: toastId,
            },
          );
        } finally {
          setGeneratingReport(
            false,
          );
        }
      },
      [
        generatingReport,
      ],
    );

  const handleRunAudit =
    useCallback(
      async () => {
        if (
          runningAudit
        ) {
          return;
        }

        setRunningAudit(
          true,
        );

        const toastId =
          toast.loading(
            "Running AI audit...",
          );

        try {
          const result =
            await runAIAudit();

          toast.success(
            result?.message ??
              "AI audit completed successfully.",
            {
              id: toastId,
            },
          );

          await refresh();
        } catch (
          auditError: unknown
        ) {
          console.error(
            "[AdminDashboard] AI audit failed:",
            auditError,
          );

          if (
            isForbiddenError(
              auditError,
            )
          ) {
            toast.error(
              "Administrator authorization is required to run the AI audit.",
              {
                id: toastId,
              },
            );

            return;
          }

          toast.error(
            getApiErrorMessage(
              auditError,
              "Failed to run AI audit.",
            ),
            {
              id: toastId,
            },
          );
        } finally {
          setRunningAudit(
            false,
          );
        }
      },
      [
        runningAudit,
        refresh,
      ],
    );

  const handleRefresh =
    useCallback(
      async () => {
        try {
          await refresh();

          toast.success(
            "Dashboard data refreshed.",
          );
        } catch (
          refreshError: unknown
        ) {
          console.error(
            "[AdminDashboard] Refresh failed:",
            refreshError,
          );

          toast.error(
            getApiErrorMessage(
              refreshError,
              "Unable to refresh dashboard data.",
            ),
          );
        }
      },
      [refresh],
    );

  /* ==========================================================================
   * DERIVED METRICS
   * ======================================================================== */

  const healthMetrics =
    useMemo<
      HealthMetric[]
    >(
      () =>
        health.map(
          (
            item,
          ) => ({
            title:
              item.title,

            value:
              formatMetricValue(
                item.value,
              ),

            status:
              item.status,

            icon:
              healthIcons[
                item.title
              ] ??
              Activity,
          }),
        ),
      [health],
    );

  const documentMetrics =
    useMemo<
      DashboardMetric[]
    >(
      () => {
        if (
          dashboard
            ?.documentMetrics
            ?.length
        ) {
          return dashboard.documentMetrics.map(
            (
              metric,
            ) => ({
              label:
                metric.title,

              value:
                metric.value,
            }),
          );
        }

        if (
          !documents
        ) {
          return [];
        }

        const metrics:
          DashboardMetric[] =
          [];

        if (
          documents.ocrAccuracy !==
          undefined
        ) {
          metrics.push({
            label:
              "OCR Accuracy",

            value:
              formatPercentage(
                documents.ocrAccuracy,
              ),
          });
        }

        if (
          documents.fraudAlerts !==
          undefined
        ) {
          metrics.push({
            label:
              "Fraud Alerts",

            value:
              documents.fraudAlerts,
          });
        }

        if (
          documents.averageProcessingTime !==
          undefined
        ) {
          metrics.push({
            label:
              "Processing Time",

            value:
              documents.averageProcessingTime,
          });
        }

        if (
          documents.successRate !==
          undefined
        ) {
          metrics.push({
            label:
              "Success Rate",

            value:
              formatPercentage(
                documents.successRate,
              ),
          });
        }

        return metrics;
      },
      [
        dashboard
          ?.documentMetrics,
        documents,
      ],
    );

  const kpis =
    useMemo<
      KPIData[]
    >(
      () => [
        {
          title:
            "Active Users",

          value:
            formatNumber(
              executive
                ?.activeUsers,
            ),

          change:
            formatChange(
              executive
                ?.activeUsersChange,
            ),

          icon:
            Users,
        },

        {
          title:
            "Active Cases",

          value:
            formatNumber(
              executive
                ?.activeCases,
            ),

          change:
            formatChange(
              executive
                ?.activeCasesChange,
            ),

          icon:
            Briefcase,
        },

        {
          title:
            "Monthly Revenue",

          value:
            formatCurrency(
              executive
                ?.monthlyRevenue,
            ),

          change:
            formatChange(
              executive
                ?.revenueChange,
            ),

          icon:
            CreditCard,
        },

        {
          title:
            "AI Accuracy",

          value:
            formatPercentage(
              executive
                ?.aiAccuracy,
            ),

          change:
            formatChange(
              executive
                ?.aiAccuracyChange,
            ),

          icon:
            Bot,
        },
      ],
      [executive],
    );

  /* ==========================================================================
   * CONFIGURATION
   * ======================================================================== */

  const platformStatus =
    toStringValue(
      platform?.status,
      "UNKNOWN",
    ).toUpperCase();

  const platformOnline =
    platformStatus ===
    "ONLINE";

  const aiModels =
    formatNumber(
      platform?.aiModels,
    );

  const platformUptime =
    formatPercentage(
      platform?.uptime,
    );

  const platformName =
    appConfig.branding
      ?.platformName ??
    "AI Platform";

  const dashboardHeadline =
    appConfig.branding
      ?.dashboardHeadline ??
    "Immigration Command Center";

  const dashboardDescription =
    appConfig.branding
      ?.dashboardDescription ??
    "Enterprise administration dashboard for immigration workflows, artificial intelligence operations, security monitoring and global platform management.";

  /* ==========================================================================
   * LOADING STATE
   * ======================================================================== */

  if (
    loading
  ) {
    return (
      <section
        className={`${UI.darkPanel} relative min-h-[520px] overflow-hidden`}
      >
        <div
          className="absolute -right-32 -top-32 h-96 w-96 rounded-full bg-violet-600/20 blur-3xl"
          aria-hidden="true"
        />

        <div
          className="absolute -bottom-24 left-1/3 h-72 w-72 rounded-full bg-cyan-500/10 blur-3xl"
          aria-hidden="true"
        />

        <div className="relative flex min-h-[520px] flex-col items-center justify-center px-6 text-center">
          <div className="flex h-20 w-20 items-center justify-center rounded-3xl bg-gradient-to-br from-indigo-500 via-violet-600 to-fuchsia-600 text-white shadow-2xl shadow-violet-900/40">
            <Bot
              size={38}
              className="animate-pulse"
            />
          </div>

          <h1 className="mt-7 text-2xl font-black text-white sm:text-3xl">
            Loading enterprise dashboard
          </h1>

          <p className="mt-3 max-w-xl text-sm leading-7 text-slate-400 sm:text-base">
            Retrieving the latest platform,
            AI, security and immigration
            intelligence.
          </p>

          <div className="mt-7 flex items-center gap-2 text-sm font-semibold text-violet-300">
            <RefreshCw
              size={16}
              className="animate-spin"
            />

            Synchronizing platform data
          </div>
        </div>
      </section>
    );
  }

  /* ==========================================================================
   * ERROR STATE
   * ======================================================================== */

  if (
    error ||
    !dashboard
  ) {
    const errorMessage =
      typeof error ===
        "string" &&
      error.trim()
        ? error
        : "Unable to retrieve dashboard information from the backend.";

    const isForbidden =
      /403|forbidden|unauthorized|access denied/i.test(
        errorMessage,
      );

    return (
      <section
        className={`${UI.panel} flex min-h-[520px] items-center justify-center p-8`}
      >
        <div className="max-w-2xl text-center">
          <div
            className={`mx-auto flex h-20 w-20 items-center justify-center rounded-3xl ${
              isForbidden
                ? "bg-amber-50 text-amber-600"
                : "bg-rose-50 text-rose-600"
            }`}
          >
            {isForbidden ? (
              <ShieldAlert
                size={38}
              />
            ) : (
              <AlertTriangle
                size={38}
              />
            )}
          </div>

          <h1 className="mt-7 text-3xl font-black text-slate-900">
            {isForbidden
              ? "Administrator access required"
              : "Dashboard unavailable"}
          </h1>

          <p className="mt-4 text-sm leading-7 text-slate-500">
            {isForbidden
              ? "The backend rejected this request. Make sure the authenticated account has the ADMIN role and that the JWT Bearer token is being sent with the request."
              : errorMessage}
          </p>

          {isForbidden && (
            <div className="mx-auto mt-6 max-w-xl rounded-2xl border border-amber-200 bg-amber-50 p-5 text-left">
              <p className="text-sm font-black text-amber-800">
                Authorization check
              </p>

              <ul className="mt-3 space-y-2 text-sm leading-6 text-amber-700">
                <li>
                  • The administrator
                  must be authenticated.
                </li>

                <li>
                  • The JWT must be sent
                  as{" "}
                  <code className="rounded bg-amber-100 px-1.5 py-0.5 font-mono text-xs">
                    Authorization: Bearer &lt;token&gt;
                  </code>
                </li>

                <li>
                  • The authenticated
                  user must have the{" "}
                  <strong>
                    ADMIN
                  </strong>{" "}
                  role.
                </li>

                <li>
                  • Spring Security must
                  permit the admin endpoint
                  for that role.
                </li>
              </ul>
            </div>
          )}

          <button
            type="button"
            onClick={
              handleRefresh
            }
            className={`${UI.primaryButton} mt-8`}
          >
            <RefreshCw
              size={18}
            />

            Retry Dashboard
          </button>
        </div>
      </section>
    );
  }

  /* ==========================================================================
   * DASHBOARD
   * ======================================================================== */

  return (
    <motion.div
      initial={{
        opacity: 0,
        y: 14,
      }}
      animate={{
        opacity: 1,
        y: 0,
      }}
      transition={{
        duration: 0.4,
      }}
      className={UI.page}
    >
      {/* ====================================================================
          TOP ACTION BAR
      ==================================================================== */}

      <section
        aria-label="Dashboard actions"
        className="sticky top-4 z-30"
      >
        <div className="rounded-3xl border border-slate-200/80 bg-white/95 p-3 shadow-xl shadow-slate-200/40 backdrop-blur-xl">
          <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
            <div className="flex items-center gap-3 px-2">
              <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-gradient-to-br from-indigo-600 via-violet-600 to-fuchsia-600 text-white shadow-lg shadow-indigo-500/20">
                <Sparkles
                  size={20}
                />
              </div>

              <div>
                <p className="text-xs font-black uppercase tracking-[0.18em] text-indigo-600">
                  Quick Actions
                </p>

                <p className="text-sm font-semibold text-slate-500">
                  Manage your enterprise platform
                </p>
              </div>
            </div>

            <div className="flex flex-wrap gap-2">
              <button
                type="button"
                onClick={
                  handleCreateCase
                }
                disabled={
                  creatingCase
                }
                aria-busy={
                  creatingCase
                }
                className={
                  UI.primaryButton
                }
              >
                <Briefcase
                  size={18}
                />

                {creatingCase
                  ? "Opening..."
                  : "Create Case"}

                <ChevronRight
                  size={16}
                />
              </button>

              <button
                type="button"
                onClick={
                  handleGenerateReport
                }
                disabled={
                  generatingReport
                }
                aria-busy={
                  generatingReport
                }
                className={
                  UI.secondaryButton
                }
              >
                {generatingReport ? (
                  <RefreshCw
                    size={18}
                    className="animate-spin"
                  />
                ) : (
                  <FileCheck2
                    size={18}
                  />
                )}

                {generatingReport
                  ? "Generating..."
                  : "Generate Report"}
              </button>

              <button
                type="button"
                onClick={
                  handleRunAudit
                }
                disabled={
                  runningAudit
                }
                aria-busy={
                  runningAudit
                }
                className={
                  UI.darkButton
                }
              >
                {runningAudit ? (
                  <RefreshCw
                    size={18}
                    className="animate-spin"
                  />
                ) : (
                  <ShieldCheck
                    size={18}
                  />
                )}

                {runningAudit
                  ? "Auditing..."
                  : "Run AI Audit"}
              </button>

              <button
                type="button"
                onClick={
                  handleRefresh
                }
                aria-label="Refresh dashboard"
                className="inline-flex items-center justify-center gap-2 rounded-2xl border border-slate-200 bg-slate-50 px-5 py-3 font-bold text-slate-700 transition-all duration-200 hover:-translate-y-0.5 hover:border-cyan-200 hover:bg-cyan-50 hover:text-cyan-700 hover:shadow-md focus:outline-none focus:ring-2 focus:ring-cyan-500 focus:ring-offset-2"
              >
                <RefreshCw
                  size={18}
                />

                Refresh
              </button>
            </div>
          </div>
        </div>
      </section>

      {/* ====================================================================
          HERO HEADER
      ==================================================================== */}

      <section
        aria-labelledby="dashboard-header-title"
        className={`${UI.darkPanel} relative overflow-hidden`}
      >
        <div
          className="pointer-events-none absolute -right-32 -top-32 h-96 w-96 rounded-full bg-violet-600/20 blur-3xl"
          aria-hidden="true"
        />

        <div
          className="pointer-events-none absolute -bottom-40 left-1/3 h-96 w-96 rounded-full bg-cyan-500/10 blur-3xl"
          aria-hidden="true"
        />

        <div
          className="pointer-events-none absolute right-1/3 top-1/2 h-40 w-40 rounded-full bg-fuchsia-500/10 blur-3xl"
          aria-hidden="true"
        />

        <div className="relative p-6 sm:p-8 lg:p-10">
          <div className="max-w-4xl">
            <div className="flex items-center gap-3">
              <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-indigo-500 via-violet-600 to-fuchsia-600 text-white shadow-xl shadow-indigo-900/40">
                <Sparkles
                  size={23}
                />
              </div>

              <div>
                <p className="text-xs font-black uppercase tracking-[0.22em] text-violet-300">
                  {platformName}
                </p>

                <p className="mt-1 text-xs font-medium text-slate-500">
                  Enterprise Intelligence Platform
                </p>
              </div>
            </div>

            <h1
              id="dashboard-header-title"
              className="mt-6 text-3xl font-black tracking-tight text-white sm:text-4xl lg:text-5xl xl:text-6xl"
            >
              {dashboardHeadline}
            </h1>

            <p className="mt-5 max-w-3xl text-sm leading-7 text-slate-400 sm:text-base">
              {dashboardDescription}
            </p>

            <div className="mt-7 flex flex-wrap gap-3">
              <StatusBadge
                label={
                  platformOnline
                    ? "All Systems Operational"
                    : "System Attention Required"
                }
                status={
                  platformOnline
                    ? "success"
                    : "warning"
                }
              />

              <StatusBadge
                label={`${aiModels} AI Models Active`}
                status="info"
              />

              <StatusBadge
                label={`${platformUptime} Platform Uptime`}
                status={
                  platformOnline
                    ? "success"
                    : "warning"
                }
              />
            </div>
          </div>
        </div>
      </section>

      {/* ====================================================================
          EXECUTIVE KPI GRID
      ==================================================================== */}

      <section aria-label="Executive metrics">
        <div className="mb-5 flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="text-xs font-black uppercase tracking-[0.2em] text-indigo-500">
              Executive overview
            </p>

            <h2 className="mt-1 text-2xl font-black text-slate-900">
              Platform performance
            </h2>
          </div>

          <p className="text-sm text-slate-500">
            Real-time enterprise indicators
          </p>
        </div>

        <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-4">
          {kpis.map(
            (
              item,
            ) => (
              <KPICard
                key={
                  item.title
                }
                title={
                  item.title
                }
                value={
                  item.value
                }
                change={
                  item.change
                }
                icon={
                  item.icon
                }
              />
            ),
          )}
        </div>
      </section>

      {/* ====================================================================
          OPERATIONS / AI / SECURITY
      ==================================================================== */}

      <section>
        <div className="mb-5">
          <p className="text-xs font-black uppercase tracking-[0.2em] text-violet-500">
            Intelligence centers
          </p>

          <h2 className="mt-1 text-2xl font-black text-slate-900">
            Operational command
          </h2>
        </div>

        <div className="grid gap-6 xl:grid-cols-3">
          <SectionCard
            title="Immigration Operations"
            subtitle="Application processing intelligence"
          >
            <MetricList
              metrics={
                operations
              }
              accent="indigo"
            />
          </SectionCard>

          <SectionCard
            title="AI Intelligence Center"
            subtitle="Machine learning performance"
          >
            <MetricList
              metrics={
                aiMetrics
              }
              accent="violet"
            />
          </SectionCard>

          <SectionCard
            title="Security Operations"
            subtitle="Threat detection and security monitoring"
          >
            <MetricList
              metrics={
                security
              }
              accent="emerald"
            />
          </SectionCard>
        </div>
      </section>

      {/* ====================================================================
          DOCUMENT INTELLIGENCE
      ==================================================================== */}

      <section>
        <SectionCard
          title="AI Document Intelligence"
          subtitle="OCR, fraud detection and immigration document verification analytics"
        >
          {documentMetrics.length ===
          0 ? (
            <EmptyState
              icon={
                FileSearch
              }
              title="No document intelligence data"
              description="Document analytics will appear here once the backend begins returning OCR, fraud and processing metrics."
            />
          ) : (
            <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-4">
              {documentMetrics.map(
                (
                  metric,
                  index,
                ) => {
                  const configs = [
                    {
                      icon:
                        FileSearch,

                      wrapper:
                        "bg-cyan-50 text-cyan-600",

                      border:
                        "hover:border-cyan-200",
                    },

                    {
                      icon:
                        ShieldAlert,

                      wrapper:
                        "bg-rose-50 text-rose-600",

                      border:
                        "hover:border-rose-200",
                    },

                    {
                      icon:
                        Clock3,

                      wrapper:
                        "bg-amber-50 text-amber-600",

                      border:
                        "hover:border-amber-200",
                    },

                    {
                      icon:
                        CheckCircle2,

                      wrapper:
                        "bg-emerald-50 text-emerald-600",

                      border:
                        "hover:border-emerald-200",
                    },
                  ];

                  const config =
                    configs[
                      index %
                        configs.length
                    ];

                  const Icon =
                    config.icon;

                  return (
                    <motion.div
                      key={
                        metric.label
                      }
                      whileHover={{
                        y: -5,
                      }}
                      transition={{
                        duration:
                          0.2,
                      }}
                      className={`rounded-3xl border border-slate-200 bg-gradient-to-br from-white to-slate-50 p-6 shadow-sm transition ${config.border}`}
                    >
                      <div
                        className={`flex h-12 w-12 items-center justify-center rounded-2xl ${config.wrapper}`}
                      >
                        <Icon
                          size={24}
                        />
                      </div>

                      <p className="mt-5 text-sm font-semibold text-slate-500">
                        {
                          metric.label
                        }
                      </p>

                      <h3 className="mt-2 break-words text-3xl font-black tracking-tight text-slate-900">
                        {formatMetricValue(
                          metric.value,
                        )}
                      </h3>

                      <div className="mt-5 flex items-center gap-2 text-xs font-semibold text-slate-400">
                        <Activity
                          size={13}
                        />

                        Live document intelligence
                      </div>
                    </motion.div>
                  );
                },
              )}
            </div>
          )}
        </SectionCard>
      </section>

      {/* ====================================================================
          AI MODEL MONITORING
      ==================================================================== */}

      <section>
        <SectionCard
          title="AI Model Monitoring"
          subtitle="Production artificial intelligence health, accuracy and availability"
        >
          <div className="grid gap-5 md:grid-cols-3">
            <div className="group relative overflow-hidden rounded-3xl border border-violet-100 bg-gradient-to-br from-violet-50 via-white to-indigo-50 p-6">
              <div className="absolute -right-8 -top-8 h-24 w-24 rounded-full bg-violet-200/40 blur-2xl" />

              <div className="relative">
                <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-violet-100 text-violet-600">
                  <Bot
                    size={27}
                  />
                </div>

                <p className="mt-5 text-sm font-semibold text-slate-500">
                  Model Accuracy
                </p>

                <h3 className="mt-2 text-4xl font-black tracking-tight text-slate-900">
                  {formatPercentage(
                    executive
                      ?.aiAccuracy,
                  )}
                </h3>

                <div
                  className="mt-5 h-2 overflow-hidden rounded-full bg-violet-100"
                  aria-label={`AI accuracy ${formatPercentage(executive?.aiAccuracy)}`}
                >
                  <motion.div
                    initial={{
                      width: 0,
                    }}
                    animate={{
                      width: `${Math.min(
                        Math.max(
                          toNumber(
                            executive
                              ?.aiAccuracy,
                          ),
                          0,
                        ),
                        100,
                      )}%`,
                    }}
                    transition={{
                      duration:
                        0.8,
                    }}
                    className="h-full rounded-full bg-gradient-to-r from-violet-500 to-fuchsia-500"
                  />
                </div>
              </div>
            </div>

            <div className="group relative overflow-hidden rounded-3xl border border-amber-100 bg-gradient-to-br from-amber-50 via-white to-orange-50 p-6">
              <div className="absolute -right-8 -top-8 h-24 w-24 rounded-full bg-amber-200/40 blur-2xl" />

              <div className="relative">
                <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-amber-100 text-amber-600">
                  <Sparkles
                    size={27}
                  />
                </div>

                <p className="mt-5 text-sm font-semibold text-slate-500">
                  Active AI Models
                </p>

                <h3 className="mt-2 text-4xl font-black tracking-tight text-slate-900">
                  {aiModels}
                </h3>

                <p className="mt-4 flex items-center gap-2 text-xs font-bold text-amber-600">
                  <Zap
                    size={14}
                  />

                  Production models online
                </p>
              </div>
            </div>

            <div className="group relative overflow-hidden rounded-3xl border border-emerald-100 bg-gradient-to-br from-emerald-50 via-white to-teal-50 p-6">
              <div className="absolute -right-8 -top-8 h-24 w-24 rounded-full bg-emerald-200/40 blur-2xl" />

              <div className="relative">
                <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-600">
                  <Activity
                    size={27}
                  />
                </div>

                <p className="mt-5 text-sm font-semibold text-slate-500">
                  AI Service Availability
                </p>

                <h3 className="mt-2 text-4xl font-black tracking-tight text-slate-900">
                  {platformUptime}
                </h3>

                <p className="mt-4 flex items-center gap-2 text-xs font-bold text-emerald-600">
                  <CheckCircle2
                    size={14}
                  />

                  Service health monitored
                </p>
              </div>
            </div>
          </div>
        </SectionCard>
      </section>

      {/* ====================================================================
          PLATFORM HEALTH
      ==================================================================== */}

      {healthMetrics.length >
        0 && (
        <section>
          <div className="mb-5">
            <p className="text-xs font-black uppercase tracking-[0.2em] text-emerald-500">
              Infrastructure
            </p>

            <h2 className="mt-1 text-2xl font-black text-slate-900">
              Platform health
            </h2>
          </div>

          <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-4">
            {healthMetrics.map(
              (
                metric,
              ) => (
                <KPICard
                  key={
                    metric.title
                  }
                  title={
                    metric.title
                  }
                  value={
                    metric.value
                  }
                  change={
                    metric.status
                  }
                  icon={
                    metric.icon
                  }
                />
              ),
            )}
          </div>
        </section>
      )}

      {/* ====================================================================
          AUDIT / ALERTS
      ==================================================================== */}

      <section>
        <div className="mb-5">
          <p className="text-xs font-black uppercase tracking-[0.2em] text-rose-500">
            Governance
          </p>

          <h2 className="mt-1 text-2xl font-black text-slate-900">
            Security & activity
          </h2>
        </div>

        <div className="grid gap-6 xl:grid-cols-2">
          <SectionCard
            title="Recent Audit Activities"
            subtitle="Administrator actions and system events"
          >
            <div className="space-y-3">
              {auditLogs.length ===
              0 ? (
                <EmptyState
                  icon={
                    Fingerprint
                  }
                  title="No audit activity"
                  description="Administrator actions and system events will appear here."
                />
              ) : (
                auditLogs.map(
                  (
                    log,
                  ) => (
                    <div
                      key={
                        log.id
                      }
                      className="group rounded-2xl border border-slate-200 bg-slate-50/70 p-4 transition hover:border-indigo-200 hover:bg-white hover:shadow-md"
                    >
                      <div className="flex items-start gap-4">
                        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-indigo-100 text-indigo-600">
                          <Fingerprint
                            size={
                              19
                            }
                          />
                        </div>

                        <div className="min-w-0 flex-1">
                          <p className="text-sm font-semibold leading-6 text-slate-700">
                            {
                              log.message
                            }
                          </p>

                          {log.time && (
                            <p className="mt-1 text-xs font-medium text-slate-400">
                              {
                                log.time
                              }
                            </p>
                          )}
                        </div>

                        <ChevronRight
                          size={17}
                          className="mt-1 shrink-0 text-slate-300 transition group-hover:translate-x-1 group-hover:text-indigo-500"
                        />
                      </div>
                    </div>
                  ),
                )
              )}
            </div>
          </SectionCard>

          <SectionCard
            title="System Alerts"
            subtitle="Critical platform notifications"
          >
            <div className="space-y-3">
              {alerts.length ===
              0 ? (
                <div className="rounded-2xl border border-emerald-100 bg-gradient-to-r from-emerald-50 to-teal-50 p-6">
                  <div className="flex items-center gap-4">
                    <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-emerald-100 text-emerald-600">
                      <ShieldCheck
                        size={
                          22
                        }
                      />
                    </div>

                    <div>
                      <p className="font-bold text-emerald-700">
                        No active system alerts
                      </p>

                      <p className="mt-1 text-xs text-emerald-600/80">
                        Your platform is currently operating normally.
                      </p>
                    </div>
                  </div>
                </div>
              ) : (
                alerts.map(
                  (
                    alert,
                  ) => (
                    <div
                      key={
                        alert.id
                      }
                      className="rounded-2xl border border-rose-100 bg-gradient-to-r from-rose-50 to-orange-50 p-5"
                    >
                      <div className="flex items-start gap-4">
                        <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-rose-100 text-rose-600">
                          <AlertTriangle
                            size={
                              21
                            }
                          />
                        </div>

                        <div>
                          <p className="text-sm font-bold leading-6 text-rose-700">
                            {
                              alert.message
                            }
                          </p>

                          {alert.time && (
                            <p className="mt-2 text-xs font-medium text-rose-500">
                              {
                                alert.time
                              }
                            </p>
                          )}
                        </div>
                      </div>
                    </div>
                  ),
                )
              )}
            </div>
          </SectionCard>
        </div>
      </section>

      {/* ====================================================================
          GLOBAL IMMIGRATION INTELLIGENCE
      ==================================================================== */}

      <section>
        <SectionCard
          title="Global Immigration Intelligence"
          subtitle="Regional visa demand, application trends and immigration analytics"
        >
          {regions.length ===
          0 ? (
            <div className="flex min-h-72 flex-col items-center justify-center rounded-3xl bg-gradient-to-br from-slate-50 to-indigo-50/50 p-8 text-center">
              <div className="flex h-20 w-20 items-center justify-center rounded-3xl bg-indigo-100 text-indigo-500 shadow-lg shadow-indigo-100">
                <Globe
                  size={40}
                />
              </div>

              <p className="mt-5 font-black text-slate-700">
                No regional data available
              </p>

              <p className="mt-2 max-w-md text-sm leading-6 text-slate-500">
                Regional immigration analytics will appear here when the backend provides reporting data.
              </p>
            </div>
          ) : (
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
              {regions.map(
                (
                  region,
                ) => (
                  <motion.div
                    key={
                      region.id
                    }
                    whileHover={{
                      y: -4,
                    }}
                    className="rounded-2xl border border-slate-200 bg-gradient-to-br from-white to-slate-50 p-5 shadow-sm transition hover:border-indigo-200 hover:shadow-lg"
                  >
                    <div className="flex items-center justify-between gap-4">
                      <div className="flex min-w-0 items-center gap-3">
                        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600">
                          <MapPinned
                            size={
                              20
                            }
                          />
                        </div>

                        <span className="truncate font-bold text-slate-800">
                          {
                            region.region
                          }
                        </span>
                      </div>

                      <Globe
                        size={17}
                        className="shrink-0 text-slate-300"
                      />
                    </div>

                    <div className="mt-6 flex items-end justify-between gap-4">
                      <div>
                        <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                          Applications
                        </p>

                        <strong className="mt-1 block text-3xl font-black text-slate-900">
                          {formatMetricValue(
                            region.value,
                          )}
                        </strong>
                      </div>

                      {region.trend && (
                        <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-3 py-1.5 text-xs font-black text-emerald-600">
                          <TrendingUp
                            size={13}
                          />

                          {
                            region.trend
                          }
                        </span>
                      )}
                    </div>
                  </motion.div>
                ),
              )}
            </div>
          )}
        </SectionCard>
      </section>

      {/* ====================================================================
          PLATFORM STATUS
      ==================================================================== */}

      <section>
        <SectionCard
          title={`${platformName} Status`}
          subtitle="Enterprise infrastructure monitoring"
        >
          <div className="relative overflow-hidden rounded-3xl border border-slate-200 bg-gradient-to-r from-slate-50 via-white to-indigo-50/60 p-6 sm:p-7">
            <div className="absolute -right-16 -top-16 h-40 w-40 rounded-full bg-indigo-100/60 blur-3xl" />

            <div className="relative flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
              <div className="flex items-center gap-4">
                <div
                  className={`flex h-14 w-14 items-center justify-center rounded-2xl ${
                    platformOnline
                      ? "bg-emerald-100 text-emerald-600"
                      : "bg-amber-100 text-amber-600"
                  }`}
                >
                  {platformOnline ? (
                    <CheckCircle2
                      size={27}
                    />
                  ) : (
                    <AlertTriangle
                      size={27}
                    />
                  )}
                </div>

                <div>
                  <p className="text-sm font-semibold text-slate-500">
                    Infrastructure Status
                  </p>

                  <span
                    className={`mt-1 block text-lg font-black ${
                      platformOnline
                        ? "text-emerald-600"
                        : "text-amber-600"
                    }`}
                  >
                    {platformOnline
                      ? "All Systems Operational"
                      : "System Monitoring Required"}
                  </span>
                </div>
              </div>

              <div className="grid gap-3 sm:grid-cols-3">
                <div className="rounded-2xl border border-slate-200 bg-white px-5 py-4 shadow-sm">
                  <div className="flex items-center gap-2 text-slate-400">
                    <ServerCog
                      size={15}
                    />

                    <p className="text-xs font-bold uppercase tracking-wider">
                      Status
                    </p>
                  </div>

                  <p className="mt-1 font-black text-slate-900">
                    {
                      platformStatus
                    }
                  </p>
                </div>

                <div className="rounded-2xl border border-slate-200 bg-white px-5 py-4 shadow-sm">
                  <div className="flex items-center gap-2 text-slate-400">
                    <Bot
                      size={15}
                    />

                    <p className="text-xs font-bold uppercase tracking-wider">
                      AI Models
                    </p>
                  </div>

                  <p className="mt-1 font-black text-slate-900">
                    {
                      aiModels
                    }
                  </p>
                </div>

                <div className="rounded-2xl border border-slate-200 bg-white px-5 py-4 shadow-sm">
                  <div className="flex items-center gap-2 text-slate-400">
                    <Activity
                      size={15}
                    />

                    <p className="text-xs font-bold uppercase tracking-wider">
                      Uptime
                    </p>
                  </div>

                  <p className="mt-1 font-black text-slate-900">
                    {
                      platformUptime
                    }
                  </p>
                </div>
              </div>
            </div>
          </div>
        </SectionCard>
      </section>

      {/* ====================================================================
          FOOTER SUMMARY
      ==================================================================== */}

      <section>
        <div className="overflow-hidden rounded-3xl border border-indigo-100 bg-gradient-to-r from-indigo-50 via-violet-50 to-fuchsia-50 p-6 sm:p-8">
          <div className="flex flex-col gap-5 md:flex-row md:items-center md:justify-between">
            <div className="flex items-start gap-4">
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-white text-indigo-600 shadow-sm">
                <Sparkles
                  size={23}
                />
              </div>

              <div>
                <h3 className="font-black text-slate-900">
                  Enterprise AI Command Center
                </h3>

                <p className="mt-1 max-w-2xl text-sm leading-6 text-slate-500">
                  Monitor immigration operations,
                  document intelligence, AI performance,
                  security events and global application
                  activity from one centralized workspace.
                </p>
              </div>
            </div>

            <div className="flex shrink-0 items-center gap-2 rounded-2xl border border-white/80 bg-white/70 px-4 py-3 text-sm font-bold text-indigo-700 shadow-sm">
              <Activity
                size={16}
              />

              Live Monitoring
            </div>
          </div>
        </div>
      </section>
    </motion.div>
  );
}

