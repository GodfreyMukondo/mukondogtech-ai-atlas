import { useCallback, useEffect, useState } from "react";

import { Link, useNavigate, useParams } from "react-router-dom";

import { motion } from "framer-motion";

import {
  AlertTriangle,
  ArrowLeft,
  BadgeCheck,
  BookOpen,
  ChevronDown,
  ChevronRight,
  FileText,
  Gavel,
  RefreshCw,
  Sparkles,
  Waypoints,
} from "lucide-react";

import { getPathwayExplanationApi } from "../../api/pathwayApi";
import { getAssessmentErrorMessage } from "../../utils/assessmentErrors";
import { getCertaintyLabel, getOutcomeDisplay } from "../../utils/assessmentOutcome";
import {
  getEvidenceSourceLabel,
  getProvenanceLabel,
  getRegulatoryVerificationLabel,
} from "../../utils/explanationLabels";

import type {
  FactExplanation,
  PathwayExplanation,
  RequirementExplanation,
} from "../../types/pathwayExplanation";

import Badge from "../../components/common/Badge";
import Button from "../../components/common/Button";
import Loader from "../../components/common/Loader";
import ErrorAlert from "../../components/common/ErrorAlert";
import EmptyState from "../../components/common/EmptyState";

/**
 * ============================================================================
 * PATHWAY EXPLANATION PAGE
 * ============================================================================
 *
 * Answers "why does MukondoGTech AI think I may qualify for this pathway"
 * by rendering the full explainability chain returned by
 * GET /api/pathways/assessments/{assessmentId}/explanation:
 *
 *   PATHWAY -> REQUIREMENT -> REQUIREMENT EVALUATION -> FACT -> EVIDENCE
 *           -> REGULATORY VERSION
 *
 * Reuses the exact assessmentId already exposed by
 * PathwayAssessmentResultsPage's route - this page never mints or receives
 * a second identifier for the same assessment.
 *
 * This page renders only what the API returns; it never fetches or caches
 * Fact data on its own, and never re-labels INSUFFICIENT_EVIDENCE/UNKNOWN
 * as NOT_SATISFIED, or a CONFLICTED outcome as fraud.
 * ============================================================================
 */
export default function PathwayExplanationPage() {

  const { assessmentId } = useParams<{ assessmentId: string }>();
  const navigate = useNavigate();

  const [explanation, setExplanation] = useState<PathwayExplanation | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const numericAssessmentId = assessmentId ? Number(assessmentId) : null;

  const loadExplanation = useCallback(async () => {

    if (!numericAssessmentId || Number.isNaN(numericAssessmentId)) {
      setError("This assessment link is invalid.");
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);

      const result = await getPathwayExplanationApi(numericAssessmentId);

      setExplanation(result);

    } catch (err) {
      setError(getAssessmentErrorMessage(err));
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [assessmentId]);

  useEffect(() => {
    void loadExplanation();
  }, [loadExplanation]);

  const backToResultsPath = assessmentId
    ? `/dashboard/pathways/assessments/${assessmentId}`
    : "/dashboard/pathways/assessments/new";

  if (loading) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-16 sm:px-6">
        <Loader text="Building your explanation..." />
      </div>
    );
  }

  if (error) {
    return (
      <div className="mx-auto max-w-4xl space-y-4 px-4 py-10 sm:px-6">
        <ErrorAlert message={error} />
        <div className="flex gap-3">
          <Button variant="secondary" onClick={() => void loadExplanation()}>
            <RefreshCw size={16} />
            Try again
          </Button>
          <Link to={backToResultsPath}>
            <Button variant="outline">Back to results</Button>
          </Link>
        </div>
      </div>
    );
  }

  if (!explanation) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-10 sm:px-6">
        <EmptyState
          icon={<BookOpen size={40} />}
          title="No explanation available"
          description="We could not find an explanation for this assessment."
          action={
            <Link to={backToResultsPath}>
              <Button variant="primary">Back to results</Button>
            </Link>
          }
        />
      </div>
    );
  }

  const overallOutcome = getOutcomeDisplay(explanation.overallOutcome);

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className="mx-auto max-w-4xl px-4 py-10 sm:px-6"
    >
      <button
        type="button"
        onClick={() => navigate(backToResultsPath)}
        className="mb-6 inline-flex items-center gap-1 text-sm text-slate-400 transition hover:text-white"
      >
        <ArrowLeft size={16} />
        Back to assessment results
      </button>

      <header
        className="
          rounded-2xl
          border
          border-white/10
          bg-white/5
          p-6
          shadow-sm
        "
      >
        <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-[#C6A15B]/20 bg-[#C6A15B]/10 px-3 py-1 text-xs font-semibold text-[#C6A15B]">
          <Sparkles size={14} />
          Why this result
        </div>

        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-xs uppercase tracking-wide text-slate-500">
              Assessment #{explanation.pathwayAssessmentId}
            </p>

            <h1 className="mt-1 text-2xl font-black text-white">
              {explanation.pathwayName}
            </h1>
          </div>

          <Badge variant={overallOutcome.badgeVariant}>{overallOutcome.label}</Badge>
        </div>

        <p className="mt-3 text-sm text-slate-300">{overallOutcome.description}</p>
      </header>

      <section className="mt-8 space-y-4">
        <h2 className="text-lg font-bold text-white">Requirement by requirement</h2>

        {explanation.requirements.length === 0 ? (
          <EmptyState
            icon={<AlertTriangle size={40} />}
            title="No requirements to explain"
            description="This assessment did not evaluate any requirements."
          />
        ) : (
          explanation.requirements.map((requirement) => (
            <RequirementExplanationCard key={requirement.requirementId} requirement={requirement} />
          ))
        )}
      </section>
    </motion.div>
  );
}

