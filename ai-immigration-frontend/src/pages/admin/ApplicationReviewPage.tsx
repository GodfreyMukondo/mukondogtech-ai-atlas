"use client";

import React, {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ChangeEvent,
  type ElementType,
} from "react";

import axios from "axios";
import { useNavigate } from "react-router-dom";

import { appConfig } from "../../config/appConfig";

import {
  AlertTriangle,
  CheckCircle2,
  Clock3,
  Eye,
  FileCheck2,
  FileSearch,
  Filter,
  Globe2,
  RefreshCw,
  Search,
  ShieldAlert,
  UserCheck,
  X,
  XCircle,
} from "lucide-react";

/**
 * ============================================================
 * APPLICATION REVIEW PAGE
 * ============================================================
 *
 * Production responsibilities:
 *
 * - Load applications from the backend
 * - Display application statistics
 * - Search applications
 * - Filter by status
 * - Advanced filtering
 * - View application
 * - Approve application
 * - Review documents
 * - Refresh data
 * - Handle API errors
 * - Provide loading states
 * - Responsive administration UI
 *
 * Expected API:
 *
 * GET   /api/admin/applications
 * PATCH /api/admin/applications/{id}/approve
 *
 * Expected response:
 *
 * {
 *   content: Application[],
 *   statistics: ApplicationStatistics
 * }
 *
 * ============================================================
 */

/* ============================================================
   TYPES
============================================================ */

export type ApplicationStatus =
  | "PENDING"
  | "UNDER_REVIEW"
  | "APPROVED"
  | "REJECTED"
  | "MORE_INFO_REQUIRED";

export type RiskLevel =
  | "LOW"
  | "MEDIUM"
  | "HIGH";

export interface Application {
  id: string;
  applicantName: string;
  country: string;
  visaType: string;
  submittedAt: string;
  status: ApplicationStatus;
  riskLevel: RiskLevel;
  aiConfidence: number;
  documentCount: number;
}

export interface ApplicationStatistics {
  pendingReviews: number;
  approvedCases: number;
  rejectedCases: number;
  fraudAlerts: number;
}

interface ApiError {
  message: string;
}

interface ApplicationResponse {
  content?: Application[];
  statistics?: ApplicationStatistics;
}

interface StatusOption {
  label: string;
  value: ApplicationStatus | "ALL";
}

interface StatCardProps {
  title: string;
  value: number;
  icon: ElementType;
  description: string;
  iconClassName: string;
  iconBackgroundClassName: string;
  valueClassName?: string;
}

interface FilterState {
  country: string;
  visaType: string;
  riskLevel: RiskLevel | "ALL";
}

/* ============================================================
   CONSTANTS
============================================================ */

const STATUS_OPTIONS: StatusOption[] = [
  {
    label: "All Applications",
    value: "ALL",
  },
  {
    label: "Pending",
    value: "PENDING",
  },
  {
    label: "Under Review",
    value: "UNDER_REVIEW",
  },
  {
    label: "Approved",
    value: "APPROVED",
  },
  {
    label: "Rejected",
    value: "REJECTED",
  },
  {
    label: "More Information Required",
    value: "MORE_INFO_REQUIRED",
  },
];

const INITIAL_STATISTICS: ApplicationStatistics = {
  pendingReviews: 0,
  approvedCases: 0,
  rejectedCases: 0,
  fraudAlerts: 0,
};

const INITIAL_FILTERS: FilterState = {
  country: "",
  visaType: "",
  riskLevel: "ALL",
};

/* ============================================================
   HELPERS
============================================================ */

function statusColor(
  status: ApplicationStatus,
): string {
  switch (status) {
    case "APPROVED":
      return "border-emerald-200 bg-emerald-50 text-emerald-700";

    case "REJECTED":
      return "border-red-200 bg-red-50 text-red-700";

    case "UNDER_REVIEW":
      return "border-blue-200 bg-blue-50 text-blue-700";

    case "MORE_INFO_REQUIRED":
      return "border-amber-200 bg-amber-50 text-amber-700";

    case "PENDING":
    default:
      return "border-slate-200 bg-slate-50 text-slate-700";
  }
}

function riskColor(
  risk: RiskLevel,
): string {
  switch (risk) {
    case "LOW":
      return "border-emerald-200 bg-emerald-50 text-emerald-700";

    case "MEDIUM":
      return "border-amber-200 bg-amber-50 text-amber-700";

    case "HIGH":
      return "border-red-200 bg-red-50 text-red-700";

    default:
      return "border-slate-200 bg-slate-50 text-slate-700";
  }
}

function formatStatus(
  status: ApplicationStatus,
): string {
  return status
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/\b\w/g, (letter) =>
      letter.toUpperCase(),
    );
}

function formatRisk(
  risk: RiskLevel,
): string {
  return (
    risk.charAt(0) +
    risk.slice(1).toLowerCase()
  );
}

