import { Link } from "react-router-dom";

import { BadgeCheck, ChevronRight, FileText, Waypoints } from "lucide-react";

import {
  formatFactKeyLabel,
  formatFactValue,
  getFactConfidenceLabel,
} from "../../utils/factLabels";
import { getEvidenceSourceLabel, getProvenanceLabel } from "../../utils/explanationLabels";

import type { Fact } from "../../types/fact";

import Badge from "../common/Badge";

/**
 * ============================================================================
 * FACT CARD
 * ============================================================================
 *
 * Renders one Fact's value alongside its verification/provenance/confidence/
 * evidence - shared by the Digital Twin/Profile page and the Fact Conflict
 * detail page so both present the exact same semantics for a Fact,
 * never two diverging renderings of "verified", "confidence", or evidence.
 *
 * Deliberate distinctions preserved here:
 *
 * - The "Verified" badge reflects ONLY isVerified - it is never derived
 *   from confidenceLevel, and confidenceLevel is never labeled "verified".
 * - Provenance (how the Fact originated) is shown separately from both.
 * ============================================================================
 */
export default function FactCard({
  fact,
  highlight,
}: {
  fact: Fact;
  /** Optional emphasis border (e.g. to mark the winning side of a resolved conflict). */
  highlight?: "success" | "neutral";
}) {

  const highlightClass =
    highlight === "success"
      ? "border-emerald-400/30 bg-emerald-400/5"
      : "border-white/10 bg-white/5";

  return (
    <div
      className={`
        rounded-xl
        border
        p-5
        transition
        hover:border-white/20
        ${highlightClass}
      `}
    >
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-xs uppercase tracking-wide text-slate-500">
            {formatFactKeyLabel(fact.factKey)}
          </p>
          <p className="mt-1 text-lg font-semibold text-white">{formatFactValue(fact)}</p>
        </div>

        {fact.isVerified ? (
          <span className="inline-flex items-center gap-1 rounded-full border border-emerald-400/20 bg-emerald-400/10 px-2 py-0.5 text-[10px] font-semibold text-emerald-300">
            <BadgeCheck size={12} />
            Verified
          </span>
        ) : (
          <Badge variant="neutral">Not independently verified</Badge>
        )}
      </div>

      <div className="mt-3 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-400">
        <span>{getProvenanceLabel(fact.provenanceType)}</span>
        <span aria-hidden="true">·</span>
        <span>{getFactConfidenceLabel(fact.confidenceLevel)}</span>

        {fact.effectiveFrom && (
          <>
            <span aria-hidden="true">·</span>
            <span>
              Effective since {new Date(fact.effectiveFrom).toLocaleDateString()}
              {fact.effectiveTo && <> until {new Date(fact.effectiveTo).toLocaleDateString()}</>}
            </span>
          </>
        )}
      </div>

      {fact.evidence.length > 0 && (
        <ul className="mt-3 space-y-1.5 border-t border-white/10 pt-2">
          {fact.evidence.map((evidence) => (
            <li key={evidence.id} className="flex items-start gap-2 text-xs text-slate-500">
              <FileText size={13} className="mt-0.5 shrink-0" />
              <span>
                {getEvidenceSourceLabel(evidence.sourceType)}
                {evidence.documentId != null && <> · Document #{evidence.documentId}</>}
                {evidence.sourceLocator && <> · {evidence.sourceLocator}</>}
              </span>
            </li>
          ))}
        </ul>
      )}

      <div className="mt-3 border-t border-white/10 pt-2 text-right">
        <Link
          to={`/dashboard/evidence-graph?fact=${fact.id}`}
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