/* ============================================================================
   REQUIREMENT EXPLANATION CARD
============================================================================ */

function RequirementExplanationCard({
  requirement,
}: {
  requirement: RequirementExplanation;
}) {
  const outcomeDisplay = getOutcomeDisplay(requirement.outcome);

  return (
    <details
      open
      className="
        group
        rounded-xl
        border
        border-white/10
        bg-white/5
        p-5
        transition
        hover:border-white/20
      "
    >
      <summary className="flex cursor-pointer list-none flex-wrap items-start justify-between gap-3">
        <div>
          <p className="font-semibold text-white">{requirement.requirementTitle}</p>
          <p className="text-xs text-slate-500">
            {requirement.requirementKey}
            {requirement.mandatory ? " · Mandatory" : " · Optional"}
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Badge variant={outcomeDisplay.badgeVariant}>{outcomeDisplay.label}</Badge>
          <ChevronDown size={16} className="text-slate-500 transition group-open:rotate-180" />
        </div>
      </summary>

      <div className="mt-4 space-y-4 border-t border-white/10 pt-4">
        <p className="text-sm text-slate-300">{outcomeDisplay.description}</p>

        {requirement.explanation && (
          <p className="text-sm text-slate-400">{requirement.explanation}</p>
        )}

        <p className="text-xs text-slate-500">
          {getCertaintyLabel(requirement.certaintyLevel)}
        </p>

        {requirement.unresolvedConflictIds.length > 0 && (
          <div className="rounded-lg border border-amber-400/20 bg-amber-400/10 px-3 py-2 text-xs text-amber-300">
            {requirement.unresolvedConflictIds.length} unresolved conflict
            {requirement.unresolvedConflictIds.length === 1 ? "" : "s"} affect this requirement.
            This means two pieces of information disagree and require review - it is not a
            finding of fraud.
          </div>
        )}

        {/* ------------------------------------------------------------------
            REGULATORY SOURCE
        ------------------------------------------------------------------ */}

        <div className="flex items-start gap-2 rounded-lg border border-white/10 bg-black/20 p-3 text-xs text-slate-400">
          <Gavel size={14} className="mt-0.5 shrink-0 text-[#C6A15B]" />
          <div>
            <p className="text-slate-300">
              Based on regulatory information from{" "}
              <span className="font-semibold text-white">
                {requirement.regulatorySourceAuthority}
              </span>
              {requirement.regulatorySourceReference && (
                <> ({requirement.regulatorySourceReference})</>
              )}
              .
            </p>
            <p className="mt-1">
              {getRegulatoryVerificationLabel(requirement.regulatoryVerificationStatus)}
            </p>
          </div>
        </div>

        {/* ------------------------------------------------------------------
            CONTRIBUTING FACTS
        ------------------------------------------------------------------ */}

        {requirement.contributingFacts.length > 0 && (
          <details className="rounded-lg border border-white/10 bg-black/10">
            <summary className="cursor-pointer list-none px-3 py-2 text-xs font-semibold text-slate-300">
              Based on {requirement.contributingFacts.length} fact
              {requirement.contributingFacts.length === 1 ? "" : "s"} from your case
            </summary>

            <div className="space-y-3 border-t border-white/10 p-3">
              {requirement.contributingFacts.map((fact) => (
                <FactExplanationRow key={fact.factId} fact={fact} />
              ))}
            </div>
          </details>
        )}
      </div>
    </details>
  );
}

/* ============================================================================
   FACT EXPLANATION ROW
============================================================================ */

function FactExplanationRow({ fact }: { fact: FactExplanation }) {
  return (
    <div className="rounded-lg border border-white/10 bg-white/5 p-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="text-sm font-medium text-white">
          {fact.valueSummary ?? fact.factKey}
        </p>

        {fact.isVerified && (
          <span className="inline-flex items-center gap-1 rounded-full border border-emerald-400/20 bg-emerald-400/10 px-2 py-0.5 text-[10px] font-semibold text-emerald-300">
            <BadgeCheck size={12} />
            Verified
          </span>
        )}
      </div>

      <p className="mt-1 text-xs text-slate-500">{fact.factKey}</p>

      <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-400">
        <span>{getProvenanceLabel(fact.provenanceType)}</span>
        <span aria-hidden="true">·</span>
        <span>Evaluation confidence: {getCertaintyLabel(fact.confidenceLevel).toLowerCase()}</span>
      </div>

      {fact.evidence.length > 0 && (
        <ul className="mt-3 space-y-2 border-t border-white/10 pt-2">
          {fact.evidence.map((evidence) => (
            <li key={evidence.id} className="flex items-start gap-2 text-xs text-slate-400">
              <FileText size={13} className="mt-0.5 shrink-0 text-slate-500" />
              <div>
                <p>
                  {getEvidenceSourceLabel(evidence.sourceType)}
                  {evidence.documentId != null && <> · Document #{evidence.documentId}</>}
                  {evidence.sourceLocator && <> · {evidence.sourceLocator}</>}
                </p>
                {evidence.sourceSnippet && (
                  <p className="mt-0.5 italic text-slate-500">&ldquo;{evidence.sourceSnippet}&rdquo;</p>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}

      <div className="mt-3 border-t border-white/10 pt-2 text-right">
        <Link
          to={`/dashboard/evidence-graph?fact=${fact.factId}`}
          className="inline-flex items-center gap-1 text-xs font-semibold text-[#C6A15B] transition hover:text-[#dbb877]"
        >
          <Waypoints size={12} />
          Trace this Fact
          <ChevronRight size={12} />
        </Link>
      </div>
    </div>
  );
}
