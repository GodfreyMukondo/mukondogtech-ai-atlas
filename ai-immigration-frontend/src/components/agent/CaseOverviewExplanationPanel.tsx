import { useCallback, useEffect, useState } from "react";

import { motion } from "framer-motion";

import {
  AlertTriangle,
  CheckCircle2,
  Clock,
  RefreshCw,
  ShieldQuestion,
  Sparkles,
  X,
} from "lucide-react";

import { explainCaseOverviewApi } from "../../api/agentApi";
import { errorService } from "../../services/errorService";
import { getRiskBandBadgeVariant, getRiskBandLabel } from "../../utils/caseIntelligenceLabels";

import type { CaseOverviewAgentRunResponse } from "../../types/agent";

import Badge from "../common/Badge";
import Loader from "../common/Loader";
import ErrorAlert from "../common/ErrorAlert";

/**
 * ============================================================================
 * CASE OVERVIEW EXPLANATION PANEL
 * ============================================================================
 *
 * Renders the result of POST /api/agent/case-overview-runs (goal:
 * EXPLAIN_CASE_OVERVIEW, Phase 5.4) - the case-wide counterpart to {@code
 * PathwayDiscoveryExplanationPanel}/{@code PathwayAssessmentExplanationPanel},
 * same layout/style family. Never fetches or renders anything beyond what
 * the backend's structured ExplainCaseOverviewResult already returned - the
 * contradictions/timeline/signals shown here are copied straight from that
 * response (the exact same shapes CaseTimelinePage's own three endpoints
 * already return), never derived or invented on the frontend. Only the
 * narrative and recommended next step are the LLM's own prose.
 *
 * `subjectUserId` is always the authenticated user's own id, passed down
 * from CaseTimelinePage exactly like that page's own three API calls - this
 * panel never lets a user supply an arbitrary subject id; the backend
 * independently re-verifies authorization on every call regardless.
 *
 * GRACEFUL AI DEGRADATION (Phase 5.4, built in from the start): the
 * deterministic overview (`result`) is ALWAYS present on a successful
 * response, whether or not the AI explanation was produced - see
 * `result.aiExplanationStatus`. This panel never conflates "the LLM could
 * not explain" with "the overview failed": the deterministic sections
 * render identically in both cases; only the Narrative section and a small
 * status strip change. "Retry AI Explanation" re-invokes ONLY the agent (a
 * new, lightweight POST against the same subject) - it never re-computes
 * contradictions/timeline/signals outside the agent, alters Facts, or
 * recreates anything.
 * ============================================================================
 */
export default function CaseOverviewExplanationPanel({
  subjectUserId,
  onClose,
}: {
  subjectUserId: number;
  onClose: () => void;
}) {

  const [run, setRun] = useState<CaseOverviewAgentRunResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [retrying, setRetrying] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async (isRetry: boolean) => {

    try {
      isRetry ? setRetrying(true) : setLoading(true);
      setError(null);

      const result = await explainCaseOverviewApi({ subjectUserId });

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
          Explain My Case
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

      {loading && <Loader text="Analyzing your case signals..." />}

      {error && <ErrorAlert message={error} />}

      {!loading && !error && result && (
        <div className="flex-1 space-y-4 overflow-y-auto pr-1 text-sm">

          {result.humanReviewRequired && (
            <span className="inline-flex items-center gap-1 rounded-full border border-amber-400/20 bg-amber-400/10 px-2 py-0.5 text-[10px] font-semibold text-amber-300">
              <ShieldQuestion size={11} />
              Human review recommended
            </span>
          )}

          {/* Case overview vs. AI explanation are two distinct,
              independently-reported states - never conflated. */}
          <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
            <div className="flex items-center gap-1.5 rounded-lg border border-emerald-400/20 bg-emerald-400/10 px-2.5 py-1.5 text-xs font-semibold text-emerald-300">
              <CheckCircle2 size={13} />
              Case overview - Completed
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
                  Your case overview was completed successfully. The AI explanation service is currently
                  unavailable, but your contradictions, timeline, and risk signals remain available below.
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
            <h4 className="mb-1.5 flex items-center justify-between text-xs font-semibold uppercase tracking-wide text-slate-500">
              <span>Risk Signals</span>
              <Badge variant={getRiskBandBadgeVariant(result.signals.riskBand)}>
                {getRiskBandLabel(result.signals.riskBand)} band
              </Badge>
            </h4>
            <div className="grid grid-cols-2 gap-2 rounded-lg border border-white/10 bg-white/5 p-3 text-[11px] text-slate-300">
              <span>Open contradictions: {result.signals.openContradictionCount}</span>
              <span>Potential overlaps: {result.signals.potentialOverlapContradictionCount}</span>
              <span>Fraud-flagged docs: {result.signals.fraudFlaggedDocumentCount}</span>
              <span>High-risk docs: {result.signals.highRiskDocumentCount}</span>
              <span>Rejected evidence: {result.signals.rejectedEvidenceItemCount}</span>
              <span>Failed validation: {result.signals.validationFailedEvidenceItemCount}</span>
            </div>
          </section>

          <section>
            <h4 className="mb-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
              Contradictions ({result.contradictions.openContradictions.length})
            </h4>
            {result.contradictions.openContradictions.length === 0
              && result.contradictions.potentialOverlaps.length === 0 ? (
              <p className="rounded-lg border border-emerald-400/20 bg-emerald-400/10 p-3 text-xs text-emerald-300">
                No open contradictions or potential overlaps.
              </p>
            ) : (
              <ul className="space-y-1.5">
                {result.contradictions.openContradictions.map((conflict) => (
                  <li
                    key={conflict.id}
                    className="rounded-lg border border-amber-400/20 bg-amber-400/10 p-2.5 text-xs text-amber-200"
                  >
                    {conflict.factKey} · {conflict.status}
                  </li>
                ))}
                {result.contradictions.potentialOverlaps.map((overlap, index) => (
                  <li
                    key={`${overlap.factKey}-${overlap.factAId}-${overlap.factBId}-${index}`}
                    className="rounded-lg border border-amber-400/20 bg-amber-400/10 p-2.5 text-xs text-amber-200"
                  >
                    {overlap.factKey} · {overlap.overlapDays} day{overlap.overlapDays === 1 ? "" : "s"} overlap
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section>
            <h4 className="mb-1.5 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
              <Clock size={12} />
              Timeline ({result.timeline.events.length} entries)
            </h4>
            {result.timeline.events.length === 0 ? (
              <p className="rounded-lg border border-white/10 bg-white/5 p-3 text-xs text-slate-400">
                No timeline entries recorded yet.
              </p>
            ) : (
              <p className="rounded-lg border border-white/10 bg-white/5 p-3 text-xs text-slate-300">
                {result.timeline.events.filter((e) => e.overlapDaysWithPreviousEntry != null).length} potential
                overlap(s) and {result.timeline.events.filter((e) => e.gapDaysBeforeThisEntry != null).length} gap(s)
                found across your recorded history.
              </p>
            )}
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

          <p className="pt-1 text-[11px] italic text-slate-500">{result.signals.note}</p>
        </div>
      )}
    </motion.aside>
  );
}
