import { useMemo, useState } from "react";

import {
  Background,
  Controls,
  MarkerType,
  MiniMap,
  ReactFlow,
  ReactFlowProvider,
  type Edge,
  type Node,
} from "@xyflow/react";

import "@xyflow/react/dist/style.css";

import { Search } from "lucide-react";

import { computeGraphLayout } from "./graphLayout";
import EvidenceGraphNode, { type EvidenceGraphNodeData } from "./EvidenceGraphNode";
import { getNodeTypeLabel, getRelationshipLabel } from "../../utils/evidenceGraphLabels";

import type { EvidenceGraph, GraphNodeType } from "../../types/evidenceGraph";

/**
 * ============================================================================
 * EVIDENCE GRAPH CANVAS
 * ============================================================================
 *
 * Renders one EvidenceGraph (nodes + edges already authorized and returned
 * by the backend) using React Flow. This component never fabricates a
 * node/edge - everything drawn corresponds 1:1 to an entry already present
 * in the `graph` prop.
 *
 * Interaction surface: pan/zoom/fit-to-view/minimap (native to React Flow),
 * search-by-label, node-type filtering (dims non-matching nodes rather than
 * removing them, so the surrounding chain stays visible for context), and
 * node selection -> `onSelectEvidence` for EVIDENCE nodes specifically,
 * which the page wires to the detail panel.
 * ============================================================================
 */

const nodeTypes = { evidenceGraphNode: EvidenceGraphNode };

function toFlowNode(node: EvidenceGraph["nodes"][number], position: { x: number; y: number }): Node {

  const data: EvidenceGraphNodeData = {
    nodeType: node.type,
    label: node.label,
    metadata: node.metadata,
  };

  return {
    id: node.id,
    type: "evidenceGraphNode",
    position,
    data: data as unknown as Record<string, unknown>,
  };
}

function toFlowEdge(edge: EvidenceGraph["edges"][number]): Edge {

  const isConflict = edge.relationship === "CONFLICTS_WITH";

  return {
    id: edge.id,
    source: edge.sourceNodeId,
    target: edge.targetNodeId,
    label: getRelationshipLabel(edge.relationship),
    animated: isConflict,
    style: { stroke: isConflict ? "#D9822B" : "#4B5568", strokeWidth: isConflict ? 2 : 1.4 },
    labelStyle: { fill: "#94A3B8", fontSize: 10 },
    labelBgStyle: { fill: "#0B1220", fillOpacity: 0.9 },
    markerEnd: { type: MarkerType.ArrowClosed, color: isConflict ? "#D9822B" : "#4B5568" },
  };
}

export default function EvidenceGraphCanvas({
  graph,
  onSelectEvidence,
}: {
  graph: EvidenceGraph;
  /** Called with the EvidenceItem id backing a selected EVIDENCE node, or null when none applies. */
  onSelectEvidence: (evidenceItemId: number | null) => void;
}) {

  const [search, setSearch] = useState("");
  const [hiddenTypes, setHiddenTypes] = useState<Set<GraphNodeType>>(new Set());

  const presentTypes = useMemo(
    () => Array.from(new Set(graph.nodes.map((node) => node.type))),
    [graph.nodes],
  );

  const flowNodes = useMemo(() => {

    const positions = computeGraphLayout(graph.nodes);
    const query = search.trim().toLowerCase();

    return graph.nodes.map((node) => {

      const flowNode = toFlowNode(node, positions.get(node.id) ?? { x: 0, y: 0 });

      const matchesSearch = query.length === 0 || node.label.toLowerCase().includes(query);
      const typeHidden = hiddenTypes.has(node.type);

      (flowNode.data as EvidenceGraphNodeData).dimmed = typeHidden || !matchesSearch;

      return flowNode;
    });
  }, [graph.nodes, search, hiddenTypes]);

  const flowEdges = useMemo(() => graph.edges.map(toFlowEdge), [graph.edges]);

  function toggleType(type: GraphNodeType) {

    setHiddenTypes((previous) => {

      const next = new Set(previous);

      if (next.has(type)) {
        next.delete(type);
      } else {
        next.add(type);
      }

      return next;
    });
  }

  function handleNodeClick(_: unknown, node: Node) {

    const data = node.data as unknown as EvidenceGraphNodeData;

    if (data.nodeType !== "EVIDENCE") {
      onSelectEvidence(null);
      return;
    }

    const evidenceItemId = data.metadata.evidenceItemId;
    onSelectEvidence(typeof evidenceItemId === "number" ? evidenceItemId : null);
  }

  return (
    <div className="flex h-full flex-col">
      <div className="flex flex-wrap items-center gap-2 border-b border-white/10 bg-[#0B1220] px-3 py-2">
        <div className="flex items-center gap-1.5 rounded-lg border border-white/15 bg-white/5 px-2 py-1">
          <Search size={13} className="text-slate-500" />
          <input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search nodes..."
            className="w-40 bg-transparent text-xs text-white placeholder:text-slate-500 focus:outline-none"
          />
        </div>

        <div className="flex flex-wrap items-center gap-1.5">
          {presentTypes.map((type) => {
            const active = !hiddenTypes.has(type);
            return (
              <button
                key={type}
                type="button"
                onClick={() => toggleType(type)}
                className={`
                  rounded-full border px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide transition
                  ${active ? "border-white/20 bg-white/10 text-white" : "border-white/10 text-slate-500"}
                `}
              >
                {getNodeTypeLabel(type)}
              </button>
            );
          })}
        </div>
      </div>

      <div className="min-h-0 flex-1">
        <ReactFlowProvider>
          <ReactFlow
            nodes={flowNodes}
            edges={flowEdges}
            nodeTypes={nodeTypes}
            onNodeClick={handleNodeClick}
            fitView
            fitViewOptions={{ padding: 0.3 }}
            proOptions={{ hideAttribution: true }}
            colorMode="dark"
          >
            <Background color="#1F2937" gap={20} />
            <Controls showInteractive={false} />
            <MiniMap
              pannable
              zoomable
              nodeColor="#3A4460"
              maskColor="rgba(11,18,32,0.8)"
              style={{ backgroundColor: "#0B1220" }}
            />
          </ReactFlow>
        </ReactFlowProvider>
      </div>
    </div>
  );
}
