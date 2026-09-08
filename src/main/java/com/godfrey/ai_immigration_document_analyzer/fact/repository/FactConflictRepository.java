package com.godfrey.ai_immigration_document_analyzer.fact.repository;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.ConflictStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConflict;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FactConflictRepository extends JpaRepository<FactConflict, Long> {

    Optional<FactConflict> findByIdAndSubjectUserId(Long id, Long subjectUserId);

    List<FactConflict> findBySubjectUserIdAndStatus(Long subjectUserId, ConflictStatus status);

    List<FactConflict> findByFactAIdOrFactBId(Long factAId, Long factBId);
}
