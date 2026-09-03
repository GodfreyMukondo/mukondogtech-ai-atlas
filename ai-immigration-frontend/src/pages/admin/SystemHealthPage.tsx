import React, {
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";

import axios from "axios";

import {
  Activity,
  AlertTriangle,
  CheckCircle2,
  Cloud,
  Cpu,
  Database,
  Gauge,
  HardDrive,
  MemoryStick,
  Network,
  RefreshCcw,
  Server,
  ShieldCheck,
  Wifi,
  XCircle,
  Zap,
} from "lucide-react";

/* ============================================================
   TYPES
   ============================================================ */

type HealthStatus =
  | "HEALTHY"
  | "OPERATIONAL"
  | "ONLINE"
  | "DEGRADED"
  | "WARNING"
  | "DOWN"
  | "OFFLINE"
  | "UNKNOWN";

type HealthCardColor =
  | "blue"
  | "green"
  | "yellow"
  | "purple"
  | "red"
  | "orange";

type HealthCardProps = {
  title: string;
  value: string;
  status: string;
  icon: React.ElementType;
  color: HealthCardColor;
};

type HealthMetric = {
  key: string;
  label: string;
  value: number;
  unit?: string;
  status: HealthStatus;
};

type ServiceHealth = {
  id: string;
  name: string;
  status: HealthStatus;
  description?: string;
  responseTimeMs?: number;
  uptimePercentage?: number;
};

type DatabaseMetric = {
  key: string;
  label: string;
  value: string;
  status: HealthStatus;
};

type SystemIncident = {
  id: string;
  severity: "INFO" | "WARNING" | "CRITICAL";
  message: string;
  createdAt: string;
  resolved: boolean;
};

type SystemAction = {
  id: string;
  label: string;
  action: string;
  enabled: boolean;
  requiresConfirmation: boolean;
};

type SystemHealthDashboard = {
  healthScore: number;
  healthStatus: HealthStatus;
  healthMessage: string;

  uptimePercentage: number;

  activeServices: number;
  totalServices: number;

  services: ServiceHealth[];

  serverResources: HealthMetric[];

  databaseMetrics: DatabaseMetric[];

  incidents: SystemIncident[];

  administrationActions: SystemAction[];

  lastCheckedAt: string;

  environment?: string;

  version?: string;
};

type ApiErrorResponse = {
  message?: string;
  error?: string;
};

/* ============================================================
   CONSTANTS
   ============================================================ */

const API_BASE_URL =
  import.meta.env.VITE_API_URL?.replace(/\/$/, "") ||
  "http://localhost:8080/api";

const DASHBOARD_ENDPOINT =
  `${API_BASE_URL}/admin/system-health/dashboard`;

const DIAGNOSTICS_ENDPOINT =
  `${API_BASE_URL}/admin/system-health/diagnostics`;

/* ============================================================
   HELPERS
   ============================================================ */

function getAuthToken(): string | null {
  const tokenKeys = [
    "token",
    "accessToken",
    "authToken",
    "jwt",
  ];

  for (const key of tokenKeys) {
    const token = localStorage.getItem(key);

    if (token) {
      return token;
    }
  }

  return null;
}

function getAuthHeaders() {
  const token = getAuthToken();

  return token
    ? {
        Authorization: `Bearer ${token}`,
      }
    : {};
}

function formatNumber(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return "—";
  }

  return new Intl.NumberFormat().format(value);
}

function formatPercentage(
  value: number | null | undefined,
  decimals = 2,
): string {
  if (
    value === null ||
    value === undefined ||
    Number.isNaN(value)
  ) {
    return "—";
  }

  return `${value.toFixed(decimals)}%`;
}

function formatDateTime(
  value: string | null | undefined,
): string {
  if (!value) {
    return "Never";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "Unknown";
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

function getApiErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    return (
      error.response?.data?.message ||
      error.response?.data?.error ||
      error.message ||
      "Unable to communicate with the server."
    );
  }

  if (error instanceof Error) {
    return error.message;
  }

  return "An unexpected error occurred.";
}

