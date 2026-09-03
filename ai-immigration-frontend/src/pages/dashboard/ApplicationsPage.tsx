"use client";

import React, {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ElementType,
} from "react";

import {
  AlertTriangle,
  ArrowUpRight,
  CheckCircle2,
  ChevronDown,
  Clock3,
  FileCheck,
  FileSearch,
  Filter,
  Globe2,
  Plus,
  RefreshCw,
  Search,
  ShieldCheck,
  Sparkles,
  TrendingUp,
  UserCheck,
  Users,
} from "lucide-react";

/* ============================================================================
   TYPES
============================================================================ */

const APPLICATION_STATUSES = [
  "DRAFT",
  "IN_REVIEW",
  "DOCUMENTS_REQUIRED",
  "APPROVED",
  "REJECTED",
  "SUBMITTED",
] as const;

type ApplicationStatus = (typeof APPLICATION_STATUSES)[number];

type RiskLevel = "Low" | "Medium" | "High";

interface Application {
  id: string;
  applicantName: string;
  country: string;
  visaType: string;
  status: ApplicationStatus;
  completion: number;
  aiScore: number;
  riskLevel: RiskLevel;
  documentsUploaded: number;
  documentsRequired: number;
  lastUpdated: string;
}

interface ApplicationStatistics {
  totalApplications: number;
  approvedApplications: number;
  reviewApplications: number;
  averageAIScore: number;
}

interface AIInsights {
  recommendations: string[];
  complianceRate: number;
  flaggedApplications: number;
}

interface RecentActivity {
  id: string;
  message: string;
  timestamp: string;
  type?: string;
}

interface ApplicationsResponse {
  applications: Application[];
  statistics?: ApplicationStatistics;
  insights?: AIInsights;
  recentActivity?: RecentActivity[];
  total?: number;
}

interface ApiErrorResponse {
  message?: string;
  detail?: string;
  error?: string;
}

/* ============================================================================
   ENVIRONMENT CONFIGURATION
============================================================================ */

const APPLICATIONS_ENDPOINT = String(
  import.meta.env.VITE_APPLICATIONS_ENDPOINT ?? ""
).trim();

const CREATE_APPLICATION_PATH = String(
  import.meta.env.VITE_CREATE_APPLICATION_PATH ?? ""
).trim();

/*
 * Required environment variables:
 *
 * VITE_APPLICATIONS_ENDPOINT=
 * VITE_CREATE_APPLICATION_PATH=
 *
 * Example:
 *
 * VITE_APPLICATIONS_ENDPOINT=http://localhost:8080/api/applications
 * VITE_CREATE_APPLICATION_PATH=/applications/new
 *
 * Production:
 *
 * VITE_APPLICATIONS_ENDPOINT=https://your-api-domain.example/api/applications
 * VITE_CREATE_APPLICATION_PATH=/applications/new
 *
 * Never place secrets inside VITE_* variables.
 */

/* ============================================================================
   HELPERS
============================================================================ */

function clampPercentage(value: unknown): number {
  const numericValue = typeof value === "number" ? value : Number(value);

  if (!Number.isFinite(numericValue)) {
    return 0;
  }

  return Math.min(100, Math.max(0, numericValue));
}

function toNonNegativeInteger(value: unknown): number {
  const numericValue = Number(value);

  if (!Number.isFinite(numericValue) || numericValue < 0) {
    return 0;
  }

  return Math.floor(numericValue);
}

function normalizeStatus(value: unknown): ApplicationStatus {
  const status = String(value ?? "")
    .trim()
    .toUpperCase();

  if (APPLICATION_STATUSES.includes(status as ApplicationStatus)) {
    return status as ApplicationStatus;
  }

  return "DRAFT";
}

function normalizeRiskLevel(value: unknown): RiskLevel {
  const risk = String(value ?? "")
    .trim()
    .toLowerCase();

  switch (risk) {
    case "low":
      return "Low";

    case "medium":
      return "Medium";

    case "high":
      return "High";

    default:
      return "Medium";
  }
}

function getNestedValue(
  source: Record<string, unknown>,
  path: string[]
): unknown {
  let current: unknown = source;

  for (const key of path) {
    if (!current || typeof current !== "object") {
      return undefined;
    }

    current = (current as Record<string, unknown>)[key];
  }

  return current;
}

function firstDefined(
  source: Record<string, unknown>,
  paths: string[][]
): unknown {
  for (const path of paths) {
    const value = getNestedValue(source, path);

    if (value !== undefined && value !== null) {
      return value;
    }
  }

  return undefined;
}

function normalizeApplication(value: unknown): Application | null {
  if (!value || typeof value !== "object") {
    return null;
  }

  const item = value as Record<string, unknown>;

  const id = String(
    firstDefined(item, [
      ["id"],
      ["applicationId"],
      ["application_id"],
    ]) ?? ""
  ).trim();

  const applicantName = String(
    firstDefined(item, [
      ["applicantName"],
      ["applicant_name"],
      ["applicant", "name"],
      ["user", "name"],
      ["user", "fullName"],
    ]) ?? ""
  ).trim();

  if (!id || !applicantName) {
    return null;
  }

  const country = String(
    firstDefined(item, [
      ["country"],
      ["destinationCountry"],
      ["destination_country"],
      ["countryName"],
    ]) ?? ""
  ).trim();

  const visaType = String(
    firstDefined(item, [
      ["visaType"],
      ["visa_type"],
      ["applicationType"],
      ["application_type"],
    ]) ?? ""
  ).trim();

  const status = normalizeStatus(
    firstDefined(item, [
      ["status"],
      ["applicationStatus"],
      ["application_status"],
    ])
  );

  const completion = clampPercentage(
    firstDefined(item, [
      ["completion"],
      ["completionPercentage"],
      ["completion_percentage"],
    ])
  );

  const aiScore = clampPercentage(
    firstDefined(item, [
      ["aiScore"],
      ["aiEligibilityScore"],
      ["ai_eligibility_score"],
      ["eligibilityScore"],
    ])
  );

  const riskLevel = normalizeRiskLevel(
    firstDefined(item, [
      ["riskLevel"],
      ["risk_level"],
      ["risk"],
    ])
  );

  const documentsUploaded = toNonNegativeInteger(
    firstDefined(item, [
      ["documentsUploaded"],
      ["uploadedDocuments"],
      ["documents_uploaded"],
      ["uploaded_documents"],
    ])
  );

  const documentsRequired = toNonNegativeInteger(
    firstDefined(item, [
      ["documentsRequired"],
      ["requiredDocuments"],
      ["documents_required"],
      ["required_documents"],
    ])
  );

  const lastUpdated = String(
    firstDefined(item, [
      ["lastUpdated"],
      ["updatedAt"],
      ["updated_at"],
      ["modifiedAt"],
    ]) ?? ""
  ).trim();

  return {
    id,
    applicantName,
    country: country || "—",
    visaType: visaType || "—",
    status,
    completion,
    aiScore,
    riskLevel,
    documentsUploaded,
    documentsRequired,
    lastUpdated,
  };
}