function formatDate(
  value: string,
): string {
  if (!value) {
    return "—";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat(
    "en-GB",
    {
      day: "2-digit",
      month: "short",
      year: "numeric",
    },
  ).format(date);
}

function getConfidenceColor(
  confidence: number,
): string {
  if (confidence >= 85) {
    return "text-emerald-600";
  }

  if (confidence >= 65) {
    return "text-amber-600";
  }

  return "text-red-600";
}

function getInitials(
  name: string,
): string {
  return name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map(
      (part) =>
        part.charAt(0).toUpperCase(),
    )
    .join("");
}

/* ============================================================
   STAT CARD
============================================================ */

function StatCard({
  title,
  value,
  icon: Icon,
  description,
  iconClassName,
  iconBackgroundClassName,
  valueClassName = "text-[#0B1736]",
}: StatCardProps) {
  return (
    <article
      className="
        group
        relative
        overflow-hidden
        rounded-3xl
        border
        border-slate-200/80
        bg-white
        p-5
        shadow-[0_12px_40px_rgba(11,23,54,0.06)]
        transition-all
        duration-300
        hover:-translate-y-1
        hover:shadow-[0_20px_50px_rgba(11,23,54,0.10)]
        sm:p-6
      "
    >
      {/* Decorative background */}

      <div
        className="
          pointer-events-none
          absolute
          -right-10
          -top-10
          h-28
          w-28
          rounded-full
          bg-slate-50
          opacity-80
          transition-transform
          duration-500
          group-hover:scale-150
        "
        aria-hidden="true"
      />

      <div className="relative flex items-start justify-between gap-4">
        <div className="min-w-0">
          <p
            className="
              text-xs
              font-bold
              uppercase
              tracking-[0.16em]
              text-slate-400
            "
          >
            {title}
          </p>

          <h3
            className={`
              mt-2
              text-3xl
              font-black
              tracking-tight
              ${valueClassName}
            `}
          >
            {value.toLocaleString()}
          </h3>

          <p
            className="
              mt-2
              text-xs
              font-medium
              leading-relaxed
              text-slate-500
            "
          >
            {description}
          </p>
        </div>

        <div
          className={`
            flex
            h-12
            w-12
            shrink-0
            items-center
            justify-center
            rounded-2xl
            ${iconBackgroundClassName}
          `}
        >
          <Icon
            size={23}
            className={iconClassName}
            aria-hidden="true"
          />
        </div>
      </div>
    </article>
  );
}

/* ============================================================
   MAIN COMPONENT
============================================================ */

export default function ApplicationReviewPage() {
  const navigate = useNavigate();

  /* ==========================================================
     STATE
  ========================================================== */

  const [
    applications,
    setApplications,
  ] = useState<Application[]>([]);

  const [
    statistics,
    setStatistics,
  ] = useState<ApplicationStatistics>(
    INITIAL_STATISTICS,
  );

  const [
    search,
    setSearch,
  ] = useState("");

  const [
    statusFilter,
    setStatusFilter,
  ] = useState<
    ApplicationStatus | "ALL"
  >("ALL");

  const [
    advancedFilters,
    setAdvancedFilters,
  ] = useState<FilterState>(
    INITIAL_FILTERS,
  );

  const [
    showAdvancedFilters,
    setShowAdvancedFilters,
  ] = useState(false);

  const [
    loading,
    setLoading,
  ] = useState(false);

  const [
    approvingId,
    setApprovingId,
  ] = useState<string | null>(null);

  const [
    error,
    setError,
  ] = useState<ApiError | null>(null);

  /* ==========================================================
     LOAD APPLICATIONS
  ========================================================== */

  const loadApplications =
    useCallback(async () => {
      try {
        setLoading(true);
        setError(null);

        const response =
          await axios.get<ApplicationResponse>(
            `${appConfig.apiUrl}/admin/applications`,
          );

        const data = response.data;

        setApplications(
          Array.isArray(data.content)
            ? data.content
            : [],
        );

        setStatistics(
          data.statistics ?? INITIAL_STATISTICS,
        );
      } catch (requestError: unknown) {
        console.error(
          "Failed loading applications:",
          requestError,
        );

        let message =
          "Unable to load applications. Please try again.";

        if (axios.isAxiosError(requestError)) {
          message =
            requestError.response?.data?.message ??
            requestError.message ??
            message;
        }

        setError({
          message,
        });
      } finally {
        setLoading(false);
      }
    }, []);

  /* ==========================================================
     INITIAL LOAD
  ========================================================== */

  useEffect(() => {
    void loadApplications();
  }, [loadApplications]);

  /* ==========================================================
     FILTER APPLICATIONS
  ========================================================== */

  const filteredApplications =
    useMemo(() => {
      const keyword =
        search.trim().toLowerCase();

      const country =
        advancedFilters.country
          .trim()
          .toLowerCase();

      const visaType =
        advancedFilters.visaType
          .trim()
          .toLowerCase();

      return applications.filter(
        (application) => {
          const searchableText = [
            application.id,
            application.applicantName,
            application.country,
            application.visaType,
            application.status,
            application.riskLevel,
          ]
            .join(" ")
            .toLowerCase();

          const matchesSearch =
            keyword === "" ||
            searchableText.includes(keyword);

          const matchesStatus =
            statusFilter === "ALL" ||
            application.status ===
              statusFilter;

          const matchesCountry =
            country === "" ||
            application.country
              .toLowerCase()
              .includes(country);

          const matchesVisaType =
            visaType === "" ||
            application.visaType
              .toLowerCase()
              .includes(visaType);

          const matchesRisk =
            advancedFilters.riskLevel ===
              "ALL" ||
            application.riskLevel ===
              advancedFilters.riskLevel;

          return (
            matchesSearch &&
            matchesStatus &&
            matchesCountry &&
            matchesVisaType &&
            matchesRisk
          );
        },
      );
    }, [
      applications,
      search,
      statusFilter,
      advancedFilters,
    ]);

  /* ==========================================================
     INPUT HANDLERS
  ========================================================== */

  const handleSearchChange =
    useCallback(
      (
        event: ChangeEvent<HTMLInputElement>,
      ) => {
        setSearch(event.target.value);
      },
      [],
    );

  const handleStatusChange =
    useCallback(
      (
        event: ChangeEvent<HTMLSelectElement>,
      ) => {
        const value =
          event.target.value as
            | ApplicationStatus
            | "ALL";

        setStatusFilter(value);
      },
      [],
    );

  const handleCountryChange =
    useCallback(
      (
        event: ChangeEvent<HTMLInputElement>,
      ) => {
        setAdvancedFilters(
          (previous) => ({
            ...previous,
            country:
              event.target.value,
          }),
        );
      },
      [],
    );

  const handleVisaTypeChange =
    useCallback(
      (
        event: ChangeEvent<HTMLInputElement>,
      ) => {
        setAdvancedFilters(
          (previous) => ({
            ...previous,
            visaType:
              event.target.value,
          }),
        );
      },
      [],
    );

  const handleRiskChange =
    useCallback(
      (
        event: ChangeEvent<HTMLSelectElement>,
      ) => {
        setAdvancedFilters(
          (previous) => ({
            ...previous,
            riskLevel:
              event.target.value as
                | RiskLevel
                | "ALL",
          }),
        );
      },
      [],
    );

  /* ==========================================================
     CLEAR FILTERS
  ========================================================== */

  const clearFilters =
    useCallback(() => {
      setSearch("");
      setStatusFilter("ALL");
      setAdvancedFilters(
        INITIAL_FILTERS,
      );
    }, []);

  const hasActiveFilters =
    search.trim() !== "" ||
    statusFilter !== "ALL" ||
    advancedFilters.country.trim() !== "" ||
    advancedFilters.visaType.trim() !== "" ||
    advancedFilters.riskLevel !== "ALL";

  /* ==========================================================
     VIEW APPLICATION
  ========================================================== */

  const handleViewApplication =
    useCallback(
      (id: string) => {
        navigate(
          `/admin/applications/${encodeURIComponent(id)}`,
        );
      },
      [navigate],
    );

  /* ==========================================================
     APPROVE APPLICATION
  ========================================================== */

  const handleApproveApplication =
    useCallback(
      async (id: string) => {
        if (approvingId) {
          return;
        }

        const confirmed =
          window.confirm(
            "Are you sure you want to approve this application?",
          );

        if (!confirmed) {
          return;
        }

        try {
          setApprovingId(id);
          setError(null);

          await axios.patch(
            `${appConfig.apiUrl}/admin/applications/${encodeURIComponent(id)}/approve`,
          );

          await loadApplications();
        } catch (requestError: unknown) {
          console.error(
            "Failed approving application:",
            requestError,
          );

          let message =
            "Failed to approve application. Please try again.";

          if (axios.isAxiosError(requestError)) {
            message =
              requestError.response?.data?.message ??
              requestError.message ??
              message;
          }

          setError({
            message,
          });
        } finally {
          setApprovingId(null);
        }
      },
      [
        approvingId,
        loadApplications,
      ],
    );

  /* ==========================================================
     REVIEW DOCUMENTS
  ========================================================== */

  const handleReviewDocuments =
    useCallback(
      (id: string) => {
        navigate(
          `/admin/applications/${encodeURIComponent(id)}/documents`,
        );
      },
      [navigate],
    );

  /* ==========================================================
     RENDER
  ========================================================== */

  return (
    <section
      className="
        min-h-full
        space-y-6
        pb-10
      "
    >
      {/* ======================================================
          HERO
      ====================================================== */}

      <div
        className="
          relative
          overflow-hidden
          rounded-[2rem]
          border
          border-slate-800/20
          bg-gradient-to-br
          from-[#07152F]
          via-[#0B1736]
          to-[#172554]
          p-6
          text-white
          shadow-[0_24px_70px_rgba(11,23,54,0.20)]
          sm:p-8
        "
      >
        {/* Decorative circles */}

        <div
          className="
            pointer-events-none
            absolute
            -right-24
            -top-28
            h-72
            w-72
            rounded-full
            bg-[#F4B81A]/10
            blur-2xl
          "
          aria-hidden="true"
        />

        <div
          className="
            pointer-events-none
            absolute
            -bottom-32
            right-1/4
            h-64
            w-64
            rounded-full
            bg-blue-400/10
            blur-3xl
          "
          aria-hidden="true"
        />

        <div className="relative">
          <div
            className="
              flex
              flex-col
              gap-6
              lg:flex-row
              lg:items-end
              lg:justify-between
            "
          >
            <div className="max-w-3xl">
              <div
                className="
                  mb-4
                  inline-flex
                  items-center
                  gap-2
                  rounded-full
                  border
                  border-white/10
                  bg-white/10
                  px-3
                  py-1.5
                  text-xs
                  font-bold
                  uppercase
                  tracking-[0.15em]
                  text-[#F8D56B]
                  backdrop-blur-md
                "
              >
                <ShieldAlert
                  size={14}
                  aria-hidden="true"
                />

                AI-Powered Review Center
              </div>

              <h2
                className="
                  text-3xl
                  font-black
                  tracking-tight
                  sm:text-4xl
                "
              >
                Application Review
              </h2>

              <p
                className="
                  mt-3
                  max-w-2xl
                  text-sm
                  leading-7
                  text-slate-300
                  sm:text-base
                "
              >
                Review immigration applications,
                evaluate AI risk signals, verify
                supporting documents, and manage
                application decisions from one
                centralized workspace.
              </p>
            </div>

            <button
              type="button"
              onClick={() =>
                void loadApplications()
              }
              disabled={loading}
              className="
                inline-flex
                h-12
                items-center
                justify-center
                gap-2
                rounded-2xl
                border
                border-[#F4B81A]/40
                bg-[#F4B81A]
                px-5
                text-sm
                font-black
                text-[#0B1736]
                shadow-lg
                shadow-[#F4B81A]/10
                transition-all
                duration-200
                hover:bg-[#FFD15A]
                hover:shadow-xl
                hover:shadow-[#F4B81A]/20
                focus:outline-none
                focus:ring-2
                focus:ring-[#F4B81A]
                focus:ring-offset-2
                focus:ring-offset-[#0B1736]
                disabled:cursor-not-allowed
                disabled:opacity-60
              "
            >
              <RefreshCw
                size={18}
                className={
                  loading
                    ? "animate-spin"
                    : ""
                }
                aria-hidden="true"
              />

              {loading
                ? "Refreshing..."
                : "Refresh Applications"}
            </button>
          </div>
        </div>
      </div>

      {/* ======================================================
          ERROR
      ====================================================== */}

      {error && (
        <div
          role="alert"
          className="
            flex
            flex-col
            gap-4
            rounded-2xl
            border
            border-red-200
            bg-gradient-to-r
            from-red-50
            to-orange-50
            p-4
            shadow-sm
            sm:flex-row
            sm:items-center
            sm:justify-between
          "
        >
          <div
            className="
              flex
              min-w-0
              items-start
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
                bg-red-100
                text-red-600
              "
            >
              <AlertTriangle
                size={19}
                aria-hidden="true"
              />
            </div>

            <div>
              <p
                className="
                  text-sm
                  font-black
                  text-red-800
                "
              >
                Unable to complete request
              </p>

              <p
                className="
                  mt-0.5
                  text-sm
                  text-red-700
                "
              >
                {error.message}
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={() =>
              void loadApplications()
            }
            className="
              inline-flex
              shrink-0
              items-center
              justify-center
              gap-2
              rounded-xl
              border
              border-red-200
              bg-white
              px-4
              py-2.5
              text-sm
              font-bold
              text-red-700
              transition
              hover:bg-red-50
              focus:outline-none
              focus:ring-2
              focus:ring-red-400
            "
          >
            <RefreshCw
              size={16}
              aria-hidden="true"
            />

            Try Again
          </button>
        </div>
      )}

      {/* ======================================================
          STATISTICS
      ====================================================== */}

      <div
        className="
          grid
          gap-4
          sm:grid-cols-2
          xl:grid-cols-4
        "
      >
        <StatCard
          title="Pending Reviews"
          value={
            statistics.pendingReviews
          }
          icon={Clock3}
          description="Applications waiting for administrative review."
          iconClassName="text-blue-600"
          iconBackgroundClassName="bg-blue-50"
        />

        <StatCard
          title="Approved Cases"
          value={
            statistics.approvedCases
          }
          icon={CheckCircle2}
          description="Applications successfully approved by administrators."
          iconClassName="text-emerald-600"
          iconBackgroundClassName="bg-emerald-50"
          valueClassName="text-emerald-700"
        />

        <StatCard
          title="Rejected Cases"
          value={
            statistics.rejectedCases
          }
          icon={XCircle}
          description="Applications that have been rejected."
          iconClassName="text-red-600"
          iconBackgroundClassName="bg-red-50"
          valueClassName="text-red-700"
        />

        <StatCard
          title="Fraud Alerts"
          value={
            statistics.fraudAlerts
          }
          icon={ShieldAlert}
          description="Cases requiring immediate fraud investigation."
          iconClassName="text-amber-600"
          iconBackgroundClassName="bg-amber-50"
          valueClassName="text-amber-700"
        />
      </div>

      {/* ======================================================
          FILTER PANEL
      ====================================================== */}

      <div
        className="
          overflow-hidden
          rounded-[2rem]
          border
          border-slate-200/80
          bg-white
          shadow-[0_12px_40px_rgba(11,23,54,0.06)]
        "
      >
        {/* Filter header */}

        <div
          className="
            flex
            flex-col
            gap-4
            border-b
            border-slate-100
            p-5
            sm:p-6
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
                  bg-[#0B1736]
                  text-[#F4B81A]
                "
              >
                <Filter
                  size={17}
                  aria-hidden="true"
                />
              </div>

              <h3
                className="
                  text-base
                  font-black
                  text-[#0B1736]
                "
              >
                Application Filters
              </h3>
            </div>

            <p
              className="
                mt-2
                text-sm
                text-slate-500
              "
            >
              Search and refine the application
              queue using multiple criteria.
            </p>
          </div>

          <div
            className="
              flex
              items-center
              gap-2
            "
          >
            {hasActiveFilters && (
              <button
                type="button"
                onClick={clearFilters}
                className="
                  inline-flex
                  items-center
                  gap-1.5
                  rounded-xl
                  px-3
                  py-2
                  text-xs
                  font-bold
                  text-slate-500
                  transition
                  hover:bg-slate-100
                  hover:text-[#0B1736]
                "
              >
                <X
                  size={14}
                  aria-hidden="true"
                />

                Clear
              </button>
            )}

            <button
              type="button"
              onClick={() =>
                setShowAdvancedFilters(
                  (previous) =>
                    !previous,
                )
              }
              className={`
                inline-flex
                items-center
                gap-2
                rounded-xl
                border
                px-4
                py-2.5
                text-sm
                font-bold
                transition-all
                ${
                  showAdvancedFilters
                    ? "border-[#0B1736] bg-[#0B1736] text-white"
                    : "border-slate-200 bg-white text-slate-700 hover:border-slate-300 hover:bg-slate-50"
                }
              `}
            >
              <Filter
                size={16}
                aria-hidden="true"
              />

              {showAdvancedFilters
                ? "Hide Filters"
                : "Advanced Filters"}
            </button>
          </div>
        </div>

        {/* Main filters */}

        <div className="p-5 sm:p-6">
          <div
            className="
              grid
              gap-4
              lg:grid-cols-[minmax(0,1fr)_240px]
            "
          >
            {/* Search */}

            <div className="relative">
              <label
                htmlFor="application-search"
                className="
                  mb-2
                  block
                  text-xs
                  font-bold
                  uppercase
                  tracking-wider
                  text-slate-500
                "
              >
                Search Applications
              </label>

              <Search
                size={18}
                className="
                  pointer-events-none
                  absolute
                  left-4
                  top-[43px]
                  -translate-y-1/2
                  text-slate-400
                "
                aria-hidden="true"
              />

              <input
                id="application-search"
                type="search"
                value={search}
                onChange={
                  handleSearchChange
                }
                placeholder="Search applicant, country, visa type or application ID..."
                autoComplete="off"
                className="
                  h-12
                  w-full
                  rounded-2xl
                  border
                  border-slate-200
                  bg-slate-50/70
                  py-3
                  pl-11
                  pr-11
                  text-sm
                  font-medium
                  text-[#0B1736]
                  outline-none
                  transition-all
                  placeholder:text-slate-400
                  focus:border-[#F4B81A]
                  focus:bg-white
                  focus:ring-4
                  focus:ring-[#F4B81A]/10
                "
              />

              {search && (
                <button
                  type="button"
                  aria-label="Clear search"
                  onClick={() =>
                    setSearch("")
                  }
                  className="
                    absolute
                    right-3
                    top-[43px]
                    flex
                    h-7
                    w-7
                    -translate-y-1/2
                    items-center
                    justify-center
                    rounded-lg
                    text-slate-400
                    transition
                    hover:bg-slate-200
                    hover:text-slate-700
                  "
                >
                  <X
                    size={15}
                    aria-hidden="true"
                  />
                </button>
              )}
            </div>

            {/* Status */}

            <div>
              <label
                htmlFor="application-status"
                className="
                  mb-2
                  block
                  text-xs
                  font-bold
                  uppercase
                  tracking-wider
                  text-slate-500
                "
              >
                Status
              </label>

              <select
                id="application-status"
                value={statusFilter}
                onChange={
                  handleStatusChange
                }
                className="
                  h-12
                  w-full
                  rounded-2xl
                  border
                  border-slate-200
                  bg-slate-50/70
                  px-4
                  text-sm
                  font-semibold
                  text-[#0B1736]
                  outline-none
                  transition-all
                  focus:border-[#F4B81A]
                  focus:bg-white
                  focus:ring-4
                  focus:ring-[#F4B81A]/10
                "
              >
                {STATUS_OPTIONS.map(
                  (option) => (
                    <option
                      key={option.value}
                      value={option.value}
                    >
                      {option.label}
                    </option>
                  ),
                )}
              </select>
            </div>
          </div>

          {/* Advanced filters */}

          {showAdvancedFilters && (
            <div
              className="
                mt-5
                grid
                gap-4
                border-t
                border-slate-100
                pt-5
                md:grid-cols-3
              "
            >
              {/* Country */}

              <div>
                <label
                  htmlFor="country-filter"
                  className="
                    mb-2
                    block
                    text-xs
                    font-bold
                    uppercase
                    tracking-wider
                    text-slate-500
                  "
                >
                  Country
                </label>

                <div className="relative">
                  <Globe2
                    size={17}
                    className="
                      pointer-events-none
                      absolute
                      left-4
                      top-1/2
                      -translate-y-1/2
                      text-slate-400
                    "
                    aria-hidden="true"
                  />

                  <input
                    id="country-filter"
                    type="text"
                    value={
                      advancedFilters.country
                    }
                    onChange={
                      handleCountryChange
                    }
                    placeholder="e.g. Zimbabwe"
                    className="
                      h-12
                      w-full
                      rounded-2xl
                      border
                      border-slate-200
                      bg-slate-50/70
                      px-4
                      pl-11
                      text-sm
                      font-medium
                      text-[#0B1736]
                      outline-none
                      transition
                      placeholder:text-slate-400
                      focus:border-[#F4B81A]
                      focus:bg-white
                      focus:ring-4
                      focus:ring-[#F4B81A]/10
                    "
                  />
                </div>
              </div>

              {/* Visa type */}

              <div>
                <label
                  htmlFor="visa-filter"
                  className="
                    mb-2
                    block
                    text-xs
                    font-bold
                    uppercase
                    tracking-wider
                    text-slate-500
                  "
                >
                  Visa Type
                </label>

                <div className="relative">
                  <FileSearch
                    size={17}
                    className="
                      pointer-events-none
                      absolute
                      left-4
                      top-1/2
                      -translate-y-1/2
                      text-slate-400
                    "
                    aria-hidden="true"
                  />

                  <input
                    id="visa-filter"
                    type="text"
                    value={
                      advancedFilters.visaType
                    }
                    onChange={
                      handleVisaTypeChange
                    }
                    placeholder="e.g. Skilled Worker"
                    className="
                      h-12
                      w-full
                      rounded-2xl
                      border
                      border-slate-200
                      bg-slate-50/70
                      px-4
                      pl-11
                      text-sm
                      font-medium
                      text-[#0B1736]
                      outline-none
                      transition
                      placeholder:text-slate-400
                      focus:border-[#F4B81A]
                      focus:bg-white
                      focus:ring-4
                      focus:ring-[#F4B81A]/10
                    "
                  />
                </div>
              </div>

              {/* Risk */}

              <div>
                <label
                  htmlFor="risk-filter"
                  className="
                    mb-2
                    block
                    text-xs
                    font-bold
                    uppercase
                    tracking-wider
                    text-slate-500
                  "
                >
                  Risk Level
                </label>

                <select
                  id="risk-filter"
                  value={
                    advancedFilters.riskLevel
                  }
                  onChange={
                    handleRiskChange
                  }
                  className="
                    h-12
                    w-full
                    rounded-2xl
                    border
                    border-slate-200
                    bg-slate-50/70
                    px-4
                    text-sm
                    font-semibold
                    text-[#0B1736]
                    outline-none
                    transition
                    focus:border-[#F4B81A]
                    focus:bg-white
                    focus:ring-4
                    focus:ring-[#F4B81A]/10
                  "
                >
                  <option value="ALL">
                    All Risk Levels
                  </option>

                  <option value="LOW">
                    Low Risk
                  </option>

                  <option value="MEDIUM">
                    Medium Risk
                  </option>

                  <option value="HIGH">
                    High Risk
                  </option>
                </select>
              </div>
            </div>
          )}

          {/* Results summary */}

          <div
            className="
              mt-5
              flex
              flex-col
              gap-2
              border-t
              border-slate-100
              pt-4
              text-sm
              sm:flex-row
              sm:items-center
              sm:justify-between
            "
          >
            <p className="text-slate-500">
              Showing{" "}
              <span className="font-black text-[#0B1736]">
                {filteredApplications.length}
              </span>{" "}
              of{" "}
              <span className="font-black text-[#0B1736]">
                {applications.length}
              </span>{" "}
              applications
            </p>

            {hasActiveFilters && (
              <span
                className="
                  inline-flex
                  w-fit
                  items-center
                  gap-1.5
                  rounded-full
                  bg-[#F4B81A]/10
                  px-3
                  py-1
                  text-xs
                  font-bold
                  text-[#9A6900]
                "
              >
                <Filter
                  size={13}
                  aria-hidden="true"
                />

                Filters active
              </span>
            )}
          </div>
        </div>
      </div>

      {/* ======================================================
          APPLICATION TABLE
      ====================================================== */}

      <div
        className="
          overflow-hidden
          rounded-[2rem]
          border
          border-slate-200/80
          bg-white
          shadow-[0_12px_40px_rgba(11,23,54,0.06)]
        "
      >
        {/* Table heading */}

        <div
          className="
            flex
            flex-col
            gap-3
            border-b
            border-slate-100
            p-5
            sm:p-6
            lg:flex-row
            lg:items-center
            lg:justify-between
          "
        >
          <div>
            <h3
              className="
                text-lg
                font-black
                tracking-tight
                text-[#0B1736]
              "
            >
              Immigration Applications
            </h3>

            <p
              className="
                mt-1
                text-sm
                text-slate-500
              "
            >
              Review applicant information,
              AI confidence, risk levels and
              application status.
            </p>
          </div>

          <div
            className="
              inline-flex
              w-fit
              items-center
              gap-2
              rounded-xl
              bg-slate-50
              px-3
              py-2
              text-xs
              font-bold
              text-slate-500
            "
          >
            <FileCheck2
              size={15}
              className="text-[#F4B81A]"
              aria-hidden="true"
            />

            {filteredApplications.length}{" "}
            results
          </div>
        </div>

        {/* Horizontal scrolling table */}

        <div className="overflow-x-auto">
          <table
            className="
              min-w-[1050px]
              w-full
              border-collapse
            "
          >
            <thead>
              <tr
                className="
                  border-b
                  border-slate-100
                  bg-slate-50/80
                "
              >
                {[
                  "Application",
                  "Country",
                  "Visa Type",
                  "Submitted",
                  "AI Score",
                  "Risk",
                  "Status",
                  "Actions",
                ].map((heading) => (
                  <th
                    key={heading}
                    scope="col"
                    className="
                      whitespace-nowrap
                      px-5
                      py-4
                      text-left
                      text-[11px]
                      font-black
                      uppercase
                      tracking-[0.12em]
                      text-slate-500
                    "
                  >
                    {heading}
                  </th>
                ))}
              </tr>
            </thead>

            <tbody>
              {/* Loading */}

              {loading ? (
                <tr>
                  <td
                    colSpan={8}
                    className="px-5 py-16"
                  >
                    <div
                      className="
                        flex
                        flex-col
                        items-center
                        justify-center
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
                          bg-[#0B1736]
                          text-[#F4B81A]
                        "
                      >
                        <RefreshCw
                          size={24}
                          className="animate-spin"
                          aria-hidden="true"
                        />
                      </div>

                      <p
                        className="
                          mt-4
                          text-sm
                          font-black
                          text-[#0B1736]
                        "
                      >
                        Loading applications
                      </p>

                      <p
                        className="
                          mt-1
                          text-xs
                          text-slate-400
                        "
                      >
                        Fetching the latest
                        application data...
                      </p>
                    </div>
                  </td>
                </tr>
              ) : filteredApplications.length ===
                0 ? (
                <tr>
                  <td
                    colSpan={8}
                    className="px-5 py-16"
                  >
                    <div
                      className="
                        flex
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
                          rounded-3xl
                          bg-slate-100
                          text-slate-400
                        "
                      >
                        <Search
                          size={27}
                          aria-hidden="true"
                        />
                      </div>

                      <p
                        className="
                          mt-4
                          text-base
                          font-black
                          text-[#0B1736]
                        "
                      >
                        No applications found
                      </p>

                      <p
                        className="
                          mt-1
                          max-w-md
                          text-sm
                          leading-6
                          text-slate-500
                        "
                      >
                        Try changing your search
                        term or clearing one or
                        more filters.
                      </p>

                      {hasActiveFilters && (
                        <button
                          type="button"
                          onClick={
                            clearFilters
                          }
                          className="
                            mt-5
                            inline-flex
                            items-center
                            gap-2
                            rounded-xl
                            bg-[#0B1736]
                            px-4
                            py-2.5
                            text-sm
                            font-bold
                            text-white
                            transition
                            hover:bg-[#172554]
                            focus:outline-none
                            focus:ring-2
                            focus:ring-[#F4B81A]
                            focus:ring-offset-2
                          "
                        >
                          <X
                            size={15}
                            aria-hidden="true"
                          />

                          Clear Filters
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ) : (
                filteredApplications.map(
                  (application) => {
                    const isApproving =
                      approvingId ===
                      application.id;

                    return (
                      <tr
                        key={
                          application.id
                        }
                        className="
                          border-b
                          border-slate-100
                          transition-colors
                          last:border-0
                          hover:bg-[#F8F6F1]/60
                        "
                      >
                        {/* Application */}

                        <td className="px-5 py-5">
                          <div
                            className="
                              flex
                              items-center
                              gap-3
                            "
                          >
                            <div
                              className="
                                flex
                                h-11
                                w-11
                                shrink-0
                                items-center
                                justify-center
                                rounded-2xl
                                bg-gradient-to-br
                                from-[#0B1736]
                                to-[#172554]
                                text-xs
                                font-black
                                text-[#F4B81A]
                              "
                            >
                              {getInitials(
                                application.applicantName,
                              )}
                            </div>

                            <div className="min-w-0">
                              <p
                                className="
                                  max-w-[190px]
                                  truncate
                                  text-sm
                                  font-black
                                  text-[#0B1736]
                                "
                                title={
                                  application.applicantName
                                }
                              >
                                {
                                  application.applicantName
                                }
                              </p>

                              <p
                                className="
                                  mt-0.5
                                  text-xs
                                  font-medium
                                  text-slate-400
                                "
                              >
                                #
                                {
                                  application.id
                                }
                              </p>
                            </div>
                          </div>
                        </td>

                        {/* Country */}

                        <td className="px-5 py-5">
                          <div
                            className="
                              flex
                              items-center
                              gap-2
                              text-sm
                              font-semibold
                              text-slate-700
                            "
                          >
                            <Globe2
                              size={15}
                              className="text-slate-400"
                              aria-hidden="true"
                            />

                            {
                              application.country
                            }
                          </div>
                        </td>

                        {/* Visa Type */}

                        <td className="px-5 py-5">
                          <p
                            className="
                              max-w-[180px]
                              truncate
                              text-sm
                              font-semibold
                              text-slate-700
                            "
                            title={
                              application.visaType
                            }
                          >
                            {
                              application.visaType
                            }
                          </p>

                          <p
                            className="
                              mt-1
                              text-xs
                              text-slate-400
                            "
                          >
                            {
                              application.documentCount
                            }{" "}
                            document
                            {application.documentCount !==
                            1
                              ? "s"
                              : ""}
                          </p>
                        </td>

                        {/* Submitted */}

                        <td className="px-5 py-5">
                          <p
                            className="
                              whitespace-nowrap
                              text-sm
                              font-semibold
                              text-slate-700
                            "
                          >
                            {formatDate(
                              application.submittedAt,
                            )}
                          </p>
                        </td>

                        {/* AI Score */}

                        <td className="px-5 py-5">
                          <div className="w-28">
                            <div
                              className="
                                mb-1.5
                                flex
                                items-center
                                justify-between
                              "
                            >
                              <span
                                className={`
                                  text-sm
                                  font-black
                                  ${getConfidenceColor(
                                    application.aiConfidence,
                                  )}
                                `}
                              >
                                {
                                  application.aiConfidence
                                }
                                %
                              </span>
                            </div>

                            <div
                              className="
                                h-1.5
                                overflow-hidden
                                rounded-full
                                bg-slate-100
                              "
                            >
                              <div
                                className={`
                                  h-full
                                  rounded-full
                                  transition-all
                                  ${
                                    application.aiConfidence >=
                                    85
                                      ? "bg-emerald-500"
                                      : application.aiConfidence >=
                                          65
                                        ? "bg-amber-500"
                                        : "bg-red-500"
                                  }
                                `}
                                style={{
                                  width: `${Math.min(
                                    Math.max(
                                      application.aiConfidence,
                                      0,
                                    ),
                                    100,
                                  )}%`,
                                }}
                              />
                            </div>
                          </div>
                        </td>

                        {/* Risk */}

                        <td className="px-5 py-5">
                          <span
                            className={`
                              inline-flex
                              items-center
                              gap-1.5
                              rounded-full
                              border
                              px-3
                              py-1.5
                              text-xs
                              font-black
                              ${riskColor(
                                application.riskLevel,
                              )}
                            `}
                          >
                            {application.riskLevel ===
                              "HIGH" && (
                              <ShieldAlert
                                size={13}
                                aria-hidden="true"
                              />
                            )}

                            {formatRisk(
                              application.riskLevel,
                            )}
                          </span>
                        </td>

                        {/* Status */}

                        <td className="px-5 py-5">
                          <span
                            className={`
                              inline-flex
                              whitespace-nowrap
                              rounded-full
                              border
                              px-3
                              py-1.5
                              text-xs
                              font-black
                              ${statusColor(
                                application.status,
                              )}
                            `}
                          >
                            {formatStatus(
                              application.status,
                            )}
                          </span>
                        </td>

                        {/* Actions */}

                        <td className="px-5 py-5">
                          <div
                            className="
                              flex
                              items-center
                              gap-1.5
                            "
                          >
                            {/* View */}

                            <button
                              type="button"
                              onClick={() =>
                                handleViewApplication(
                                  application.id,
                                )
                              }
                              className="
                                flex
                                h-9
                                w-9
                                items-center
                                justify-center
                                rounded-xl
                                border
                                border-blue-100
                                bg-blue-50
                                text-blue-600
                                transition
                                hover:border-blue-200
                                hover:bg-blue-100
                                focus:outline-none
                                focus:ring-2
                                focus:ring-blue-400
                              "
                              title="View application"
                              aria-label={`View application ${application.id}`}
                            >
                              <Eye
                                size={16}
                                aria-hidden="true"
                              />
                            </button>

                            {/* Approve */}

                            <button
                              type="button"
                              onClick={() =>
                                void handleApproveApplication(
                                  application.id,
                                )
                              }
                              disabled={
                                isApproving ||
                                application.status ===
                                  "APPROVED"
                              }
                              className="
                                flex
                                h-9
                                w-9
                                items-center
                                justify-center
                                rounded-xl
                                border
                                border-emerald-100
                                bg-emerald-50
                                text-emerald-600
                                transition
                                hover:border-emerald-200
                                hover:bg-emerald-100
                                focus:outline-none
                                focus:ring-2
                                focus:ring-emerald-400
                                disabled:cursor-not-allowed
                                disabled:opacity-40
                              "
                              title={
                                application.status ===
                                "APPROVED"
                                  ? "Application already approved"
                                  : "Approve application"
                              }
                              aria-label={`Approve application ${application.id}`}
                            >
                              {isApproving ? (
                                <RefreshCw
                                  size={16}
                                  className="animate-spin"
                                  aria-hidden="true"
                                />
                              ) : (
                                <UserCheck
                                  size={16}
                                  aria-hidden="true"
                                />
                              )}
                            </button>

                            {/* Documents */}

                            <button
                              type="button"
                              onClick={() =>
                                handleReviewDocuments(
                                  application.id,
                                )
                              }
                              className="
                                flex
                                h-9
                                w-9
                                items-center
                                justify-center
                                rounded-xl
                                border
                                border-amber-100
                                bg-amber-50
                                text-amber-600
                                transition
                                hover:border-amber-200
                                hover:bg-amber-100
                                focus:outline-none
                                focus:ring-2
                                focus:ring-amber-400
                              "
                              title="Review documents"
                              aria-label={`Review documents for application ${application.id}`}
                            >
                              <FileSearch
                                size={16}
                                aria-hidden="true"
                              />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  },
                )
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* ======================================================
          FRAUD ALERT
      ====================================================== */}

      <div
        className="
          relative
          overflow-hidden
          rounded-[2rem]
          border
          border-red-200
          bg-gradient-to-r
          from-red-50
          via-white
          to-amber-50
          p-5
          shadow-sm
          sm:p-6
        "
      >
        <div
          className="
            pointer-events-none
            absolute
            -right-16
            -top-20
            h-48
            w-48
            rounded-full
            bg-red-100/60
            blur-3xl
          "
          aria-hidden="true"
        />

        <div
          className="
            relative
            flex
            flex-col
            gap-4
            sm:flex-row
            sm:items-center
            sm:justify-between
          "
        >
          <div
            className="
              flex
              items-start
              gap-4
            "
          >
            <div
              className="
                flex
                h-12
                w-12
                shrink-0
                items-center
                justify-center
                rounded-2xl
                bg-red-100
                text-red-600
              "
            >
              <ShieldAlert
                size={23}
                aria-hidden="true"
              />
            </div>

            <div>
              <p
                className="
                  text-sm
                  font-black
                  text-red-800
                "
              >
                AI Fraud Detection
              </p>

              <p
                className="
                  mt-1
                  max-w-3xl
                  text-sm
                  leading-6
                  text-red-700/80
                "
              >
                <span className="font-black">
                  {statistics.fraudAlerts}
                </span>{" "}
                application
                {statistics.fraudAlerts !==
                1
                  ? "s are"
                  : " is"}{" "}
                currently flagged by the
                AI fraud detection engine and
                require immediate administrative
                review.
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={() => {
              setStatusFilter("ALL");
              setAdvancedFilters(
                (previous) => ({
                  ...previous,
                  riskLevel: "HIGH",
                }),
              );
              setShowAdvancedFilters(
                true,
              );
            }}
            className="
              inline-flex
              shrink-0
              items-center
              justify-center
              gap-2
              rounded-xl
              border
              border-red-200
              bg-white
              px-4
              py-2.5
              text-sm
              font-black
              text-red-700
              transition
              hover:bg-red-50
              focus:outline-none
              focus:ring-2
              focus:ring-red-400
              focus:ring-offset-2
            "
          >
            <ShieldAlert
              size={16}
              aria-hidden="true"
            />

            Review High-Risk Cases
          </button>
        </div>
      </div>
    </section>
  );
}