import { useCallback, useEffect, useState } from "react";

import { Link, useNavigate, useParams } from "react-router-dom";

import { motion } from "framer-motion";

import {
  AlertTriangle,
  ArrowLeft,
  Gauge,
  Gavel,
  Lightbulb,
  ListChecks,
  RefreshCw,
  Search,
} from "lucide-react";

import { getCaseIntelligenceApi } from "../../api/caseIntelligenceApi";
import { getAssessmentErrorMessage } from "../../utils/assessmentErrors";
import { getCertaintyLabel, getOutcomeDisplay } from "../../utils/assessmentOutcome";
import { getProvenanceLabel, getRegulatoryVerificationLabel } from "../../utils/explanationLabels";
import {
  getNecessityLabel,
  getRiskBandBadgeVariant,
  getRiskBandLabel,
  getSeverityBadgeVariant,
  getSeverityLabel,
  getSupportStatusBadgeVariant,
  getSupportStatusDescription,
  getSupportStatusLabel,
} from "../../utils/caseIntelligenceLabels";
import { formatFactKeyLabel } from "../../utils/factLabels";

import type {
  CaseIntelligence,
  CaseOverviewSummary,
  MissingEvidenceItem,
  OutstandingIssue,
  RequirementEvidenceMatrixRow,
} from "../../types/caseIntelligence";

import Badge from "../../components/common/Badge";
import Button from "../../components/common/Button";
import Loader from "../../components/common/Loader";
import ErrorAlert from "../../components/common/ErrorAlert";
import EmptyState from "../../components/common/EmptyState";

/**
 * ============================================================================
 * CASE INTELLIGENCE PAGE
 * ============================================================================
 *
 * Renders GET /api/cases/pathway-assessments/{assessmentId}/intelligence:
 * a Case Overview summary, multi-dimensional readiness breakdown, the
 * Requirement-to-Evidence Matrix, and a prioritized missing-evidence list
 * for one PathwayAssessment (Master Platform Expansion, Phase 2).
 *
 *   Immigration Profile
 *   |- Case Intelligence (this page)
 *      |- Case Overview
 *      |- Requirement Coverage (readiness + evidence matrix)
 *      |- Missing Evidence
 *   (Contradictions / Timeline / Risk & Anomaly Signals live on the
 *   case-wide Case Timeline & Signals page, linked from here.)
 *
 * Reuses the exact assessmentId already exposed by
 * PathwayAssessmentResultsPage's route - this page never mints a second
 * identifier for the same assessment, and never re-derives or caches Fact
 * data beyond what this one response returns.
 *
 * PATHWAY-AGNOSTIC RENDERING: nothing on this page branches on a pathway
 * or requirement key - every section renders purely from the generic
 * support-status/severity/necessity vocabulary the backend returns, so a
 * newly published pathway renders correctly with zero frontend changes.
 *
 * IMPORTANT: readiness percentages are never a probability of visa
 * approval, and no outstanding issue or missing-evidence entry here is, or
 * implies, a fraud finding.
 * ============================================================================
 */
