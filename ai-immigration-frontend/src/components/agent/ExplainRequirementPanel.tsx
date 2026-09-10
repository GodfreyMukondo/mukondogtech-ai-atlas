import { useEffect, useState } from "react";

import { Link } from "react-router-dom";

import { motion } from "framer-motion";

import {
  AlertTriangle,
  BadgeCheck,
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
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {

    let cancelled = false;

    async function load() {
      try {
        setLoading(true);
        setError(null);

        const result = await explainRequirementApi({ pathwayAssessmentId, requirementId });

        if (!cancelled) {
          setRun(result);
        }

      } catch (err) {
        if (!cancelled) {
          setError(errorService.getMessage(err));
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void load();

    return () => {
      cancelled = true;
    };
  }, [pathwayAssessmentId, requirementId]);

  const result = run?.result ?? null;
  const outcomeDisplay = result ? getOutcomeDisplay(result.currentStatus) : null;
  const firstFactId = result?.factsConsidered[0]?.factId;

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

          <section>
            <h4 className="mb-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
              Explanation
            </h4>
            <p className="rounded-lg border border-white/10 bg-white/5 p-3 text-xs leading-relaxed text-slate-300">
              {result.explanation}
            </p>
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
