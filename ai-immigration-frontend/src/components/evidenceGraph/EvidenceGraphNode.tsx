import { Handle, Position, type NodeProps } from "@xyflow/react";

import {
  BadgeCheck,
  BookMarked,
  ClipboardList,
  Compass,
  Database,
  FileStack,
  FileText,
  Gavel,
  ScrollText,
  ShieldQuestion,
} from "lucide-react";

import type { DocumentNodeMetadata, RegulatoryVersionNodeMetadata } from "../../types/evidenceGraph";

import { getNodeTypeLabel } from "../../utils/evidenceGraphLabels";

import type { GraphNodeType } from "../../types/evidenceGraph";

/**
 * ============================================================================
 * EVIDENCE GRAPH NODE
 * ============================================================================
 *
 * One custom React Flow node renderer, switching presentation by node
 * type rather than one component per type - the Evidence Intelligence
 * Graph's node set is small and each type's "extra" metadata is a single
 * badge, so a single component stays easier to keep visually consistent
 * than five near-duplicate ones.
 *
 * Deliberate semantic distinctions preserved here (never conflated):
 *
 * - The "Verified" badge on a FACT node reflects ONLY isVerified metadata -
 *   never derived from confidenceLevel.
 * - A CONFLICTED status renders as a distinct amber warning, never as if
 *   it were a fraud indicator.
 * - Confidence is its own badge, never relabelled as verification.
 * ============================================================================
 */

const NODE_STYLE: Record<GraphNodeType, { icon: typeof FileText; accent: string }> = {
  DOCUMENT: { icon: FileText, accent: "#8AA0B8" },
  DOCUMENT_VERSION: { icon: FileStack, accent: "#8AA0B8" },
  EVIDENCE: { icon: ClipboardList, accent: "#1B6E77" },
  FACT: { icon: Database, accent: "#C6A15B" },
  REQUIREMENT: { icon: ScrollText, accent: "#5B4A99" },
  REQUIREMENT_EVALUATION: { icon: Gavel, accent: "#5B4A99" },
  PATHWAY: { icon: Compass, accent: "#2E8B6E" },
  PATHWAY_ASSESSMENT: { icon: Compass, accent: "#2E8B6E" },
  REGULATORY_VERSION: { icon: BookMarked, accent: "#9C6B16" },
  CONFLICT: { icon: ShieldQuestion, accent: "#D9822B" },
};

export interface EvidenceGraphNodeData {
  nodeType: GraphNodeType;
  label: string;
  metadata: Record<string, unknown>;
  dimmed?: boolean;
  [key: string]: unknown;
}

export default function EvidenceGraphNode({ data, selected }: NodeProps) {

  const nodeData = data as unknown as EvidenceGraphNodeData;
  const { nodeType, label, metadata, dimmed } = nodeData;
  const style = NODE_STYLE[nodeType];
  const Icon = style.icon;

  const isConflicted =
    nodeType === "FACT" && metadata.status === "CONTESTED";

  return (
    <div
      className={`
        min-w-[200px] max-w-[240px] rounded-xl border bg-[#0F1826] px-3 py-2.5 shadow-sm transition
        ${selected ? "border-white/60 ring-2 ring-white/30" : "border-white/10"}
        ${dimmed ? "opacity-30" : "opacity-100"}
      `}
      style={{ borderLeftColor: style.accent, borderLeftWidth: 3 }}
    >
      <Handle type="target" position={Position.Left} className="!bg-white/30" />
      <Handle type="source" position={Position.Right} className="!bg-white/30" />

      <div className="flex items-center gap-1.5 text-[10px] font-semibold uppercase tracking-wide text-slate-500">
        <Icon size={12} style={{ color: style.accent }} />
        {getNodeTypeLabel(nodeType)}
      </div>

      <p className="mt-1 truncate text-sm font-semibold text-white" title={label}>
        {label}
      </p>

      <div className="mt-1.5 flex flex-wrap items-center gap-1">
        {nodeType === "FACT" && (
          <>
            {metadata.isVerified === true && (
              <span className="inline-flex items-center gap-0.5 rounded-full border border-emerald-400/20 bg-emerald-400/10 px-1.5 py-0.5 text-[9px] font-semibold text-emerald-300">
                <BadgeCheck size={9} />
                Verified
              </span>
            )}
            {isConflicted && (
              <span className="inline-flex items-center gap-0.5 rounded-full border border-amber-400/20 bg-amber-400/10 px-1.5 py-0.5 text-[9px] font-semibold text-amber-300">
                <ShieldQuestion size={9} />
                Conflicted
              </span>
            )}
            {typeof metadata.confidenceLevel === "string" && (
              <span className="rounded-full border border-white/15 bg-white/5 px-1.5 py-0.5 text-[9px] text-slate-400">
                {metadata.confidenceLevel}
              </span>
            )}
          </>
        )}

        {nodeType === "EVIDENCE" && typeof metadata.sourceType === "string" && (
          <span className="rounded-full border border-white/15 bg-white/5 px-1.5 py-0.5 text-[9px] text-slate-400">
            {String(metadata.sourceType).replace(/_/g, " ")}
          </span>
        )}

        {nodeType === "REQUIREMENT_EVALUATION" && typeof metadata.outcome === "string" && (
          <span className="rounded-full border border-white/15 bg-white/5 px-1.5 py-0.5 text-[9px] text-slate-400">
            {String(metadata.outcome).replace(/_/g, " ")}
          </span>
        )}

        {nodeType === "REQUIREMENT" && metadata.mandatory === true && (
          <span className="rounded-full border border-white/15 bg-white/5 px-1.5 py-0.5 text-[9px] text-slate-400">
            Mandatory
          </span>
        )}

        {nodeType === "DOCUMENT" && (() => {
          const documentMetadata = metadata as unknown as DocumentNodeMetadata;
          return (
            <>
              {documentMetadata.fraudDetected === true && (
                <span
                  className="inline-flex items-center gap-0.5 rounded-full border border-amber-400/20 bg-amber-400/10 px-1.5 py-0.5 text-[9px] font-semibold text-amber-300"
                  title="Legacy risk indicator - a signal only, never a fraud determination"
                >
                  <ShieldQuestion size={9} />
                  Risk signal
                </span>
              )}
              {typeof documentMetadata.riskLevel === "string" && documentMetadata.riskLevel !== "LOW" && (
                <span className="rounded-full border border-white/15 bg-white/5 px-1.5 py-0.5 text-[9px] text-slate-400">
                  {documentMetadata.riskLevel} risk
                </span>
              )}
            </>
          );
        })()}

        {nodeType === "REGULATORY_VERSION" && (() => {
          const regulatoryMetadata = metadata as unknown as RegulatoryVersionNodeMetadata;
          return (
            <span className="rounded-full border border-white/15 bg-white/5 px-1.5 py-0.5 text-[9px] text-slate-400">
              {regulatoryMetadata.verificationStatus.replace(/_/g, " ")}
            </span>
          );
        })()}

        {nodeType === "CONFLICT" && typeof metadata.status === "string" && (
          <span className="inline-flex items-center gap-0.5 rounded-full border border-amber-400/20 bg-amber-400/10 px-1.5 py-0.5 text-[9px] font-semibold text-amber-300">
            {String(metadata.status)}
          </span>
        )}
      </div>
    </div>
  );
}
