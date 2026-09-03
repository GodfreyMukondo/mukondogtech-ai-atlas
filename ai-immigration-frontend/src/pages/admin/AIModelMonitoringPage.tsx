import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

import {
  Activity,
  AlertTriangle,
  BarChart3,
  BrainCircuit,
  CheckCircle2,
  Clock3,
  Cpu,
  Database,
  Download,
  Eye,
  RefreshCw,
  Server,
  ShieldCheck,
  TrendingUp,
  Bot,
  Zap,
  X,
  type LucideIcon,
} from "lucide-react";

import API from "@/api/axios";
import env from "@/config/env";

/* ============================================================================
 * CONFIGURATION
 * ========================================================================== */

const AI_MONITORING_ENDPOINT = "/admin/ai-monitoring";

const AI_MONITORING_ACTION_ENDPOINT =
  "/admin/ai-monitoring/actions";

const AI_MONITORING_POLL_INTERVAL =
  env.AI_MONITORING_AUTO_REFRESH
    ? env.AI_MONITORING_REFRESH_INTERVAL_MS
    : 0;

/* ============================================================================
 * TYPES
 * ========================================================================== */

type MetricTrend = {
  value: number;
  label: string;
  positive: boolean;
};

type MetricCardData = {
  id: string;
  title: string;
  value: string;
  subtitle: string;
  icon: string;
  color: string;
  trend?: MetricTrend;
};

type ModelStatus =
  | "Healthy"
  | "Monitoring"
  | "Degraded"
  | "Unknown";

type AIModel = {
  id: string;
  name: string;
  status: ModelStatus;
  accuracy: number;
  latency: number;
  requests: number;
  version: string;
};

type InfrastructureStatus =
  | "Healthy"
  | "Operational"
  | "Connected"
  | "Active"
  | "Degraded"
  | "Unavailable"
  | "Unknown";

type InfrastructureMetric = {
  id: string;
  label: string;
  value: string;
  status: InfrastructureStatus;
  percentage?: number;
};

type AlertType =
  | "critical"
  | "warning"
  | "success";

type AlertItem = {
  id: string;
  type: AlertType;
  title: string;
  description: string;
  timestamp?: string;
};

type AdministrationAction = {
  id: string;
  icon: string;
  label: string;
  description: string;
  action: string;
};

type RawAIModelMonitoringResponse = {
  metrics?: unknown[];
  secondaryMetrics?: unknown[];
  secondary_metrics?: unknown[];
  secondary_metrics_data?: unknown[];
  models?: unknown[];
  infrastructure?: unknown[];
  alerts?: unknown[];
  administrationActions?: unknown[];
  administration_actions?: unknown[];
  lastUpdated?: string;
  last_updated?: string;
};

type AIModelMonitoringResponse = {
  metrics: MetricCardData[];
  secondaryMetrics: MetricCardData[];
  models: AIModel[];
  infrastructure: InfrastructureMetric[];
  alerts: AlertItem[];
  administrationActions: AdministrationAction[];
  lastUpdated: string;
};

type ApiErrorResponse = {
  message?: string;
  error?: string;
  timestamp?: string;
  status?: number;
};

/* ============================================================================
 * ICONS
 * ========================================================================== */

const ICONS: Record<string, LucideIcon> = {
  activity: Activity,
  alertTriangle: AlertTriangle,
  barChart: BarChart3,
  brain: BrainCircuit,
  check: CheckCircle2,
  clock: Clock3,
  cpu: Cpu,
  database: Database,
  download: Download,
  eye: Eye,
  refresh: RefreshCw,
  server: Server,
  shield: ShieldCheck,
  trending: TrendingUp,
  bot: Bot,
  zap: Zap,
};

/* ============================================================================
 * STYLES
 * ========================================================================== */

const MODEL_STATUS_STYLES: Record<
  ModelStatus,
  string
> = {
  Healthy:
    "bg-emerald-100 text-emerald-700",
  Monitoring:
    "bg-amber-100 text-amber-700",
  Degraded:
    "bg-red-100 text-red-700",
  Unknown:
    "bg-slate-100 text-slate-600",
};

const INFRASTRUCTURE_STATUS_STYLES: Record<
  InfrastructureStatus,
  string
> = {
  Healthy:
    "bg-emerald-100 text-emerald-700",
  Operational:
    "bg-emerald-100 text-emerald-700",
  Connected:
    "bg-blue-100 text-blue-700",
  Active:
    "bg-blue-100 text-blue-700",
  Degraded:
    "bg-amber-100 text-amber-700",
  Unavailable:
    "bg-red-100 text-red-700",
  Unknown:
    "bg-slate-100 text-slate-600",
};

const ALERT_STYLES: Record<
  AlertType,
  string
> = {
  critical:
    "bg-red-50 border-red-100 text-red-700",
  warning:
    "bg-amber-50 border-amber-100 text-amber-700",
  success:
    "bg-emerald-50 border-emerald-100 text-emerald-700",
};

const ALERT_ICONS: Record<
  AlertType,
  LucideIcon
> = {
  critical: AlertTriangle,
  warning: Clock3,
  success: CheckCircle2,
};

/* ============================================================================
 * HELPERS
 * ========================================================================== */

