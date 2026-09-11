import { useCallback, useEffect, useState } from "react";

import { Link } from "react-router-dom";

import { motion } from "framer-motion";

import {
  AlertTriangle,
  BadgeCheck,
  CheckCircle2,
  RefreshCw,
  ShieldQuestion,
  Sparkles,
  Waypoints,
  X,
} from "lucide-react";

import { explainRequirementApi } from "../../api/agentApi";
import { errorService } from "../../services/errorService";
import { getOutcomeDisplay } from "../../utils/assessmentOutcome";

import type { AgentRunResponse } from "../../types/agent";

import Badge from "../common/Badge";
import Loader from "../common/Loader";
import ErrorAlert from "../common/ErrorAlert";

/**
 * ============================================================================
 * EXPLAIN REQUIREMENT PANEL
 * ============================================================================
 *
 * Renders the result of POST /api/agent/runs (goal: EXPLAIN_REQUIREMENT) as
 * a slide-in panel - the same layout/style family as EvidenceDetailPanel, so
 * this reads as part of the existing pathway assessment experience rather
 * than a new "agent" surface. Never fetches or renders anything beyond what
 * the backend's structured ExplainRequirementResult already returned - no
 * fact/evidence/conflict shown here is derived or invented on the frontend.
 *
 * Deep provenance exploration is deliberately NOT duplicated here - it links
 * out to the existing, unmodified Evidence Graph page instead of embedding a
 * second graph renderer inside this narrow panel.
 *
 * PHASE 5.1 PRODUCTION HARDENING - GRACEFUL AI DEGRADATION:
 * The deterministic requirement result (`result`) is ALWAYS present on a
 * successful response, whether or not the AI explanation was produced - see
 * `result.aiExplanationStatus`. This panel never conflates "the LLM could
 * not explain this" with "the requirement assessment failed": the status/
 * facts/evidence-gaps/conflicts/provenance/human-review sections below
 * render identically in both cases; only the Explanation section and a
 * small status strip change. "Retry AI Explanation" re-invokes ONLY the
 * agent (a new, lightweight POST against the same already-computed
 * requirement result) - it never re-uploads documents, alters facts, or
 * changes the requirement's status.
 * ============================================================================
 */