function normalizeHealthStatus(
  status: HealthStatus | undefined,
): HealthStatus {
  return status || "UNKNOWN";
}

/* ============================================================
   STATUS HELPERS
   ============================================================ */

function getStatusColor(status: HealthStatus): string {
  switch (normalizeHealthStatus(status)) {
    case "HEALTHY":
    case "OPERATIONAL":
    case "ONLINE":
      return "text-green-600";

    case "WARNING":
    case "DEGRADED":
      return "text-yellow-600";

    case "DOWN":
    case "OFFLINE":
      return "text-red-600";

    default:
      return "text-slate-500";
  }
}

function getStatusBackground(status: HealthStatus): string {
  switch (normalizeHealthStatus(status)) {
    case "HEALTHY":
    case "OPERATIONAL":
    case "ONLINE":
      return "bg-green-100 text-green-700";

    case "WARNING":
    case "DEGRADED":
      return "bg-yellow-100 text-yellow-700";

    case "DOWN":
    case "OFFLINE":
      return "bg-red-100 text-red-700";

    default:
      return "bg-slate-100 text-slate-600";
  }
}

function getStatusLabel(status: HealthStatus): string {
  switch (normalizeHealthStatus(status)) {
    case "HEALTHY":
      return "Healthy";

    case "OPERATIONAL":
      return "Operational";

    case "ONLINE":
      return "Online";

    case "DEGRADED":
      return "Degraded";

    case "WARNING":
      return "Warning";

    case "DOWN":
      return "Down";

    case "OFFLINE":
      return "Offline";

    default:
      return "Unknown";
  }
}

function getHealthCardColor(
  status: HealthStatus,
): HealthCardColor {
  switch (normalizeHealthStatus(status)) {
    case "HEALTHY":
    case "OPERATIONAL":
    case "ONLINE":
      return "green";

    case "WARNING":
    case "DEGRADED":
      return "yellow";

    case "DOWN":
    case "OFFLINE":
      return "red";

    default:
      return "blue";
  }
}

/* ============================================================
   ICON HELPERS
   ============================================================ */

function getServiceIcon(name: string): React.ElementType {
  const normalized = name.toLowerCase();

  if (normalized.includes("database")) {
    return Database;
  }

  if (
    normalized.includes("ai") ||
    normalized.includes("model") ||
    normalized.includes("ocr") ||
    normalized.includes("fraud")
  ) {
    return Cpu;
  }

  if (
    normalized.includes("storage") ||
    normalized.includes("cloud")
  ) {
    return Cloud;
  }

  if (
    normalized.includes("network") ||
    normalized.includes("api") ||
    normalized.includes("gateway")
  ) {
    return Network;
  }

  return Server;
}

function getResourceIcon(key: string): React.ElementType {
  const normalized = key.toLowerCase();

  if (normalized.includes("cpu")) {
    return Gauge;
  }

  if (
    normalized.includes("memory") ||
    normalized.includes("ram")
  ) {
    return MemoryStick;
  }

  if (
    normalized.includes("disk") ||
    normalized.includes("storage")
  ) {
    return HardDrive;
  }

  if (
    normalized.includes("network") ||
    normalized.includes("traffic")
  ) {
    return Wifi;
  }

  return Activity;
}

/* ============================================================
   HEALTH CARD
   ============================================================ */

