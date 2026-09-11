import { useEffect, useState } from "react";

import { motion } from "framer-motion";

import {
  AlertTriangle,
  BadgeCheck,
  Brain,
  FileText,
  Sparkles,
  X,
} from "lucide-react";

import { getEvidenceItemApi } from "../../api/evidenceGraphApi";
import { errorService } from "../../services/errorService";
import { getDirectnessLabel, getEvidenceStatusLabel } from "../../utils/evidenceGraphLabels";

import type { EvidenceItem } from "../../types/evidenceGraph";

import Badge from "../common/Badge";
import Loader from "../common/Loader";
import ErrorAlert from "../common/ErrorAlert";

/**
 * ============================================================================
 * EVIDENCE DETAIL PANEL
 * ============================================================================
 *
 * Renders GET /api/evidence-graph/evidence-items/{evidenceItemId} as three
 * strictly separate sections - never merged into one flat list - so an
 * AI's interpretation of a document can never be mistaken for the
 * document's own content, and a derived Fact's confidence can never be
 * mistaken for independent verification:
 *
 *   SOURCE DATA           - what the document itself says
 *   SYSTEM INTERPRETATION - how/when the system read it
 *   DERIVED CONCLUSION    - the Fact this evidence produced, and its own,
 *                           entirely separate verification/confidence state
 * ============================================================================
 */

export default function EvidenceDetailPanel({
  evidenceItemId,
  onClose,
}: {
  evidenceItemId: number;
  onClose: () => void;
}) {

  const [item, setItem] = useState<EvidenceItem | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {

    let cancelled = false;

    async function load() {
      try {
        setLoading(true);
        setError(null);

        const result = await getEvidenceItemApi(evidenceItemId);

        if (!cancelled) {
          setItem(result);
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
  }, [evidenceItemId]);

  return (
    <motion.aside
      initial={{ opacity: 0, x: 24 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.2 }}
      className="flex h-full w-[340px] shrink-0 flex-col border-l border-white/10 bg-[#0B1220] p-4"
    >
      <div className="mb-3 flex items-center justify-between">
        <h3 className="text-sm font-bold text-white">Evidence Detail</h3>
        <button
          type="button"
          onClick={onClose}
          className="rounded-lg p-1 text-slate-400 transition hover:bg-white/10 hover:text-white"
          aria-label="Close evidence detail"
        >
          <X size={16} />
        </button>
      </div>

      {loading && <Loader text="Loading evidence..." />}

      {error && <ErrorAlert message={error} />}

      {!loading && !error && item && (
        <div className="flex-1 space-y-4 overflow-y-auto pr-1 text-sm">
          <div className="flex flex-wrap items-center gap-1.5">
            <Badge variant="neutral">{getEvidenceStatusLabel(item.status)}</Badge>
            {item.directness && <Badge variant="primary">{getDirectnessLabel(item.directness)}</Badge>}
          </div>

          {item.rejectionReason && (
            <div className="flex items-start gap-2 rounded-lg border border-red-400/20 bg-red-400/10 p-2.5 text-xs text-red-300">
              <AlertTriangle size={13} className="mt-0.5 shrink-0" />
              {item.rejectionReason}
            </div>
          )}

          <section>
            <h4 className="mb-1.5 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
              <FileText size={13} />
              Source Data
            </h4>
            <div className="space-y-1 rounded-lg border border-white/10 bg-white/5 p-3 text-xs text-slate-300">
              <p>
                <span className="text-slate-500">Document:</span>{" "}
                {item.sourceData.documentFileName ?? "—"}
                {item.sourceData.documentVersionNumber != null && (
                  <> (version {item.sourceData.documentVersionNumber})</>
                )}
              </p>
              {item.sourceData.sourceSnippet && (
                <p className="italic text-slate-400">&ldquo;{item.sourceData.sourceSnippet}&rdquo;</p>
              )}
              {item.sourceData.documentIssueDate && (
                <p>
                  <span className="text-slate-500">Issued:</span>{" "}
                  {new Date(item.sourceData.documentIssueDate).toLocaleDateString()}
                </p>
              )}
              {item.sourceData.documentExpiryDate && (
                <p>
                  <span className="text-slate-500">Expires:</span>{" "}
                  {new Date(item.sourceData.documentExpiryDate).toLocaleDateString()}
                </p>
              )}
            </div>
          </section>

          <section>
            <h4 className="mb-1.5 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
              <Sparkles size={13} />
              System Interpretation
            </h4>
            <div className="space-y-1 rounded-lg border border-white/10 bg-white/5 p-3 text-xs text-slate-300">
              <p>
                <span className="text-slate-500">Extraction method:</span>{" "}
                {item.systemInterpretation.extractionMethod ?? "—"}
              </p>
              <p>
                <span className="text-slate-500">Extraction confidence:</span>{" "}
                {item.systemInterpretation.extractionConfidence != null
                  ? `${Math.round(item.systemInterpretation.extractionConfidence * 100)}%`
                  : "—"}
              </p>
              <p>
                <span className="text-slate-500">Extracted:</span>{" "}
                {new Date(item.systemInterpretation.extractedAt).toLocaleString()}
              </p>
              <p className="pt-1 text-[11px] italic text-slate-500">
                This is the system's reading of the document, not the document's own content.
              </p>
            </div>
          </section>

          <section>
            <h4 className="mb-1.5 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
              <Brain size={13} />
              Derived Conclusion
            </h4>
            {item.derivedConclusion ? (
              <div className="space-y-1.5 rounded-lg border border-white/10 bg-white/5 p-3 text-xs text-slate-300">
                <p>
                  <span className="text-slate-500">Fact:</span> {item.derivedConclusion.factKey}
                </p>
                <p>
                  <span className="text-slate-500">Value:</span>{" "}
                  {item.derivedConclusion.valueSummary ?? "—"}
                </p>
                <div className="flex items-center gap-1.5 pt-1">
                  {item.derivedConclusion.isVerified ? (
                    <span className="inline-flex items-center gap-1 rounded-full border border-emerald-400/20 bg-emerald-400/10 px-2 py-0.5 text-[10px] font-semibold text-emerald-300">
                      <BadgeCheck size={11} />
                      Independently verified
                    </span>
                  ) : (
                    <Badge variant="neutral">Not independently verified</Badge>
                  )}
                </div>
                <p className="pt-1 text-[11px] italic text-slate-500">
                  Confidence and verification are separate: this evidence's extraction confidence never
                  by itself makes the Fact verified.
                </p>
              </div>
            ) : (
              <p className="text-xs text-slate-500">This evidence has not produced a Fact yet.</p>
            )}
          </section>
        </div>
      )}
    </motion.aside>
  );
}
