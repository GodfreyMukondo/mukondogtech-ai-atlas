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

import { explainPathwayDiscoveryApi } from "../../api/agentApi";
import { errorService } from "../../services/errorService";
import { getOutcomeDisplay } from "../../utils/assessmentOutcome";

import type { PathwayDiscoveryAgentRunResponse } from "../../types/agent";

import Badge from "../common/Badge";
import Loader from "../common/Loader";
import ErrorAlert from "../common/ErrorAlert";

/**
 * ============================================================================
 * PATHWAY DISCOVERY EXPLANATION PANEL
 * ============================================================================
 *
 * Renders the result of POST /api/agent/discovery-runs (goal:
 * EXPLAIN_PATHWAY_DISCOVERY) as a slide-in panel - the upstream, pre-
 * assessment counterpart to PathwayAssessmentExplanationPanel, same layout/
 * style family. Never fetches or renders anything beyond what the backend's
 * structured ExplainPathwayDiscoveryResult already returned - every ranked
 * pathway shown here is copied straight from that response, never derived
 * or invented on the frontend. Only the narrative and recommended next step
 * are the LLM's own prose.
 *
 * GRACEFUL AI DEGRADATION (Phase 5.3, built in from the start): the
 * deterministic ranking (`result`) is ALWAYS present on a successful
 * response, whether or not the AI recommendation was produced - see
 * `result.aiExplanationStatus`. This panel never conflates "the LLM could
 * not recommend" with "discovery failed": the ranked-pathways list renders
 * identically in both cases; only the Narrative section and a small status
 * strip change. "Retry AI Recommendation" re-invokes ONLY the agent (a new,
 * lightweight POST against the same subject) - it never re-evaluates
 * pathways outside the agent, alters facts, or recreates anything.
 * ============================================================================
 */
export default function PathwayDiscoveryExplanationPanel({
  subjectUserId,
  onClose,
}: {
  subjectUserId: number;
  onClose: () => void;
}) {

  const [run, setRun] = useState<PathwayDiscoveryAgentRunResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [retrying, setRetrying] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async (isRetry: boolean) => {

    try {
      isRetry ? setRetrying(true) : setLoading(true);
      setError(null);

      const result = await explainPathwayDiscoveryApi({ subjectUserId });

      setRun(result);

    } catch (err) {
      setError(errorService.getMessage(err));
    } finally {
      isRetry ? setRetrying(false) : setLoading(false);
    }
  }, [subjectUserId]);

  useEffect(() => {
    void load(false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [subjectUserId]);

  const result = run?.result ?? null;
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
          Explain My Recommendations
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

      {loading && <Loader text="Analyzing your pathway options..." />}

      {error && <ErrorAlert message={error} />}

      {!loading && !error && result && (
        <div className="flex-1 space-y-4 overflow-y-auto pr-1 text-sm">

          {result.humanReviewRequired && (
            <span className="inline-flex items-center gap-1 rounded-full border border-amber-400/20 bg-amber-400/10 px-2 py-0.5 text-[10px] font-semibold text-amber-300">
              <ShieldQuestion size={11} />
              Human review recommended
            </span>
          )}

          {/* Discovery vs. AI recommendation are two distinct,
              independently-reported states - never conflated. */}
          <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
            <div className="flex items-center gap-1.5 rounded-lg border border-emerald-400/20 bg-emerald-400/10 px-2.5 py-1.5 text-xs font-semibold text-emerald-300">
              <CheckCircle2 size={13} />
              Pathway discovery - Completed
            </div>
            <div
              className={
                aiStatus === "GENERATED"
                  ? "flex items-center gap-1.5 rounded-lg border border-emerald-400/20 bg-emerald-400/10 px-2.5 py-1.5 text-xs font-semibold text-emerald-300"
                  : "flex items-center gap-1.5 rounded-lg border border-amber-400/20 bg-amber-400/10 px-2.5 py-1.5 text-xs font-semibold text-amber-300"
              }
            >
              {aiStatus === "GENERATED" ? <CheckCircle2 size={13} /> : <AlertTriangle size={13} />}
              AI recommendation - {aiStatus === "GENERATED" ? "Generated" : "Temporarily unavailable"}
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
                  Your pathway rankings were completed successfully. The AI recommendation service is
                  currently unavailable, but your ranked pathways and their alignment details remain
                  available below.
                </p>
                <button
                  type="button"
                  onClick={() => void load(true)}
                  disabled={retrying}
                  className="inline-flex items-center gap-1.5 rounded-lg border border-amber-400/30 bg-amber-400/10 px-2.5 py-1 text-xs font-semibold text-amber-200 transition hover:bg-amber-400/20 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  <RefreshCw size={12} className={retrying ? "animate-spin" : undefined} />
                  {retrying ? "Retrying..." : "Retry AI Recommendation"}
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
              Ranked Pathways ({result.rankedPathways.length})
            </h4>
            <ul className="space-y-1.5">
              {result.rankedPathways.map((row) => {
                const outcomeDisplay = getOutcomeDisplay(row.outcome);
                return (
                  <li
                    key={row.pathwayId}
                    className="rounded-lg border border-white/10 bg-white/5 p-2.5 text-xs text-slate-300"
                  >
                    <div className="flex items-center justify-between gap-2">
                      <span className="font-medium text-white">
                        #{row.rank} {row.name}
                      </span>
                      <Badge variant={outcomeDisplay.badgeVariant}>{outcomeDisplay.label}</Badge>
                    </div>
                    <p className="mt-1 text-[11px] text-slate-500">
                      Alignment {Math.round(row.overallAlignmentScore)}% · {row.jurisdiction} · {row.category}
                    </p>
                    {row.missingRequirementCount > 0 && (
                      <p className="mt-1 text-[11px] text-amber-300">
                        {row.missingRequirementCount} requirement{row.missingRequirementCount === 1 ? "" : "s"} missing
                      </p>
                    )}
                    {row.conflictingRequirementCount > 0 && (
                      <p className="mt-1 text-[11px] text-amber-300">
                        {row.conflictingRequirementCount} conflicting requirement{row.conflictingRequirementCount === 1 ? "" : "s"}
                      </p>
                    )}
                  </li>
                );
              })}
            </ul>
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

          <p className="pt-1 text-[11px] italic text-slate-500">{result.disclaimer}</p>
        </div>
      )}
    </motion.aside>
  );
}
