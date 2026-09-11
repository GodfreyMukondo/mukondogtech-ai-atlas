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
  ChevronDown,
  Clock3,
  Download,
  Eye,
  FileText,
  Filter,
  Globe2,
  LockKeyhole,
  RefreshCw,
  Search,
  Server,
  ShieldAlert,
  ShieldCheck,
  UserCheck,
  Users,
  X,
  Zap,
} from "lucide-react";

/* ============================================================
   CONFIGURATION
============================================================ */

const API_BASE_URL =
  import.meta.env.VITE_API_URL?.replace(/\/+$/, "") ||
  "http://localhost:8080/api";

const AUDIT_LOGS_ENDPOINT =
  import.meta.env.VITE_ADMIN_AUDIT_LOGS_ENDPOINT ||
  "/admin/audit-logs";

const AUDIT_STATS_ENDPOINT =
  import.meta.env.VITE_ADMIN_AUDIT_STATS_ENDPOINT ||
  "/admin/audit-logs/stats";

const APP_TIMEZONE =
  import.meta.env.VITE_APP_TIMEZONE ||
  Intl.DateTimeFormat().resolvedOptions().timeZone;

/* ============================================================
   AXIOS INSTANCE
============================================================ */

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    Accept: "application/json",
    "Content-Type": "application/json",
  },
});

/* ============================================================
   TYPES
============================================================ */

type AuditSeverity =
  | "INFO"
  | "WARNING"
  | "CRITICAL";

type AuditLog = {
  id: number | string;
  user: string;
  action: string;
  category: string;
  ip: string;
  time: string;
  timestamp: string;
  severity: AuditSeverity;
  details?: string;
};

type AuditStats = {
  eventsToday: number;
  adminActions: number;
  securityEvents: number;
  activeUsers: number;
};

type AuditApiResponse = {
  content?: AuditLog[];
  data?: AuditLog[];
  items?: AuditLog[];
  logs?: AuditLog[];
  totalElements?: number;
  total?: number;
};

type AuditStatsApiResponse = {
  data?: Partial<AuditStats>;
  eventsToday?: number;
  adminActions?: number;
  securityEvents?: number;
  activeUsers?: number;
};

type StatCardProps = {
  title: string;
  value: string;
  icon: React.ElementType;
  iconClassName: string;
  iconBackground: string;
  description: string;
  trend?: string;
};

type SeverityBadgeProps = {
  severity: AuditSeverity;
};

type AuditDetailsModalProps = {
  log: AuditLog;
  onClose: () => void;
};

/* ============================================================
   DEFAULT VALUES
   These are UI-safe fallbacks, NOT mock audit records.
============================================================ */

const EMPTY_STATS: AuditStats = {
  eventsToday: 0,
  adminActions: 0,
  securityEvents: 0,
  activeUsers: 0,
};

/* ============================================================
   API HELPERS
============================================================ */

function normalizeAuditSeverity(
  value: unknown
): AuditSeverity {
  const normalized = String(value ?? "INFO").toUpperCase();

  if (normalized === "CRITICAL") {
    return "CRITICAL";
  }

  if (normalized === "WARNING") {
    return "WARNING";
  }

  return "INFO";
}

function normalizeAuditLog(
  item: Partial<AuditLog> & Record<string, unknown>
): AuditLog {
  const timestamp =
    String(
      item.timestamp ??
        item.createdAt ??
        item.created_at ??
        new Date().toISOString()
    );

  return {
    id: (item.id ?? item.auditId ?? crypto.randomUUID()) as string | number,
    user: String(
      item.user ??
        item.username ??
        item.userName ??
        item.email ??
        "Unknown"
    ),
    action: String(
      item.action ??
        item.event ??
        item.eventType ??
        "Platform activity"
    ),
    category: String(
      item.category ??
        item.eventCategory ??
        "General"
    ),
    ip: String(
      item.ip ??
        item.ipAddress ??
        "Unavailable"
    ),
    time: String(
      item.time ??
        item.relativeTime ??
        formatRelativeTime(timestamp)
    ),
    timestamp,
    severity: normalizeAuditSeverity(
      item.severity
    ),
    details:
      item.details != null
        ? String(item.details)
        : item.description != null
          ? String(item.description)
          : undefined,
  };
}

function extractAuditLogs(
  response: AuditApiResponse | AuditLog[]
): AuditLog[] {
  if (Array.isArray(response)) {
    return response.map((item) =>
      normalizeAuditLog(
        item as Partial<AuditLog> &
          Record<string, unknown>
      )
    );
  }

  const rawItems =
    response.content ??
    response.data ??
    response.items ??
    response.logs ??
    [];

  return rawItems.map((item) =>
    normalizeAuditLog(
      item as Partial<AuditLog> &
        Record<string, unknown>
    )
  );
}

function extractStats(
  response: AuditStatsApiResponse | AuditStats
): AuditStats {
  const source =
    "data" in response && response.data
      ? response.data
      : response;

  return {
    eventsToday: Number(
      source.eventsToday ?? 0
    ),
    adminActions: Number(
      source.adminActions ?? 0
    ),
    securityEvents: Number(
      source.securityEvents ?? 0
    ),
    activeUsers: Number(
      source.activeUsers ?? 0
    ),
  };
}

function formatRelativeTime(
  timestamp: string
): string {
  const date = new Date(timestamp);

  if (Number.isNaN(date.getTime())) {
    return "Unknown";
  }

  const difference =
    Date.now() - date.getTime();

  const seconds = Math.floor(
    difference / 1000
  );

  if (seconds < 60) {
    return "Just now";
  }

  const minutes = Math.floor(
    seconds / 60
  );

  if (minutes < 60) {
    return `${minutes}m ago`;
  }

  const hours = Math.floor(
    minutes / 60
  );

  if (hours < 24) {
    return `${hours}h ago`;
  }

  const days = Math.floor(
    hours / 24
  );

  return `${days}d ago`;
}

