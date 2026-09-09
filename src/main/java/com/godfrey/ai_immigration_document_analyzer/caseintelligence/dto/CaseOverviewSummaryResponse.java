package com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto;

/**
 * The single-glance "Case Overview" tab summary for one PathwayAssessment -
 * every figure here is computed elsewhere ({@code CaseIntelligenceService}'s
 * own readiness calculation, {@code CaseOverviewService}'s contradiction/
 * timeline/signals methods) and simply gathered into one object so a
 * consumer does not need three extra round trips to render an overview.
 *
 * {@code overallReadinessPercent} and every count here are internal
 * MukondoGTech AI heuristics, never a government-defined score or an
 * approval probability - see {@code CaseReadinessResponse} and
 * {@code CaseRiskBand} for the underlying calculation rules.
 */
public record CaseOverviewSummaryResponse(
        Double requirementCoveragePercent,
        Double evidenceCoveragePercent,
        Double overallReadinessPercent,
        long missingEvidenceCount,
        long verificationNeededCount,
        long contradictionCount,
        long timelineIssueCount,
        CaseRiskBand riskBand,
        String note
) {
}
