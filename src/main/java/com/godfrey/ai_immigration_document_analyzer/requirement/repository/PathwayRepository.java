package com.godfrey.ai_immigration_document_analyzer.requirement.repository;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Pathway;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PathwayRepository extends JpaRepository<Pathway, Long> {

    Optional<Pathway> findByPathwayKey(String pathwayKey);

    List<Pathway> findByStatus(PathwayStatus status);

    /**
     * The currently PUBLISHED row for a pathwayKey, if any - used by
     * {@code PathwayAdminService} to auto-supersede the previous version
     * when a new one is published. At most one row per key is ever
     * PUBLISHED at a time; this is the invariant that method maintains.
     */
    Optional<Pathway> findByPathwayKeyAndStatus(String pathwayKey, PathwayStatus status);
}