function HealthCard({
  title,
  value,
  status,
  icon: Icon,
  color,
}: HealthCardProps) {
  const colorClasses: Record<HealthCardColor, string> = {
    blue: "bg-blue-100 text-blue-700",
    green: "bg-green-100 text-green-700",
    yellow: "bg-yellow-100 text-yellow-700",
    purple: "bg-purple-100 text-purple-700",
    red: "bg-red-100 text-red-700",
    orange: "bg-orange-100 text-orange-700",
  };

  return (
    <div
      className="
        rounded-3xl
        border
        border-slate-200
        bg-white
        p-6
        shadow-sm
        transition
        hover:-translate-y-0.5
        hover:shadow-xl
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
              text-slate-500
            "
          >
            {title}
          </p>

          <h2
            className="
              mt-2
              break-words
              text-3xl
              font-black
              text-[#071330]
            "
          >
            {value}
          </h2>

          <p
            className={`
              mt-2
              text-sm
              font-semibold
              ${getStatusColor(
                status as HealthStatus,
              )}
            `}
          >
            {status}
          </p>
        </div>

        <div
          className={`
            shrink-0
            rounded-2xl
            p-3
            ${colorClasses[color]}
          `}
        >
          <Icon size={25} />
        </div>
      </div>
    </div>
  );
}

/* ============================================================
   PANEL
   ============================================================ */

function Panel({
  title,
  icon: Icon,
  children,
}: {
  title: string;
  icon: React.ElementType;
  children: React.ReactNode;
}) {
  return (
    <section
      className="
        rounded-3xl
        border
        border-slate-200
        bg-white
        p-6
        shadow-sm
      "
    >
      <div
        className="
          mb-6
          flex
          items-center
          gap-3
        "
      >
        <div
          className="
            rounded-xl
            bg-[#071330]
            p-2
            text-[#F4B81A]
          "
        >
          <Icon size={20} />
        </div>

        <h2
          className="
            text-lg
            font-black
            text-[#071330]
          "
        >
          {title}
        </h2>
      </div>

      {children}
    </section>
  );
}

/* ============================================================
   LOADING SKELETON
   ============================================================ */

function LoadingSkeleton() {
  return (
    <div className="animate-pulse space-y-8">
      <div className="h-32 rounded-3xl bg-slate-200" />

      <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-4">
        {Array.from({ length: 4 }).map((_, index) => (
          <div
            key={index}
            className="h-36 rounded-3xl bg-slate-200"
          />
        ))}
      </div>

      <div className="grid gap-6 xl:grid-cols-3">
        {Array.from({ length: 3 }).map((_, index) => (
          <div
            key={index}
            className="h-80 rounded-3xl bg-slate-200"
          />
        ))}
      </div>
    </div>
  );
}

/* ============================================================
   EMPTY STATE
   ============================================================ */

function EmptyState({
  message,
}: {
  message: string;
}) {
  return (
    <div
      className="
        rounded-2xl
        border
        border-dashed
        border-slate-300
        bg-slate-50
        p-8
        text-center
        text-sm
        text-slate-500
      "
    >
      {message}
    </div>
  );
}

/* ============================================================
   INCIDENT
   ============================================================ */

function IncidentItem({
  incident,
}: {
  incident: SystemIncident;
}) {
  const severityStyles = {
    INFO: "bg-blue-50 text-blue-700",
    WARNING: "bg-yellow-50 text-yellow-700",
    CRITICAL: "bg-red-50 text-red-700",
  };

  const Icon =
    incident.severity === "CRITICAL"
      ? AlertTriangle
      : incident.severity === "WARNING"
        ? AlertTriangle
        : Activity;

  return (
    <div
      className={`
        flex
        items-start
        gap-3
        rounded-2xl
        p-4
        ${severityStyles[incident.severity]}
      `}
    >
      <Icon className="mt-0.5 shrink-0" size={20} />

      <div className="min-w-0 flex-1">
        <p className="font-semibold">
          {incident.message}
        </p>

        <p className="mt-1 text-xs opacity-75">
          {formatDateTime(incident.createdAt)}
          {incident.resolved
            ? " • Resolved"
            : " • Active"}
        </p>
      </div>
    </div>
  );
}

/* ============================================================
   ADMIN ACTION BUTTON
   ============================================================ */

function AdministrationAction({
  action,
  loading,
  onExecute,
}: {
  action: SystemAction;
  loading: boolean;
  onExecute: (action: SystemAction) => void;
}) {
  return (
    <button
      type="button"
      disabled={!action.enabled || loading}
      onClick={() => onExecute(action)}
      className="
        rounded-2xl
        border
        border-slate-200
        bg-white
        p-4
        text-left
        font-semibold
        transition
        hover:border-[#F4B81A]
        hover:bg-[#FFF4D1]
        disabled:cursor-not-allowed
        disabled:opacity-50
      "
    >
      <div className="flex items-center justify-between gap-3">
        <span>{action.label}</span>

        {loading && (
          <RefreshCcw
            size={16}
            className="animate-spin"
          />
        )}
      </div>
    </button>
  );
}

/* ============================================================
   MAIN PAGE
   ============================================================ */

export default function SystemHealthPage() {
  const [dashboard, setDashboard] =
    useState<SystemHealthDashboard | null>(null);

  const [loading, setLoading] =
    useState<boolean>(true);

  const [refreshing, setRefreshing] =
    useState<boolean>(false);

  const [diagnosticsRunning, setDiagnosticsRunning] =
    useState<boolean>(false);

  const [actionLoading, setActionLoading] =
    useState<string | null>(null);

  const [error, setError] =
    useState<string | null>(null);

  /* ==========================================================
     FETCH DASHBOARD
     ========================================================== */

  const fetchDashboard = useCallback(
    async (isRefresh = false) => {
      try {
        setError(null);

        if (isRefresh) {
          setRefreshing(true);
        } else {
          setLoading(true);
        }

        const response =
          await axios.get<SystemHealthDashboard>(
            DASHBOARD_ENDPOINT,
            {
              headers: {
                ...getAuthHeaders(),
              },
              timeout: 30000,
            },
          );

        setDashboard(response.data);
      } catch (err) {
        console.error(
          "[SystemHealthPage] Failed to load dashboard:",
          err,
        );

        setError(getApiErrorMessage(err));
      } finally {
        setLoading(false);
        setRefreshing(false);
      }
    },
    [],
  );

  /* ==========================================================
     INITIAL LOAD
     ========================================================== */

  useEffect(() => {
    void fetchDashboard();
  }, [fetchDashboard]);

  /* ==========================================================
     RUN DIAGNOSTICS
     ========================================================== */

  const runDiagnostics = useCallback(async () => {
    try {
      setDiagnosticsRunning(true);
      setError(null);

      await axios.post(
        DIAGNOSTICS_ENDPOINT,
        {},
        {
          headers: {
            ...getAuthHeaders(),
          },
          timeout: 120000,
        },
      );

      await fetchDashboard(true);
    } catch (err) {
      console.error(
        "[SystemHealthPage] Diagnostics failed:",
        err,
      );

      setError(getApiErrorMessage(err));
    } finally {
      setDiagnosticsRunning(false);
    }
  }, [fetchDashboard]);

  /* ==========================================================
     ADMIN ACTION
     ========================================================== */

  const executeAdministrationAction = useCallback(
    async (action: SystemAction) => {
      if (!action.enabled) {
        return;
      }

      if (action.requiresConfirmation) {
        const confirmed = window.confirm(
          `Are you sure you want to execute "${action.label}"?`,
        );

        if (!confirmed) {
          return;
        }
      }

      try {
        setActionLoading(action.id);
        setError(null);

        await axios.post(
          `${API_BASE_URL}/admin/system-health/actions/${encodeURIComponent(
            action.action,
          )}`,
          {},
          {
            headers: {
              ...getAuthHeaders(),
            },
            timeout: 120000,
          },
        );

        await fetchDashboard(true);
      } catch (err) {
        console.error(
          `[SystemHealthPage] Action failed: ${action.action}`,
          err,
        );

        setError(getApiErrorMessage(err));
      } finally {
        setActionLoading(null);
      }
    },
    [fetchDashboard],
  );

  /* ==========================================================
     HEALTH CARD DATA
     ========================================================== */

  const healthCards = useMemo(() => {
    if (!dashboard) {
      return [];
    }

    return dashboard.services.slice(0, 4).map((service) => {
      const Icon = getServiceIcon(service.name);

      const value =
        service.uptimePercentage !== undefined
          ? formatPercentage(
              service.uptimePercentage,
            )
          : getStatusLabel(service.status);

      return {
        title: service.name,
        value,
        status:
          service.description ||
          getStatusLabel(service.status),
        icon: Icon,
        color: getHealthCardColor(
          service.status,
        ),
      };
    });
  }, [dashboard]);

  /* ==========================================================
     RESOURCE BAR WIDTH
     ========================================================== */

  const getResourcePercentage = (
    metric: HealthMetric,
  ): number => {
    const value = Number(metric.value);

    if (!Number.isFinite(value)) {
      return 0;
    }

    return Math.min(100, Math.max(0, value));
  };

  /* ==========================================================
     RENDER
     ========================================================== */

  return (
    <div
      className="
        min-h-screen
        bg-gradient-to-br
        from-[#F8F6F1]
        via-white
        to-blue-50
      "
    >
      <div
        className="
          mx-auto
          max-w-[1700px]
          p-6
        "
      >
        {/* ==================================================
            HEADER
        ================================================== */}

        <div
          className="
            mb-8
            flex
            flex-col
            gap-5
            lg:flex-row
            lg:items-center
            lg:justify-between
          "
        >
          <div>
            <div className="flex items-center gap-3">
              <h1
                className="
                  text-4xl
                  font-black
                  text-[#071330]
                "
              >
                System Health Center
              </h1>

              {dashboard?.environment && (
                <span
                  className="
                    rounded-full
                    bg-slate-100
                    px-3
                    py-1
                    text-xs
                    font-bold
                    uppercase
                    tracking-wide
                    text-slate-600
                  "
                >
                  {dashboard.environment}
                </span>
              )}
            </div>

            <p
              className="
                mt-3
                max-w-3xl
                text-slate-600
              "
            >
              Monitor infrastructure, AI services,
              database performance, cloud resources,
              and platform reliability.
            </p>

            {dashboard?.lastCheckedAt && (
              <p
                className="
                  mt-2
                  text-xs
                  text-slate-400
                "
              >
                Last checked:{" "}
                {formatDateTime(
                  dashboard.lastCheckedAt,
                )}
              </p>
            )}
          </div>

          <div className="flex flex-wrap gap-3">
            <button
              type="button"
              onClick={() =>
                void fetchDashboard(true)
              }
              disabled={
                loading ||
                refreshing ||
                diagnosticsRunning
              }
              className="
                flex
                items-center
                gap-2
                rounded-2xl
                border
                border-slate-200
                bg-white
                px-5
                py-3
                font-bold
                text-[#071330]
                shadow-sm
                transition
                hover:border-[#F4B81A]
                hover:bg-[#FFF4D1]
                disabled:cursor-not-allowed
                disabled:opacity-50
              "
            >
              <RefreshCcw
                size={18}
                className={
                  refreshing
                    ? "animate-spin"
                    : ""
                }
              />

              Refresh
            </button>

            <button
              type="button"
              onClick={() =>
                void runDiagnostics()
              }
              disabled={
                loading ||
                refreshing ||
                diagnosticsRunning
              }
              className="
                flex
                items-center
                gap-2
                rounded-2xl
                bg-[#071330]
                px-6
                py-3
                font-bold
                text-white
                shadow-sm
                transition
                hover:bg-[#183B6B]
                disabled:cursor-not-allowed
                disabled:opacity-50
              "
            >
              <RefreshCcw
                size={18}
                className={
                  diagnosticsRunning
                    ? "animate-spin"
                    : ""
                }
              />

              {diagnosticsRunning
                ? "Running Diagnostics..."
                : "Run Diagnostics"}
            </button>
          </div>
        </div>

        {/* ==================================================
            ERROR
        ================================================== */}

        {error && (
          <div
            className="
              mb-8
              flex
              items-start
              gap-3
              rounded-2xl
              border
              border-red-200
              bg-red-50
              p-4
              text-red-700
            "
          >
            <XCircle
              size={21}
              className="mt-0.5 shrink-0"
            />

            <div className="flex-1">
              <p className="font-bold">
                Unable to load system health
              </p>

              <p className="mt-1 text-sm">
                {error}
              </p>
            </div>

            <button
              type="button"
              onClick={() =>
                void fetchDashboard(true)
              }
              className="
                rounded-xl
                bg-white
                px-4
                py-2
                text-sm
                font-bold
                text-red-700
                shadow-sm
                hover:bg-red-100
              "
            >
              Retry
            </button>
          </div>
        )}

        {/* ==================================================
            LOADING
        ================================================== */}

        {loading && !dashboard ? (
          <LoadingSkeleton />
        ) : dashboard ? (
          <>
            {/* ==============================================
                HEALTH SCORE
            ============================================== */}

            <div
              className="
                rounded-3xl
                bg-gradient-to-r
                from-[#071330]
                via-[#183B6B]
                to-[#0B1736]
                p-8
                text-white
                shadow-xl
              "
            >
              <div
                className="
                  flex
                  flex-col
                  gap-6
                  lg:flex-row
                  lg:items-center
                  lg:justify-between
                "
              >
                <div>
                  <p
                    className="
                      text-sm
                      text-blue-200
                    "
                  >
                    Platform Health Score
                  </p>

                  <h2
                    className="
                      text-6xl
                      font-black
                      text-[#F4B81A]
                    "
                  >
                    {formatPercentage(
                      dashboard.healthScore,
                      2,
                    )}
                  </h2>

                  <div className="mt-3 flex items-center gap-2">
                    {dashboard.healthStatus ===
                      "HEALTHY" ||
                    dashboard.healthStatus ===
                      "OPERATIONAL" ||
                    dashboard.healthStatus ===
                      "ONLINE" ? (
                      <CheckCircle2
                        size={18}
                        className="text-green-400"
                      />
                    ) : (
                      <AlertTriangle
                        size={18}
                        className="text-yellow-400"
                      />
                    )}

                    <p className="text-blue-100">
                      {dashboard.healthMessage}
                    </p>
                  </div>
                </div>

                <div
                  className="
                    grid
                    grid-cols-2
                    gap-4
                  "
                >
                  <div
                    className="
                      rounded-2xl
                      bg-white/10
                      p-5
                    "
                  >
                    <p
                      className="
                        text-xs
                        text-blue-200
                      "
                    >
                      Uptime
                    </p>

                    <h3
                      className="
                        text-2xl
                        font-black
                      "
                    >
                      {formatPercentage(
                        dashboard.uptimePercentage,
                        2,
                      )}
                    </h3>
                  </div>

                  <div
                    className="
                      rounded-2xl
                      bg-white/10
                      p-5
                    "
                  >
                    <p
                      className="
                        text-xs
                        text-blue-200
                      "
                    >
                      Active Services
                    </p>

                    <h3
                      className="
                        text-2xl
                        font-black
                      "
                    >
                      {formatNumber(
                        dashboard.activeServices,
                      )}
                      /
                      {formatNumber(
                        dashboard.totalServices,
                      )}
                    </h3>
                  </div>
                </div>
              </div>
            </div>

            {/* ==============================================
                MAIN HEALTH CARDS
            ============================================== */}

            <div
              className="
                mt-8
                grid
                gap-5
                md:grid-cols-2
                xl:grid-cols-4
              "
            >
              {healthCards.length > 0 ? (
                healthCards.map((card) => (
                  <HealthCard
                    key={card.title}
                    title={card.title}
                    value={card.value}
                    status={card.status}
                    icon={card.icon}
                    color={card.color}
                  />
                ))
              ) : (
                <div className="md:col-span-2 xl:col-span-4">
                  <EmptyState message="No service health information is currently available." />
                </div>
              )}
            </div>

            {/* ==============================================
                MAIN PANELS
            ============================================== */}

            <div
              className="
                mt-8
                grid
                gap-6
                xl:grid-cols-3
              "
            >
              {/* SERVER RESOURCES */}

              <Panel
                title="Server Resources"
                icon={Server}
              >
                {dashboard.serverResources.length >
                0 ? (
                  <div className="space-y-4">
                    {dashboard.serverResources.map(
                      (metric) => {
                        const Icon =
                          getResourceIcon(
                            metric.key,
                          );

                        const percentage =
                          getResourcePercentage(
                            metric,
                          );

                        return (
                          <div
                            key={metric.key}
                            className="
                              rounded-xl
                              bg-slate-50
                              p-4
                            "
                          >
                            <div
                              className="
                                flex
                                items-center
                                justify-between
                                gap-3
                              "
                            >
                              <div className="flex items-center gap-2">
                                <Icon
                                  size={17}
                                  className="text-slate-500"
                                />

                                <span>
                                  {metric.label}
                                </span>
                              </div>

                              <span
                                className="
                                  font-bold
                                  text-[#071330]
                                "
                              >
                                {metric.value}
                                {metric.unit || ""}
                              </span>
                            </div>

                            <div
                              className="
                                mt-3
                                h-2
                                overflow-hidden
                                rounded-full
                                bg-slate-200
                              "
                            >
                              <div
                                className="
                                  h-2
                                  rounded-full
                                  bg-[#F4B81A]
                                  transition-all
                                "
                                style={{
                                  width: `${percentage}%`,
                                }}
                              />
                            </div>

                            <p
                              className={`
                                mt-2
                                text-xs
                                font-semibold
                                ${getStatusColor(
                                  metric.status,
                                )}
                              `}
                            >
                              {getStatusLabel(
                                metric.status,
                              )}
                            </p>
                          </div>
                        );
                      },
                    )}
                  </div>
                ) : (
                  <EmptyState message="No server resource metrics are available." />
                )}
              </Panel>

              {/* AI / SERVICES */}

              <Panel
                title="Platform Services"
                icon={Cpu}
              >
                {dashboard.services.length > 0 ? (
                  <div className="space-y-4">
                    {dashboard.services.map(
                      (service) => {
                        const Icon =
                          getServiceIcon(
                            service.name,
                          );

                        return (
                          <div
                            key={service.id}
                            className="
                              rounded-xl
                              bg-slate-50
                              p-4
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
                              <div className="flex min-w-0 items-center gap-3">
                                <div
                                  className="
                                    rounded-xl
                                    bg-white
                                    p-2
                                    shadow-sm
                                  "
                                >
                                  <Icon
                                    size={18}
                                    className="text-[#071330]"
                                  />
                                </div>

                                <div className="min-w-0">
                                  <p className="truncate font-semibold">
                                    {service.name}
                                  </p>

                                  {service.responseTimeMs !==
                                    undefined && (
                                    <p className="text-xs text-slate-500">
                                      {formatNumber(
                                        service.responseTimeMs,
                                      )}
                                      ms response
                                    </p>
                                  )}
                                </div>
                              </div>

                              <span
                                className={`
                                  shrink-0
                                  rounded-full
                                  px-3
                                  py-1
                                  text-xs
                                  font-bold
                                  ${getStatusBackground(
                                    service.status,
                                  )}
                                `}
                              >
                                {getStatusLabel(
                                  service.status,
                                )}
                              </span>
                            </div>

                            {service.description && (
                              <p className="mt-3 text-xs text-slate-500">
                                {service.description}
                              </p>
                            )}
                          </div>
                        );
                      },
                    )}
                  </div>
                ) : (
                  <EmptyState message="No platform services are currently available." />
                )}
              </Panel>

              {/* DATABASE */}

              <Panel
                title="Database Monitoring"
                icon={Database}
              >
                {dashboard.databaseMetrics.length >
                0 ? (
                  <div className="space-y-4">
                    {dashboard.databaseMetrics.map(
                      (metric) => (
                        <div
                          key={metric.key}
                          className="
                            flex
                            items-center
                            justify-between
                            gap-4
                            rounded-xl
                            bg-slate-50
                            p-4
                          "
                        >
                          <span className="text-slate-600">
                            {metric.label}
                          </span>

                          <div className="text-right">
                            <p
                              className={`
                                font-bold
                                ${getStatusColor(
                                  metric.status,
                                )}
                              `}
                            >
                              {metric.value}
                            </p>

                            <p className="text-xs text-slate-400">
                              {getStatusLabel(
                                metric.status,
                              )}
                            </p>
                          </div>
                        </div>
                      ),
                    )}
                  </div>
                ) : (
                  <EmptyState message="No database monitoring information is currently available." />
                )}
              </Panel>
            </div>

            {/* ==============================================
                INCIDENTS
            ============================================== */}

            <div className="mt-8">
              <Panel
                title="System Incidents"
                icon={AlertTriangle}
              >
                {dashboard.incidents.length > 0 ? (
                  <div className="space-y-4">
                    {dashboard.incidents.map(
                      (incident) => (
                        <IncidentItem
                          key={incident.id}
                          incident={incident}
                        />
                      ),
                    )}
                  </div>
                ) : (
                  <div
                    className="
                      flex
                      items-center
                      gap-3
                      rounded-2xl
                      bg-green-50
                      p-4
                      text-green-700
                    "
                  >
                    <CheckCircle2 size={21} />

                    <span>
                      No active system incidents.
                    </span>
                  </div>
                )}
              </Panel>
            </div>

            {/* ==============================================
                ADMINISTRATION
            ============================================== */}

            <div className="mt-8">
              <Panel
                title="System Administration"
                icon={ShieldCheck}
              >
                {dashboard.administrationActions
                  .length > 0 ? (
                  <div
                    className="
                      grid
                      gap-4
                      md:grid-cols-2
                      xl:grid-cols-4
                    "
                  >
                    {dashboard.administrationActions.map(
                      (action) => (
                        <AdministrationAction
                          key={action.id}
                          action={action}
                          loading={
                            actionLoading ===
                            action.id
                          }
                          onExecute={
                            executeAdministrationAction
                          }
                        />
                      ),
                    )}
                  </div>
                ) : (
                  <EmptyState message="No system administration actions are currently available." />
                )}
              </Panel>
            </div>

            {/* ==============================================
                FOOTER INFORMATION
            ============================================== */}

            <div
              className="
                mt-8
                flex
                flex-col
                gap-2
                rounded-2xl
                border
                border-slate-200
                bg-white
                p-4
                text-xs
                text-slate-500
                sm:flex-row
                sm:items-center
                sm:justify-between
              "
            >
              <div className="flex items-center gap-2">
                <Activity size={15} />

                <span>
                  Health data is retrieved from the
                  platform monitoring service.
                </span>
              </div>

              {dashboard.version && (
                <span>
                  Platform version:{" "}
                  <strong className="text-slate-700">
                    {dashboard.version}
                  </strong>
                </span>
              )}
            </div>
          </>
        ) : (
          <div
            className="
              rounded-3xl
              border
              border-slate-200
              bg-white
              p-12
              text-center
              shadow-sm
            "
          >
            <Server
              size={42}
              className="mx-auto text-slate-400"
            />

            <h2
              className="
                mt-4
                text-xl
                font-black
                text-[#071330]
              "
            >
              System health unavailable
            </h2>

            <p className="mt-2 text-slate-500">
              No health information was returned by
              the backend.
            </p>

            <button
              type="button"
              onClick={() =>
                void fetchDashboard(true)
              }
              className="
                mt-6
                rounded-2xl
                bg-[#071330]
                px-6
                py-3
                font-bold
                text-white
                hover:bg-[#183B6B]
              "
            >
              Try Again
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

