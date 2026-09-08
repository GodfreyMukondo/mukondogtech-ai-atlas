package com.godfrey.ai_immigration_document_analyzer.fact.service;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.AccessorType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactAccessAuditLog;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactSensitivityTier;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactAccessAuditLogRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

/**
 * Records every attempt (granted or denied) to access security-sensitive
 * Fact data. Denials are recorded with the same rigor as grants - they are
 * the evidence base for IDOR/least-privilege testing and for reviewing
 * whether an agent's declared task scope was ever exceeded.
 *
 * Like {@code FactTimelineService}, this deliberately does not swallow
 * exceptions - a security access log is not the place for a best-effort
 * write.
 */
@Service
@RequiredArgsConstructor
public class FactAccessAuditService {

    private final FactAccessAuditLogRepository auditLogRepository;

    public void recordAccess(
            Long subjectUserId,
            Long factId,
            Long accessedByUserId,
            AccessorType accessorType,
            String purpose,
            FactSensitivityTier sensitivityTierAccessed,
            boolean granted,
            String denialReason
    ) {

        FactAccessAuditLog logEntry = FactAccessAuditLog.builder()
                .subjectUserId(subjectUserId)
                .factId(factId)
                .accessedByUserId(accessedByUserId)
                .accessorType(accessorType)
                .purpose(purpose)
                .sensitivityTierAccessed(sensitivityTierAccessed)
                .granted(granted)
                .denialReason(denialReason)
                .build();

        auditLogRepository.save(logEntry);
    }
}
