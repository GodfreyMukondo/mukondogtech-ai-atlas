import { useCallback, useEffect, useState } from "react";

import { motion } from "framer-motion";

import {
  AlertTriangle,
  CheckCircle2,
  RefreshCw,
  ShieldQuestion,
  Sparkles,
  X,
} from "lucide-react";

import { explainPathwayAssessmentApi } from "../../api/agentApi";
import { errorService } from "../../services/errorService";
import { getOutcomeDisplay } from "../../utils/assessmentOutcome";

import type { PathwayAssessmentAgentRunResponse } from "../../types/agent";

import Badge from "../common/Badge";
import Loader from "../common/Loader";
import ErrorAlert from "../common/ErrorAlert";

/**
 * ============================================================================
 * PATHWAY ASSESSMENT EXPLANATION PANEL
 * ============================================================================
 *
 * Renders the result of POST /api/agent/pathway-runs (goal:
 * EXPLAIN_PATHWAY_ASSESSMENT) as a slide-in panel - the whole-pathway
 * counterpart to ExplainRequirementPanel, same layout/style family. Never
 * fetches or renders anything beyond what the backend's structured
 * ExplainPathwayAssessmentResult already returned - every requirement row,
 * missing-evidence item, and readiness figure shown here is copied straight
 * from that response, never derived or invented on the frontend. Only the
 * narrative and recommended next step are the LLM's own prose.
 *
 * PHASE 5.2 PRODUCTION HARDENING - GRACEFUL AI DEGRADATION:
 * The deterministic pathway assessment (`result`) is ALWAYS present on a
 * successful response, whether or not the AI narrative was produced - see
 * `result.aiExplanationStatus`. This panel never conflates "the LLM could
 * not narrate this" with "the assessment failed": the pathway/readiness/
 * requirements/missing-evidence/human-review sections below render
 * identically in both cases; only the Narrative section and a small status
 * strip change. "Retry AI Explanation" re-invokes ONLY the agent (a new,
 * lightweight POST against the same already-computed assessment) - it never
 * re-uploads documents, re-runs the pathway assessment, or alters any fact/
 * requirement/evidence data.
 * ============================================================================
 */
export default function PathwayAssessmentExplanationPanel({
  pathwayAssessmentId,
  onClose,
}: {
  pathwayAssessmentId: number;
  onClose: () => void;
}) {

  const [run, setRun] = useState<PathwayAssessmentAgentRunResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [retrying, setRetrying] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async (isRetry: boolean) => {

    try {
      isRetry ? setRetrying(true) : setLoading(true);
      setError(null);

      const result = await explainPathwayAssessmentApi({ pathwayAssessmentId });

      setRun(result);

    } catch (err) {
      setError(errorService.getMessage(err));
    } finally {
      isRetry ? setRetrying(false) : setLoading(false);
    }
  }, [pathwayAssessmentId]);

  useEffect(() => {
    void load(false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pathwayAssessmentId]);

  const result = run?.result ?? null;
  const outcomeDisplay = result ? getOutcomeDisplay(result.overallOutcome) : null;
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
          Explain This Pathway
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

      {loading && <Loader text="Investigating this pathway assessment..." />}

      {error && <ErrorAlert message={error} />}

      {!loading && !error && result && outcomeDisplay && (
        <div className="flex-1 space-y-4 overflow-y-auto pr-1 text-sm">

          <div className="flex flex-wrap items-center gap-1.5">
            <Badge variant={outcomeDisplay.badgeVariant}>{outcomeDisplay.label}</Badge>
            {result.humanReviewRequired && (
              <span className="inline-flex items-center gap-1 rounded-full border border-amber-400/20 bg-amber-400/10 px-2 py-0.5 text-[10px] font-semibold text-amber-300">
                <ShieldQuestion size={11} />
                Human review recommended
              </span>
            )}
          </div>

          <p className="text-xs text-slate-500">
            {result.pathwayName} <span className="text-slate-600">({result.pathwayKey})</span>
          </p>

          {/* Pathway assessment vs. AI explanation are two distinct,
              independently-reported states - never conflated (Phase 5.2
              Production Hardening, "never show 'assessment failed' when
              only the AI narrative failed"). */}
          <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
            <div className="flex items-center gap-1.5 rounded-lg border border-emerald-400/20 bg-emerald-400/10 px-2.5 py-1.5 text-xs font-semibold text-emerald-300">
              <CheckCircle2 size={13} />
              Pathway assessment - Completed
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
              Narrative
            </h4>

            {aiDegraded ? (
              <div className="space-y-2 rounded-lg border border-amber-400/20 bg-amber-400/10 p-3 text-xs leading-relaxed text-amber-200">
                <p className="font-semibold">AI explanation is temporarily unavailable.</p>
                <p>
                  Your pathway assessment was completed successfully. The AI explanation service is
                  currently unavailable, but your assessment results and supporting evidence remain
                  available below.
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

          <section>
            <h4 className="mb-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
              Readiness
            </h4>
            <div className="grid grid-cols-2 gap-2 text-xs">
              <div className="rounded-lg border border-white/10 bg-white/5 p-2.5 text-slate-300">
                <p className="text-slate-500">Requirement coverage</p>
                <p className="font-semibold text-white">{result.readiness.requirementCoveragePercent}%</p>
              </div>
              <div className="rounded-lg border border-white/10 bg-white/5 p-2.5 text-slate-300">
                <p className="text-slate-500">Evidence coverage</p>
                <p className="font-semibold text-white">{result.readiness.evidenceCoveragePercent}%</p>
              </div>
              <div className="rounded-lg border border-white/10 bg-white/5 p-2.5 text-slate-300">
                <p className="text-slate-500">Consistency</p>
                <p className="font-semibold text-white">{result.readiness.consistencyPercent}%</p>
              </div>
              <div className="rounded-lg border border-white/10 bg-white/5 p-2.5 text-slate-300">
                <p className="text-slate-500">Overall</p>
                <p className="font-semibold text-white">{result.readiness.overallReadinessPercent}%</p>
              </div>
            </div>
          </section>

          <section>
            <h4 className="mb-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
              Requirements ({result.requirementSummaries.length})
            </h4>
            <ul className="space-y-1.5">
              {result.requirementSummaries.map((row) => (
                <li
                  key={row.requirementId}
                  className="rounded-lg border border-white/10 bg-white/5 p-2.5 text-xs text-slate-300"
                >
                  <div className="flex items-center justify-between gap-2">
                    <span className="font-medium text-white">{row.requirementTitle}</span>
                    <Badge variant="neutral">{row.supportStatus.replace(/_/g, " ")}</Badge>
                  </div>
                  {row.missingFactKeys.length > 0 && (
                    <p className="mt-1 text-[11px] text-amber-300">
                      Missing: {row.missingFactKeys.join(", ")}
                    </p>
                  )}
                </li>
              ))}
            </ul>
          </section>

          {result.missingEvidence.length > 0 && (
            <section>
              <h4 className="mb-1.5 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
                <AlertTriangle size={12} />
                Missing Evidence
              </h4>
              <ul className="space-y-1.5">
                {result.missingEvidence.map((item) => (
                  <li
                    key={item.requirementId}
                    className="rounded-lg border border-amber-400/20 bg-amber-400/10 p-2.5 text-xs text-amber-200"
                  >
                    {item.requirementTitle} ({item.necessity.toLowerCase()})
                  </li>
                ))}
              </ul>
            </section>
          )}

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
