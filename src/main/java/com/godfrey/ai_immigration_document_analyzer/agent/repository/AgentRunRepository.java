package com.godfrey.ai_immigration_document_analyzer.agent.repository;

import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentRun;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentRunRepository extends JpaRepository<AgentRun, Long> {
}