function formatNumber(
  value: number
): string {
  if (!Number.isFinite(value)) {
    return "0";
  }

  return new Intl.NumberFormat(
    undefined,
    {
      notation: "compact",
      maximumFractionDigits: 1,
    }
  ).format(value);
}

function formatPercentage(
  value: number
): string {
  if (!Number.isFinite(value)) {
    return "0%";
  }

  return `${value.toFixed(1)}%`;
}

function formatLatency(
  value: number
): string {
  if (!Number.isFinite(value)) {
    return "0 ms";
  }

  if (value >= 1000) {
    return `${(value / 1000).toFixed(1)}s`;
  }

  return `${Math.round(value)}ms`;
}

function formatDate(
  value?: string
): string {
  if (!value) {
    return "Never";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "Unknown";
  }

  return new Intl.DateTimeFormat(
    undefined,
    {
      dateStyle: "medium",
      timeStyle: "short",
    }
  ).format(date);
}

function getApiErrorMessage(
  error: unknown
): string {
  if (
    typeof error === "object" &&
    error !== null
  ) {
    const axiosLikeError =
      error as {
        response?: {
          data?: ApiErrorResponse;
          status?: number;
        };
        message?: string;
      };

    const responseData =
      axiosLikeError.response?.data;

    if (responseData?.message) {
      return responseData.message;
    }

    if (responseData?.error) {
      return responseData.error;
    }

    if (axiosLikeError.message) {
      return axiosLikeError.message;
    }
  }

  if (error instanceof Error) {
    return error.message;
  }

  return "An unexpected error occurred.";
}

function normalizeModelStatus(
  status: unknown
): ModelStatus {
  switch (status) {
    case "Healthy":
    case "Monitoring":
    case "Degraded":
    case "Unknown":
      return status;

    default:
      return "Unknown";
  }
}

function normalizeInfrastructureStatus(
  status: unknown
): InfrastructureStatus {
  switch (status) {
    case "Healthy":
    case "Operational":
    case "Connected":
    case "Active":
    case "Degraded":
    case "Unavailable":
    case "Unknown":
      return status;

    default:
      return "Unknown";
  }
}

function normalizeAlertType(
  type: unknown
): AlertType {
  switch (type) {
    case "critical":
    case "warning":
    case "success":
      return type;

    default:
      return "warning";
  }
}

function normalizeMetric(
  metric: unknown,
  index: number
): MetricCardData {
  const source =
    (
      metric ?? {}
    ) as Partial<MetricCardData>;

  const trendSource =
    source.trend;

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
        : "0",

    subtitle:
      typeof source.subtitle === "string"
        ? source.subtitle
        : "",

    icon:
      typeof source.icon === "string"
        ? source.icon
        : "activity",

    color:
      typeof source.color === "string"
        ? source.color
        : "bg-blue-100 text-blue-600",

    trend:
      trendSource &&
      typeof trendSource === "object"
        ? {
            value:
              Number.isFinite(
                trendSource.value
              )
                ? trendSource.value
                : 0,

            label:
              typeof trendSource.label ===
              "string"
                ? trendSource.label
                : "",

            positive:
              Boolean(
                trendSource.positive
              ),
          }
        : undefined,
  };
}

/* ============================================================================
 * NORMALIZATION
 *
 * The backend response is normalized here.
 *
 * This prevents runtime errors such as:
 *
 * Cannot read properties of undefined (reading 'length')
 *
 * Every array expected by the UI receives [] when the backend does not return
 * that field.
 * ========================================================================== */

