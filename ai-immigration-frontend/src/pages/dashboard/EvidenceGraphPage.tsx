import { useCallback, useEffect, useState } from "react";

import { Link, useNavigate, useSearchParams } from "react-router-dom";

import { motion } from "framer-motion";

import { ArrowLeft, RefreshCw, Waypoints } from "lucide-react";

import {
  getEvidenceGraphForDocumentApi,
  getEvidenceGraphForFactApi,
  getEvidenceGraphForRequirementEvaluationApi,
} from "../../api/evidenceGraphApi";
import { errorService } from "../../services/errorService";

import type { EvidenceGraph } from "../../types/evidenceGraph";

import EvidenceGraphCanvas from "../../components/evidenceGraph/EvidenceGraphCanvas";
import EvidenceDetailPanel from "../../components/evidenceGraph/EvidenceDetailPanel";
import Button from "../../components/common/Button";
import Loader from "../../components/common/Loader";
import ErrorAlert from "../../components/common/ErrorAlert";
import EmptyState from "../../components/common/EmptyState";

/**
 * ============================================================================
 * EVIDENCE INTELLIGENCE GRAPH PAGE
 * ============================================================================
 *
 * Renders one of three read-only traceability views, chosen by which query
 * parameter is present on the URL:
 *
 *   ?fact={factId}        GET /api/evidence-graph/facts/{factId}
 *                          forward trace: evidence, documents, conflicts,
 *                          and requirement evaluations that relate to one Fact
 *   ?document={documentId} GET /api/evidence-graph/documents/{documentId}
 *                          backward trace + document impact (Phase 3)
 *   ?evaluation={evaluationId} GET /api/evidence-graph/requirement-evaluations/{evaluationId}
 *                          requirement traceability (Phase 3)
 *
 * Entered via a contextual "Trace" link from an existing Fact/Document/
 * Requirement-evaluation view (never a free-standing "browse everything"
 * screen) - the id is the one thing this page reads from the URL, and the
 * backend re-derives and re-authorizes the subject from it on every load,
 * exactly like every other scoped page in this application.
 *
 * This page never caches or persists graph data beyond one render - every
 * visit re-fetches live from the backend, which is itself a pure read
 * projection over existing Fact/Evidence/Requirement/Document records. All
 * three modes share the exact same canvas and detail-panel components -
 * nothing here is a second graph viewer.
 * ============================================================================
 */

type TraceMode = "fact" | "document" | "evaluation";

const TRACE_MODE_LABEL: Record<TraceMode, string> = {
  fact: "Fact",
  document: "Document",
  evaluation: "Requirement",
};

function getGraphErrorMessage(error: unknown, mode: TraceMode | null): string {

  const status = errorService.getStatus(error);
  const subject = mode ? TRACE_MODE_LABEL[mode] : "record";

  if (status === 404) {
    return `We could not find this ${subject}.`;
  }

  if (status === 403) {
    return "You are not authorized to view this evidence graph.";
  }

  return errorService.getMessage(error);
}

export default function EvidenceGraphPage() {

  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const factId = searchParams.get("fact");
  const documentId = searchParams.get("document");
  const evaluationId = searchParams.get("evaluation");

  const numericFactId = factId ? Number(factId) : null;
  const numericDocumentId = documentId ? Number(documentId) : null;
  const numericEvaluationId = evaluationId ? Number(evaluationId) : null;

  const mode: TraceMode | null =
    numericFactId && !Number.isNaN(numericFactId) ? "fact"
      : numericDocumentId && !Number.isNaN(numericDocumentId) ? "document"
        : numericEvaluationId && !Number.isNaN(numericEvaluationId) ? "evaluation"
          : null;

  const [graph, setGraph] = useState<EvidenceGraph | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedEvidenceItemId, setSelectedEvidenceItemId] = useState<number | null>(null);

  const loadGraph = useCallback(async () => {

    try {
      setLoading(true);
      setError(null);

      let result: EvidenceGraph;

      if (mode === "fact" && numericFactId) {
        result = await getEvidenceGraphForFactApi(numericFactId);
      } else if (mode === "document" && numericDocumentId) {
        result = await getEvidenceGraphForDocumentApi(numericDocumentId);
      } else if (mode === "evaluation" && numericEvaluationId) {
        result = await getEvidenceGraphForRequirementEvaluationApi(numericEvaluationId);
      } else {
        setError("This trace link is invalid - no Fact, Document, or Requirement was specified.");
        setLoading(false);
        return;
      }

      setGraph(result);

    } catch (err) {
      setError(getGraphErrorMessage(err, mode));
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [factId, documentId, evaluationId]);

  useEffect(() => {
    void loadGraph();
  }, [loadGraph]);

  const backPath =
    mode === "document" ? "/dashboard/documents"
      : "/dashboard/immigration-profile";

  const backLabel =
    mode === "document" ? "Back to your documents"
      : "Back to your Immigration Profile";

  if (loading) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-16 sm:px-6">
        <Loader text="Building the evidence graph..." />
      </div>
    );
  }

  if (error) {
    return (
      <div className="mx-auto max-w-4xl space-y-4 px-4 py-10 sm:px-6">
        <ErrorAlert message={error} />
        <div className="flex gap-3">
          <Button variant="secondary" onClick={() => void loadGraph()}>
            <RefreshCw size={16} />
            Try again
          </Button>
          <Link to={backPath}>
            <Button variant="outline">Back to profile</Button>
          </Link>
        </div>
      </div>
    );
  }

  if (!graph || graph.nodes.length === 0) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-10 sm:px-6">
        <EmptyState
          icon={<Waypoints size={40} />}
          title="Nothing to trace yet"
          description={
            mode === "document"
              ? "This document has no extracted evidence, facts, or dependent requirement evaluations recorded yet."
              : mode === "evaluation"
                ? "This requirement evaluation has no contributing facts, evidence, or conflicts recorded yet."
                : "This Fact has no evidence, documents, or dependent requirement evaluations recorded yet."
          }
          action={
            <Link to={backPath}>
              <Button variant="primary">{backLabel}</Button>
            </Link>
          }
        />
      </div>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className="flex h-[calc(100vh-4rem)] flex-col"
    >
      <div className="flex items-center justify-between border-b border-white/10 px-4 py-3 sm:px-6">
        <button
          type="button"
          onClick={() => navigate(backPath)}
          className="inline-flex items-center gap-1 text-sm text-slate-400 transition hover:text-white"
        >
          <ArrowLeft size={16} />
          {backLabel}
        </button>

        <div className="flex items-center gap-2 text-sm font-semibold text-white">
          <Waypoints size={16} className="text-[#C6A15B]" />
          {mode ? `${TRACE_MODE_LABEL[mode]} Evidence Trace` : "Evidence Trace"}
        </div>
      </div>

      <div className="flex min-h-0 flex-1">
        <div className="min-w-0 flex-1">
          <EvidenceGraphCanvas graph={graph} onSelectEvidence={setSelectedEvidenceItemId} />
        </div>

        {selectedEvidenceItemId != null && (
          <EvidenceDetailPanel
            evidenceItemId={selectedEvidenceItemId}
            onClose={() => setSelectedEvidenceItemId(null)}
          />
        )}
      </div>
    </motion.div>
  );
}
