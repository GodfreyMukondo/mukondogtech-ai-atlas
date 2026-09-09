package com.godfrey.ai_immigration_document_analyzer.requirement.dto;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVerificationStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;

import java.util.List;

/**
 * One published Pathway's transient, read-only alignment result within a
 * {@link PathwayDiscoveryResponse} (Phase 4 - Pathway Discovery). Nothing
 * here is a persisted {@code PathwayAssessment}/{@code RequirementEvaluation}
 * row - see {@code PathwayDiscoveryService} for why.
 *
 * {@code overallAlignmentScore} is the SAME three-dimension formula Case
 * Intelligence's {@code CaseReadinessResponse.overallReadinessPercent}
 * already uses (via the shared {@code RequirementReadinessCalculator}) -
 * never a second, incompatible score, and never framed as an approval
 * probability.
 */
public record PathwayRankingRow(
        int rank,
        Long pathwayId,
        String pathwayKey,
        String name,
        String jurisdiction,
        String category,

        RequirementEvaluationOutcome outcome,

        double overallAlignmentScore,
        double requirementCoveragePercent,
        double evidenceCoveragePercent,
        double consistencyPercent,

        /** The least-trusted {@link RegulatoryVerificationStatus} among every RegulatoryVersion this pathway's requirements were evaluated under - never silently upgraded. */
        RegulatoryVerificationStatus regulatoryCertainty,

        long supportedRequirementCount,
        long partialRequirementCount,
        long missingRequirementCount,
        long conflictingRequirementCount,
        long needsVerificationCount,

        List<TopMissingRequirement> topMissingRequirements,

        String explanation
) {
}