function normalizeStatistics(
  value: unknown
): ApplicationStatistics | undefined {
  if (!value || typeof value !== "object") {
    return undefined;
  }

  const data = value as Record<string, unknown>;

  return {
    totalApplications: toNonNegativeInteger(
      firstDefined(data, [
        ["totalApplications"],
        ["total"],
        ["total_applications"],
      ])
    ),

    approvedApplications: toNonNegativeInteger(
      firstDefined(data, [
        ["approvedApplications"],
        ["approved"],
        ["approved_applications"],
      ])
    ),

    reviewApplications: toNonNegativeInteger(
      firstDefined(data, [
        ["reviewApplications"],
        ["inReview"],
        ["in_review"],
        ["review_applications"],
      ])
    ),

    averageAIScore: clampPercentage(
      firstDefined(data, [
        ["averageAIScore"],
        ["averageAiScore"],
        ["average_ai_score"],
        ["averageScore"],
        ["average_score"],
      ])
    ),
  };
}

function normalizeInsights(value: unknown): AIInsights | undefined {
  if (!value || typeof value !== "object") {
    return undefined;
  }

  const data = value as Record<string, unknown>;

  const rawRecommendations = firstDefined(data, [
    ["recommendations"],
    ["aiRecommendations"],
    ["ai_recommendations"],
  ]);

  const recommendations = Array.isArray(rawRecommendations)
    ? rawRecommendations
        .map((item) => String(item).trim())
        .filter(Boolean)
    : [];

  return {
    recommendations,

    complianceRate: clampPercentage(
      firstDefined(data, [
        ["complianceRate"],
        ["compliancePercentage"],
        ["compliance_rate"],
        ["compliance_percentage"],
      ])
    ),

    flaggedApplications: toNonNegativeInteger(
      firstDefined(data, [
        ["flaggedApplications"],
        ["riskCount"],
        ["manualReviewCount"],
        ["flagged_applications"],
        ["manual_review_count"],
      ])
    ),
  };
}

function normalizeRecentActivity(
  value: unknown,
  index: number
): RecentActivity | null {
  if (!value || typeof value !== "object") {
    return null;
  }

  const data = value as Record<string, unknown>;

  const message = String(
    firstDefined(data, [
      ["message"],
      ["description"],
      ["activity"],
    ]) ?? ""
  ).trim();

  if (!message) {
    return null;
  }

  const suppliedId = String(
    firstDefined(data, [
      ["id"],
      ["activityId"],
      ["activity_id"],
    ]) ?? ""
  ).trim();

  const id =
    suppliedId || `activity-${index}-${message.slice(0, 32)}`;

  const timestamp = String(
    firstDefined(data, [
      ["timestamp"],
      ["createdAt"],
      ["created_at"],
      ["updatedAt"],
      ["updated_at"],
    ]) ?? ""
  ).trim();

  const typeValue = firstDefined(data, [
    ["type"],
    ["activityType"],
    ["activity_type"],
  ]);

  return {
    id,
    message,
    timestamp,
    type:
      typeValue !== undefined && typeValue !== null
        ? String(typeValue)
        : undefined,
  };
}

function normalizeApplicationsResponse(
  payload: unknown
): ApplicationsResponse {
  if (Array.isArray(payload)) {
    return {
      applications: payload
        .map(normalizeApplication)
        .filter(
          (application): application is Application =>
            application !== null
        ),
    };
  }

  if (!payload || typeof payload !== "object") {
    return {
      applications: [],
    };
  }

  const data = payload as Record<string, unknown>;

  const rawApplications = Array.isArray(data.applications)
    ? data.applications
    : Array.isArray(data.data)
    ? data.data
    : Array.isArray(data.items)
    ? data.items
    : Array.isArray(data.content)
    ? data.content
    : [];

  const applications = rawApplications
    .map(normalizeApplication)
    .filter(
      (application): application is Application =>
        application !== null
    );

  const statistics = normalizeStatistics(
    firstDefined(data, [
      ["statistics"],
      ["stats"],
      ["metrics"],
    ])
  );

  const insights = normalizeInsights(
    firstDefined(data, [
      ["insights"],
      ["aiInsights"],
      ["ai_insights"],
    ])
  );

  const rawRecentActivity = firstDefined(data, [
    ["recentActivity"],
    ["recent_activity"],
    ["activity"],
  ]);

  const recentActivity = Array.isArray(rawRecentActivity)
    ? rawRecentActivity
        .map(normalizeRecentActivity)
        .filter(
          (activity): activity is RecentActivity =>
            activity !== null
        )
    : undefined;

  const totalValue = firstDefined(data, [
    ["total"],
    ["totalElements"],
    ["total_elements"],
  ]);

  const numericTotal = Number(totalValue);

  return {
    applications,
    statistics,
    insights,
    recentActivity,
    total: Number.isFinite(numericTotal)
      ? Math.max(0, Math.floor(numericTotal))
      : undefined,
  };
}