export default function CaseIntelligencePage() {

  const { assessmentId } = useParams<{ assessmentId: string }>();
  const navigate = useNavigate();

  const [intelligence, setIntelligence] = useState<CaseIntelligence | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const numericAssessmentId = assessmentId ? Number(assessmentId) : null;

  const loadIntelligence = useCallback(async () => {

    if (!numericAssessmentId || Number.isNaN(numericAssessmentId)) {
      setError("This assessment link is invalid.");
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);

      const result = await getCaseIntelligenceApi(numericAssessmentId);

      setIntelligence(result);

    } catch (err) {
      setError(getAssessmentErrorMessage(err));
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [assessmentId]);

  useEffect(() => {
    void loadIntelligence();
  }, [loadIntelligence]);

  const backToResultsPath = assessmentId
    ? `/dashboard/pathways/assessments/${assessmentId}`
    : "/dashboard/pathways/assessments/new";

  if (loading) {
    return (
      <div className="w-full px-4 py-16 sm:px-6 lg:px-8">
        <Loader text="Building your case intelligence..." />
      </div>
    );
  }

  if (error) {
    return (
      <div className="w-full space-y-4 px-4 py-10 sm:px-6 lg:px-8">
        <ErrorAlert message={error} />
        <div className="flex gap-3">
          <Button variant="secondary" onClick={() => void loadIntelligence()}>
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

  if (!intelligence) {
    return (
      <div className="w-full px-4 py-10 sm:px-6 lg:px-8">
        <EmptyState
          icon={<Gauge size={40} />}
          title="No case intelligence available"
          description="We could not build a readiness breakdown for this assessment."
          action={
            <Link to={backToResultsPath}>
              <Button variant="primary">Back to results</Button>
            </Link>
          }
        />
      </div>
    );
  }

  const overallOutcome = getOutcomeDisplay(intelligence.overallOutcome);

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className="w-full px-4 py-10 sm:px-6 lg:px-8"
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
          <Gauge size={14} />
          Case Intelligence
        </div>

        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-xs uppercase tracking-wide text-slate-500">
              Assessment #{intelligence.pathwayAssessmentId}
            </p>
            <h1 className="mt-1 text-2xl font-black text-white">{intelligence.pathwayName}</h1>
          </div>

          <Badge variant={overallOutcome.badgeVariant}>{overallOutcome.label}</Badge>
        </div>

        <p className="mt-3 text-xs text-slate-500">
          This is not a prediction of the final decision - it reflects how complete and
          consistent your case information is against this pathway's requirements as of{" "}
          {new Date(intelligence.generatedAt).toLocaleString()}.
        </p>

        <div className="mt-4">
          <Link
            to="/dashboard/case-timeline"
            className="text-xs font-semibold text-[#C6A15B] transition hover:text-[#dbb877]"
          >
            View case-wide contradictions, timeline &amp; risk signals &rarr;
          </Link>
        </div>
      </header>

      <CaseOverviewSection overview={intelligence.overview} />

      <ReadinessSection readiness={intelligence.readiness} />

      <MissingEvidenceSection
        items={intelligence.missingEvidence}
        recommendedEvidenceGuidance={intelligence.recommendedEvidenceGuidance}
      />

      <EvidenceMatrixSection rows={intelligence.evidenceMatrix} />
    </motion.div>
  );
}

/* ============================================================================
   CASE OVERVIEW SECTION
============================================================================ */

function CaseOverviewSection({ overview }: { overview: CaseOverviewSummary }) {
  return (
    <section className="mt-8">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="flex items-center gap-2 text-lg font-bold text-white">
          <Gauge size={20} className="text-[#C6A15B]" />
          Case Overview
        </h2>
        <Badge variant={getRiskBandBadgeVariant(overview.riskBand)}>
          {getRiskBandLabel(overview.riskBand)} risk band
        </Badge>
      </div>

      <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
        <OverviewTile label="Overall Readiness" value={`${overview.overallReadinessPercent.toFixed(0)}%`} emphasis />
        <OverviewTile label="Requirement Coverage" value={`${overview.requirementCoveragePercent.toFixed(0)}%`} />
        <OverviewTile label="Evidence Coverage" value={`${overview.evidenceCoveragePercent.toFixed(0)}%`} />
        <OverviewTile label="Missing Evidence" value={String(overview.missingEvidenceCount)} />
        <OverviewTile label="Needs Verification" value={String(overview.verificationNeededCount)} />
        <OverviewTile label="Contradictions" value={String(overview.contradictionCount)} />
      </div>

      <p className="mt-3 text-xs text-slate-500">{overview.note}</p>
    </section>
  );
}

function OverviewTile({ label, value, emphasis }: { label: string; value: string; emphasis?: boolean }) {
  return (
    <div
      className={`
        rounded-xl border p-4
        ${emphasis ? "border-[#C6A15B]/30 bg-[#C6A15B]/10" : "border-white/10 bg-white/5"}
      `}
    >
      <p className={`text-2xl font-black tabular-nums ${emphasis ? "text-[#C6A15B]" : "text-white"}`}>{value}</p>
      <p className="mt-1 text-xs text-slate-500">{label}</p>
    </div>
  );
}

/* ============================================================================
   READINESS SECTION
============================================================================ */

function ReadinessSection({ readiness }: { readiness: CaseIntelligence["readiness"] }) {
  return (
    <section className="mt-8">
      <h2 className="mb-4 flex items-center gap-2 text-lg font-bold text-white">
        <ListChecks size={20} className="text-[#C6A15B]" />
        Requirement Coverage
      </h2>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <ReadinessTile label="Requirement Coverage" value={readiness.requirementCoveragePercent} />
        <ReadinessTile label="Evidence Coverage" value={readiness.evidenceCoveragePercent} />
        <ReadinessTile label="Consistency" value={readiness.consistencyPercent} />
      </div>

      {readiness.outstandingIssues.length > 0 && (
        <div className="mt-4 space-y-3">
          <h3 className="text-sm font-semibold text-slate-300">Outstanding Issues</h3>
          <ul className="space-y-2">
            {readiness.outstandingIssues.map((issue) => (
              <OutstandingIssueRow key={issue.requirementId} issue={issue} />
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}

function ReadinessTile({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-xl border border-white/10 bg-white/5 p-5">
      <p className="text-xs uppercase tracking-wide text-slate-500">{label}</p>
      <p className="mt-2 text-3xl font-black tabular-nums text-white">{value.toFixed(0)}%</p>
    </div>
  );
}

function OutstandingIssueRow({ issue }: { issue: OutstandingIssue }) {
  const outcomeDisplay = getOutcomeDisplay(issue.outcome);

  return (
    <li className="rounded-xl border border-white/10 bg-white/5 p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="font-semibold text-white">{issue.requirementTitle}</p>
          <p className="text-xs text-slate-500">
            {issue.requirementKey}
            {issue.mandatory ? " · Mandatory" : " · Optional"}
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Badge variant={getSeverityBadgeVariant(issue.severity)}>{getSeverityLabel(issue.severity)}</Badge>
          <Badge variant={outcomeDisplay.badgeVariant}>{outcomeDisplay.label}</Badge>
        </div>
      </div>

      {issue.reason && <p className="mt-2 text-sm text-slate-400">{issue.reason}</p>}
    </li>
  );
}

/* ============================================================================
   MISSING EVIDENCE SECTION
============================================================================ */

function MissingEvidenceSection({
  items,
  recommendedEvidenceGuidance,
}: {
  items: MissingEvidenceItem[];
  recommendedEvidenceGuidance?: string | null;
}) {
  return (
    <section className="mt-8">
      <h2 className="mb-4 flex items-center gap-2 text-lg font-bold text-white">
        <Search size={20} className="text-[#C6A15B]" />
        Missing Evidence
      </h2>

      {items.length === 0 ? (
        <div className="rounded-xl border border-emerald-400/20 bg-emerald-400/10 p-4 text-sm text-emerald-300">
          No requirement is currently blocked on missing evidence.
        </div>
      ) : (
        <ul className="space-y-3">
          {items.map((item) => (
            <li key={item.requirementId} className="rounded-xl border border-white/10 bg-white/5 p-4">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <p className="font-semibold text-white">{item.requirementTitle}</p>
                  <p className="text-xs text-slate-500">
                    {item.requirementKey} · {getNecessityLabel(item.necessity)}
                  </p>
                </div>

                <Badge variant={getSeverityBadgeVariant(item.priority)}>
                  {getSeverityLabel(item.priority)} priority
                </Badge>
              </div>

              {item.missingFactKeys.length > 0 && (
                <div className="mt-3 flex flex-wrap gap-2">
                  {item.missingFactKeys.map((factKey) => (
                    <span
                      key={factKey}
                      className="rounded-full border border-white/10 bg-black/20 px-2.5 py-1 text-xs text-slate-300"
                    >
                      {formatFactKeyLabel(factKey)}
                    </span>
                  ))}
                </div>
              )}

              {item.reason && <p className="mt-3 text-sm text-slate-400">{item.reason}</p>}
            </li>
          ))}
        </ul>
      )}

      {recommendedEvidenceGuidance && (
        <div className="mt-4 flex items-start gap-2 rounded-xl border border-blue-400/20 bg-blue-400/10 p-4 text-sm text-blue-200">
          <Lightbulb size={16} className="mt-0.5 shrink-0" />
          <div>
            <p className="font-semibold text-blue-100">Recommended, not required</p>
            <p className="mt-1 text-blue-200/90">{recommendedEvidenceGuidance}</p>
          </div>
        </div>
      )}
    </section>
  );
}

/* ============================================================================
   EVIDENCE MATRIX SECTION
============================================================================ */

function EvidenceMatrixSection({ rows }: { rows: RequirementEvidenceMatrixRow[] }) {
  return (
    <section className="mt-8">
      <h2 className="mb-4 flex items-center gap-2 text-lg font-bold text-white">
        <ListChecks size={20} className="text-[#C6A15B]" />
        Requirement-to-Evidence Matrix
      </h2>

      {rows.length === 0 ? (
        <EmptyState
          icon={<AlertTriangle size={40} />}
          title="No requirements to show"
          description="This assessment did not evaluate any requirements."
        />
      ) : (
        <div className="overflow-x-auto rounded-xl border border-white/10">
          <table className="w-full min-w-[860px] border-collapse text-sm">
            <thead>
              <tr className="border-b border-white/10 bg-white/5 text-left text-xs uppercase tracking-wide text-slate-500">
                <th className="px-4 py-3 font-semibold">Requirement</th>
                <th className="px-4 py-3 font-semibold">Status</th>
                <th className="px-4 py-3 font-semibold">Supporting Evidence</th>
                <th className="px-4 py-3 font-semibold">Missing</th>
                <th className="px-4 py-3 font-semibold">Certainty</th>
                <th className="px-4 py-3 font-semibold">Regulatory Source</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.requirementId} className="border-b border-white/5 last:border-0">
                  <td className="px-4 py-3 align-top">
                    <p className="font-medium text-white">{row.requirementTitle}</p>
                    <p className="text-xs text-slate-500">
                      {row.requirementKey}
                      {row.mandatory ? " · Mandatory" : " · Optional"}
                    </p>
                    {row.explanation && <p className="mt-1 text-xs text-slate-400">{row.explanation}</p>}
                    {row.conflictingFactConflictIds.length > 0 && (
                      <p className="mt-1 text-xs text-amber-400">
                        {row.conflictingFactConflictIds.length} unresolved conflict
                        {row.conflictingFactConflictIds.length === 1 ? "" : "s"}
                      </p>
                    )}
                  </td>
                  <td className="px-4 py-3 align-top">
                    <Badge variant={getSupportStatusBadgeVariant(row.supportStatus)}>
                      {getSupportStatusLabel(row.supportStatus)}
                    </Badge>
                    <p className="mt-1 max-w-[220px] text-[11px] text-slate-500">
                      {getSupportStatusDescription(row.supportStatus)}
                    </p>
                  </td>
                  <td className="px-4 py-3 align-top text-slate-300">
                    {row.supportingFacts.length === 0 ? (
                      <span className="text-slate-500">None yet</span>
                    ) : (
                      <ul className="space-y-1">
                        {row.supportingFacts.map((fact) => (
                          <li key={fact.factId} className="text-xs">
                            <span className="text-white">{fact.valueSummary ?? fact.factKey}</span>
                            <span className="text-slate-500"> · {getProvenanceLabel(fact.provenanceType)}</span>
                          </li>
                        ))}
                      </ul>
                    )}
                  </td>
                  <td className="px-4 py-3 align-top text-slate-300">
                    {row.missingFactKeys.length === 0 ? (
                      <span className="text-slate-500">—</span>
                    ) : (
                      <ul className="space-y-1">
                        {row.missingFactKeys.map((factKey) => (
                          <li key={factKey} className="text-xs text-slate-300">
                            {formatFactKeyLabel(factKey)}
                          </li>
                        ))}
                      </ul>
                    )}
                  </td>
                  <td className="px-4 py-3 align-top text-xs text-slate-400">
                    {getCertaintyLabel(row.certaintyLevel)}
                  </td>
                  <td className="px-4 py-3 align-top text-xs text-slate-400">
                    <div className="flex items-start gap-1.5">
                      <Gavel size={12} className="mt-0.5 shrink-0 text-slate-500" />
                      <div>
                        <p>{row.regulatorySourceAuthority}</p>
                        <p className="text-slate-500">
                          {getRegulatoryVerificationLabel(row.regulatoryVerificationStatus)}
                        </p>
                      </div>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
