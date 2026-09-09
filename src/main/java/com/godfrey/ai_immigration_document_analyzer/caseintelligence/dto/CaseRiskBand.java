package com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto;

/**
 * A transparent, count/threshold-based banding of legacy risk-indicator
 * signals for one case - NEVER a fraud determination, NEVER an ML/
 * probability score, and never itself a reason to deny or delay anything.
 * Every input that feeds this band ({@code Document.fraudDetected}/
 * {@code riskLevel}, open Fact conflicts, rejected/failed evidence items)
 * is itself already documented elsewhere as a non-forensic signal - this
 * enum only aggregates their counts into one glanceable indicator for a
 * case worker.
 */
public enum CaseRiskBand {
    NORMAL,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