function calculateStatistics(
  applications: Application[],
  backendTotal?: number
): ApplicationStatistics {
  const total = applications.length;

  const approved = applications.filter(
    (application) => application.status === "APPROVED"
  ).length;

  const inReview = applications.filter(
    (application) => application.status === "IN_REVIEW"
  ).length;

  const average =
    total > 0
      ? applications.reduce(
          (sum, application) => sum + application.aiScore,
          0
        ) / total
      : 0;

  return {
    totalApplications: backendTotal ?? total,
    approvedApplications: approved,
    reviewApplications: inReview,
    averageAIScore: Math.round(average),
  };
}

function formatDate(value: string): string {
  if (!value) {
    return "—";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

function getStatusLabel(status: ApplicationStatus): string {
  return status
    .replace(/_/g, " ")
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function getStatusColor(status: ApplicationStatus): string {
  switch (status) {
    case "APPROVED":
      return "bg-emerald-50 text-emerald-700 ring-emerald-200";

    case "REJECTED":
      return "bg-red-50 text-red-700 ring-red-200";

    case "IN_REVIEW":
      return "bg-blue-50 text-blue-700 ring-blue-200";

    case "DOCUMENTS_REQUIRED":
      return "bg-amber-50 text-amber-700 ring-amber-200";

    case "SUBMITTED":
      return "bg-violet-50 text-violet-700 ring-violet-200";

    case "DRAFT":
    default:
      return "bg-slate-50 text-slate-700 ring-slate-200";
  }
}

function getRiskColor(risk: RiskLevel): string {
  switch (risk) {
    case "Low":
      return "text-emerald-600";

    case "Medium":
      return "text-amber-600";

    case "High":
      return "text-red-600";

    default:
      return "text-slate-600";
  }
}

function getScoreColor(score: number): string {
  if (score >= 80) {
    return "text-emerald-600";
  }

  if (score >= 60) {
    return "text-amber-600";
  }

  return "text-red-600";
}

function getScoreBackground(score: number): string {
  if (score >= 80) {
    return "bg-emerald-500";
  }

  if (score >= 60) {
    return "bg-amber-500";
  }

  return "bg-red-500";
}

/* ============================================================================
   STAT CARD
============================================================================ */

function StatCard({
  title,
  value,
  subtitle,
  icon: Icon,
  accent,
}: {
  title: string;
  value: string;
  subtitle: string;
  icon: ElementType;
  accent: string;
}) {
  return (
    <article className="group relative overflow-hidden rounded-[24px] border border-slate-200/80 bg-white p-5 shadow-[0_10px_40px_rgba(15,23,42,0.05)] transition duration-300 hover:-translate-y-0.5 hover:shadow-[0_16px_50px_rgba(15,23,42,0.09)]">
      <div
        className={`absolute inset-x-0 top-0 h-1 ${accent}`}
        aria-hidden="true"
      />

      <div className="flex items-start justify-between gap-4">
        <div className="min-w-0">
          <p className="text-[13px] font-semibold tracking-wide text-slate-500">
            {title}
          </p>

          <p className="mt-3 text-3xl font-black tracking-tight text-slate-950">
            {value}
          </p>

          <p className="mt-2 text-xs font-medium text-slate-500">
            {subtitle}
          </p>
        </div>

        <div className="rounded-2xl bg-slate-950 p-3.5 text-[#F4B81A] shadow-lg transition group-hover:scale-105">
          <Icon size={21} aria-hidden="true" />
        </div>
      </div>
    </article>
  );
}

/* ============================================================================
   STATUS BADGE
============================================================================ */

function StatusBadge({
  status,
}: {
  status: ApplicationStatus;
}) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-[11px] font-bold ring-1 ${getStatusColor(
        status
      )}`}
    >
      <span
        className="h-1.5 w-1.5 rounded-full bg-current"
        aria-hidden="true"
      />

      {getStatusLabel(status)}
    </span>
  );
}

/* ============================================================================
   APPLICATION ROW
============================================================================ */

function ApplicationRow({
  application,
}: {
  application: Application;
}) {
  const completion = clampPercentage(application.completion);

  return (
    <tr className="group border-b border-slate-100 transition hover:bg-slate-50/80">
      <td className="px-5 py-5">
        <div className="flex min-w-[220px] items-center gap-3">
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-slate-950 to-[#183B6B] text-sm font-black text-white shadow-sm">
            {application.applicantName
              .split(" ")
              .map((part) => part[0])
              .join("")
              .slice(0, 2)
              .toUpperCase()}
          </div>

          <div className="min-w-0">
            <p className="truncate font-bold text-slate-900">
              {application.applicantName}
            </p>

            <p className="mt-1 text-xs font-medium text-slate-400">
              {application.id}
            </p>
          </div>
        </div>
      </td>

      <td className="px-5 py-5">
        <div className="flex items-center gap-2 text-sm font-medium text-slate-700">
          <Globe2
            size={16}
            className="text-slate-400"
            aria-hidden="true"
          />

          {application.country}
        </div>
      </td>

      <td className="px-5 py-5">
        <span className="text-sm font-semibold text-slate-700">
          {application.visaType}
        </span>
      </td>

      <td className="px-5 py-5">
        <StatusBadge status={application.status} />
      </td>

      <td className="px-5 py-5">
        <div className="w-44">
          <div className="mb-2 flex items-center justify-between">
            <span className="text-xs font-bold text-slate-700">
              {Math.round(completion)}%
            </span>

            <span className="text-[10px] font-medium uppercase tracking-wide text-slate-400">
              Complete
            </span>
          </div>

          <div
            className="h-2 overflow-hidden rounded-full bg-slate-100"
            role="progressbar"
            aria-valuenow={completion}
            aria-valuemin={0}
            aria-valuemax={100}
            aria-label={`Application completion ${completion}%`}
          >
            <div
              className="h-full rounded-full bg-gradient-to-r from-[#F4B81A] to-[#FFD96A] transition-all duration-500"
              style={{
                width: `${completion}%`,
              }}
            />
          </div>
        </div>
      </td>

      <td className="px-5 py-5">
        <div className="flex items-center gap-2">
          <span
            className={`text-sm font-black ${getScoreColor(
              application.aiScore
            )}`}
          >
            {Math.round(application.aiScore)}%
          </span>

          <Sparkles
            size={14}
            className="text-[#F4B81A]"
            aria-hidden="true"
          />
        </div>
      </td>

      <td className="px-5 py-5">
        <span
          className={`text-sm font-bold ${getRiskColor(
            application.riskLevel
          )}`}
        >
          {application.riskLevel}
        </span>
      </td>

      <td className="px-5 py-5">
        <div className="flex items-center gap-2 text-sm font-semibold text-slate-700">
          <FileCheck
            size={16}
            className="text-slate-400"
            aria-hidden="true"
          />

          {application.documentsUploaded}/
          {application.documentsRequired}
        </div>
      </td>

      <td className="whitespace-nowrap px-5 py-5">
        <span className="text-xs font-medium text-slate-500">
          {formatDate(application.lastUpdated)}
        </span>
      </td>
    </tr>
  );
}

/* ============================================================================
   PAGE
============================================================================ */

export default function ApplicationsPage() {
  const [applications, setApplications] = useState<Application[]>([]);

  const [statistics, setStatistics] =
    useState<ApplicationStatistics>({
      totalApplications: 0,
      approvedApplications: 0,
      reviewApplications: 0,
      averageAIScore: 0,
    });

  const [insights, setInsights] = useState<AIInsights>({
    recommendations: [],
    complianceRate: 0,
    flaggedApplications: 0,
  });

  const [recentActivity, setRecentActivity] = useState<
    RecentActivity[]
  >([]);

  const [search, setSearch] = useState("");

  const [statusFilter, setStatusFilter] = useState<
    ApplicationStatus | "ALL"
  >("ALL");

  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  /* ==========================================================================
     LOAD APPLICATIONS
  ========================================================================== */

  const loadApplications = useCallback(
    async (signal?: AbortSignal) => {
      if (!APPLICATIONS_ENDPOINT) {
        const message =
          "Applications API endpoint is not configured. Set VITE_APPLICATIONS_ENDPOINT in your frontend environment.";

        setError(message);
        setLoading(false);
        setRefreshing(false);

        return;
      }

      try {
        setError(null);

        const response = await fetch(
          APPLICATIONS_ENDPOINT,
          {
            method: "GET",

            headers: {
              Accept: "application/json",
            },

            credentials: "include",

            signal,
          }
        );

        if (!response.ok) {
          let message = `Unable to load applications (${response.status}).`;

          try {
            const body =
              (await response.json()) as ApiErrorResponse;

            message =
              body.detail ??
              body.message ??
              body.error ??
              message;
          } catch {
            // Non-JSON response.
          }

          throw new Error(message);
        }

        const payload =
          (await response.json()) as unknown;

        const normalized =
          normalizeApplicationsResponse(payload);

        setApplications(normalized.applications);

        setStatistics(
          normalized.statistics ??
            calculateStatistics(
              normalized.applications,
              normalized.total
            )
        );

        setInsights(
          normalized.insights ?? {
            recommendations: [],
            complianceRate: 0,
            flaggedApplications: 0,
          }
        );

        setRecentActivity(
          normalized.recentActivity ?? []
        );
      } catch (requestError) {
        if (
          requestError instanceof DOMException &&
          requestError.name === "AbortError"
        ) {
          return;
        }

        const message =
          requestError instanceof Error
            ? requestError.message
            : "An unexpected error occurred while loading applications.";

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
  ========================================================================== */

  useEffect(() => {
    const controller = new AbortController();

    void loadApplications(controller.signal);

    return () => controller.abort();
  }, [loadApplications]);

  /* ==========================================================================
     FILTERING
  ========================================================================== */

  const filteredApplications = useMemo(() => {
    const normalizedSearch = search
      .trim()
      .toLowerCase();

    return applications.filter((application) => {
      const matchesSearch =
        !normalizedSearch ||
        application.applicantName
          .toLowerCase()
          .includes(normalizedSearch) ||
        application.country
          .toLowerCase()
          .includes(normalizedSearch) ||
        application.visaType
          .toLowerCase()
          .includes(normalizedSearch) ||
        application.id
          .toLowerCase()
          .includes(normalizedSearch);

      const matchesStatus =
        statusFilter === "ALL" ||
        application.status === statusFilter;

      return matchesSearch && matchesStatus;
    });
  }, [applications, search, statusFilter]);

  /* ==========================================================================
     DERIVED METRICS
  ========================================================================== */

  const approvalRate = useMemo(() => {
    if (statistics.totalApplications <= 0) {
      return 0;
    }

    return Math.round(
      (statistics.approvedApplications /
        statistics.totalApplications) *
        100
    );
  }, [statistics]);

  const highRiskCount = useMemo(
    () =>
      applications.filter(
        (application) =>
          application.riskLevel === "High"
      ).length,
    [applications]
  );

  /* ==========================================================================
     ACTIONS
  ========================================================================== */

  const handleRefresh = useCallback(async () => {
    setRefreshing(true);

    await loadApplications();
  }, [loadApplications]);

  const handleCreateApplication = useCallback(() => {
    if (!CREATE_APPLICATION_PATH) {
      setError(
        "Create application route is not configured. Set VITE_CREATE_APPLICATION_PATH."
      );

      return;
    }

    window.location.assign(CREATE_APPLICATION_PATH);
  }, []);

  const clearFilters = useCallback(() => {
    setSearch("");
    setStatusFilter("ALL");
  }, []);

  /* ==========================================================================
     RENDER
  ========================================================================== */

  return (
    <main className="min-h-screen bg-[#F6F8FC] text-slate-900">
      <div className="mx-auto max-w-[1800px] px-4 py-5 sm:px-6 lg:px-8 lg:py-8">
        {/* ================================================================
            HERO
        ================================================================= */}

        <section
          aria-labelledby="applications-title"
          className="relative mb-7 overflow-hidden rounded-[30px] bg-[#071330] shadow-[0_25px_70px_rgba(7,19,48,0.18)]"
        >
          <div
            className="absolute -right-32 -top-32 h-80 w-80 rounded-full bg-[#F4B81A]/10 blur-3xl"
            aria-hidden="true"
          />

          <div
            className="absolute -bottom-40 left-1/3 h-80 w-80 rounded-full bg-blue-500/10 blur-3xl"
            aria-hidden="true"
          />

          <div className="relative p-6 sm:p-8 lg:p-10">
            <div className="flex flex-col gap-8 xl:flex-row xl:items-center xl:justify-between">
              <div className="max-w-3xl">
                <div className="mb-5 flex flex-wrap items-center gap-3">
                  <div className="flex items-center gap-2 rounded-full border border-white/10 bg-white/10 px-3 py-1.5 text-xs font-bold uppercase tracking-[0.14em] text-white backdrop-blur">
                    <Sparkles
                      size={14}
                      className="text-[#F4B81A]"
                      aria-hidden="true"
                    />

                    AI Immigration Platform
                  </div>

                  <span className="rounded-full border border-emerald-400/20 bg-emerald-400/10 px-3 py-1.5 text-xs font-semibold text-emerald-200">
                    Live Monitoring
                  </span>
                </div>

                <h1
                  id="applications-title"
                  className="text-3xl font-black tracking-tight text-white sm:text-4xl lg:text-5xl"
                >
                  Applications
                  <span className="text-[#F4B81A]"> Center</span>
                </h1>

                <p className="mt-4 max-w-2xl text-sm leading-7 text-blue-100/80 sm:text-base">
                  Centralize immigration case management,
                  monitor document readiness, evaluate AI
                  eligibility scores, and identify applications
                  that require attention.
                </p>

                <div className="mt-6 flex flex-wrap gap-3">
                  <div className="flex items-center gap-2 rounded-xl border border-white/10 bg-white/5 px-3.5 py-2.5 text-xs font-medium text-blue-100">
                    <ShieldCheck
                      size={15}
                      className="text-emerald-300"
                      aria-hidden="true"
                    />

                    Compliance monitoring
                  </div>

                  <div className="flex items-center gap-2 rounded-xl border border-white/10 bg-white/5 px-3.5 py-2.5 text-xs font-medium text-blue-100">
                    <Sparkles
                      size={15}
                      className="text-[#F4B81A]"
                      aria-hidden="true"
                    />

                    AI-assisted assessment
                  </div>
                </div>
              </div>

              <div className="flex flex-col gap-3 sm:flex-row xl:flex-col">
                <button
                  type="button"
                  onClick={handleRefresh}
                  disabled={loading || refreshing}
                  className="inline-flex min-h-12 items-center justify-center gap-2.5 rounded-2xl border border-white/15 bg-white/10 px-5 text-sm font-bold text-white backdrop-blur transition hover:bg-white/15 focus:outline-none focus:ring-2 focus:ring-white/50 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  <RefreshCw
                    size={18}
                    className={
                      refreshing ? "animate-spin" : ""
                    }
                    aria-hidden="true"
                  />

                  {refreshing
                    ? "Refreshing..."
                    : "Refresh Data"}
                </button>

                <button
                  type="button"
                  onClick={handleCreateApplication}
                  className="inline-flex min-h-12 items-center justify-center gap-2.5 rounded-2xl bg-[#F4B81A] px-5 text-sm font-black text-[#071330] shadow-[0_10px_30px_rgba(244,184,26,0.25)] transition hover:bg-[#FFD96A] focus:outline-none focus:ring-2 focus:ring-[#F4B81A] focus:ring-offset-2 focus:ring-offset-[#071330]"
                >
                  <Plus
                    size={18}
                    aria-hidden="true"
                  />

                  New Application
                </button>
              </div>
            </div>
          </div>
        </section>

        {/* ================================================================
            ERROR
        ================================================================= */}

        {error && (
          <section
            role="alert"
            aria-live="assertive"
            className="mb-7 flex flex-col gap-4 rounded-2xl border border-red-200 bg-red-50 p-5 sm:flex-row sm:items-center sm:justify-between"
          >
            <div className="flex items-start gap-3">
              <div className="rounded-xl bg-red-100 p-2 text-red-600">
                <AlertTriangle
                  size={18}
                  aria-hidden="true"
                />
              </div>

              <div>
                <p className="font-bold text-red-900">
                  Unable to load application data
                </p>

                <p className="mt-1 text-sm leading-6 text-red-700">
                  {error}
                </p>
              </div>
            </div>

            <button
              type="button"
              onClick={handleRefresh}
              disabled={refreshing}
              className="rounded-xl bg-red-600 px-4 py-2.5 text-sm font-bold text-white transition hover:bg-red-700 disabled:opacity-50"
            >
              Try Again
            </button>
          </section>
        )}

        {/* ================================================================
            KPI SECTION
        ================================================================= */}

        <section
          aria-label="Application statistics"
          className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4"
        >
          <StatCard
            title="Total Applications"
            value={
              loading
                ? "—"
                : statistics.totalApplications.toLocaleString()
            }
            subtitle="Applications currently tracked"
            icon={Users}
            accent="bg-blue-500"
          />

          <StatCard
            title="Approved Cases"
            value={
              loading
                ? "—"
                : statistics.approvedApplications.toLocaleString()
            }
            subtitle={
              loading
                ? "Approval performance"
                : `${approvalRate}% approval rate`
            }
            icon={CheckCircle2}
            accent="bg-emerald-500"
          />

          <StatCard
            title="Under Review"
            value={
              loading
                ? "—"
                : statistics.reviewApplications.toLocaleString()
            }
            subtitle="Cases awaiting assessment"
            icon={Clock3}
            accent="bg-amber-500"
          />

          <StatCard
            title="Average AI Score"
            value={
              loading
                ? "—"
                : `${Math.round(
                    statistics.averageAIScore
                  )}%`
            }
            subtitle="Average eligibility confidence"
            icon={TrendingUp}
            accent="bg-[#F4B81A]"
          />
        </section>

        {/* ================================================================
            INSIGHT STRIP
        ================================================================= */}

        <section
          aria-label="Application overview"
          className="mt-5 grid gap-4 lg:grid-cols-3"
        >
          <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-bold uppercase tracking-wider text-slate-400">
                  Compliance
                </p>

                <p className="mt-2 text-2xl font-black text-slate-950">
                  {loading
                    ? "—"
                    : `${Math.round(
                        insights.complianceRate
                      )}%`}
                </p>
              </div>

              <div className="rounded-xl bg-emerald-50 p-3 text-emerald-600">
                <ShieldCheck
                  size={20}
                  aria-hidden="true"
                />
              </div>
            </div>

            <div className="mt-4 h-1.5 overflow-hidden rounded-full bg-slate-100">
              <div
                className="h-full rounded-full bg-emerald-500 transition-all duration-700"
                style={{
                  width: `${clampPercentage(
                    insights.complianceRate
                  )}%`,
                }}
              />
            </div>
          </article>

          <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-bold uppercase tracking-wider text-slate-400">
                  Flagged Applications
                </p>

                <p className="mt-2 text-2xl font-black text-slate-950">
                  {loading
                    ? "—"
                    : insights.flaggedApplications}
                </p>
              </div>

              <div className="rounded-xl bg-amber-50 p-3 text-amber-600">
                <AlertTriangle
                  size={20}
                  aria-hidden="true"
                />
              </div>
            </div>

            <p className="mt-3 text-xs font-medium text-slate-500">
              Applications requiring additional attention.
            </p>
          </article>

          <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-bold uppercase tracking-wider text-slate-400">
                  High Risk
                </p>

                <p className="mt-2 text-2xl font-black text-slate-950">
                  {loading ? "—" : highRiskCount}
                </p>
              </div>

              <div className="rounded-xl bg-red-50 p-3 text-red-600">
                <AlertTriangle
                  size={20}
                  aria-hidden="true"
                />
              </div>
            </div>

            <p className="mt-3 text-xs font-medium text-slate-500">
              High-risk applications identified in the current dataset.
            </p>
          </article>
        </section>

        {/* ================================================================
            FILTER BAR
        ================================================================= */}

        <section
          aria-label="Application filters"
          className="mt-7 rounded-[24px] border border-slate-200 bg-white p-4 shadow-sm sm:p-5"
        >
          <div className="flex flex-col gap-4 xl:flex-row xl:items-center">
            <div className="relative min-w-0 flex-1">
              <Search
                size={18}
                className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-400"
                aria-hidden="true"
              />

              <label
                htmlFor="application-search"
                className="sr-only"
              >
                Search applications
              </label>

              <input
                id="application-search"
                type="search"
                value={search}
                onChange={(event) =>
                  setSearch(event.target.value)
                }
                placeholder="Search applicant, ID, country or visa type..."
                autoComplete="off"
                className="h-12 w-full rounded-xl border border-slate-200 bg-slate-50/70 pl-11 pr-4 text-sm font-medium text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-[#F4B81A] focus:bg-white focus:ring-4 focus:ring-[#F4B81A]/10"
              />
            </div>

            <div className="relative w-full xl:w-64">
              <Filter
                size={17}
                className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-400"
                aria-hidden="true"
              />

              <ChevronDown
                size={16}
                className="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 text-slate-400"
                aria-hidden="true"
              />

              <label
                htmlFor="application-status"
                className="sr-only"
              >
                Filter applications by status
              </label>

              <select
                id="application-status"
                value={statusFilter}
                onChange={(event) =>
                  setStatusFilter(
                    event.target.value as
                      | ApplicationStatus
                      | "ALL"
                  )
                }
                className="h-12 w-full appearance-none rounded-xl border border-slate-200 bg-slate-50/70 pl-11 pr-10 text-sm font-semibold text-slate-700 outline-none transition focus:border-[#F4B81A] focus:bg-white focus:ring-4 focus:ring-[#F4B81A]/10"
              >
                <option value="ALL">
                  All Statuses
                </option>

                {APPLICATION_STATUSES.map(
                  (status) => (
                    <option
                      key={status}
                      value={status}
                    >
                      {getStatusLabel(status)}
                    </option>
                  )
                )}
              </select>
            </div>

            {(search || statusFilter !== "ALL") && (
              <button
                type="button"
                onClick={clearFilters}
                className="h-12 rounded-xl border border-slate-200 px-5 text-sm font-bold text-slate-600 transition hover:bg-slate-50 hover:text-slate-900"
              >
                Clear Filters
              </button>
            )}
          </div>
        </section>

        {/* ================================================================
            APPLICATIONS TABLE
        ================================================================= */}

        <section
          aria-labelledby="applications-table-title"
          className="mt-7 overflow-hidden rounded-[26px] border border-slate-200 bg-white shadow-sm"
        >
          <header className="border-b border-slate-200 px-5 py-5 sm:px-6">
            <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <div className="flex items-center gap-3">
                  <div className="rounded-xl bg-slate-950 p-2.5 text-[#F4B81A]">
                    <FileSearch
                      size={18}
                      aria-hidden="true"
                    />
                  </div>

                  <div>
                    <h2
                      id="applications-table-title"
                      className="text-lg font-black tracking-tight text-slate-950"
                    >
                      Immigration Applications
                    </h2>

                    <p className="mt-1 text-xs font-medium text-slate-500">
                      {loading
                        ? "Loading application records..."
                        : `${filteredApplications.length} application${
                            filteredApplications.length ===
                            1
                              ? ""
                              : "s"
                          } displayed`}
                    </p>
                  </div>
                </div>
              </div>

              {!loading && (
                <div className="flex items-center gap-2 text-xs font-semibold text-slate-500">
                  <span className="h-2 w-2 rounded-full bg-emerald-500" />

                  {applications.length} records loaded
                </div>
              )}
            </div>
          </header>

          <div className="overflow-x-auto">
            {loading ? (
              <div
                className="flex min-h-[360px] items-center justify-center"
                aria-live="polite"
              >
                <div className="flex flex-col items-center">
                  <div className="rounded-2xl bg-slate-100 p-4">
                    <RefreshCw
                      size={25}
                      className="animate-spin text-[#183B6B]"
                      aria-hidden="true"
                    />
                  </div>

                  <p className="mt-4 text-sm font-bold text-slate-700">
                    Loading applications
                  </p>

                  <p className="mt-1 text-xs text-slate-400">
                    Fetching the latest application data...
                  </p>
                </div>
              </div>
            ) : filteredApplications.length === 0 ? (
              <div className="flex min-h-[360px] flex-col items-center justify-center px-6 py-12 text-center">
                <div className="rounded-2xl bg-slate-100 p-4">
                  <FileSearch
                    size={30}
                    className="text-slate-400"
                    aria-hidden="true"
                  />
                </div>

                <h3 className="mt-5 text-lg font-black text-slate-900">
                  No applications found
                </h3>

                <p className="mt-2 max-w-md text-sm leading-6 text-slate-500">
                  {applications.length === 0
                    ? "There are currently no application records available."
                    : "No applications match your current search or status filter."}
                </p>

                {(search || statusFilter !== "ALL") && (
                  <button
                    type="button"
                    onClick={clearFilters}
                    className="mt-5 rounded-xl bg-slate-950 px-5 py-2.5 text-sm font-bold text-white transition hover:bg-slate-800"
                  >
                    Reset Filters
                  </button>
                )}
              </div>
            ) : (
              <table className="w-full min-w-[1350px]">
                <caption className="sr-only">
                  Immigration applications
                </caption>

                <thead>
                  <tr className="border-b border-slate-200 bg-slate-50/80">
                    {[
                      "Application",
                      "Country",
                      "Visa Type",
                      "Status",
                      "Completion",
                      "AI Score",
                      "Risk",
                      "Documents",
                      "Updated",
                    ].map((heading) => (
                      <th
                        key={heading}
                        scope="col"
                        className="px-5 py-4 text-left text-[10px] font-black uppercase tracking-[0.12em] text-slate-400"
                      >
                        {heading}
                      </th>
                    ))}
                  </tr>
                </thead>

                <tbody>
                  {filteredApplications.map(
                    (application) => (
                      <ApplicationRow
                        key={application.id}
                        application={application}
                      />
                    )
                  )}
                </tbody>
              </table>
            )}
          </div>
        </section>

        {/* ================================================================
            AI INTELLIGENCE
        ================================================================= */}

        <section
          aria-labelledby="ai-intelligence-title"
          className="mt-7"
        >
          <div className="mb-4 flex items-center justify-between">
            <div>
              <div className="flex items-center gap-2">
                <Sparkles
                  size={19}
                  className="text-[#F4B81A]"
                  aria-hidden="true"
                />

                <h2
                  id="ai-intelligence-title"
                  className="text-xl font-black tracking-tight text-slate-950"
                >
                  AI Intelligence
                </h2>
              </div>

              <p className="mt-1 text-sm text-slate-500">
                Automated insights from your current application portfolio.
              </p>
            </div>
          </div>

          <div className="grid gap-5 xl:grid-cols-3">
            {/* Recommendations */}

            <article className="rounded-[24px] border border-slate-200 bg-white p-6 shadow-sm">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="rounded-xl bg-amber-50 p-2.5 text-[#D99A00]">
                    <Sparkles
                      size={18}
                      aria-hidden="true"
                    />
                  </div>

                  <div>
                    <h3 className="font-black text-slate-900">
                      Recommendations
                    </h3>

                    <p className="text-xs text-slate-400">
                      AI-generated actions
                    </p>
                  </div>
                </div>

                <ArrowUpRight
                  size={17}
                  className="text-slate-300"
                  aria-hidden="true"
                />
              </div>

              <div className="mt-6">
                {loading ? (
                  <div className="flex items-center gap-2 text-sm text-slate-500">
                    <RefreshCw
                      size={15}
                      className="animate-spin"
                      aria-hidden="true"
                    />

                    Analyzing applications...
                  </div>
                ) : insights.recommendations.length ===
                  0 ? (
                  <div className="rounded-xl bg-slate-50 p-4 text-sm text-slate-500">
                    No AI recommendations are currently available.
                  </div>
                ) : (
                  <ul className="space-y-3">
                    {insights.recommendations.map(
                      (recommendation, index) => (
                        <li
                          key={`${index}-${recommendation}`}
                          className="flex gap-3 rounded-xl border border-slate-100 bg-slate-50/70 p-3"
                        >
                          <span
                            className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-[#F4B81A]"
                            aria-hidden="true"
                          />

                          <span className="text-sm leading-6 text-slate-600">
                            {recommendation}
                          </span>
                        </li>
                      )
                    )}
                  </ul>
                )}
              </div>
            </article>

            {/* Compliance */}

            <article className="rounded-[24px] border border-slate-200 bg-white p-6 shadow-sm">
              <div className="flex items-center gap-3">
                <div className="rounded-xl bg-emerald-50 p-2.5 text-emerald-600">
                  <ShieldCheck
                    size={18}
                    aria-hidden="true"
                  />
                </div>

                <div>
                  <h3 className="font-black text-slate-900">
                    Compliance Status
                  </h3>

                  <p className="text-xs text-slate-400">
                    Portfolio compliance
                  </p>
                </div>
              </div>

              <div className="mt-7 flex items-end justify-between gap-4">
                <p className="text-5xl font-black tracking-tight text-emerald-600">
                  {loading
                    ? "—"
                    : `${Math.round(
                        insights.complianceRate
                      )}%`}
                </p>

                <span className="mb-1 inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2.5 py-1 text-[10px] font-bold text-emerald-700">
                  <TrendingUp size={12} />

                  Monitoring
                </span>
              </div>

              <div className="mt-5 h-2 overflow-hidden rounded-full bg-slate-100">
                <div
                  className="h-full rounded-full bg-emerald-500 transition-all duration-700"
                  style={{
                    width: `${clampPercentage(
                      insights.complianceRate
                    )}%`,
                  }}
                />
              </div>

              <p className="mt-4 text-sm leading-6 text-slate-500">
                Current compliance assessment based on available application and document data.
              </p>
            </article>

            {/* Risk */}

            <article className="rounded-[24px] border border-slate-200 bg-white p-6 shadow-sm">
              <div className="flex items-center gap-3">
                <div className="rounded-xl bg-red-50 p-2.5 text-red-600">
                  <AlertTriangle
                    size={18}
                    aria-hidden="true"
                  />
                </div>

                <div>
                  <h3 className="font-black text-slate-900">
                    Risk Monitoring
                  </h3>

                  <p className="text-xs text-slate-400">
                    Manual review signals
                  </p>
                </div>
              </div>

              <div className="mt-7 flex items-end justify-between">
                <p className="text-5xl font-black tracking-tight text-red-600">
                  {loading
                    ? "—"
                    : insights.flaggedApplications}
                </p>

                <span className="mb-1 rounded-full bg-red-50 px-2.5 py-1 text-[10px] font-bold text-red-700">
                  Flagged
                </span>
              </div>

              <div className="mt-5 grid grid-cols-3 gap-2">
                <div className="rounded-xl bg-emerald-50 p-3 text-center">
                  <p className="text-lg font-black text-emerald-700">
                    {loading
                      ? "—"
                      : applications.filter(
                          (item) =>
                            item.riskLevel ===
                            "Low"
                        ).length}
                  </p>

                  <p className="text-[10px] font-bold uppercase text-emerald-600">
                    Low
                  </p>
                </div>

                <div className="rounded-xl bg-amber-50 p-3 text-center">
                  <p className="text-lg font-black text-amber-700">
                    {loading
                      ? "—"
                      : applications.filter(
                          (item) =>
                            item.riskLevel ===
                            "Medium"
                        ).length}
                  </p>

                  <p className="text-[10px] font-bold uppercase text-amber-600">
                    Medium
                  </p>
                </div>

                <div className="rounded-xl bg-red-50 p-3 text-center">
                  <p className="text-lg font-black text-red-700">
                    {loading ? "—" : highRiskCount}
                  </p>

                  <p className="text-[10px] font-bold uppercase text-red-600">
                    High
                  </p>
                </div>
              </div>

              <p className="mt-4 text-sm leading-6 text-slate-500">
                Applications requiring closer assessment or manual intervention.
              </p>
            </article>
          </div>
        </section>

        {/* ================================================================
            RECENT ACTIVITY
        ================================================================= */}

        <section
          aria-labelledby="recent-activity-title"
          className="mt-7 rounded-[24px] border border-slate-200 bg-white p-6 shadow-sm"
        >
          <div className="flex items-center justify-between">
            <div>
              <div className="flex items-center gap-3">
                <div className="rounded-xl bg-slate-950 p-2.5 text-[#F4B81A]">
                  <Clock3
                    size={18}
                    aria-hidden="true"
                  />
                </div>

                <div>
                  <h2
                    id="recent-activity-title"
                    className="font-black text-slate-950"
                  >
                    Recent Activity
                  </h2>

                  <p className="mt-1 text-xs text-slate-400">
                    Latest application events
                  </p>
                </div>
              </div>
            </div>
          </div>

          <div className="mt-6">
            {loading ? (
              <div className="flex items-center gap-2 rounded-xl bg-slate-50 p-5 text-sm text-slate-500">
                <RefreshCw
                  size={17}
                  className="animate-spin"
                  aria-hidden="true"
                />

                Loading recent activity...
              </div>
            ) : recentActivity.length === 0 ? (
              <div className="rounded-xl bg-slate-50 p-8 text-center">
                <UserCheck
                  size={30}
                  className="mx-auto text-slate-300"
                  aria-hidden="true"
                />

                <p className="mt-3 text-sm font-semibold text-slate-500">
                  No recent activity available.
                </p>
              </div>
            ) : (
              <div className="relative space-y-3">
                <div
                  className="absolute bottom-5 left-[17px] top-5 w-px bg-slate-200"
                  aria-hidden="true"
                />

                {recentActivity.map(
                  (activity) => (
                    <article
                      key={activity.id}
                      className="relative flex items-start gap-4 rounded-2xl border border-slate-100 bg-slate-50/60 p-4 transition hover:bg-slate-50"
                    >
                      <div className="relative z-10 flex h-9 w-9 shrink-0 items-center justify-center rounded-xl border-4 border-white bg-[#F4B81A] text-[#071330] shadow-sm">
                        <UserCheck
                          size={14}
                          aria-hidden="true"
                        />
                      </div>

                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-semibold leading-6 text-slate-700">
                          {activity.message}
                        </p>

                        {activity.timestamp && (
                          <p className="mt-1 text-xs font-medium text-slate-400">
                            {formatDate(
                              activity.timestamp
                            )}
                          </p>
                        )}
                      </div>

                      {activity.type && (
                        <span className="hidden rounded-full bg-white px-2.5 py-1 text-[10px] font-bold uppercase tracking-wide text-slate-400 ring-1 ring-slate-200 sm:inline-flex">
                          {activity.type}
                        </span>
                      )}
                    </article>
                  )
                )}
              </div>
            )}
          </div>
        </section>
      </div>
    </main>
  );
}