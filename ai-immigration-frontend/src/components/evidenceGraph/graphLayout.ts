import type { GraphNodeType } from "../../types/evidenceGraph";

/**
 * ============================================================================
 * GRAPH LAYOUT
 * ============================================================================
 *
 * A small, dependency-free column layout - deliberately not a general
 * force-directed/auto-layout library. The Evidence Intelligence Graph is
 * shallow and fixed-shape by design (see the architecture document,
 * section 24: "traversal strategy" - at most a handful of hops, a few
 * dozen nodes per view), so a simple left-to-right column assignment by
 * node type reads more clearly than a physics simulation would, and adds
 * no extra dependency.
 *
 * Columns follow the provenance direction the rest of this feature already
 * uses: Requirement -> Evaluation -> Fact -> Evidence -> Document Version
 * -> Document, with Pathway-level nodes further left and Regulatory
 * further right, for the (rarer) full-trace view.
 * ============================================================================
 */

const COLUMN_ORDER: GraphNodeType[] = [
  "PATHWAY",
  "PATHWAY_ASSESSMENT",
  "REQUIREMENT",
  "REQUIREMENT_EVALUATION",
  "CONFLICT",
  "FACT",
  "EVIDENCE",
  "DOCUMENT_VERSION",
  "DOCUMENT",
  "REGULATORY_VERSION",
];

const COLUMN_WIDTH = 260;
const ROW_HEIGHT = 120;

export interface LaidOutPosition {
  x: number;
  y: number;
}

/**
 * Assigns every node id a deterministic (x, y) position: column by type,
 * row by order of appearance within that column.
 */
export function computeGraphLayout(
  nodes: Array<{ id: string; type: GraphNodeType }>,
): Map<string, LaidOutPosition> {

  const positions = new Map<string, LaidOutPosition>();
  const rowCountByColumn = new Map<number, number>();

  for (const node of nodes) {

    const columnIndex = Math.max(COLUMN_ORDER.indexOf(node.type), 0);
    const row = rowCountByColumn.get(columnIndex) ?? 0;

    positions.set(node.id, {
      x: columnIndex * COLUMN_WIDTH,
      y: row * ROW_HEIGHT,
    });

    rowCountByColumn.set(columnIndex, row + 1);
  }

  return positions;
}
