import { useCallback, useEffect, useState } from "react";

import { Link, useNavigate, useParams } from "react-router-dom";

import { motion } from "framer-motion";

import {
  AlertTriangle,
  ArrowLeft,
  Bot,
  Compass,
  Gauge,
  RefreshCw,
  Sparkles,
} from "lucide-react";

import { getPathwayAssessmentApi } from "../../api/pathwayApi";
import { getAssessmentErrorMessage } from "../../utils/assessmentErrors";
import {
  getCertaintyLabel,
  getOutcomeDisplay,
} from "../../utils/assessmentOutcome";

import type { PathwayAssessment } from "../../types/pathwayAssessment";

import Badge from "../../components/common/Badge";
import Button from "../../components/common/Button";
import Loader from "../../components/common/Loader";
import ErrorAlert from "../../components/common/ErrorAlert";
import EmptyState from "../../components/common/EmptyState";
import ExplainRequirementPanel from "../../components/agent/ExplainRequirementPanel";

/**
 * ============================================================================
 * PATHWAY ASSESSMENT RESULTS PAGE
 * ============================================================================
 *
 * Displays a previously computed PathwayAssessment
 * (GET /api/pathways/assessments/{assessmentId}), including every
 * Requirement Evaluation that fed it.
 *
 * The assessment ID is always present in this page's URL
 * (/dashboard/pathways/assessments/:assessmentId) so it can be shared,
 * revisited, and used by the Explainability view
 * (/dashboard/pathways/assessments/:assessmentId/explanation, linked below)
 * to request GET /api/pathways/assessments/{assessmentId}/explanation.
 *
 * This page never stores or re-derives Fact data itself: contributing
 * Facts and unresolved conflicts are shown only as counts already present
 * in the API response, never fetched or cached separately.
 * ============================================================================
 */
