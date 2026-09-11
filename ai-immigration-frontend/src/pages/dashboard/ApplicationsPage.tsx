import {
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";

import { useNavigate } from "react-router-dom";

import { motion } from "framer-motion";

import {
  CheckCircle2,
  Clock3,
  FileText,
  Globe2,
  MessageSquareWarning,
  Plus,
  RefreshCw,
  ShieldAlert,
} from "lucide-react";

import { getMyApplicationsApi } from "../../api/applicationApi";

import { errorService } from "../../services/errorService";

import type { Application } from "../../types/application";

import Loader from "../../components/common/Loader";
import ErrorAlert from "../../components/common/ErrorAlert";

/**
 * ============================================================================
 * APPLICATIONS PAGE
 * ============================================================================
 *
 * Lists the authenticated applicant's own submitted immigration
 * applications (GET /api/applications) and links to the submission form.
 *
 * SECURITY:
 *
 * This page never supplies a user ID. The backend resolves the
 * authenticated user's own applications from the JWT.
 * ============================================================================
 */

function formatDate(value?: string | null): string {
  if (!value) {
    return "—";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
  }).format(date);
}

function getStatusLabel(status: string): string {
  return status
    .replace(/_/g, " ")
    .toLowerCase()
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function getStatusColor(status: string): string {
  switch (status.toUpperCase()) {
    case "APPROVED":
      return "border-emerald-400/20 bg-emerald-400/10 text-emerald-300";

    case "REJECTED":
      return "border-red-400/20 bg-red-400/10 text-red-300";

    case "PENDING":
    default:
      return "border-[#C6A15B]/20 bg-[#C6A15B]/10 text-[#C6A15B]";
  }
}

function getRiskColor(risk: string): string {
  switch (risk.toUpperCase()) {
    case "HIGH":
      return "text-red-400";

    case "MEDIUM":
      return "text-amber-400";

    default:
      return "text-emerald-400";
  }
}

interface StatCardProps {
  icon: React.ElementType;
  label: string;
  value: string | number;
}

function StatCard({ icon: Icon, label, value }: StatCardProps) {
  return (
    <article
      className="
        rounded-xl
        border
        border-white/10
        bg-white/5
        p-5
        shadow-sm
        transition
        duration-200
        hover:border-white/20
      "
    >
      <div className="flex items-center justify-between gap-4">
        <div
          className="
            flex
            h-10
            w-10
            shrink-0
            items-center
            justify-center
            rounded-lg
            bg-blue-500/10
            text-blue-300
          "
        >
          <Icon size={20} aria-hidden="true" />
        </div>
      </div>

      <div className="mt-4 text-2xl font-bold tracking-tight text-white">
        {value}
      </div>

      <p className="mt-1 text-sm text-slate-400">{label}</p>
    </article>
  );
}

function ApplicationCard({ application }: { application: Application }) {
  return (
    <article
      className="
        rounded-2xl
        border
        border-white/10
        bg-white/5
        backdrop-blur-xl
        p-6
        transition
        duration-200
        hover:-translate-y-1
        hover:bg-white/[0.08]
      "
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <h3 className="truncate text-lg font-bold text-white">
            {application.visaType}
          </h3>

          <div className="mt-1 flex items-center gap-2 text-sm text-slate-400">
            <Globe2 size={14} aria-hidden="true" />
            {application.country}
          </div>
        </div>

        <span
          className={`shrink-0 rounded-full border px-3 py-1 text-xs font-bold ${getStatusColor(
            application.status,
          )}`}
        >
          {getStatusLabel(application.status)}
        </span>
      </div>

      <div className="mt-5 grid grid-cols-2 gap-4 text-sm">
        <div>
          <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
            Documents
          </p>

          <p className="mt-1 flex items-center gap-1.5 font-semibold text-slate-200">
            <FileText size={14} className="text-slate-400" aria-hidden="true" />
            {application.documentCount}
          </p>
        </div>

        <div>
          <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
            Risk level
          </p>

          <p
            className={`mt-1 font-semibold ${getRiskColor(
              application.riskLevel,
            )}`}
          >
            {getStatusLabel(application.riskLevel)}
          </p>
        </div>
      </div>

      <p className="mt-4 text-xs font-medium text-slate-500">
        Submitted {formatDate(application.submittedAt)}
      </p>

      {application.status.toUpperCase() === "REJECTED" &&
        application.rejectionReason && (
          <div
            className="
              mt-4
              flex
              items-start
              gap-2
              rounded-xl
              border
              border-red-400/20
              bg-red-400/10
              p-3
              text-xs
              leading-5
              text-red-300
            "
          >
            <MessageSquareWarning
              size={14}
              className="mt-0.5 shrink-0"
              aria-hidden="true"
            />

            <span>
              <span className="font-bold">Reason: </span>
              {application.rejectionReason}
            </span>
          </div>
        )}
    </article>
  );
}

function EmptyApplicationsState({
  onSubmit,
}: {
  onSubmit: () => void;
}) {
  return (
    <div
      className="
        rounded-xl
        border
        border-white/10
        bg-white/5
        px-6
        py-14
        text-center
        shadow-sm
        sm:px-10
      "
    >
      <div
        className="
          mx-auto
          flex
          h-14
          w-14
          items-center
          justify-center
          rounded-xl
          bg-white/10
          text-[#C6A15B]
        "
        aria-hidden="true"
      >
        <FileText size={27} aria-hidden="true" />
      </div>

      <h3 className="mt-5 text-lg font-semibold text-white">
        No applications yet
      </h3>

      <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-slate-400">
        You have not submitted any immigration applications yet. Start a
        new application to get the process underway.
      </p>

      <button
        type="button"
        onClick={onSubmit}
        className="
          mt-6
          inline-flex
          items-center
          gap-2
          rounded-xl
          bg-[#C6A15B]
          px-5
          py-2.5
          text-sm
          font-semibold
          text-black
          transition
          hover:bg-[#A8894D]
        "
      >
        <Plus size={16} aria-hidden="true" />
        New Application
      </button>
    </div>
  );
}

export default function ApplicationsPage() {
  const navigate = useNavigate();

  const [applications, setApplications] = useState<Application[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadApplications = useCallback(async () => {
    try {
      setError(null);

      const response = await getMyApplicationsApi();

      setApplications(response);
    } catch (requestError: unknown) {
      const appError = errorService.log(
        requestError,
        "Applications - Load",
      );

      setError(appError.message);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    void loadApplications();
  }, [loadApplications]);

  const handleRefresh = useCallback(async () => {
    setRefreshing(true);
    await loadApplications();
  }, [loadApplications]);

  const handleNewApplication = useCallback(() => {
    navigate("/dashboard/applications/new");
  }, [navigate]);

  const stats = useMemo(() => {
    const total = applications.length;

    const approved = applications.filter(
      (application) => application.status.toUpperCase() === "APPROVED",
    ).length;

    const pending = applications.filter(
      (application) => application.status.toUpperCase() === "PENDING",
    ).length;

    const highRisk = applications.filter(
      (application) => application.riskLevel.toUpperCase() === "HIGH",
    ).length;

    return { total, approved, pending, highRisk };
  }, [applications]);

  const isInitialLoading = loading && applications.length === 0;

  return (
    <div className="min-h-screen">
      <main className="w-full px-4 py-6 sm:px-6 lg:px-8 lg:py-8">
        {/* ==================================================================
            HEADER
        ================================================================== */}

        <motion.section
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3 }}
          className="mb-8"
        >
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div className="max-w-3xl">
              <h1 className="text-3xl font-bold tracking-tight text-white sm:text-4xl">
                My Applications
              </h1>

              <p className="mt-2 max-w-2xl text-base leading-7 text-slate-400">
                Track the immigration applications you have submitted and
                start a new one whenever you are ready.
              </p>
            </div>

            <div className="flex gap-3">
              <button
                type="button"
                onClick={handleRefresh}
                disabled={refreshing}
                className="
                  inline-flex
                  h-11
                  items-center
                  justify-center
                  gap-2
                  rounded-lg
                  border
                  border-white/15
                  bg-white/5
                  px-4
                  text-sm
                  font-semibold
                  text-slate-200
                  transition
                  hover:border-white/25
                  hover:bg-white/10
                  hover:text-white
                  disabled:cursor-not-allowed
                  disabled:opacity-60
                "
              >
                <RefreshCw
                  size={17}
                  className={refreshing ? "animate-spin" : undefined}
                  aria-hidden="true"
                />
                {refreshing ? "Refreshing..." : "Refresh"}
              </button>

              <button
                type="button"
                onClick={handleNewApplication}
                className="
                  inline-flex
                  h-11
                  items-center
                  justify-center
                  gap-2
                  rounded-lg
                  bg-[#C6A15B]
                  px-5
                  text-sm
                  font-bold
                  text-black
                  transition
                  hover:bg-[#A8894D]
                "
              >
                <Plus size={17} aria-hidden="true" />
                New Application
              </button>
            </div>
          </div>
        </motion.section>

        {/* ==================================================================
            STATS
        ================================================================== */}

        <motion.section
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3, delay: 0.05 }}
          className="mb-8 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4"
        >
          <StatCard icon={FileText} label="Total applications" value={stats.total} />
          <StatCard icon={CheckCircle2} label="Approved" value={stats.approved} />
          <StatCard icon={Clock3} label="Pending review" value={stats.pending} />
          <StatCard icon={ShieldAlert} label="High risk" value={stats.highRisk} />
        </motion.section>

        {/* ==================================================================
            ERROR
        ================================================================== */}

        {error && (
          <div className="mb-6" role="alert" aria-live="polite">
            <ErrorAlert message={error} />
          </div>
        )}

        {/* ==================================================================
            CONTENT
        ================================================================== */}

        {isInitialLoading ? (
          <div
            className="
              flex
              min-h-[280px]
              items-center
              justify-center
              rounded-xl
              border
              border-white/10
              bg-white/5
            "
            role="status"
            aria-live="polite"
          >
            <Loader text="Loading your applications..." />
          </div>
        ) : applications.length === 0 ? (
          <EmptyApplicationsState onSubmit={handleNewApplication} />
        ) : (
          <div className="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-3">
            {applications.map((application) => (
              <ApplicationCard
                key={application.id}
                application={application}
              />
            ))}
          </div>
        )}
      </main>
    </div>
  );
}
