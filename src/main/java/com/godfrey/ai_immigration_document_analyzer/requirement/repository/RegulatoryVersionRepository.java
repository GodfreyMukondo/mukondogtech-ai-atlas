package com.godfrey.ai_immigration_document_analyzer.requirement.repository;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVersion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RegulatoryVersionRepository extends JpaRepository<RegulatoryVersion, Long> {

    List<RegulatoryVersion> findByRegulationIdentity(String regulationIdentity);

    /** The version whose effective window covers the given instant - used for point-in-time evaluation (section 13). */
    @org.springframework.data.jpa.repository.Query(
            "SELECT v FROM RegulatoryVersion v WHERE v.regulationIdentity = :regulationIdentity "
                    + "AND v.effectiveFrom <= :asOf AND (v.effectiveTo IS NULL OR v.effectiveTo > :asOf)"
    )
    Optional<RegulatoryVersion> findEffectiveAt(String regulationIdentity, LocalDateTime asOf);
}