function formatTimestamp(
  timestamp: string
): string {
  const date = new Date(timestamp);

  if (Number.isNaN(date.getTime())) {
    return "Unavailable";
  }

  return new Intl.DateTimeFormat(
    undefined,
    {
      dateStyle: "medium",
      timeStyle: "medium",
      timeZone: APP_TIMEZONE,
    }
  ).format(date);
}

function formatNumber(
  value: number
): string {
  return new Intl.NumberFormat().format(
    value
  );
}

/* ============================================================
   API FUNCTIONS
============================================================ */

async function fetchAuditLogs(): Promise<AuditLog[]> {
  const response =
    await api.get<
      AuditApiResponse | AuditLog[]
    >(AUDIT_LOGS_ENDPOINT);

  return extractAuditLogs(response.data);
}

async function fetchAuditStats(): Promise<AuditStats> {
  const response =
    await api.get<AuditStatsApiResponse>(
      AUDIT_STATS_ENDPOINT
    );

  return extractStats(response.data);
}

/* ============================================================
   STAT CARD
============================================================ */

function StatCard({
  title,
  value,
  icon: Icon,
  iconClassName,
  iconBackground,
  description,
  trend,
}: StatCardProps) {
  return (
    <div
      className="
        group
        relative
        overflow-hidden
        rounded-[1.75rem]
        border
        border-white/10
        bg-white/5
        backdrop-blur-xl
        p-6
        shadow-[0_10px_40px_rgba(0,0,0,0.2)]
        transition-all
        duration-300
        hover:-translate-y-1
        hover:shadow-[0_20px_50px_rgba(0,0,0,0.3)]
      "
    >
      <div
        className="
          absolute
          -right-8
          -top-8
          h-24
          w-24
          rounded-full
          bg-white/5
          transition-transform
          duration-500
          group-hover:scale-150
        "
      />

      <div className="relative flex items-start justify-between gap-4">
        <div className="min-w-0">
          <p className="text-sm font-semibold text-slate-400">
            {title}
          </p>

          <h2
            className="
              mt-2
              text-3xl
              font-black
              tracking-tight
              text-white
            "
          >
            {value}
          </h2>

          <div className="mt-3 flex flex-wrap items-center gap-2">
            <p className="text-xs font-medium text-slate-500">
              {description}
            </p>

            {trend && (
              <span
                className="
                  inline-flex
                  items-center
                  rounded-full
                  bg-emerald-500/10
                  px-2
                  py-1
                  text-[10px]
                  font-bold
                  text-emerald-300
                "
              >
                {trend}
              </span>
            )}
          </div>
        </div>

        <div
          className={`
            relative
            flex
            h-14
            w-14
            shrink-0
            items-center
            justify-center
            rounded-2xl
            ${iconBackground}
          `}
        >
          <Icon
            size={25}
            strokeWidth={2}
            className={iconClassName}
          />
        </div>
      </div>
    </div>
  );
}

/* ============================================================
   SEVERITY BADGE
============================================================ */

function SeverityBadge({
  severity,
}: SeverityBadgeProps) {
  const configuration: Record<
    AuditSeverity,
    {
      className: string;
      icon: React.ElementType;
      label: string;
    }
  > = {
    INFO: {
      className:
        "bg-emerald-500/10 text-emerald-300 ring-emerald-500/20",
      icon: CheckCircle2,
      label: "Info",
    },
    WARNING: {
      className:
        "bg-amber-500/10 text-amber-300 ring-amber-500/20",
      icon: AlertTriangle,
      label: "Warning",
    },
    CRITICAL: {
      className:
        "bg-rose-500/10 text-rose-300 ring-rose-500/20",
      icon: ShieldAlert,
      label: "Critical",
    },
  };

  const config =
    configuration[severity];

  const Icon = config.icon;

  return (
    <span
      className={`
        inline-flex
        items-center
        gap-1.5
        whitespace-nowrap
        rounded-full
        px-3
        py-1.5
        text-[11px]
        font-extrabold
        uppercase
        tracking-wide
        ring-1
        ring-inset
        ${config.className}
      `}
    >
      <Icon size={13} />
      {config.label}
    </span>
  );
}

/* ============================================================
   LOADING SKELETON
============================================================ */

function AuditTableSkeleton() {
  return (
    <div className="divide-y divide-white/10">
      {Array.from({ length: 6 }).map(
        (_, index) => (
          <div
            key={index}
            className="animate-pulse p-5 lg:px-6 lg:py-5"
          >
            <div className="hidden lg:grid lg:grid-cols-[1.2fr_2fr_1.3fr_1.1fr_1fr_0.8fr_0.6fr] lg:items-center lg:gap-4">
              <div className="h-5 rounded-lg bg-white/10" />
              <div className="h-5 rounded-lg bg-white/10" />
              <div className="h-5 rounded-lg bg-white/10" />
              <div className="h-5 rounded-lg bg-white/10" />
              <div className="h-5 rounded-lg bg-white/10" />
              <div className="h-7 rounded-full bg-white/10" />
              <div className="mx-auto h-9 w-9 rounded-xl bg-white/10" />
            </div>

            <div className="lg:hidden">
              <div className="flex items-center gap-3">
                <div className="h-10 w-10 rounded-xl bg-white/10" />
                <div className="flex-1 space-y-2">
                  <div className="h-4 w-40 rounded bg-white/10" />
                  <div className="h-3 w-20 rounded bg-white/10" />
                </div>
              </div>

              <div className="mt-5 h-12 rounded-xl bg-white/10" />
            </div>
          </div>
        )
      )}
    </div>
  );
}

/* ============================================================
   ERROR STATE
============================================================ */

function ErrorState({
  message,
  onRetry,
}: {
  message: string;
  onRetry: () => void;
}) {
  return (
    <div className="px-6 py-16 text-center">
      <div
        className="
          mx-auto
          flex
          h-16
          w-16
          items-center
          justify-center
          rounded-3xl
          bg-rose-500/10
          text-rose-300
        "
      >
        <ShieldAlert size={28} />
      </div>

      <h3 className="mt-5 text-lg font-black text-white">
        Unable to load audit logs
      </h3>

      <p className="mx-auto mt-2 max-w-lg text-sm leading-6 text-slate-400">
        {message}
      </p>

      <button
        type="button"
        onClick={onRetry}
        className="
          mt-6
          inline-flex
          items-center
          gap-2
          rounded-xl
          border
          border-white/15
          bg-white/10
          px-5
          py-2.5
          text-sm
          font-bold
          text-white
          transition
          hover:bg-white/20
          focus:outline-none
          focus:ring-2
          focus:ring-amber-400
          focus:ring-offset-2
        "
      >
        <RefreshCw size={16} />
        Try Again
      </button>
    </div>
  );
}

