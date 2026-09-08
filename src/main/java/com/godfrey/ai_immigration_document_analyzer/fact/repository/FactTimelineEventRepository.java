package com.godfrey.ai_immigration_document_analyzer.fact.repository;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactTimelineEvent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FactTimelineEventRepository extends JpaRepository<FactTimelineEvent, Long> {

    List<FactTimelineEvent> findBySubjectUserIdOrderByOccurredAtDesc(Long subjectUserId);

    List<FactTimelineEvent> findByFactIdOrderByOccurredAtDesc(Long factId);
}
