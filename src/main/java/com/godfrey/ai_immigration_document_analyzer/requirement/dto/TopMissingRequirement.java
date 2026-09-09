package com.godfrey.ai_immigration_document_analyzer.requirement.dto;

import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseRequirementSupportStatus;

import java.util.List;

/**
 * One unresolved (not SATISFIED) requirement standing between the subject
 * and a higher-ranked outcome for one Pathway (Phase 4 - Pathway Discovery).
 * Deliberately reuses {@link CaseRequirementSupportStatus} - the same
 * vocabulary the Requirement/Evidence Matrix already uses - rather than a
 * second "why is this missing" classification.
 */
public record TopMissingRequirement(
        Long requirementId,
        String requirementKey,
        String requirementTitle,
        Boolean mandatory,
        CaseRequirementSupportStatus supportStatus,
        List<String> missingFactKeys
) {
}
