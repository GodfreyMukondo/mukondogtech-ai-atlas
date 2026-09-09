package com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto;

import java.time.LocalDateTime;

/**
 * Case-wide evidence/anomaly signal summary. Every count here comes from an
 * existing, already-documented non-forensic subsystem, or from a
 * read-time-only observation that is never itself persisted as a finding:
 *
 * - fraudFlaggedDocumentCount / highRiskDocumentCount / mediumRiskDocumentCount:
 *   {@code Document.fraudDetected}/{@code riskLevel}, the legacy
 *   keyword-based indicator - never forensic, never proof of anything.
 * - openContradictionCount: open {@code FactConflict} rows for this subject.
 * - potentialOverlapContradictionCount: read-time-only overlapping-period
 *   observations (see {@link PotentialOverlapContradiction}) - never a
 *   persisted conflict, never a fraud finding.
 * - rejectedEvidenceItemCount / validationFailedEvidenceItemCount:
 *   Evidence Intelligence Graph {@code EvidenceItem} terminal statuses.
 *
 * {@code riskBand} is a simple, transparent aggregation of these counts -
 * an INTERNAL MukondoGTech AI heuristic only. It is never a fraud label,
 * never a claim that any anomaly proves fraud, and never represented as a
 * government-defined or immigration-law threshold.
 */
public record CaseSignalsResponse(
        Long subjectUserId,
        CaseRiskBand riskBand,
        long fraudFlaggedDocumentCount,
        long highRiskDocumentCount,
        long mediumRiskDocumentCount,
        long openContradictionCount,
        long potentialOverlapContradictionCount,
        long rejectedEvidenceItemCount,
        long validationFailedEvidenceItemCount,
        LocalDateTime generatedAt,
        String note
) {
}