/* ============================================================
   EMPTY STATE
============================================================ */

function EmptyState({
  hasActiveFilters,
  onReset,
}: {
  hasActiveFilters: boolean;
  onReset: () => void;
}) {
  return (
    <div className="px-6 py-16 text-center">
      <div
        className="
          mx-auto
          flex
          h-16
          w-16
          items-center
          justify-center
          rounded-3xl
          bg-white/5
          text-slate-400
        "
      >
        <Search size={28} />
      </div>

      <h3 className="mt-5 text-lg font-black text-white">
        No audit events found
      </h3>

      <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-slate-400">
        {hasActiveFilters
          ? "No audit activity matches your current search or filter criteria."
          : "There are currently no audit events available."}
      </p>

      {hasActiveFilters && (
        <button
          type="button"
          onClick={onReset}
          className="
            mt-5
            rounded-xl
            border
            border-white/15
            bg-white/10
            px-5
            py-2.5
            text-sm
            font-bold
            text-white
            transition
            hover:bg-white/20
            focus:outline-none
            focus:ring-2
            focus:ring-amber-400
            focus:ring-offset-2
          "
        >
          Clear Filters
        </button>
      )}
    </div>
  );
}

/* ============================================================
   AUDIT DETAILS MODAL
============================================================ */

function AuditDetailsModal({
  log,
  onClose,
}: AuditDetailsModalProps) {
  useEffect(() => {
    const handleKeyDown = (
      event: KeyboardEvent
    ) => {
      if (event.key === "Escape") {
        onClose();
      }
    };

    document.addEventListener(
      "keydown",
      handleKeyDown
    );

    const previousOverflow =
      document.body.style.overflow;

    document.body.style.overflow = "hidden";

    return () => {
      document.removeEventListener(
        "keydown",
        handleKeyDown
      );

      document.body.style.overflow =
        previousOverflow;
    };
  }, [onClose]);

  return (
    <div
      className="
        fixed
        inset-0
        z-[100]
        flex
        items-center
        justify-center
        bg-slate-950/70
        p-4
        backdrop-blur-md
      "
      role="dialog"
      aria-modal="true"
      aria-labelledby="audit-details-title"
      onMouseDown={(event) => {
        if (
          event.target ===
          event.currentTarget
        ) {
          onClose();
        }
      }}
    >
      <div
        className="
          max-h-[90vh]
          w-full
          max-w-2xl
          overflow-y-auto
          rounded-[2rem]
          border
          border-white/10
          bg-[#1F314A]
          backdrop-blur-xl
          shadow-[0_30px_100px_rgba(0,0,0,0.30)]
        "
      >
        <div
          className="
            sticky
            top-0
            z-10
            flex
            items-center
            justify-between
            border-b
            border-white/10
            bg-gradient-to-r
            from-slate-950
            via-blue-950
            to-indigo-950
            px-6
            py-5
          "
        >
          <div>
            <div className="flex items-center gap-2">
              <ShieldCheck
                size={16}
                className="text-amber-400"
              />

              <p
                className="
                  text-[11px]
                  font-extrabold
                  uppercase
                  tracking-[0.18em]
                  text-amber-400
                "
              >
                Security Audit
              </p>
            </div>

            <h2
              id="audit-details-title"
              className="
                mt-1
                text-xl
                font-black
                text-white
              "
            >
              Event Details
            </h2>
          </div>

          <button
            type="button"
            onClick={onClose}
            aria-label="Close audit details"
            className="
              rounded-xl
              p-2.5
              text-white/70
              transition
              hover:bg-white/10
              hover:text-white
              focus:outline-none
              focus:ring-2
              focus:ring-amber-400
            "
          >
            <X size={20} />
          </button>
        </div>

        <div className="space-y-6 p-6">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <SeverityBadge
              severity={log.severity}
            />

            <span className="rounded-full bg-white/10 px-3 py-1.5 text-xs font-bold text-slate-300">
              Event ID #{log.id}
            </span>
          </div>

          <div>
            <p className="text-[11px] font-extrabold uppercase tracking-wider text-slate-400">
              Action
            </p>

            <p className="mt-2 text-xl font-black leading-8 text-white">
              {log.action}
            </p>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="rounded-2xl border border-blue-500/15 bg-blue-500/5 p-4">
              <p className="text-[11px] font-extrabold uppercase tracking-wider text-blue-300">
                User
              </p>

              <p className="mt-2 font-bold text-white">
                {log.user}
              </p>
            </div>

            <div className="rounded-2xl border border-violet-500/15 bg-violet-500/5 p-4">
              <p className="text-[11px] font-extrabold uppercase tracking-wider text-violet-300">
                Category
              </p>

              <p className="mt-2 font-bold text-white">
                {log.category}
              </p>
            </div>

            <div className="rounded-2xl border border-cyan-500/15 bg-cyan-500/5 p-4">
              <p className="text-[11px] font-extrabold uppercase tracking-wider text-cyan-300">
                IP Address
              </p>

              <p className="mt-2 break-all font-mono text-sm font-bold text-white">
                {log.ip}
              </p>
            </div>

            <div className="rounded-2xl border border-amber-500/15 bg-amber-500/5 p-4">
              <p className="text-[11px] font-extrabold uppercase tracking-wider text-amber-300">
                Timestamp
              </p>

              <p className="mt-2 text-sm font-bold text-white">
                {formatTimestamp(
                  log.timestamp
                )}
              </p>
            </div>
          </div>

          {log.details && (
            <div className="rounded-2xl border border-white/10 bg-white/5 p-5">
              <p className="text-[11px] font-extrabold uppercase tracking-wider text-slate-400">
                Description
              </p>

              <p className="mt-3 leading-7 text-slate-300">
                {log.details}
              </p>
            </div>
          )}
        </div>

        <div className="border-t border-white/10 bg-white/5 px-6 py-4">
          <button
            type="button"
            onClick={onClose}
            className="
              w-full
              rounded-2xl
              bg-gradient-to-r
              from-slate-950
              to-blue-950
              px-5
              py-3
              font-bold
              text-white
              shadow-lg
              shadow-blue-950/10
              transition
              hover:-translate-y-0.5
              hover:from-blue-950
              hover:to-indigo-950
              focus:outline-none
              focus:ring-2
              focus:ring-amber-400
              focus:ring-offset-2
            "
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
}

/* ============================================================
   MAIN PAGE
============================================================ */

export default function AuditLogsPage() {
  const [auditLogs, setAuditLogs] =
    useState<AuditLog[]>([]);

  const [stats, setStats] =
    useState<AuditStats>(EMPTY_STATS);

  const [search, setSearch] =
    useState("");

  const [severityFilter, setSeverityFilter] =
    useState<"ALL" | AuditSeverity>("ALL");

  const [categoryFilter, setCategoryFilter] =
    useState("ALL");

  const [selectedLog, setSelectedLog] =
    useState<AuditLog | null>(null);

  const [showFilters, setShowFilters] =
    useState(false);

  const [loading, setLoading] =
    useState(true);

  const [refreshing, setRefreshing] =
    useState(false);

  const [error, setError] =
    useState<string | null>(null);

  const [lastUpdated, setLastUpdated] =
    useState<Date | null>(null);

  /* ============================================================
     LOAD DATA
  ============================================================ */

  const loadData = useCallback(
    async (showInitialLoader = false) => {
      try {
        if (showInitialLoader) {
          setLoading(true);
        } else {
          setRefreshing(true);
        }

        setError(null);

        const [
          logsResult,
          statsResult,
        ] = await Promise.allSettled([
          fetchAuditLogs(),
          fetchAuditStats(),
        ]);

        if (
          logsResult.status === "rejected"
        ) {
          throw logsResult.reason;
        }

        setAuditLogs(
          logsResult.value
        );

        if (
          statsResult.status === "fulfilled"
        ) {
          setStats(statsResult.value);
        } else {
          setStats(EMPTY_STATS);
        }

        setLastUpdated(new Date());
      } catch (requestError) {
        let message =
          "An unexpected error occurred while loading audit activity.";

        if (axios.isAxiosError(requestError)) {
          if (requestError.response) {
            message =
              requestError.response.data
                ?.message ||
              requestError.response.data
                ?.error ||
              `The audit service returned HTTP ${requestError.response.status}.`;
          } else if (
            requestError.request
          ) {
            message =
              "The audit service could not be reached. Check that the Spring Boot backend is running and the API URL is configured correctly.";
          } else if (
            requestError.message
          ) {
            message =
              requestError.message;
          }
        } else if (
          requestError instanceof Error
        ) {
          message =
            requestError.message;
        }

        setError(message);
      } finally {
        setLoading(false);
        setRefreshing(false);
      }
    },
    []
  );

  useEffect(() => {
    void loadData(true);
  }, [loadData]);

  /* ============================================================
     AUTO REFRESH
  ============================================================ */

  useEffect(() => {
    const interval = window.setInterval(
      () => {
        void loadData(false);
      },
      60000
    );

    return () => {
      window.clearInterval(interval);
    };
  }, [loadData]);

  /* ============================================================
     CATEGORIES
  ============================================================ */

  const categories = useMemo(() => {
    return Array.from(
      new Set(
        auditLogs
          .map((log) => log.category)
          .filter(Boolean)
      )
    ).sort((a, b) =>
      a.localeCompare(b)
    );
  }, [auditLogs]);

  /* ============================================================
     FILTER LOGS
  ============================================================ */

  const filteredLogs = useMemo(() => {
    const normalizedSearch =
      search.trim().toLowerCase();

    return auditLogs.filter((log) => {
      const searchableValues = [
        log.user,
        log.action,
        log.category,
        log.ip,
        log.severity,
        log.details ?? "",
      ];

      const matchesSearch =
        normalizedSearch.length === 0 ||
        searchableValues.some((value) =>
          value
            .toLowerCase()
            .includes(normalizedSearch)
        );

      const matchesSeverity =
        severityFilter === "ALL" ||
        log.severity === severityFilter;

      const matchesCategory =
        categoryFilter === "ALL" ||
        log.category === categoryFilter;

      return (
        matchesSearch &&
        matchesSeverity &&
        matchesCategory
      );
    });
  }, [
    auditLogs,
    search,
    severityFilter,
    categoryFilter,
  ]);

  /* ============================================================
     RESET FILTERS
  ============================================================ */

  const resetFilters = useCallback(() => {
    setSearch("");
    setSeverityFilter("ALL");
    setCategoryFilter("ALL");
  }, []);

  /* ============================================================
     ACTIVE FILTERS
  ============================================================ */

  const hasActiveFilters =
    search.trim() !== "" ||
    severityFilter !== "ALL" ||
    categoryFilter !== "ALL";

  /* ============================================================
     CSV EXPORT
  ============================================================ */

  const exportAuditReport = useCallback(() => {
    if (filteredLogs.length === 0) {
      return;
    }

    const headers = [
      "ID",
      "User",
      "Action",
      "Category",
      "IP Address",
      "Time",
      "Severity",
      "Timestamp",
      "Details",
    ];

    const escapeCsvValue = (
      value: unknown
    ) => {
      const stringValue =
        String(value ?? "");

      return `"${stringValue.replace(
        /"/g,
        '""'
      )}"`;
    };

    const rows = filteredLogs.map(
      (log) => [
        log.id,
        log.user,
        log.action,
        log.category,
        log.ip,
        log.time,
        log.severity,
        log.timestamp,
        log.details ?? "",
      ]
    );

    const csv = [
      headers
        .map(escapeCsvValue)
        .join(","),
      ...rows.map((row) =>
        row
          .map(escapeCsvValue)
          .join(",")
      ),
    ].join("\r\n");

    const blob = new Blob(
      ["\uFEFF", csv],
      {
        type: "text/csv;charset=utf-8;",
      }
    );

    const url =
      URL.createObjectURL(blob);

    const link =
      document.createElement("a");

    const date =
      new Date()
        .toISOString()
        .slice(0, 10);

    link.href = url;
    link.download = `audit-report-${date}.csv`;
    link.style.display = "none";

    document.body.appendChild(link);
    link.click();
    link.remove();

    window.setTimeout(() => {
      URL.revokeObjectURL(url);
    }, 100);
  }, [filteredLogs]);

  /* ============================================================
     OPEN MODAL
  ============================================================ */

  const openAuditDetails = useCallback(
    (log: AuditLog) => {
      setSelectedLog(log);
    },
    []
  );

  const closeAuditDetails =
    useCallback(() => {
      setSelectedLog(null);
    }, []);

  /* ============================================================
     RENDER
  ============================================================ */

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
          lg:px-8
          xl:px-10
        "
      >
        {/* =====================================================
            HEADER
        ===================================================== */}

        <header
          className="
            mb-8
            flex
            flex-col
            gap-6
            lg:flex-row
            lg:items-center
            lg:justify-between
          "
        >
          <div className="min-w-0">
            <div className="mb-4 flex items-center gap-3">
              <div
                className="
                  flex
                  h-11
                  w-11
                  items-center
                  justify-center
                  rounded-2xl
                  bg-gradient-to-br
                  from-slate-950
                  via-blue-950
                  to-indigo-950
                  text-amber-400
                  shadow-lg
                  shadow-blue-950/10
                "
              >
                <ShieldCheck
                  size={22}
                />
              </div>

              <div>
                <p
                  className="
                    text-[11px]
                    font-extrabold
                    uppercase
                    tracking-[0.18em]
                    text-blue-300
                  "
                >
                  Security & Compliance
                </p>

                <p className="text-xs font-medium text-slate-400">
                  Platform audit monitoring
                </p>
              </div>
            </div>

            <h1
              className="
                text-3xl
                font-black
                tracking-tight
                text-white
                sm:text-4xl
                xl:text-5xl
              "
            >
              Audit Logs Center
            </h1>

            <p
              className="
                mt-3
                max-w-3xl
                text-sm
                leading-7
                text-slate-300
                sm:text-base
              "
            >
              Monitor administrator actions, user
              activity, document processing, AI events,
              authentication activity and security
              events across the platform.
            </p>

            {lastUpdated && (
              <div className="mt-4 flex flex-wrap items-center gap-2 text-xs text-slate-400">
                <Clock3 size={14} />

                <span>
                  Last updated{" "}
                  {lastUpdated.toLocaleTimeString()}
                </span>

                {refreshing && (
                  <>
                    <span>•</span>
                    <span className="font-semibold text-blue-300">
                      Refreshing...
                    </span>
                  </>
                )}
              </div>
            )}
          </div>

          <div className="flex flex-col gap-3 sm:flex-row">
            <button
              type="button"
              onClick={() =>
                void loadData(false)
              }
              disabled={refreshing}
              className="
                inline-flex
                items-center
                justify-center
                gap-2
                rounded-2xl
                border
                border-white/15
                bg-white/5
                px-5
                py-3.5
                font-bold
                text-slate-200
                shadow-sm
                transition-all
                hover:-translate-y-0.5
                hover:border-blue-400/40
                hover:bg-blue-500/10
                hover:text-blue-300
                disabled:cursor-not-allowed
                disabled:opacity-60
                focus:outline-none
                focus:ring-2
                focus:ring-amber-400
                focus:ring-offset-2
              "
            >
              <RefreshCw
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
              onClick={
                exportAuditReport
              }
              disabled={
                loading ||
                filteredLogs.length === 0
              }
              className="
                inline-flex
                items-center
                justify-center
                gap-2
                rounded-2xl
                bg-gradient-to-r
                from-slate-950
                via-blue-950
                to-indigo-950
                px-6
                py-3.5
                font-bold
                text-white
                shadow-lg
                shadow-blue-950/15
                transition-all
                hover:-translate-y-0.5
                hover:from-blue-950
                hover:to-indigo-950
                disabled:cursor-not-allowed
                disabled:opacity-50
                focus:outline-none
                focus:ring-2
                focus:ring-amber-400
                focus:ring-offset-2
              "
            >
              <Download size={18} />
              Export Audit Report
            </button>
          </div>
        </header>

        {/* =====================================================
            STATISTICS
        ===================================================== */}

        <section
          aria-label="Audit statistics"
          className="
            grid
            gap-5
            sm:grid-cols-2
            xl:grid-cols-4
          "
        >
          <StatCard
            title="Events Today"
            value={formatNumber(
              stats.eventsToday
            )}
            icon={Activity}
            iconClassName="text-blue-300"
            iconBackground="bg-gradient-to-br from-blue-500/15 to-cyan-500/10"
            description="Platform activity"
            trend="Live"
          />

          <StatCard
            title="Admin Actions"
            value={formatNumber(
              stats.adminActions
            )}
            icon={ShieldCheck}
            iconClassName="text-violet-300"
            iconBackground="bg-gradient-to-br from-violet-500/15 to-purple-500/10"
            description="Administrative events"
          />

          <StatCard
            title="Security Events"
            value={formatNumber(
              stats.securityEvents
            )}
            icon={LockKeyhole}
            iconClassName="text-rose-300"
            iconBackground="bg-gradient-to-br from-rose-500/15 to-pink-500/10"
            description="Events requiring monitoring"
          />

          <StatCard
            title="Active Users"
            value={formatNumber(
              stats.activeUsers
            )}
            icon={Users}
            iconClassName="text-emerald-300"
            iconBackground="bg-gradient-to-br from-emerald-500/15 to-teal-500/10"
            description="Currently active accounts"
          />
        </section>

        {/* =====================================================
            SEARCH / FILTER PANEL
        ===================================================== */}

        <section
          className="
            mt-8
            rounded-[1.75rem]
            border
            border-white/10
            bg-white/5
            backdrop-blur-xl
            p-4
            shadow-[0_10px_40px_rgba(0,0,0,0.2)]
            sm:p-5
          "
        >
          <div className="flex flex-col gap-4 lg:flex-row">
            <div className="relative flex-1">
              <Search
                className="
                  pointer-events-none
                  absolute
                  left-4
                  top-1/2
                  -translate-y-1/2
                  text-slate-400
                "
                size={20}
              />

              <input
                value={search}
                onChange={(event) =>
                  setSearch(
                    event.target.value
                  )
                }
                type="search"
                aria-label="Search audit activity"
                placeholder="Search user, action, category, IP address..."
                className="
                  w-full
                  rounded-2xl
                  border
                  border-white/15
                  bg-white/5
                  py-3.5
                  pl-12
                  pr-4
                  text-sm
                  font-medium
                  text-white
                  outline-none
                  transition
                  placeholder:text-slate-500
                  focus:border-blue-400
                  focus:bg-white/10
                  focus:ring-4
                  focus:ring-blue-500/10
                "
              />

              {search && (
                <button
                  type="button"
                  onClick={() =>
                    setSearch("")
                  }
                  aria-label="Clear search"
                  className="
                    absolute
                    right-3
                    top-1/2
                    -translate-y-1/2
                    rounded-lg
                    p-1.5
                    text-slate-400
                    transition
                    hover:bg-white/10
                    hover:text-white
                  "
                >
                  <X size={16} />
                </button>
              )}
            </div>

            <button
              type="button"
              onClick={() =>
                setShowFilters(
                  (current) => !current
                )
              }
              aria-expanded={
                showFilters
              }
              className="
                inline-flex
                items-center
                justify-center
                gap-2
                rounded-2xl
                border
                border-white/15
                bg-white/5
                px-5
                py-3
                font-bold
                text-slate-200
                transition
                hover:border-blue-400/40
                hover:bg-blue-500/10
                hover:text-blue-300
                focus:outline-none
                focus:ring-2
                focus:ring-amber-400
              "
            >
              <Filter size={18} />

              Filters

              {(severityFilter !==
                "ALL" ||
                categoryFilter !==
                  "ALL") && (
                <span
                  className="
                    flex
                    h-5
                    min-w-5
                    items-center
                    justify-center
                    rounded-full
                    bg-blue-600
                    px-1.5
                    text-[10px]
                    font-black
                    text-white
                  "
                >
                  {
                    [
                      severityFilter !==
                        "ALL",
                      categoryFilter !==
                        "ALL",
                    ].filter(Boolean)
                      .length
                  }
                </span>
              )}

              <ChevronDown
                size={17}
                className={`
                  transition-transform
                  duration-300
                  ${
                    showFilters
                      ? "rotate-180"
                      : ""
                  }
                `}
              />
            </button>
          </div>

          {showFilters && (
            <div
              className="
                mt-5
                grid
                gap-4
                border-t
                border-white/10
                pt-5
                sm:grid-cols-2
                lg:grid-cols-3
              "
            >
              <div>
                <label
                  htmlFor="severity-filter"
                  className="
                    mb-2
                    block
                    text-xs
                    font-extrabold
                    uppercase
                    tracking-wider
                    text-slate-400
                  "
                >
                  Severity
                </label>

                <select
                  id="severity-filter"
                  value={
                    severityFilter
                  }
                  onChange={(event) =>
                    setSeverityFilter(
                      event.target
                        .value as
                        | "ALL"
                        | AuditSeverity
                    )
                  }
                  className="
                    w-full
                    rounded-2xl
                    border
                    border-white/15
                    bg-white/5
                    px-4
                    py-3
                    text-sm
                    font-semibold
                    text-white
                    outline-none
                    transition
                    focus:border-blue-400
                    focus:ring-4
                    focus:ring-blue-500/10
                  "
                >
                  <option value="ALL">
                    All Severities
                  </option>

                  <option value="INFO">
                    Info
                  </option>

                  <option value="WARNING">
                    Warning
                  </option>

                  <option value="CRITICAL">
                    Critical
                  </option>
                </select>
              </div>

              <div>
                <label
                  htmlFor="category-filter"
                  className="
                    mb-2
                    block
                    text-xs
                    font-extrabold
                    uppercase
                    tracking-wider
                    text-slate-400
                  "
                >
                  Category
                </label>

                <select
                  id="category-filter"
                  value={
                    categoryFilter
                  }
                  onChange={(event) =>
                    setCategoryFilter(
                      event.target.value
                    )
                  }
                  className="
                    w-full
                    rounded-2xl
                    border
                    border-white/15
                    bg-white/5
                    px-4
                    py-3
                    text-sm
                    font-semibold
                    text-white
                    outline-none
                    transition
                    focus:border-blue-400
                    focus:ring-4
                    focus:ring-blue-500/10
                  "
                >
                  <option value="ALL">
                    All Categories
                  </option>

                  {categories.map(
                    (category) => (
                      <option
                        key={category}
                        value={category}
                      >
                        {category}
                      </option>
                    )
                  )}
                </select>
              </div>

              <div className="flex items-end">
                <button
                  type="button"
                  onClick={
                    resetFilters
                  }
                  disabled={
                    !hasActiveFilters
                  }
                  className="
                    w-full
                    rounded-2xl
                    border
                    border-white/15
                    px-4
                    py-3
                    text-sm
                    font-bold
                    text-slate-200
                    transition
                    hover:border-rose-400/40
                    hover:bg-rose-500/10
                    hover:text-rose-300
                    disabled:cursor-not-allowed
                    disabled:opacity-40
                  "
                >
                  Reset Filters
                </button>
              </div>
            </div>
          )}

          <div
            className="
              mt-4
              flex
              flex-wrap
              items-center
              justify-between
              gap-3
              text-sm
            "
          >
            <p className="text-slate-400">
              Showing{" "}
              <span className="font-black text-white">
                {filteredLogs.length}
              </span>{" "}
              of{" "}
              <span className="font-black text-white">
                {auditLogs.length}
              </span>{" "}
              audit events
            </p>

            {hasActiveFilters && (
              <button
                type="button"
                onClick={
                  resetFilters
                }
                className="
                  font-bold
                  text-blue-300
                  transition
                  hover:text-white
                "
              >
                Clear all filters
              </button>
            )}
          </div>
        </section>

        {/* =====================================================
            AUDIT TABLE
        ===================================================== */}

        <section
          className="
            mt-8
            overflow-hidden
            rounded-[1.75rem]
            border
            border-white/10
            bg-white/5
            backdrop-blur-xl
            shadow-[0_10px_40px_rgba(0,0,0,0.2)]
          "
          aria-label="Audit events"
        >
          <div
            className="
              hidden
              grid-cols-[1.2fr_2fr_1.3fr_1.1fr_1fr_0.8fr_0.6fr]
              gap-4
              border-b
              border-blue-900/30
              bg-gradient-to-r
              from-slate-950
              via-blue-950
              to-indigo-950
              px-6
              py-4
              text-[10px]
              font-extrabold
              uppercase
              tracking-[0.12em]
              text-blue-100
              lg:grid
            "
          >
            <span>User</span>
            <span>Action</span>
            <span>Category</span>
            <span>IP Address</span>
            <span>Time</span>
            <span>Status</span>
            <span className="text-center">
              View
            </span>
          </div>

          {loading ? (
            <AuditTableSkeleton />
          ) : error ? (
            <ErrorState
              message={error}
              onRetry={() =>
                void loadData(true)
              }
            />
          ) : filteredLogs.length ===
            0 ? (
            <EmptyState
              hasActiveFilters={
                hasActiveFilters
              }
              onReset={
                resetFilters
              }
            />
          ) : (
            <div className="divide-y divide-white/10">
              {filteredLogs.map(
                (log) => (
                  <article
                    key={log.id}
                    className="
                      group
                      transition-colors
                      hover:bg-blue-500/5
                    "
                  >
                    {/* DESKTOP */}

                    <div
                      className="
                        hidden
                        grid-cols-[1.2fr_2fr_1.3fr_1.1fr_1fr_0.8fr_0.6fr]
                        items-center
                        gap-4
                        px-6
                        py-5
                        lg:grid
                      "
                    >
                      <div className="flex min-w-0 items-center gap-3">
                        <div
                          className="
                            flex
                            h-10
                            w-10
                            shrink-0
                            items-center
                            justify-center
                            rounded-xl
                            bg-[#C6A15B]/15
                            text-[#C6A15B]
                            ring-1
                            ring-[#C6A15B]/20
                          "
                        >
                          <UserCheck
                            size={17}
                          />
                        </div>

                        <span
                          className="
                            truncate
                            text-sm
                            font-bold
                            text-white
                          "
                          title={log.user}
                        >
                          {log.user}
                        </span>
                      </div>

                      <div className="min-w-0">
                        <span
                          className="
                            line-clamp-2
                            text-sm
                            font-semibold
                            leading-6
                            text-slate-300
                          "
                          title={log.action}
                        >
                          {log.action}
                        </span>
                      </div>

                      <div>
                        <span
                          className="
                            inline-flex
                            max-w-full
                            items-center
                            rounded-lg
                            bg-white/10
                            px-2.5
                            py-1.5
                            text-xs
                            font-bold
                            text-slate-300
                          "
                        >
                          {log.category}
                        </span>
                      </div>

                      <div
                        className="
                          truncate
                          font-mono
                          text-xs
                          font-semibold
                          text-slate-400
                        "
                        title={log.ip}
                      >
                        {log.ip}
                      </div>

                      <div className="text-sm font-medium text-slate-400">
                        {log.time}
                      </div>

                      <div>
                        <SeverityBadge
                          severity={
                            log.severity
                          }
                        />
                      </div>

                      <div className="flex justify-center">
                        <button
                          type="button"
                          onClick={() =>
                            openAuditDetails(
                              log
                            )
                          }
                          aria-label={`View details for audit event ${log.id}`}
                          className="
                            rounded-xl
                            p-2.5
                            text-slate-400
                            transition
                            hover:bg-blue-500/10
                            hover:text-blue-300
                            focus:outline-none
                            focus:ring-2
                            focus:ring-amber-400
                          "
                        >
                          <Eye size={18} />
                        </button>
                      </div>
                    </div>

                    {/* MOBILE */}

                    <div className="p-5 lg:hidden">
                      <div className="flex items-start justify-between gap-4">
                        <div className="flex min-w-0 items-center gap-3">
                          <div
                            className="
                              flex
                              h-11
                              w-11
                              shrink-0
                              items-center
                              justify-center
                              rounded-xl
                              bg-[#C6A15B]/15
                              text-[#C6A15B]
                              ring-1
                              ring-[#C6A15B]/20
                            "
                          >
                            <UserCheck
                              size={18}
                            />
                          </div>

                          <div className="min-w-0">
                            <p className="truncate font-black text-white">
                              {log.user}
                            </p>

                            <p className="mt-0.5 text-xs font-medium text-slate-500">
                              {log.time}
                            </p>
                          </div>
                        </div>

                        <SeverityBadge
                          severity={
                            log.severity
                          }
                        />
                      </div>

                      <div className="mt-5 rounded-2xl bg-white/5 p-4">
                        <p className="text-sm font-bold leading-6 text-white">
                          {log.action}
                        </p>
                      </div>

                      <div className="mt-4 grid gap-4 sm:grid-cols-3">
                        <div>
                          <p className="text-[10px] font-extrabold uppercase tracking-wider text-slate-500">
                            Category
                          </p>

                          <p className="mt-1 text-sm font-semibold text-slate-300">
                            {log.category}
                          </p>
                        </div>

                        <div>
                          <p className="text-[10px] font-extrabold uppercase tracking-wider text-slate-500">
                            IP Address
                          </p>

                          <p className="mt-1 break-all font-mono text-xs font-semibold text-slate-300">
                            {log.ip}
                          </p>
                        </div>

                        <div className="sm:text-right">
                          <button
                            type="button"
                            onClick={() =>
                              openAuditDetails(
                                log
                              )
                            }
                            className="
                              inline-flex
                              items-center
                              gap-2
                              rounded-xl
                              bg-blue-500/10
                              px-4
                              py-2.5
                              text-sm
                              font-bold
                              text-blue-300
                              transition
                              hover:bg-blue-500/20
                              focus:outline-none
                              focus:ring-2
                              focus:ring-amber-400
                            "
                          >
                            <Eye
                              size={16}
                            />
                            View Details
                          </button>
                        </div>
                      </div>
                    </div>
                  </article>
                )
              )}
            </div>
          )}
        </section>

        {/* =====================================================
            INFORMATION / COMPLIANCE
        ===================================================== */}

        <section
          className="
            mt-8
            grid
            gap-6
            lg:grid-cols-3
          "
        >
          <div
            className="
              group
              overflow-hidden
              rounded-[1.75rem]
              border
              border-blue-500/15
              bg-blue-500/5
              p-6
              shadow-sm
              transition
              hover:-translate-y-1
              hover:shadow-xl
            "
          >
            <div
              className="
                flex
                h-13
                w-13
                items-center
                justify-center
                rounded-2xl
                bg-blue-500/10
                text-blue-300
              "
            >
              <Globe2 size={25} />
            </div>

            <h3 className="mt-5 text-lg font-black text-white">
              Compliance Monitoring
            </h3>

            <p className="mt-2 text-sm leading-7 text-slate-300">
              Platform activity is recorded to support
              security reviews, accountability,
              investigations and audit requirements.
            </p>
          </div>

          <div
            className="
              group
              overflow-hidden
              rounded-[1.75rem]
              border
              border-violet-500/15
              bg-violet-500/5
              p-6
              shadow-sm
              transition
              hover:-translate-y-1
              hover:shadow-xl
            "
          >
            <div
              className="
                flex
                h-13
                w-13
                items-center
                justify-center
                rounded-2xl
                bg-violet-500/10
                text-violet-300
              "
            >
              <FileText size={25} />
            </div>

            <h3 className="mt-5 text-lg font-black text-white">
              Document Tracking
            </h3>

            <p className="mt-2 text-sm leading-7 text-slate-300">
              Document uploads, access events, processing
              activities and AI analysis events can be
              monitored through the audit trail.
            </p>
          </div>

          <div
            className="
              group
              overflow-hidden
              rounded-[1.75rem]
              border
              border-emerald-500/15
              bg-emerald-500/5
              p-6
              shadow-sm
              transition
              hover:-translate-y-1
              hover:shadow-xl
            "
          >
            <div
              className="
                flex
                h-13
                w-13
                items-center
                justify-center
                rounded-2xl
                bg-emerald-500/10
                text-emerald-300
              "
            >
              <CheckCircle2
                size={25}
              />
            </div>

            <h3 className="mt-5 text-lg font-black text-white">
              Audit Infrastructure
            </h3>

            <div className="mt-4 flex items-center gap-2">
              <span className="relative flex h-3 w-3">
                <span
                  className="
                    absolute
                    inline-flex
                    h-full
                    w-full
                    animate-ping
                    rounded-full
                    bg-emerald-400
                    opacity-60
                  "
                />

                <span
                  className="
                    relative
                    inline-flex
                    h-3
                    w-3
                    rounded-full
                    bg-emerald-500
                  "
                />
              </span>

              <p className="text-sm font-black text-emerald-300">
                Monitoring active
              </p>
            </div>

            <p className="mt-2 text-sm leading-7 text-slate-300">
              Audit monitoring is connected to the
              platform backend and automatically refreshes
              to display current activity.
            </p>
          </div>
        </section>

        {/* =====================================================
            SYSTEM CAPABILITIES
        ===================================================== */}

        <section
          className="
            mt-8
            rounded-[1.75rem]
            border
            border-white/10
            bg-gradient-to-r
            from-slate-950
            via-blue-950
            to-indigo-950
            p-6
            text-white
            shadow-2xl
            shadow-blue-950/10
            sm:p-8
          "
        >
          <div className="flex flex-col gap-8 lg:flex-row lg:items-center lg:justify-between">
            <div className="max-w-2xl">
              <div className="flex items-center gap-2">
                <Zap
                  size={17}
                  className="text-amber-400"
                />

                <span className="text-[11px] font-extrabold uppercase tracking-[0.18em] text-amber-400">
                  Audit Infrastructure
                </span>
              </div>

              <h2 className="mt-3 text-2xl font-black">
                Centralized platform visibility
              </h2>

              <p className="mt-3 text-sm leading-7 text-blue-100/70">
                Monitor security events, administrator
                actions, document activity, authentication
                events and AI processing activity from one
                centralized audit interface.
              </p>
            </div>

            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
              {[
                {
                  label: "Security",
                  icon: LockKeyhole,
                },
                {
                  label: "Users",
                  icon: Users,
                },
                {
                  label: "Documents",
                  icon: FileText,
                },
                {
                  label: "Infrastructure",
                  icon: Server,
                },
              ].map(
                ({
                  label,
                  icon: Icon,
                }) => (
                  <div
                    key={label}
                    className="
                      flex
                      min-w-[120px]
                      flex-col
                      items-center
                      gap-2
                      rounded-2xl
                      border
                      border-white/10
                      bg-white/5
                      px-4
                      py-4
                      text-center
                    "
                  >
                    <Icon
                      size={19}
                      className="text-amber-400"
                    />

                    <span className="text-xs font-bold text-white/80">
                      {label}
                    </span>
                  </div>
                )
              )}
            </div>
          </div>
        </section>
      </div>

      {/* =====================================================
          MODAL
      ===================================================== */}

      {selectedLog && (
        <AuditDetailsModal
          log={selectedLog}
          onClose={
            closeAuditDetails
          }
        />
      )}
    </div>
  );
}