export default function ExplainRequirementPanel({
  pathwayAssessmentId,
  requirementId,
  onClose,
}: {
  pathwayAssessmentId: number;
  requirementId: number;
  onClose: () => void;
}) {

  const [run, setRun] = useState<AgentRunResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [retrying, setRetrying] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async (isRetry: boolean) => {

    try {
      isRetry ? setRetrying(true) : setLoading(true);
      setError(null);

      const result = await explainRequirementApi({ pathwayAssessmentId, requirementId });

      setRun(result);

    } catch (err) {
      setError(errorService.getMessage(err));
    } finally {
      isRetry ? setRetrying(false) : setLoading(false);
    }
  }, [pathwayAssessmentId, requirementId]);

  useEffect(() => {
    void load(false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pathwayAssessmentId, requirementId]);

  const result = run?.result ?? null;
  const outcomeDisplay = result ? getOutcomeDisplay(result.currentStatus) : null;
  const firstFactId = result?.factsConsidered[0]?.factId;
  const aiStatus = result?.aiExplanationStatus;
  const aiDegraded = aiStatus === "UNAVAILABLE" || aiStatus === "FAILED";

  return (
    <motion.aside
      initial={{ opacity: 0, x: 24 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.2 }}
      className="flex h-full w-[380px] shrink-0 flex-col border-l border-white/10 bg-[#0B1220] p-4"
    >
      <div className="mb-3 flex items-center justify-between">
        <h3 className="flex items-center gap-1.5 text-sm font-bold text-white">
          <Sparkles size={14} className="text-[#C6A15B]" />
          Explain This Requirement
        </h3>
        <button
          type="button"
          onClick={onClose}
          className="rounded-lg p-1 text-slate-400 transition hover:bg-white/10 hover:text-white"
          aria-label="Close explanation"
        >
          <X size={16} />
        </button>
      </div>

      {loading && <Loader text="Investigating this requirement..." />}

      {error && <ErrorAlert message={error} />}

      {!loading && !error && result && outcomeDisplay && (
        <div className="flex-1 space-y-4 overflow-y-auto pr-1 text-sm">

          <div className="flex flex-wrap items-center gap-1.5">
            <Badge variant={outcomeDisplay.badgeVariant}>{outcomeDisplay.label}</Badge>
            <Badge variant="neutral">{result.supportStatus.replace(/_/g, " ")}</Badge>
            {result.humanReviewRequired && (
              <span className="inline-flex items-center gap-1 rounded-full border border-amber-400/20 bg-amber-400/10 px-2 py-0.5 text-[10px] font-semibold text-amber-300">
                <ShieldQuestion size={11} />
                Human review recommended
              </span>
            )}
          </div>

          <p className="text-xs text-slate-500">
            {result.requirementTitle} <span className="text-slate-600">({result.requirementKey})</span>
          </p>

          {/* Requirement assessment vs. AI explanation are two distinct,
              independently-reported states - never conflated (Phase 5.1
              Production Hardening, "never show 'assessment failed' when
              only the AI explanation failed"). */}
          <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
            <div className="flex items-center gap-1.5 rounded-lg border border-emerald-400/20 bg-emerald-400/10 px-2.5 py-1.5 text-xs font-semibold text-emerald-300">
              <CheckCircle2 size={13} />
              Requirement assessment - Completed
            </div>
            <div
              className={
                aiStatus === "GENERATED"
                  ? "flex items-center gap-1.5 rounded-lg border border-emerald-400/20 bg-emerald-400/10 px-2.5 py-1.5 text-xs font-semibold text-emerald-300"
                  : "flex items-center gap-1.5 rounded-lg border border-amber-400/20 bg-amber-400/10 px-2.5 py-1.5 text-xs font-semibold text-amber-300"
              }
            >
              {aiStatus === "GENERATED" ? <CheckCircle2 size={13} /> : <AlertTriangle size={13} />}
              AI explanation - {aiStatus === "GENERATED" ? "Generated" : "Temporarily unavailable"}
            </div>
          </div>

          <section>
            <h4 className="mb-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
              Explanation
            </h4>

            {aiDegraded ? (
              <div className="space-y-2 rounded-lg border border-amber-400/20 bg-amber-400/10 p-3 text-xs leading-relaxed text-amber-200">
                <p className="font-semibold">AI explanation is temporarily unavailable.</p>
                <p>
                  The requirement assessment is still available. The AI explanation service is currently
                  unavailable, but your requirement status and supporting evidence remain available below.
                </p>
                <button
                  type="button"
                  onClick={() => void load(true)}
                  disabled={retrying}
                  className="inline-flex items-center gap-1.5 rounded-lg border border-amber-400/30 bg-amber-400/10 px-2.5 py-1 text-xs font-semibold text-amber-200 transition hover:bg-amber-400/20 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  <RefreshCw size={12} className={retrying ? "animate-spin" : undefined} />
                  {retrying ? "Retrying..." : "Retry AI Explanation"}
                </button>
              </div>
            ) : (
              <p className="rounded-lg border border-white/10 bg-white/5 p-3 text-xs leading-relaxed text-slate-300">
                {result.explanation}
              </p>
            )}
          </section>

          {result.factsConsidered.length > 0 && (
            <section>
              <h4 className="mb-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
                Facts Considered
              </h4>
              <ul className="space-y-1.5">
                {result.factsConsidered.map((fact) => (
                  <li
                    key={fact.factId}
                    className="flex items-start justify-between gap-2 rounded-lg border border-white/10 bg-white/5 p-2.5 text-xs text-slate-300"
                  >
                    <span>
                      <span className="text-slate-500">{fact.factKey}:</span> {fact.valueSummary ?? "—"}
                    </span>
                    {fact.isVerified && (
                      <BadgeCheck size={13} className="mt-0.5 shrink-0 text-emerald-400" />
                    )}
                  </li>
                ))}
              </ul>
            </section>
          )}

          {result.evidenceGaps.length > 0 && (
            <section>
              <h4 className="mb-1.5 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
                <AlertTriangle size={12} />
                Evidence Gaps
              </h4>
              <div className="rounded-lg border border-amber-400/20 bg-amber-400/10 p-2.5 text-xs text-amber-200">
                {result.evidenceGaps.join(", ")}
              </div>
            </section>
          )}

          {result.conflicts.length > 0 && (
            <section>
              <h4 className="mb-1.5 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
                <ShieldQuestion size={12} />
                Conflicts
              </h4>
              <p className="rounded-lg border border-amber-400/20 bg-amber-400/10 p-2.5 text-xs text-amber-200">
                {result.conflicts.length} unresolved conflict{result.conflicts.length === 1 ? "" : "s"} on facts
                this requirement depends on.
              </p>
            </section>
          )}

          <section>
            <h4 className="mb-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
              Provenance
            </h4>
            <div className="space-y-2 rounded-lg border border-white/10 bg-white/5 p-2.5 text-xs text-slate-400">
              <p>
                {result.provenance.nodes.length} record{result.provenance.nodes.length === 1 ? "" : "s"} and{" "}
                {result.provenance.edges.length} relationship{result.provenance.edges.length === 1 ? "" : "s"}{" "}
                traced in the Evidence Graph.
              </p>
              {firstFactId != null && (
                <Link
                  to={`/dashboard/evidence-graph?fact=${firstFactId}`}
                  className="inline-flex items-center gap-1 text-xs font-semibold text-[#C6A15B] transition hover:text-[#dbb877]"
                >
                  <Waypoints size={12} />
                  View full Evidence Graph
                </Link>
              )}
            </div>
          </section>

          {result.recommendedNextStep && (
            <section>
              <h4 className="mb-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
                Recommended Next Step
              </h4>
              <p className="rounded-lg border border-white/10 bg-white/5 p-3 text-xs text-slate-300">
                {result.recommendedNextStep}
              </p>
            </section>
          )}

          <p className="pt-1 text-[11px] italic text-slate-500">
            This is an internal MukondoGTech AI explanation of your existing case data - never a government
            decision, and never a guarantee of any outcome.
          </p>
        </div>
      )}
    </motion.aside>
  );
}