function normalizeMonitoringResponse(
  raw: unknown
): AIModelMonitoringResponse {
  const response =
    (raw ?? {}) as RawAIModelMonitoringResponse;

  const rawMetrics =
    Array.isArray(response.metrics)
      ? response.metrics
      : [];

  const rawSecondaryMetrics =
    Array.isArray(
      response.secondaryMetrics
    )
      ? response.secondaryMetrics
      : Array.isArray(
          response.secondary_metrics
        )
      ? response.secondary_metrics
      : Array.isArray(
          response.secondary_metrics_data
        )
      ? response.secondary_metrics_data
      : [];

  const rawModels =
    Array.isArray(response.models)
      ? response.models
      : [];

  const rawInfrastructure =
    Array.isArray(
      response.infrastructure
    )
      ? response.infrastructure
      : [];

  const rawAlerts =
    Array.isArray(response.alerts)
      ? response.alerts
      : [];

  const rawAdministrationActions =
    Array.isArray(
      response.administrationActions
    )
      ? response.administrationActions
      : Array.isArray(
          response.administration_actions
        )
      ? response.administration_actions
      : [];

  return {
    metrics: rawMetrics.map(
      (metric, index) =>
        normalizeMetric(
          metric,
          index
        )
    ),

    secondaryMetrics:
      rawSecondaryMetrics.map(
        (metric, index) =>
          normalizeMetric(
            metric,
            index
          )
      ),

    models: rawModels.map(
      (model, index) => {
        const source =
          (
            model ?? {}
          ) as Partial<AIModel>;

        return {
          id:
            typeof source.id ===
            "string"
              ? source.id
              : `model-${index}`,

          name:
            typeof source.name ===
            "string"
              ? source.name
              : "Unnamed AI Model",

          status:
            normalizeModelStatus(
              source.status
            ),

          accuracy:
            Number.isFinite(
              source.accuracy
            )
              ? source.accuracy
              : 0,

          latency:
            Number.isFinite(
              source.latency
            )
              ? source.latency
              : 0,

          requests:
            Number.isFinite(
              source.requests
            )
              ? source.requests
              : 0,

          version:
            typeof source.version ===
            "string"
              ? source.version
              : "Unknown",
        };
      }
    ),

    infrastructure:
      rawInfrastructure.map(
        (metric, index) => {
          const source =
            (
              metric ?? {}
            ) as Partial<InfrastructureMetric>;

          return {
            id:
              typeof source.id ===
              "string"
                ? source.id
                : `infrastructure-${index}`,

            label:
              typeof source.label ===
              "string"
                ? source.label
                : "Infrastructure",

            value:
              typeof source.value ===
              "string"
                ? source.value
                : "Unknown",

            status:
              normalizeInfrastructureStatus(
                source.status
              ),

            percentage:
              typeof source.percentage ===
                "number" &&
              Number.isFinite(
                source.percentage
              )
                ? Math.min(
                    Math.max(
                      source.percentage,
                      0
                    ),
                    100
                  )
                : undefined,
          };
        }
      ),

    alerts: rawAlerts.map(
      (alert, index) => {
        const source =
          (
            alert ?? {}
          ) as Partial<AlertItem>;

        return {
          id:
            typeof source.id ===
            "string"
              ? source.id
              : `alert-${index}`,

          type:
            normalizeAlertType(
              source.type
            ),

          title:
            typeof source.title ===
            "string"
              ? source.title
              : "AI Monitoring Alert",

          description:
            typeof source.description ===
            "string"
              ? source.description
              : "",

          timestamp:
            typeof source.timestamp ===
            "string"
              ? source.timestamp
              : undefined,
        };
      }
    ),

    administrationActions:
      rawAdministrationActions.map(
        (item, index) => {
          const source =
            (
              item ?? {}
            ) as Partial<AdministrationAction>;

          return {
            id:
              typeof source.id ===
              "string"
                ? source.id
                : `action-${index}`,

            icon:
              typeof source.icon ===
              "string"
                ? source.icon
                : "activity",

            label:
              typeof source.label ===
              "string"
                ? source.label
                : "Action",

            description:
              typeof source.description ===
              "string"
                ? source.description
                : "",

            action:
              typeof source.action ===
              "string"
                ? source.action
                : "",
          };
        }
      ),

    lastUpdated:
      typeof response.lastUpdated ===
      "string"
        ? response.lastUpdated
        : typeof response.last_updated ===
          "string"
        ? response.last_updated
        : new Date().toISOString(),
  };
}

/* ============================================================================
 * API
 * ========================================================================== */

async function fetchMonitoringData(
  signal?: AbortSignal
): Promise<AIModelMonitoringResponse> {
  const response = await API.get(
    AI_MONITORING_ENDPOINT,
    {
      signal,
    }
  );

  return normalizeMonitoringResponse(
    response.data
  );
}

/* ============================================================================
 * METRIC CARD
 * ========================================================================== */

function MetricCard({
  title,
  value,
  subtitle,
  icon,
  color,
  trend,
}: MetricCardData) {
  const Icon =
    ICONS[icon] ?? Activity;

  return (
    <article
      className="
        rounded-3xl
        border
        border-slate-200
        bg-white
        p-5
        shadow-sm
        transition
        duration-200
        hover:-translate-y-0.5
        hover:shadow-lg
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
        <div className="min-w-0">
          <p
            className="
              text-sm
              font-semibold
              text-slate-500
            "
          >
            {title}
          </p>

          <h3
            className="
              mt-2
              text-3xl
              font-black
              tracking-tight
              text-slate-900
            "
          >
            {value}
          </h3>

          <p
            className="
              mt-2
              text-sm
              text-slate-500
            "
          >
            {subtitle}
          </p>

          {trend && (
            <div
              className={`
                mt-3
                inline-flex
                items-center
                gap-1
                text-xs
                font-bold
                ${
                  trend.positive
                    ? "text-emerald-600"
                    : "text-amber-600"
                }
              `}
            >
              <TrendingUp
                size={14}
                aria-hidden="true"
              />

              {trend.value > 0
                ? "+"
                : ""}
              {trend.value}%
              {" "}
              {trend.label}
            </div>
          )}
        </div>

        <div
          className={`
            flex
            h-14
            w-14
            shrink-0
            items-center
            justify-center
            rounded-2xl
            ${color}
          `}
          aria-hidden="true"
        >
          <Icon size={24} />
        </div>
      </div>
    </article>
  );
}

/* ============================================================================
 * SECTION CARD
 * ========================================================================== */

function SectionCard({
  title,
  children,
  action,
}: {
  title: string;
  children: React.ReactNode;
  action?: React.ReactNode;
}) {
  return (
    <section
      className="
        overflow-hidden
        rounded-3xl
        border
        border-slate-200
        bg-white
        shadow-sm
      "
    >
      <div
        className="
          flex
          items-center
          justify-between
          gap-4
          border-b
          border-slate-100
          px-6
          py-5
        "
      >
        <h2
          className="
            text-lg
            font-black
            text-slate-900
          "
        >
          {title}
        </h2>

        {action && (
          <div className="shrink-0">
            {action}
          </div>
        )}
      </div>

      <div className="p-6">
        {children}
      </div>
    </section>
  );
}

