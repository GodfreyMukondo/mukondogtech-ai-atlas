import { useCallback, useEffect, useState } from "react";

import { Link, useNavigate } from "react-router-dom";

import { motion } from "framer-motion";

import {
  AlertTriangle,
  ArrowRight,
  Bot,
  Info,
  RefreshCw,
  ShieldQuestion,
  Telescope,
} from "lucide-react";

import { useAuth } from "../../context/AuthContext";

import { discoverPathwaysApi } from "../../api/pathwayApi";
import { getAssessmentErrorMessage } from "../../utils/assessmentErrors";
import { getOutcomeDisplay } from "../../utils/assessmentOutcome";
import {
  getSupportStatusBadgeVariant,
  getSupportStatusLabel,
} from "../../utils/caseIntelligenceLabels";
import { getRegulatoryVerificationLabel } from "../../utils/explanationLabels";

import type { PathwayDiscoveryResponse, PathwayRankingRow } from "../../types/pathwayDiscovery";

import Badge from "../../components/common/Badge";
import Button from "../../components/common/Button";
import ErrorAlert from "../../components/common/ErrorAlert";
import EmptyState from "../../components/common/EmptyState";
import Skeleton from "../../components/common/Skeleton";
import PathwayDiscoveryExplanationPanel from "../../components/agent/PathwayDiscoveryExplanationPanel";

/**
 * ============================================================================
 * PATHWAY DISCOVERY PAGE
 * ============================================================================
 *
 * The upstream entry point into the Requirement/Pathway architecture:
 * answers "which published pathways currently align with my case, and why"
 * BEFORE the user has to already know which one to request a formal
 * assessment for (see RequestPathwayAssessmentPage).
 *
 * GET /api/pathways/discovery is read-only and transient - opening this page
 * never creates a PathwayAssessment or RequirementEvaluation row, so it is
 * safe to reload every time. Selecting a ranked pathway hands off to the
 * EXISTING formal assessment flow (RequestPathwayAssessmentPage) rather than
 * duplicating it.
 *
 * The alignment score is deliberately never framed as an approval
 * probability - see PathwayDiscoveryResponse.disclaimer, always rendered
 * verbatim from the backend rather than paraphrased here.
 * ============================================================================
 */
