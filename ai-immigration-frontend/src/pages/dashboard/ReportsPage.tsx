import {
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import {
  FileText,
  Download,
  Eye,
  Search,
  Filter,
  ScanSearch,
  Sparkles,
  CheckCircle,
  Clock,
  AlertTriangle,
  ArrowRight,
  TrendingUp,
  Calendar,
  FileCheck,
  BarChart3,
  RefreshCw,
} from "lucide-react";

/* ============================================================================
   TYPES
   ========================================================================== */

const REPORT_STATUSES = [
  "Completed",
  "Processing",
  "Needs Review",
] as const;

type ReportStatus =
  (typeof REPORT_STATUSES)[number];

type ReportStatusFilter =
  | "All"
  | ReportStatus;

interface Report {
  id: string;
  documentName: string;
  type: string;
  date: string;
  status: ReportStatus;
  score: number;
  viewUrl?: string;
  downloadUrl?: string;
}

interface ReportStatistics {
  totalReports: number;
  completedReports: number;
  processingReports: number;
  averageScore: number;
}

interface ReportsResponse {
  reports: Report[];
  statistics?: ReportStatistics;
  total?: number;
}

interface ApiErrorResponse {
  message?: string;
  detail?: string;
  error?: string;
}

/* ============================================================================
   ENVIRONMENT CONFIGURATION
   ========================================================================== */

const REPORTS_ENDPOINT =
  import.meta.env.VITE_REPORTS_ENDPOINT;

const REPORT_VIEW_PATH =
  import.meta.env.VITE_REPORT_VIEW_PATH;

const REPORT_DOWNLOAD_PATH =
  import.meta.env.VITE_REPORT_DOWNLOAD_PATH;

const NEW_ANALYSIS_PATH =
  import.meta.env.VITE_NEW_ANALYSIS_PATH ||
  "/dashboard/upload";

/*
 * Example .env configuration:
 *
 * VITE_REPORTS_ENDPOINT=http://localhost:8080/api/reports
 * VITE_REPORT_VIEW_PATH=/dashboard/reports
 * VITE_REPORT_DOWNLOAD_PATH=/api/reports
 * VITE_NEW_ANALYSIS_PATH=/dashboard/upload
 *
 * Production:
 *
 * VITE_REPORTS_ENDPOINT=https://api.example.com/api/reports
 * VITE_REPORT_VIEW_PATH=/dashboard/reports
 * VITE_REPORT_DOWNLOAD_PATH=/api/reports
 * VITE_NEW_ANALYSIS_PATH=/dashboard/upload
 */

if (!REPORTS_ENDPOINT) {
  console.error(
    "VITE_REPORTS_ENDPOINT is not configured."
  );
}

/* ============================================================================
   NORMALIZATION HELPERS
   ========================================================================== */

function normalizeScore(
  value: unknown
): number {
  const numericValue =
    typeof value === "number"
      ? value
      : Number(value);

  if (!Number.isFinite(numericValue)) {
    return 0;
  }

  return Math.min(
    100,
    Math.max(0, numericValue)
  );
}

function normalizeReportStatus(
  value: unknown
): ReportStatus {
  const normalized =
    String(
      value ?? "Processing"
    )
      .trim()
      .toLowerCase();

  switch (normalized) {
    case "completed":
    case "complete":
    case "done":
      return "Completed";

    case "needs review":
    case "needs_review":
    case "review":
    case "requires review":
      return "Needs Review";

    case "processing":
    case "pending":
    case "in progress":
    case "in_progress":
      return "Processing";

    default:
      return "Processing";
  }
}

function normalizeDate(
  value: unknown
): string {
  if (!value) {
    return "";
  }

  return String(value);
}

function normalizeReport(
  value: unknown
): Report | null {
  if (
    !value ||
    typeof value !== "object"
  ) {
    return null;
  }

  const item =
    value as Record<
      string,
      unknown
    >;

  const id =
    String(
      item.id ??
        item.reportId ??
        item.report_id ??
        ""
    ).trim();

  const documentName =
    String(
      item.documentName ??
        item.document_name ??
        item.fileName ??
        item.filename ??
        item.document?.name ??
        ""
    ).trim();

  if (!id || !documentName) {
    return null;
  }

  const viewUrl =
    item.viewUrl ??
    item.view_url ??
    item.viewPath;

  const downloadUrl =
    item.downloadUrl ??
    item.download_url ??
    item.downloadPath;

  return {
    id,

    documentName,

    type:
      String(
        item.type ??
          item.reportType ??
          item.report_type ??
          item.analysisType ??
          item.analysis_type ??
          "Document Analysis"
      ).trim(),

    date: normalizeDate(
      item.date ??
        item.createdAt ??
        item.created_at ??
        item.updatedAt ??
        item.updated_at
    ),

    status:
      normalizeReportStatus(
        item.status ??
          item.reportStatus ??
          item.report_status
      ),

    score: normalizeScore(
      item.score ??
        item.aiScore ??
        item.ai_score ??
        item.analysisScore ??
        item.analysis_score
    ),

    viewUrl:
      viewUrl
        ? String(viewUrl)
        : undefined,

    downloadUrl:
      downloadUrl
        ? String(downloadUrl)
        : undefined,
  };
}

function normalizeStatistics(
  value: unknown
): ReportStatistics | undefined {
  if (
    !value ||
    typeof value !== "object"
  ) {
    return undefined;
  }

  const data =
    value as Record<
      string,
      unknown
    >;

  return {
    totalReports: Math.max(
      0,
      Number(
        data.totalReports ??
          data.total ??
          0
      ) || 0
    ),

    completedReports: Math.max(
      0,
      Number(
        data.completedReports ??
          data.completed ??
          0
      ) || 0
    ),

    processingReports: Math.max(
      0,
      Number(
        data.processingReports ??
          data.processing ??
          0
      ) || 0
    ),

    averageScore:
      normalizeScore(
        data.averageScore ??
          data.averageAIScore ??
          data.average_ai_score ??
          0
      ),
  };
}

function normalizeReportsResponse(
  payload: unknown
): ReportsResponse {
  if (Array.isArray(payload)) {
    const reports =
      payload
        .map(normalizeReport)
        .filter(
          (
            report
          ): report is Report =>
            report !== null
        );

    return {
      reports,
    };
  }

  if (
    !payload ||
    typeof payload !== "object"
  ) {
    return {
      reports: [],
    };
  }

  const data =
    payload as Record<
      string,
      unknown
    >;

  const rawReports =
    Array.isArray(
      data.reports
    )
      ? data.reports
      : Array.isArray(
          data.data
        )
      ? data.data
      : Array.isArray(
          data.items
        )
      ? data.items
      : [];

  const reports =
    rawReports
      .map(normalizeReport)
      .filter(
        (
          report
        ): report is Report =>
          report !== null
      );

  const statistics =
    normalizeStatistics(
      data.statistics
    );

  const totalValue =
    Number(data.total);

  return {
    reports,
    statistics,

    total: Number.isFinite(
      totalValue
    )
      ? totalValue
      : undefined,
  };
}

/* ============================================================================
   STATISTICS
   ========================================================================== */

function calculateStatistics(
  reports: Report[],
  backendStatistics?: ReportStatistics,
  backendTotal?: number
): ReportStatistics {
  if (backendStatistics) {
    return backendStatistics;
  }

  const totalReports =
    backendTotal ??
    reports.length;

  const completedReports =
    reports.filter(
      (report) =>
        report.status ===
        "Completed"
    ).length;

  const processingReports =
    reports.filter(
      (report) =>
        report.status ===
        "Processing"
    ).length;

  const scoredReports =
    reports.filter(
      (report) =>
        report.score > 0
    );

  const averageScore =
    scoredReports.length > 0
      ? scoredReports.reduce(
          (sum, report) =>
            sum + report.score,
          0
        ) /
        scoredReports.length
      : 0;

  return {
    totalReports,
    completedReports,
    processingReports,
    averageScore,
  };
}

/* ============================================================================
   DATE FORMATTING
   ========================================================================== */

function formatDate(
  value: string
): string {
  if (!value) {
    return "—";
  }

  const date =
    new Date(value);

  if (
    Number.isNaN(
      date.getTime()
    )
  ) {
    return value;
  }

  return new Intl.DateTimeFormat(
    undefined,
    {
      dateStyle: "medium",
      timeStyle: "short",
    }
  ).format(date);
}

/* ============================================================================
   STAT CARD
   ========================================================================== */

function StatCard({
  icon,
  title,
  value,
}: {
  icon: React.ReactNode;
  title: string;
  value: string;
}) {
  return (
    <div
      className="
        rounded-[28px]
        border
        border-white/70
        bg-white/80
        p-6
        shadow-lg
        backdrop-blur-xl
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
          bg-gradient-to-br
          from-[#071330]
          to-[#183B6B]
          text-[#F4B81A]
        "
      >
        {icon}
      </div>

      <p className="mt-5 text-sm font-medium text-slate-500">
        {title}
      </p>

      <p className="mt-1 text-4xl font-black text-[#071330]">
        {value}
      </p>
    </div>
  );
}

/* ============================================================================
   STATUS BADGE
   ========================================================================== */

function StatusBadge({
  status,
}: {
  status: ReportStatus;
}) {
  const configuration: Record<
    ReportStatus,
    {
      icon: typeof CheckCircle;
      className: string;
    }
  > = {
    Completed: {
      icon: CheckCircle,
      className:
        "bg-green-50 text-green-700 border-green-200",
    },

    Processing: {
      icon: Clock,
      className:
        "bg-blue-50 text-blue-700 border-blue-200",
    },

    "Needs Review": {
      icon: AlertTriangle,
      className:
        "bg-amber-50 text-amber-700 border-amber-200",
    },
  };

  const {
    icon: Icon,
    className,
  } = configuration[status];

  return (
    <span
      className={`
        inline-flex
        items-center
        gap-2
        rounded-full
        border
        px-3
        py-1.5
        text-sm
        font-semibold
        ${className}
      `}
    >
      <Icon
        size={14}
        aria-hidden="true"
      />

      {status}
    </span>
  );
}

/* ============================================================================
   PAGE
   ========================================================================== */

export default function ReportsPage() {
  const [
    reports,
    setReports,
  ] = useState<Report[]>([]);

  const [
    backendStatistics,
    setBackendStatistics,
  ] =
    useState<
      ReportStatistics | undefined
    >();

  const [
    backendTotal,
    setBackendTotal,
  ] = useState<
    number | undefined
  >();

  const [
    search,
    setSearch,
  ] = useState("");

  const [
    statusFilter,
    setStatusFilter,
  ] =
    useState<ReportStatusFilter>(
      "All"
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
  ] = useState<
    string | null
  >(null);

  /* ==========================================================================
     LOAD REPORTS
     ======================================================================== */

  const loadReports =
    useCallback(
      async (
        signal?: AbortSignal
      ) => {
        if (!REPORTS_ENDPOINT) {
          setError(
            "Reports API endpoint is not configured. Set VITE_REPORTS_ENDPOINT in your environment configuration."
          );

          setLoading(false);
          setRefreshing(false);

          return;
        }

        try {
          setError(null);

          const response =
            await fetch(
              REPORTS_ENDPOINT,
              {
                method: "GET",

                headers: {
                  Accept:
                    "application/json",
                },

                credentials:
                  "include",

                signal,
              }
            );

          if (!response.ok) {
            let message =
              `Unable to load reports (${response.status}).`;

            try {
              const body =
                (await response.json()) as ApiErrorResponse;

              message =
                body.detail ??
                body.message ??
                body.error ??
                message;
            } catch {
              // Keep HTTP fallback message.
            }

            throw new Error(
              message
            );
          }

          const payload =
            await response.json();

          const normalized =
            normalizeReportsResponse(
              payload
            );

          setReports(
            normalized.reports
          );

          setBackendStatistics(
            normalized.statistics
          );

          setBackendTotal(
            normalized.total
          );
        } catch (
          requestError
        ) {
          if (
            requestError instanceof
              DOMException &&
            requestError.name ===
              "AbortError"
          ) {
            return;
          }

          const message =
            requestError instanceof
              Error
              ? requestError.message
              : "An unexpected error occurred while loading reports.";

          setError(message);
        } finally {
          setLoading(false);
          setRefreshing(false);
        }
      },
      []
    );

  /* ==========================================================================
     INITIAL LOAD
     ======================================================================== */

  useEffect(() => {
    const controller =
      new AbortController();

    void loadReports(
      controller.signal
    );

    return () =>
      controller.abort();
  }, [loadReports]);

  /* ==========================================================================
     STATISTICS
     ======================================================================== */

  const statistics =
    useMemo(
      () =>
        calculateStatistics(
          reports,
          backendStatistics,
          backendTotal
        ),
      [
        reports,
        backendStatistics,
        backendTotal,
      ]
    );

  /* ==========================================================================
     FILTERED REPORTS
     ======================================================================== */

  const filteredReports =
    useMemo(() => {
      const normalizedSearch =
        search
          .trim()
          .toLowerCase();

      return reports.filter(
        (report) => {
          const matchesSearch =
            !normalizedSearch ||
            report.documentName
              .toLowerCase()
              .includes(
                normalizedSearch
              ) ||
            report.type
              .toLowerCase()
              .includes(
                normalizedSearch
              ) ||
            report.id
              .toLowerCase()
              .includes(
                normalizedSearch
              );

          const matchesStatus =
            statusFilter ===
              "All" ||
            report.status ===
              statusFilter;

          return (
            matchesSearch &&
            matchesStatus
          );
        }
      );
    }, [
      reports,
      search,
      statusFilter,
    ]);

  /* ==========================================================================
     REFRESH
     ======================================================================== */

  const handleRefresh =
    async () => {
      setRefreshing(true);

      await loadReports();
    };

  /* ==========================================================================
     REPORT URL HELPERS
     ======================================================================== */

  const buildReportUrl =
    useCallback(
      (
        basePath:
          | string
          | undefined,
        report: Report
      ) => {
        if (!basePath) {
          return undefined;
        }

        const encodedId =
          encodeURIComponent(
            report.id
          );

        return `${basePath.replace(
          /\/$/,
          ""
        )}/${encodedId}`;
      },
      []
    );

  const getViewUrl =
    useCallback(
      (report: Report) => {
        return (
          report.viewUrl ??
          buildReportUrl(
            REPORT_VIEW_PATH,
            report
          )
        );
      },
      [buildReportUrl]
    );

  const getDownloadUrl =
    useCallback(
      (report: Report) => {
        return (
          report.downloadUrl ??
          buildReportUrl(
            REPORT_DOWNLOAD_PATH,
            report
          )
        );
      },
      [buildReportUrl]
    );

  /* ==========================================================================
     RENDER
     ======================================================================== */

  return (
    <main className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-blue-50">
      <div className="mx-auto max-w-7xl px-6 py-8">
        {/* ==================================================================
            HERO
        =================================================================== */}

        <motion.section
          initial={{
            opacity: 0,
            y: 20,
          }}
          animate={{
            opacity: 1,
            y: 0,
          }}
          className="
            relative
            overflow-hidden
            rounded-[32px]
            bg-gradient-to-br
            from-[#071330]
            via-[#0B1736]
            to-[#183B6B]
            p-8
            shadow-2xl
            lg:p-10
          "
        >
          <div className="absolute right-0 top-0 h-96 w-96 rounded-full bg-[#F4B81A]/10 blur-3xl" />

          <div className="relative z-10">
            <div
              className="
                inline-flex
                items-center
                gap-2
                rounded-full
                border
                border-[#F4B81A]/20
                bg-[#F4B81A]/10
                px-4
                py-2
                text-[#F4B81A]
              "
            >
              <Sparkles
                size={16}
                aria-hidden="true"
              />

              <span className="text-xs font-bold uppercase tracking-[0.25em]">
                AI Document Intelligence
              </span>
            </div>

            <div className="mt-6 flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <h1 className="text-4xl font-black text-white lg:text-5xl">
                  Analysis Reports
                </h1>

                <p className="mt-4 max-w-3xl text-lg leading-relaxed text-slate-300">
                  Review AI-generated
                  immigration analysis,
                  document verification
                  reports, compliance
                  assessments, and
                  processing results from
                  a centralized dashboard.
                </p>
              </div>

              <div className="flex flex-col gap-3 sm:flex-row">
                <button
                  type="button"
                  onClick={
                    handleRefresh
                  }
                  disabled={
                    loading ||
                    refreshing
                  }
                  className="
                    inline-flex
                    items-center
                    justify-center
                    gap-2
                    rounded-2xl
                    border
                    border-white/20
                    bg-white/10
                    px-5
                    py-4
                    font-bold
                    text-white
                    transition
                    hover:bg-white/20
                    disabled:cursor-not-allowed
                    disabled:opacity-50
                  "
                >
                  <RefreshCw
                    size={18}
                    className={
                      refreshing
                        ? "animate-spin"
                        : ""
                    }
                    aria-hidden="true"
                  />

                  Refresh
                </button>

                <Link
                  to={
                    NEW_ANALYSIS_PATH
                  }
                  className="
                    inline-flex
                    items-center
                    justify-center
                    gap-2
                    rounded-2xl
                    bg-gradient-to-r
                    from-[#F4B81A]
                    to-[#FFD96A]
                    px-6
                    py-4
                    font-bold
                    text-[#071330]
                    shadow-lg
                    transition
                    hover:scale-[1.02]
                    focus:outline-none
                    focus:ring-2
                    focus:ring-[#F4B81A]
                  "
                >
                  New Analysis

                  <ArrowRight
                    size={18}
                    aria-hidden="true"
                  />
                </Link>
              </div>
            </div>
          </div>
        </motion.section>

        {/* ==================================================================
            ERROR
        =================================================================== */}

        {error && (
          <div
            role="alert"
            className="
              mt-6
              flex
              flex-col
              gap-4
              rounded-3xl
              border
              border-red-200
              bg-red-50
              p-5
              text-red-800
              sm:flex-row
              sm:items-center
              sm:justify-between
            "
          >
            <div className="flex items-start gap-3">
              <AlertTriangle
                className="mt-0.5 shrink-0 text-red-600"
                aria-hidden="true"
              />

              <div>
                <p className="font-bold">
                  Unable to load reports
                </p>

                <p className="mt-1 text-sm">
                  {error}
                </p>
              </div>
            </div>

            <button
              type="button"
              onClick={
                handleRefresh
              }
              disabled={refreshing}
              className="
                rounded-xl
                bg-red-600
                px-4
                py-2
                text-sm
                font-bold
                text-white
                transition
                hover:bg-red-700
                disabled:opacity-50
              "
            >
              Try Again
            </button>
          </div>
        )}

        {/* ==================================================================
            STATISTICS
        =================================================================== */}

        <div className="mt-8 grid gap-6 md:grid-cols-2 xl:grid-cols-4">
          <StatCard
            icon={
              <FileText
                size={24}
              />
            }
            title="Total Reports"
            value={
              loading
                ? "—"
                : String(
                    statistics.totalReports
                  )
            }
          />

          <StatCard
            icon={
              <CheckCircle
                size={24}
              />
            }
            title="Completed"
            value={
              loading
                ? "—"
                : String(
                    statistics.completedReports
                  )
            }
          />

          <StatCard
            icon={
              <Clock
                size={24}
              />
            }
            title="Processing"
            value={
              loading
                ? "—"
                : String(
                    statistics.processingReports
                  )
            }
          />

          <StatCard
            icon={
              <TrendingUp
                size={24}
              />
            }
            title="Average Score"
            value={
              loading
                ? "—"
                : `${Math.round(
                    statistics.averageScore
                  )}%`
            }
          />
        </div>

        {/* ==================================================================
            SEARCH & FILTERS
        =================================================================== */}

        <section
          aria-label="Report filters"
          className="
            mt-8
            rounded-[28px]
            border
            border-white/70
            bg-white/80
            p-6
            shadow-lg
            backdrop-blur-xl
          "
        >
          <div className="flex flex-col gap-4 lg:flex-row">
            <div
              className="
                flex
                flex-1
                items-center
                gap-3
                rounded-2xl
                border
                border-slate-200
                bg-slate-50
                px-4
              "
            >
              <Search
                size={18}
                className="text-slate-400"
                aria-hidden="true"
              />

              <label
                htmlFor="report-search"
                className="sr-only"
              >
                Search reports
              </label>

              <input
                id="report-search"
                type="search"
                value={search}
                onChange={(event) =>
                  setSearch(
                    event.target.value
                  )
                }
                placeholder="Search reports, document names, or analysis types..."
                autoComplete="off"
                className="
                  w-full
                  bg-transparent
                  py-4
                  outline-none
                "
              />
            </div>

            <div
              className="
                flex
                items-center
                gap-2
                rounded-2xl
                border
                border-slate-200
                bg-white
                px-4
              "
            >
              <Filter
                size={16}
                aria-hidden="true"
              />

              <label
                htmlFor="report-status"
                className="sr-only"
              >
                Filter reports by status
              </label>

              <select
                id="report-status"
                value={statusFilter}
                onChange={(event) =>
                  setStatusFilter(
                    event.target
                      .value as ReportStatusFilter
                  )
                }
                className="
                  bg-transparent
                  py-4
                  outline-none
                "
              >
                <option value="All">
                  All Reports
                </option>

                {REPORT_STATUSES.map(
                  (status) => (
                    <option
                      key={status}
                      value={status}
                    >
                      {status}
                    </option>
                  )
                )}
              </select>
            </div>
          </div>
        </section>

        {/* ==================================================================
            REPORTS TABLE
        =================================================================== */}

        <section
          aria-labelledby="reports-heading"
          className="
            mt-8
            overflow-hidden
            rounded-[32px]
            border
            border-white/70
            bg-white/80
            shadow-xl
            backdrop-blur-xl
          "
        >
          <div className="border-b border-slate-200 p-6">
            <div className="flex items-center justify-between">
              <div>
                <h2
                  id="reports-heading"
                  className="text-2xl font-bold text-[#071330]"
                >
                  Generated Reports
                </h2>

                <p className="mt-1 text-slate-500">
                  {loading
                    ? "Loading reports..."
                    : `${filteredReports.length} report${
                        filteredReports.length !==
                        1
                          ? "s"
                          : ""
                      } found`}
                </p>
              </div>

              <BarChart3
                size={24}
                className="text-[#F4B81A]"
                aria-hidden="true"
              />
            </div>
          </div>

          {loading ? (
            <div
              className="flex min-h-[300px] items-center justify-center"
              aria-live="polite"
            >
              <div className="flex items-center gap-3 text-slate-500">
                <RefreshCw
                  size={20}
                  className="animate-spin"
                  aria-hidden="true"
                />

                Loading reports...
              </div>
            </div>
          ) : filteredReports.length ===
            0 ? (
            <div className="p-16 text-center">
              <ScanSearch
                size={48}
                className="mx-auto text-slate-300"
                aria-hidden="true"
              />

              <h3 className="mt-6 text-xl font-bold text-[#071330]">
                No Reports Found
              </h3>

              <p className="mt-2 text-slate-500">
                {reports.length ===
                0
                  ? "No analysis reports are currently available."
                  : "Try adjusting your search criteria or status filter."}
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[1000px]">
                <caption className="sr-only">
                  Generated analysis reports
                </caption>

                <thead>
                  <tr className="border-b border-slate-200 bg-slate-50">
                    <th
                      scope="col"
                      className="px-6 py-4 text-left text-sm font-semibold text-slate-500"
                    >
                      Document
                    </th>

                    <th
                      scope="col"
                      className="px-6 py-4 text-left text-sm font-semibold text-slate-500"
                    >
                      Analysis Type
                    </th>

                    <th
                      scope="col"
                      className="px-6 py-4 text-left text-sm font-semibold text-slate-500"
                    >
                      Date
                    </th>

                    <th
                      scope="col"
                      className="px-6 py-4 text-left text-sm font-semibold text-slate-500"
                    >
                      Score
                    </th>

                    <th
                      scope="col"
                      className="px-6 py-4 text-left text-sm font-semibold text-slate-500"
                    >
                      Status
                    </th>

                    <th
                      scope="col"
                      className="px-6 py-4 text-right text-sm font-semibold text-slate-500"
                    >
                      Actions
                    </th>
                  </tr>
                </thead>

                <tbody>
                  {filteredReports.map(
                    (
                      report,
                      index
                    ) => {
                      const viewUrl =
                        getViewUrl(
                          report
                        );

                      const downloadUrl =
                        getDownloadUrl(
                          report
                        );

                      return (
                        <motion.tr
                          key={
                            report.id
                          }
                          initial={{
                            opacity: 0,
                            y: 10,
                          }}
                          animate={{
                            opacity: 1,
                            y: 0,
                          }}
                          transition={{
                            delay:
                              index *
                              0.03,
                          }}
                          className="
                            border-b
                            border-slate-100
                            transition
                            hover:bg-slate-50/60
                          "
                        >
                          <td className="px-6 py-5">
                            <div className="flex items-center gap-4">
                              <div
                                className="
                                  flex
                                  h-12
                                  w-12
                                  shrink-0
                                  items-center
                                  justify-center
                                  rounded-2xl
                                  bg-gradient-to-br
                                  from-[#071330]
                                  to-[#183B6B]
                                "
                              >
                                <FileCheck
                                  size={
                                    20
                                  }
                                  className="text-[#F4B81A]"
                                  aria-hidden="true"
                                />
                              </div>

                              <div className="min-w-0">
                                <p className="truncate font-semibold text-[#071330]">
                                  {
                                    report.documentName
                                  }
                                </p>

                                <p className="text-xs text-slate-500">
                                  {
                                    report.id
                                  }
                                </p>
                              </div>
                            </div>
                          </td>

                          <td className="px-6 py-5 font-medium text-[#071330]">
                            {
                              report.type
                            }
                          </td>

                          <td className="px-6 py-5">
                            <div className="flex items-center gap-2 whitespace-nowrap text-slate-600">
                              <Calendar
                                size={
                                  14
                                }
                                aria-hidden="true"
                              />

                              {formatDate(
                                report.date
                              )}
                            </div>
                          </td>

                          <td className="px-6 py-5">
                            {report.score >
                            0 ? (
                              <div className="flex items-center gap-3">
                                <div
                                  className="
                                    h-2
                                    w-24
                                    overflow-hidden
                                    rounded-full
                                    bg-slate-200
                                  "
                                  role="progressbar"
                                  aria-valuenow={
                                    report.score
                                  }
                                  aria-valuemin={
                                    0
                                  }
                                  aria-valuemax={
                                    100
                                  }
                                  aria-label={`Report score ${Math.round(
                                    report.score
                                  )}%`}
                                >
                                  <div
                                    className="
                                      h-full
                                      rounded-full
                                      bg-gradient-to-r
                                      from-[#F4B81A]
                                      to-green-500
                                    "
                                    style={{
                                      width: `${report.score}%`,
                                    }}
                                  />
                                </div>

                                <span className="font-bold text-[#071330]">
                                  {Math.round(
                                    report.score
                                  )}
                                  %
                                </span>
                              </div>
                            ) : (
                              <span className="text-slate-400">
                                —
                              </span>
                            )}
                          </td>

                          <td className="px-6 py-5">
                            <StatusBadge
                              status={
                                report.status
                              }
                            />
                          </td>

                          <td className="px-6 py-5">
                            <div className="flex justify-end gap-2">
                              {viewUrl ? (
                                <a
                                  href={
                                    viewUrl
                                  }
                                  aria-label={`View ${report.documentName}`}
                                  className="
                                    flex
                                    h-10
                                    w-10
                                    items-center
                                    justify-center
                                    rounded-xl
                                    border
                                    border-slate-200
                                    transition
                                    hover:bg-slate-100
                                    focus:outline-none
                                    focus:ring-2
                                    focus:ring-[#F4B81A]
                                  "
                                >
                                  <Eye
                                    size={
                                      18
                                    }
                                    aria-hidden="true"
                                  />
                                </a>
                              ) : (
                                <button
                                  type="button"
                                  disabled
                                  aria-label={`View ${report.documentName} unavailable`}
                                  className="
                                    flex
                                    h-10
                                    w-10
                                    cursor-not-allowed
                                    items-center
                                    justify-center
                                    rounded-xl
                                    border
                                    border-slate-200
                                    text-slate-300
                                  "
                                >
                                  <Eye
                                    size={
                                      18
                                    }
                                    aria-hidden="true"
                                  />
                                </button>
                              )}

                              {downloadUrl ? (
                                <a
                                  href={
                                    downloadUrl
                                  }
                                  download
                                  aria-label={`Download ${report.documentName}`}
                                  className="
                                    flex
                                    h-10
                                    w-10
                                    items-center
                                    justify-center
                                    rounded-xl
                                    border
                                    border-slate-200
                                    transition
                                    hover:bg-slate-100
                                    focus:outline-none
                                    focus:ring-2
                                    focus:ring-[#F4B81A]
                                  "
                                >
                                  <Download
                                    size={
                                      18
                                    }
                                    aria-hidden="true"
                                  />
                                </a>
                              ) : (
                                <button
                                  type="button"
                                  disabled
                                  aria-label={`Download ${report.documentName} unavailable`}
                                  className="
                                    flex
                                    h-10
                                    w-10
                                    cursor-not-allowed
                                    items-center
                                    justify-center
                                    rounded-xl
                                    border
                                    border-slate-200
                                    text-slate-300
                                  "
                                >
                                  <Download
                                    size={
                                      18
                                    }
                                    aria-hidden="true"
                                  />
                                </button>
                              )}
                            </div>
                          </td>
                        </motion.tr>
                      );
                    }
                  )}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </div>
    </main>
  );
}