/* ============================================================================
 * STATUS BADGE
 * ========================================================================== */

function StatusBadge({
  status,
}: {
  status: ModelStatus;
}) {
  const safeStatus =
    normalizeModelStatus(status);

  return (
    <span
      className={`
        inline-flex
        shrink-0
        items-center
        gap-2
        rounded-full
        px-3
        py-1.5
        text-xs
        font-bold
        ${
          MODEL_STATUS_STYLES[
            safeStatus
          ] ??
          MODEL_STATUS_STYLES.Unknown
        }
      `}
    >
      {safeStatus}
    </span>
  );
}

/* ============================================================================
 * INFRASTRUCTURE ROW
 * ========================================================================== */

function InfrastructureRow({
  metric,
}: {
  metric: InfrastructureMetric;
}) {
  const percentage =
    typeof metric.percentage ===
    "number"
      ? Math.min(
          Math.max(
            metric.percentage,
            0
          ),
          100
        )
      : undefined;

  const safeStatus =
    normalizeInfrastructureStatus(
      metric.status
    );

  return (
    <div
      className="
        rounded-2xl
        border
        border-slate-200
        bg-white
        p-4
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
        <div className="min-w-0 flex-1">
          <p
            className="
              font-bold
              text-slate-800
            "
          >
            {metric.label}
          </p>

          {typeof percentage ===
            "number" && (
            <div
              className="
                mt-3
                h-2
                overflow-hidden
                rounded-full
                bg-slate-200
              "
              role="progressbar"
              aria-valuemin={0}
              aria-valuemax={100}
              aria-valuenow={
                percentage
              }
              aria-label={
                metric.label
              }
            >
              <div
                className="
                  h-full
                  rounded-full
                  bg-blue-600
                  transition-all
                  duration-500
                "
                style={{
                  width: `${percentage}%`,
                }}
              />
            </div>
          )}
        </div>

        <span
          className={`
            shrink-0
            rounded-full
            px-3
            py-1.5
            text-xs
            font-bold
            ${
              INFRASTRUCTURE_STATUS_STYLES[
                safeStatus
              ] ??
              INFRASTRUCTURE_STATUS_STYLES.Unknown
            }
          `}
        >
          {metric.value}
        </span>
      </div>
    </div>
  );
}

/* ============================================================================
 * LOADING SKELETON
 * ========================================================================== */

function LoadingSkeleton() {
  return (
    <main
      className="
        min-h-screen
        bg-slate-50
        p-4
        sm:p-6
        lg:p-8
      "
    >
      <div
        className="
          mx-auto
          max-w-7xl
          space-y-6
        "
      >
        <div
          className="
            h-56
            animate-pulse
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
                h-40
                animate-pulse
                rounded-3xl
                bg-slate-200
              "
            />
          ))}
        </div>

        <div
          className="
            grid
            gap-6
            xl:grid-cols-2
          "
        >
          <div
            className="
              h-96
              animate-pulse
              rounded-3xl
              bg-slate-200
            "
          />

          <div
            className="
              h-96
              animate-pulse
              rounded-3xl
              bg-slate-200
            "
          />
        </div>
      </div>
    </main>
  );
}

/* ============================================================================
 * EMPTY STATE
 * ========================================================================== */

