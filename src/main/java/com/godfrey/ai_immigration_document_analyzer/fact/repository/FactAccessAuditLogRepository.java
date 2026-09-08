package com.godfrey.ai_immigration_document_analyzer.fact.repository;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactAccessAuditLog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FactAccessAuditLogRepository extends JpaRepository<FactAccessAuditLog, Long> {

    List<FactAccessAuditLog> findBySubjectUserIdOrderByAccessedAtDesc(Long subjectUserId);

    List<FactAccessAuditLog> findByGrantedFalseOrderByAccessedAtDesc();
}