export default function PathwayDiscoveryPage() {

  const navigate = useNavigate();
  const { user } = useAuth();

  const [discovery, setDiscovery] = useState<PathwayDiscoveryResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedPathwayId, setExpandedPathwayId] = useState<number | null>(null);
  const [explainingDiscovery, setExplainingDiscovery] = useState(false);

  const loadDiscovery = useCallback(async () => {

    if (!user?.id) {
      setError("Your account could not be identified. Please sign in again.");
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);

      const result = await discoverPathwaysApi(user.id);

      setDiscovery(result);

    } catch (err) {
      setError(getAssessmentErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [user?.id]);

  useEffect(() => {
    void loadDiscovery();
  }, [loadDiscovery]);

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className="w-full px-4 py-10 sm:px-6 lg:px-8"
    >
      <header className="mb-8 max-w-3xl">
        <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-[#C6A15B]/20 bg-[#C6A15B]/10 px-3 py-1 text-xs font-semibold text-[#C6A15B]">
          <Telescope size={14} />
          Pathway Discovery
        </div>

        <h1 className="text-2xl font-black text-white sm:text-3xl">
          Pathway Discovery
        </h1>

        <p className="mt-2 text-sm text-slate-400">
          Explore published immigration pathways and see how your current case
          information aligns with their requirements.
        </p>

        {discovery && (
          <div className="mt-4 flex items-start gap-2 rounded-xl border border-white/10 bg-black/20 p-3 text-xs text-slate-400">
            <Info size={16} className="mt-0.5 shrink-0 text-[#C6A15B]" />
            <p>{discovery.disclaimer}</p>
          </div>
        )}

        {discovery && discovery.rankedPathways.length > 0 && (
          <div className="mt-4">
            <Button variant="outline" onClick={() => setExplainingDiscovery(true)}>
              <Bot size={16} />
              Explain My Recommendations
            </Button>
          </div>
        )}
      </header>

      {loading && (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {[0, 1, 2].map((key) => (
            <div key={key} className="space-y-3 rounded-2xl border border-white/10 bg-white/5 p-6">
              <Skeleton width="w-1/3" height="h-4" />
              <Skeleton width="w-2/3" height="h-6" />
              <Skeleton width="w-1/2" height="h-4" />
              <Skeleton width="w-full" height="h-3" />
              <Skeleton width="w-full" height="h-3" />
            </div>
          ))}
        </div>
      )}

      {!loading && error && (
        <div className="max-w-2xl space-y-4">
          <ErrorAlert message={error} />
          <Button variant="secondary" onClick={() => void loadDiscovery()}>
            <RefreshCw size={16} />
            Try again
          </Button>
        </div>
      )}

      {!loading && !error && discovery && discovery.rankedPathways.length === 0 && (
        <EmptyState
          icon={<Telescope size={40} />}
          title="No published pathways available yet"
          description="There are currently no published immigration pathways available for discovery. Please check back later."
        />
      )}

      {!loading && !error && discovery && discovery.rankedPathways.length > 0 && (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {discovery.rankedPathways.map((row) => (
            <PathwayRankingCard
              key={row.pathwayId}
              row={row}
              expanded={expandedPathwayId === row.pathwayId}
              onToggleExpanded={() =>
                setExpandedPathwayId((current) => (current === row.pathwayId ? null : row.pathwayId))
              }
              onAssess={() => navigate(`/dashboard/pathways/assessments/new?pathwayId=${row.pathwayId}`)}
            />
          ))}
        </div>
      )}

      {explainingDiscovery && user?.id != null && (
        <div className="fixed inset-y-0 right-0 z-40 h-full">
          <PathwayDiscoveryExplanationPanel
            subjectUserId={user.id}
            onClose={() => setExplainingDiscovery(false)}
          />
        </div>
      )}
    </motion.div>
  );
}

/**
 * ============================================================================
 * ONE RANKED PATHWAY CARD
 * ============================================================================
 */
function PathwayRankingCard({
  row,
  expanded,
  onToggleExpanded,
  onAssess,
}: {
  row: PathwayRankingRow;
  expanded: boolean;
  onToggleExpanded: () => void;
  onAssess: () => void;
}) {

  const outcomeDisplay = getOutcomeDisplay(row.outcome);
  const unresolvedMissing = row.topMissingRequirements;

  return (
    <div className="flex flex-col rounded-2xl border border-white/10 bg-white/5 p-6 transition hover:border-white/20">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
            #{row.rank}
          </p>
          <h3 className="mt-1 text-lg font-bold text-white">{row.name}</h3>
          <p className="mt-1 text-xs uppercase tracking-wide text-slate-500">
            {row.jurisdiction} · {row.category}
          </p>
        </div>

        <Badge variant={outcomeDisplay.badgeVariant}>{outcomeDisplay.label}</Badge>
      </div>

      <div className="mt-4 rounded-xl border border-white/10 bg-black/20 p-4">
        <div className="flex items-baseline justify-between">
          <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
            Evidence-supported alignment
          </p>
          <p className="text-2xl font-black text-[#C6A15B]">
            {Math.round(row.overallAlignmentScore)}%
          </p>
        </div>

        <div className="mt-3 space-y-2">
          <MetricBar label="Requirement coverage" value={row.requirementCoveragePercent} />
          <MetricBar label="Evidence coverage" value={row.evidenceCoveragePercent} />
          <MetricBar label="Consistency" value={row.consistencyPercent} />
        </div>
      </div>

      <div className="mt-4 flex flex-wrap items-center gap-2 text-xs">
        {row.supportedRequirementCount > 0 && (
          <span className="rounded-full border border-emerald-400/20 bg-emerald-400/10 px-2.5 py-1 text-emerald-300">
            {row.supportedRequirementCount} supported
          </span>
        )}
        {row.missingRequirementCount > 0 && (
          <span className="rounded-full border border-[#C6A15B]/20 bg-[#C6A15B]/10 px-2.5 py-1 text-[#C6A15B]">
            {row.missingRequirementCount} missing
          </span>
        )}
        {row.conflictingRequirementCount > 0 && (
          <span className="rounded-full border border-amber-400/20 bg-amber-400/10 px-2.5 py-1 text-amber-300">
            {row.conflictingRequirementCount} conflicting
          </span>
        )}
        {row.needsVerificationCount > 0 && (
          <span className="rounded-full border border-indigo-400/20 bg-indigo-400/10 px-2.5 py-1 text-indigo-300">
            {row.needsVerificationCount} needs verification
          </span>
        )}
      </div>

      <div className="mt-3 flex items-center gap-1.5 text-xs text-slate-500">
        <ShieldQuestion size={13} />
        Regulatory certainty: {getRegulatoryVerificationLabel(row.regulatoryCertainty)}
      </div>

      {unresolvedMissing.length > 0 && (
        <div className="mt-4">
          <p className="mb-2 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-400">
            <AlertTriangle size={13} className="text-[#C6A15B]" />
            Missing / incomplete
          </p>
          <ul className="space-y-1.5">
            {unresolvedMissing.map((requirement) => (
              <li key={requirement.requirementId} className="flex items-start justify-between gap-2 text-sm text-slate-300">
                <span className="truncate">{requirement.requirementTitle}</span>
                <Badge variant={getSupportStatusBadgeVariant(requirement.supportStatus)}>
                  {getSupportStatusLabel(requirement.supportStatus)}
                </Badge>
              </li>
            ))}
          </ul>
        </div>
      )}

      {row.explanation && (
        <div className="mt-4">
          <button
            type="button"
            onClick={onToggleExpanded}
            className="text-xs font-semibold text-[#C6A15B] transition hover:text-[#dbb877]"
          >
            {expanded ? "Hide explanation" : "Why this pathway?"}
          </button>

          {expanded && (
            <p className="mt-2 text-sm text-slate-400">{row.explanation}</p>
          )}
        </div>
      )}

      <div className="mt-auto pt-4">
        <Link
          to={`/dashboard/pathways/assessments/new?pathwayId=${row.pathwayId}`}
          onClick={(event) => {
            event.preventDefault();
            onAssess();
          }}
          className="inline-flex w-full items-center justify-center gap-1.5 rounded-xl border border-[#C6A15B]/30 bg-[#C6A15B]/10 px-4 py-2.5 text-sm font-semibold text-[#C6A15B] transition hover:bg-[#C6A15B]/20"
        >
          Assess this pathway
          <ArrowRight size={15} />
        </Link>
      </div>
    </div>
  );
}

function MetricBar({ label, value }: { label: string; value: number }) {
  return (
    <div>
      <div className="flex items-center justify-between text-xs text-slate-400">
        <span>{label}</span>
        <span className="font-semibold text-slate-300">{Math.round(value)}%</span>
      </div>
      <div className="mt-1 h-1.5 w-full overflow-hidden rounded-full bg-white/10">
        <div
          className="h-full rounded-full bg-[#C6A15B]/70"
          style={{ width: `${Math.min(100, Math.max(0, value))}%` }}
        />
      </div>
    </div>
  );
}