function EmptyState({
  title,
  description,
  icon: Icon = Database,
}: {
  title: string;
  description: string;
  icon?: LucideIcon;
}) {
  return (
    <div
      className="
        flex
        flex-col
        items-center
        justify-center
        py-8
        text-center
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
          bg-slate-100
          text-slate-500
        "
      >
        <Icon size={24} />
      </div>

      <h3
        className="
          mt-3
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
          text-sm
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
 * MAIN PAGE
 * ========================================================================== */

export default function AIModelMonitoringPage() {
  const [
    data,
    setData,
  ] = useState<
    AIModelMonitoringResponse | null
  >(null);

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
    null
  );

  const [
    message,
    setMessage,
  ] = useState<string | null>(
    null
  );

  const [
    actionLoading,
    setActionLoading,
  ] = useState<string | null>(
    null
  );

  const abortControllerRef =
    useRef<AbortController | null>(
      null
    );

  /* --------------------------------------------------------------------------
   * LOAD DATA
   * ------------------------------------------------------------------------ */

  const loadData = useCallback(
    async (
      isRefresh = false
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
        const response =
          await fetchMonitoringData(
            controller.signal
          );

        if (
          controller.signal.aborted
        ) {
          return;
        }

        setData(response);
      } catch (requestError) {
        if (
          controller.signal.aborted
        ) {
          return;
        }

        setError(
          getApiErrorMessage(
            requestError
          )
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
    []
  );

  /* --------------------------------------------------------------------------
   * INITIAL LOAD
   * ------------------------------------------------------------------------ */

  useEffect(() => {
    void loadData();

    return () => {
      abortControllerRef.current?.abort();
    };
  }, [loadData]);

  /* --------------------------------------------------------------------------
   * AUTOMATIC REFRESH
   * ------------------------------------------------------------------------ */

  useEffect(() => {
    if (
      !Number.isFinite(
        AI_MONITORING_POLL_INTERVAL
      ) ||
      AI_MONITORING_POLL_INTERVAL <=
        0
    ) {
      return;
    }

    let interval:
      | number
      | undefined;

    const startPolling = () => {
      if (interval) {
        window.clearInterval(
          interval
        );
      }

      interval =
        window.setInterval(() => {
          if (
            document.visibilityState ===
            "visible"
          ) {
            void loadData(true);
          }
        }, AI_MONITORING_POLL_INTERVAL);
    };

    const handleVisibilityChange =
      () => {
        if (
          document.visibilityState ===
          "visible"
        ) {
          void loadData(true);
        }
      };

    startPolling();

    document.addEventListener(
      "visibilitychange",
      handleVisibilityChange
    );

    return () => {
      if (interval) {
        window.clearInterval(
          interval
        );
      }

      document.removeEventListener(
        "visibilitychange",
        handleVisibilityChange
      );
    };
  }, [loadData]);

  /* --------------------------------------------------------------------------
   * ACTION
   * ------------------------------------------------------------------------ */

  const handleAction = useCallback(
    async (
      action: string,
      label: string
    ) => {
      if (
        actionLoading ||
        !action.trim()
      ) {
        return;
      }

      setActionLoading(action);
      setError(null);
      setMessage(null);

      try {
        await API.post(
          AI_MONITORING_ACTION_ENDPOINT,
          {
            action,
          }
        );

        setMessage(
          `${label} completed successfully.`
        );

        await loadData(true);
      } catch (actionError) {
        setError(
          getApiErrorMessage(
            actionError
          )
        );
      } finally {
        setActionLoading(null);
      }
    },
    [
      actionLoading,
      loadData,
    ]
  );

  /* --------------------------------------------------------------------------
   * EXPORT
   * ------------------------------------------------------------------------ */

  const handleExport =
    useCallback(() => {
      if (!data) {
        return;
      }

      try {
        const exportData =
          JSON.stringify(
            data,
            null,
            2
          );

        const blob =
          new Blob(
            [exportData],
            {
              type:
                "application/json",
            }
          );

        const url =
          URL.createObjectURL(
            blob
          );

        const anchor =
          document.createElement(
            "a"
          );

        anchor.href = url;

        anchor.download =
          `ai-monitoring-${new Date()
            .toISOString()
            .replace(
              /[:.]/g,
              "-"
            )}.json`;

        document.body.appendChild(
          anchor
        );

        anchor.click();

        anchor.remove();

        window.setTimeout(() => {
          URL.revokeObjectURL(
            url
          );
        }, 100);

        setMessage(
          "AI monitoring metrics exported successfully."
        );
      } catch (exportError) {
        setError(
          getApiErrorMessage(
            exportError
          )
        );
      }
    }, [data]);

  /* --------------------------------------------------------------------------
   * SAFE DATA
   *
   * Even if a future state update accidentally supplies an incomplete object,
   * the UI will still have safe arrays.
   * ------------------------------------------------------------------------ */

  const safeData =
    useMemo(
      () =>
        normalizeMonitoringResponse(
          data
        ),
      [data]
    );

  const hasData =
    Boolean(
      safeData.metrics.length ||
      safeData.secondaryMetrics
        .length ||
      safeData.models.length ||
      safeData.infrastructure
        .length ||
      safeData.alerts.length ||
      safeData.administrationActions
        .length
    );

  /* --------------------------------------------------------------------------
   * SYSTEM STATUS
   * ------------------------------------------------------------------------ */

  const systemStatus =
    useMemo(() => {
      if (!data) {
        return "Unknown";
      }

      const hasDegradedModel =
        safeData.models.some(
          (model) =>
            model.status ===
            "Degraded"
        );

      const hasUnavailableInfra =
        safeData.infrastructure.some(
          (metric) =>
            metric.status ===
            "Unavailable"
        );

      if (
        hasDegradedModel ||
        hasUnavailableInfra
      ) {
        return "Attention Required";
      }

      return "AI Systems Online";
    }, [
      data,
      safeData.models,
      safeData.infrastructure,
    ]);

  /* --------------------------------------------------------------------------
   * LOADING
   * ------------------------------------------------------------------------ */

  if (loading && !data) {
    return <LoadingSkeleton />;
  }

  /* --------------------------------------------------------------------------
   * ERROR
   * ------------------------------------------------------------------------ */

  if (error && !data) {
    return (
      <main
        className="
          min-h-screen
          bg-slate-50
          p-4
          sm:p-6
          lg:p-8
        "
      >
        <div
          className="
            mx-auto
            flex
            min-h-[70vh]
            max-w-3xl
            flex-col
            items-center
            justify-center
            text-center
          "
        >
          <div
            className="
              flex
              h-16
              w-16
              items-center
              justify-center
              rounded-2xl
              bg-red-100
              text-red-600
            "
          >
            <AlertTriangle
              size={30}
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
            Unable to load AI
            monitoring
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
              void loadData()
            }
            className="
              mt-6
              inline-flex
              items-center
              gap-2
              rounded-2xl
              bg-slate-900
              px-5
              py-3
              font-bold
              text-white
              transition
              hover:bg-slate-700
              focus:outline-none
              focus:ring-2
              focus:ring-slate-500
              focus:ring-offset-2
            "
          >
            <RefreshCw
              size={17}
            />

            Retry
          </button>
        </div>
      </main>
    );
  }

  /* --------------------------------------------------------------------------
   * RENDER
   * ------------------------------------------------------------------------ */

  return (
    <main
      className="
        min-h-screen
        bg-slate-50
        p-4
        sm:p-6
        lg:p-8
      "
    >
      <div
        className="
          mx-auto
          max-w-7xl
        "
      >
        {/* ================================================================
            HEADER
        ================================================================ */}

        <header
          className="
            mb-8
            overflow-hidden
            rounded-[32px]
            bg-gradient-to-r
            from-slate-950
            via-slate-900
            to-blue-900
            p-6
            text-white
            shadow-2xl
            sm:p-8
          "
        >
          <div
            className="
              flex
              flex-col
              gap-8
              xl:flex-row
              xl:items-center
              xl:justify-between
            "
          >
            <div className="max-w-4xl">
              <div
                className="
                  mb-4
                  flex
                  flex-wrap
                  items-center
                  gap-3
                "
              >
                <div
                  className="
                    rounded-2xl
                    bg-blue-400/20
                    p-3
                  "
                >
                  <BrainCircuit
                    size={28}
                    className="text-blue-300"
                  />
                </div>

                <span
                  className="
                    inline-flex
                    items-center
                    gap-2
                    rounded-full
                    bg-emerald-500/20
                    px-4
                    py-2
                    text-sm
                    font-bold
                    text-emerald-300
                  "
                >
                  <span
                    className="
                      h-2
                      w-2
                      animate-pulse
                      rounded-full
                      bg-emerald-400
                    "
                  />

                  {systemStatus}
                </span>
              </div>

              <h1
                className="
                  text-3xl
                  font-black
                  tracking-tight
                  sm:text-4xl
                  lg:text-5xl
                "
              >
                AI Model Monitoring
                Center
              </h1>

              <p
                className="
                  mt-4
                  max-w-3xl
                  text-sm
                  leading-7
                  text-blue-100
                  sm:text-base
                "
              >
                Monitor AI model
                performance, accuracy,
                response quality,
                inference latency,
                infrastructure
                health, fraud
                detection, OCR
                processing, and AI
                operational status
                from one centralized
                control center.
              </p>
            </div>

            <div
              className="
                flex
                flex-wrap
                gap-3
              "
            >
              <button
                type="button"
                disabled={
                  actionLoading !==
                  null
                }
                onClick={() =>
                  void handleAction(
                    "DEPLOY_MODEL",
                    "Deploy model"
                  )
                }
                className="
                  inline-flex
                  items-center
                  gap-2
                  rounded-2xl
                  bg-blue-400
                  px-5
                  py-3
                  font-bold
                  text-slate-950
                  shadow-lg
                  transition
                  hover:bg-blue-300
                  disabled:cursor-not-allowed
                  disabled:opacity-50
                  focus:outline-none
                  focus:ring-2
                  focus:ring-blue-300
                  focus:ring-offset-2
                  focus:ring-offset-slate-900
                "
              >
                {actionLoading ===
                "DEPLOY_MODEL" ? (
                  <RefreshCw
                    size={17}
                    className="animate-spin"
                  />
                ) : (
                  <Cpu size={17} />
                )}

                Deploy Model
              </button>

              <button
                type="button"
                disabled={
                  actionLoading !==
                  null
                }
                onClick={() =>
                  void handleAction(
                    "AI_AUDIT",
                    "AI audit"
                  )
                }
                className="
                  inline-flex
                  items-center
                  gap-2
                  rounded-2xl
                  border
                  border-white/20
                  bg-white/10
                  px-5
                  py-3
                  font-semibold
                  backdrop-blur-md
                  transition
                  hover:bg-white/20
                  disabled:cursor-not-allowed
                  disabled:opacity-50
                  focus:outline-none
                  focus:ring-2
                  focus:ring-white/50
                "
              >
                <Activity size={17} />

                AI Audit
              </button>

              <button
                type="button"
                disabled={!hasData}
                onClick={
                  handleExport
                }
                className="
                  inline-flex
                  items-center
                  gap-2
                  rounded-2xl
                  border
                  border-white/20
                  bg-white/10
                  px-5
                  py-3
                  font-semibold
                  backdrop-blur-md
                  transition
                  hover:bg-white/20
                  disabled:cursor-not-allowed
                  disabled:opacity-50
                  focus:outline-none
                  focus:ring-2
                  focus:ring-white/50
                "
              >
                <Download
                  size={17}
                />

                Export Metrics
              </button>
            </div>
          </div>
        </header>

        {/* ================================================================
            FEEDBACK
        ================================================================ */}

        {(message || error) && (
          <div
            role={
              error
                ? "alert"
                : "status"
            }
            aria-live="polite"
            className={`
              mb-6
              flex
              items-start
              gap-3
              rounded-2xl
              border
              px-5
              py-4
              text-sm
              font-semibold
              ${
                error
                  ? "border-red-200 bg-red-50 text-red-700"
                  : "border-emerald-200 bg-emerald-50 text-emerald-700"
              }
            `}
          >
            {error ? (
              <AlertTriangle
                size={18}
                className="
                  mt-0.5
                  shrink-0
                "
              />
            ) : (
              <CheckCircle2
                size={18}
                className="
                  mt-0.5
                  shrink-0
                "
              />
            )}

            <span className="flex-1">
              {error ?? message}
            </span>

            <button
              type="button"
              onClick={() => {
                setMessage(null);
                setError(null);
              }}
              className="
                shrink-0
                rounded-lg
                p-1
                transition
                hover:bg-black/5
                focus:outline-none
                focus:ring-2
                focus:ring-current
              "
              aria-label="Dismiss notification"
            >
              <X size={16} />
            </button>
          </div>
        )}

        {/* ================================================================
            PRIMARY KPI
        ================================================================ */}

        <section
          aria-label="AI performance metrics"
          className="
            grid
            gap-5
            sm:grid-cols-2
            xl:grid-cols-4
          "
        >
          {safeData.metrics.length ===
          0 ? (
            <div
              className="
                rounded-3xl
                border
                border-slate-200
                bg-white
                p-6
                sm:col-span-2
                xl:col-span-4
              "
            >
              <EmptyState
                title="No primary metrics available"
                description="The monitoring service has not returned primary AI performance metrics."
                icon={BarChart3}
              />
            </div>
          ) : (
            safeData.metrics.map(
              (metric) => (
                <MetricCard
                  key={metric.id}
                  {...metric}
                />
              )
            )
          )}
        </section>

        {/* ================================================================
            MODEL + INFRASTRUCTURE
        ================================================================ */}

        <div
          className="
            mt-8
            grid
            gap-6
            xl:grid-cols-2
          "
        >
          <SectionCard
            title="Active AI Models"
            action={
              <button
                type="button"
                onClick={() =>
                  void loadData(true)
                }
                disabled={
                  refreshing
                }
                className="
                  inline-flex
                  items-center
                  gap-2
                  rounded-xl
                  border
                  border-slate-200
                  bg-white
                  px-3
                  py-2
                  text-sm
                  font-semibold
                  text-slate-700
                  transition
                  hover:bg-slate-50
                  disabled:cursor-not-allowed
                  disabled:opacity-50
                  focus:outline-none
                  focus:ring-2
                  focus:ring-blue-500
                  focus:ring-offset-2
                "
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
            }
          >
            {safeData.models.length ===
            0 ? (
              <EmptyState
                title="No AI models available"
                description="The monitoring service has not returned any active models."
                icon={Bot}
              />
            ) : (
              <div className="space-y-4">
                {safeData.models.map(
                  (model) => (
                    <div
                      key={model.id}
                      className="
                        rounded-2xl
                        border
                        border-slate-200
                        bg-white
                        p-4
                        transition
                        hover:border-blue-200
                        hover:bg-blue-50/30
                      "
                    >
                      <div
                        className="
                          flex
                          flex-col
                          gap-4
                          sm:flex-row
                          sm:items-center
                          sm:justify-between
                        "
                      >
                        <div className="min-w-0">
                          <div
                            className="
                              flex
                              flex-wrap
                              items-center
                              gap-2
                            "
                          >
                            <h3
                              className="
                                font-bold
                                text-slate-900
                              "
                            >
                              {
                                model.name
                              }
                            </h3>

                            <span
                              className="
                                rounded-md
                                bg-slate-100
                                px-2
                                py-1
                                text-[10px]
                                font-bold
                                text-slate-500
                              "
                            >
                              v
                              {
                                model.version
                              }
                            </span>
                          </div>

                          <div
                            className="
                              mt-2
                              flex
                              flex-wrap
                              gap-x-4
                              gap-y-1
                              text-sm
                              text-slate-500
                            "
                          >
                            <span>
                              Accuracy:{" "}
                              <strong className="text-slate-700">
                                {formatPercentage(
                                  model.accuracy
                                )}
                              </strong>
                            </span>

                            <span>
                              Latency:{" "}
                              <strong className="text-slate-700">
                                {formatLatency(
                                  model.latency
                                )}
                              </strong>
                            </span>

                            <span>
                              Requests:{" "}
                              <strong className="text-slate-700">
                                {formatNumber(
                                  model.requests
                                )}
                              </strong>
                            </span>
                          </div>
                        </div>

                        <StatusBadge
                          status={
                            model.status
                          }
                        />
                      </div>
                    </div>
                  )
                )}
              </div>
            )}
          </SectionCard>

          <SectionCard title="AI Infrastructure Health">
            {safeData.infrastructure
              .length === 0 ? (
              <EmptyState
                title="No infrastructure data"
                description="Infrastructure monitoring data is currently unavailable."
                icon={Server}
              />
            ) : (
              <div className="space-y-3">
                {safeData.infrastructure.map(
                  (metric) => (
                    <InfrastructureRow
                      key={metric.id}
                      metric={metric}
                    />
                  )
                )}
              </div>
            )}
          </SectionCard>
        </div>

        {/* ================================================================
            SECONDARY ANALYTICS
        ================================================================ */}

        {safeData.secondaryMetrics
          .length > 0 && (
          <section
            aria-label="AI analytics"
            className="
              mt-8
              grid
              gap-5
              sm:grid-cols-2
              xl:grid-cols-4
            "
          >
            {safeData.secondaryMetrics.map(
              (metric) => (
                <MetricCard
                  key={metric.id}
                  {...metric}
                />
              )
            )}
          </section>
        )}

        {/* ================================================================
            ALERTS
        ================================================================ */}

        <div className="mt-8">
          <SectionCard
            title="AI Monitoring Alerts"
            action={
              <span
                className="
                  rounded-full
                  bg-slate-100
                  px-3
                  py-1.5
                  text-xs
                  font-bold
                  text-slate-600
                "
              >
                {safeData.alerts.length}{" "}
                Active
              </span>
            }
          >
            {safeData.alerts.length ===
            0 ? (
              <EmptyState
                title="No active alerts"
                description="All monitored AI services are currently within their configured thresholds."
                icon={CheckCircle2}
              />
            ) : (
              <div className="space-y-4">
                {safeData.alerts.map(
                  (alert) => {
                    const Icon =
                      ALERT_ICONS[
                        alert.type
                      ] ??
                      AlertTriangle;

                    return (
                      <div
                        key={alert.id}
                        className={`
                          flex
                          items-start
                          gap-4
                          rounded-2xl
                          border
                          p-4
                          ${
                            ALERT_STYLES[
                              alert.type
                            ] ??
                            ALERT_STYLES.warning
                          }
                        `}
                      >
                        <Icon
                          size={20}
                          className="
                            mt-0.5
                            shrink-0
                          "
                          aria-hidden="true"
                        />

                        <div className="min-w-0">
                          <p className="font-bold">
                            {
                              alert.title
                            }
                          </p>

                          <p
                            className="
                              mt-1
                              text-sm
                              opacity-90
                            "
                          >
                            {
                              alert.description
                            }
                          </p>

                          {alert.timestamp && (
                            <p
                              className="
                                mt-2
                                text-xs
                                opacity-70
                              "
                            >
                              {formatDate(
                                alert.timestamp
                              )}
                            </p>
                          )}
                        </div>
                      </div>
                    );
                  }
                )}
              </div>
            )}
          </SectionCard>
        </div>

        {/* ================================================================
            ADMINISTRATION ACTIONS
        ================================================================ */}

        {safeData
          .administrationActions
          .length > 0 && (
          <div className="mt-8">
            <SectionCard title="AI Administration Actions">
              <div
                className="
                  grid
                  gap-4
                  sm:grid-cols-2
                  xl:grid-cols-4
                "
              >
                {safeData.administrationActions.map(
                  (item) => {
                    const Icon =
                      ICONS[item.icon] ??
                      Activity;

                    const isLoading =
                      actionLoading ===
                      item.action;

                    return (
                      <button
                        key={item.id}
                        type="button"
                        disabled={
                          actionLoading !==
                            null ||
                          !item.action.trim()
                        }
                        onClick={() =>
                          void handleAction(
                            item.action,
                            item.label
                          )
                        }
                        className="
                          group
                          rounded-2xl
                          border
                          border-slate-200
                          bg-white
                          p-5
                          text-left
                          transition
                          duration-200
                          hover:-translate-y-0.5
                          hover:border-blue-300
                          hover:shadow-lg
                          disabled:cursor-not-allowed
                          disabled:opacity-60
                          focus:outline-none
                          focus:ring-2
                          focus:ring-blue-500
                          focus:ring-offset-2
                        "
                      >
                        <div
                          className="
                            mb-4
                            flex
                            h-11
                            w-11
                            items-center
                            justify-center
                            rounded-xl
                            bg-blue-50
                            text-blue-600
                            transition
                            group-hover:bg-blue-600
                            group-hover:text-white
                          "
                        >
                          {isLoading ? (
                            <RefreshCw
                              size={22}
                              className="animate-spin"
                            />
                          ) : (
                            <Icon
                              size={22}
                            />
                          )}
                        </div>

                        <p
                          className="
                            font-bold
                            text-slate-900
                          "
                        >
                          {item.label}
                        </p>

                        <p
                          className="
                            mt-2
                            text-sm
                            leading-6
                            text-slate-500
                          "
                        >
                          {
                            item.description
                          }
                        </p>
                      </button>
                    );
                  }
                )}
              </div>
            </SectionCard>
          </div>
        )}

        {/* ================================================================
            FOOTER
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
            text-sm
            text-slate-500
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
            <span
              className={`
                h-2
                w-2
                rounded-full
                ${
                  systemStatus ===
                  "AI Systems Online"
                    ? "bg-emerald-500"
                    : systemStatus ===
                      "Unknown"
                    ? "bg-slate-400"
                    : "bg-amber-500"
                }
              `}
            />

            <span>
              {systemStatus}
            </span>
          </div>

          <span>
            Last dashboard refresh:{" "}
            {formatDate(
              safeData.lastUpdated
            )}
          </span>
        </footer>
      </div>
    </main>
  );
}