export default function PathwayAssessmentResultsPage() {

  const { assessmentId } = useParams<{ assessmentId: string }>();
  const navigate = useNavigate();

  const [assessment, setAssessment] = useState<PathwayAssessment | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [explainingRequirementId, setExplainingRequirementId] = useState<number | null>(null);

  const numericAssessmentId = assessmentId ? Number(assessmentId) : null;

  const loadAssessment = useCallback(async () => {

    if (!numericAssessmentId || Number.isNaN(numericAssessmentId)) {
      setError("This assessment link is invalid.");
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);

      const result = await getPathwayAssessmentApi(numericAssessmentId);

      setAssessment(result);

    } catch (err) {
      setError(getAssessmentErrorMessage(err));
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [assessmentId]);

  useEffect(() => {
    void loadAssessment();
  }, [loadAssessment]);

  if (loading) {
    return (
      <div className="w-full px-4 py-16 sm:px-6 lg:px-8">
        <Loader text="Loading your assessment..." />
      </div>
    );
  }

  if (error) {
    return (
      <div className="w-full space-y-4 px-4 py-10 sm:px-6 lg:px-8">
        <ErrorAlert message={error} />
        <div className="flex gap-3">
          <Button variant="secondary" onClick={() => void loadAssessment()}>
            <RefreshCw size={16} />
            Try again
          </Button>
          <Link to="/dashboard/pathways/assessments/new">
            <Button variant="outline">Request another assessment</Button>
          </Link>
        </div>
      </div>
    );
  }

  if (!assessment) {
    return (
      <div className="w-full px-4 py-10 sm:px-6 lg:px-8">
        <EmptyState
          icon={<Compass size={40} />}
          title="Assessment not found"
          description="We could not find this pathway assessment."
          action={
            <Link to="/dashboard/pathways/assessments/new">
              <Button variant="primary">Request an assessment</Button>
            </Link>
          }
        />
      </div>
    );
  }

  const overallOutcome = getOutcomeDisplay(assessment.outcome);

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className="w-full px-4 py-10 sm:px-6 lg:px-8"
    >
      <button
        type="button"
        onClick={() => navigate("/dashboard/pathways/assessments/new")}
        className="mb-6 inline-flex items-center gap-1 text-sm text-slate-400 transition hover:text-white"
      >
        <ArrowLeft size={16} />
        Request another assessment
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
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-xs uppercase tracking-wide text-slate-500">
              Assessment #{assessment.id}
            </p>

            <h1 className="mt-1 text-2xl font-black text-white">
              {assessment.pathwayName}
            </h1>

            <p className="mt-1 text-sm text-slate-400">
              Assessed as of{" "}
              {new Date(assessment.assessmentDate).toLocaleString()}
            </p>
          </div>

          <Badge variant={overallOutcome.badgeVariant}>{overallOutcome.label}</Badge>
        </div>

        <p className="mt-4 text-sm text-slate-300">{overallOutcome.description}</p>

        <p className="mt-3 text-xs text-slate-500">
          Overall {getCertaintyLabel(assessment.assessmentConfidenceLevel).toLowerCase()}
          {" · "}Computed {new Date(assessment.computedAt).toLocaleString()}
        </p>

        <div className="mt-5 flex flex-wrap gap-3">
          <Link to={`/dashboard/pathways/assessments/${assessment.id}/explanation`}>
            <Button variant="secondary">
              <Sparkles size={16} />
              Why this result?
            </Button>
          </Link>

          <Link to={`/dashboard/pathways/assessments/${assessment.id}/intelligence`}>
            <Button variant="outline">
              <Gauge size={16} />
              Case Intelligence
            </Button>
          </Link>
        </div>
      </header>

      <section className="mt-8">
        <h2 className="mb-4 text-lg font-bold text-white">Requirement Evaluations</h2>

        {assessment.requirementEvaluations.length === 0 ? (
          <EmptyState
            icon={<AlertTriangle size={40} />}
            title="No requirement evaluations"
            description="This assessment did not produce any requirement evaluations."
          />
        ) : (
          <ul className="space-y-3">
            {assessment.requirementEvaluations.map((evaluation) => {
              const display = getOutcomeDisplay(evaluation.outcome);

              return (
                <li
                  key={evaluation.id}
                  className="
                    rounded-xl
                    border
                    border-white/10
                    bg-white/5
                    p-5
                    transition
                    hover:border-white/20
                  "
                >
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <p className="font-semibold text-white">
                        {evaluation.requirementTitle}
                      </p>
                      <p className="text-xs text-slate-500">{evaluation.requirementKey}</p>
                    </div>

                    <Badge variant={display.badgeVariant}>{display.label}</Badge>
                  </div>

                  <p className="mt-3 text-sm text-slate-300">{display.description}</p>

                  {evaluation.explanation && (
                    <p className="mt-2 text-sm text-slate-400">{evaluation.explanation}</p>
                  )}

                  <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-500">
                    <span>{getCertaintyLabel(evaluation.certaintyLevel)}</span>

                    {evaluation.contributingFactIds.length > 0 && (
                      <span>
                        Based on {evaluation.contributingFactIds.length} piece
                        {evaluation.contributingFactIds.length === 1 ? "" : "s"} of evidence
                      </span>
                    )}

                    {evaluation.unresolvedConflictIds.length > 0 && (
                      <span className="text-amber-400">
                        {evaluation.unresolvedConflictIds.length} unresolved conflict
                        {evaluation.unresolvedConflictIds.length === 1 ? "" : "s"} to review
                      </span>
                    )}
                  </div>

                  <div className="mt-3 flex flex-wrap items-center justify-end gap-4 border-t border-white/10 pt-3">
                    <button
                      type="button"
                      onClick={() => setExplainingRequirementId(evaluation.requirementId)}
                      className="inline-flex items-center gap-1 text-xs font-semibold text-[#C6A15B] transition hover:text-[#dbb877]"
                    >
                      <Bot size={12} />
                      Explain this requirement
                    </button>
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </section>

      {explainingRequirementId != null && (
        <div className="fixed inset-y-0 right-0 z-40 h-full">
          <ExplainRequirementPanel
            pathwayAssessmentId={assessment.id}
            requirementId={explainingRequirementId}
            onClose={() => setExplainingRequirementId(null)}
          />
        </div>
      )}
    </motion.div>
  );
}
