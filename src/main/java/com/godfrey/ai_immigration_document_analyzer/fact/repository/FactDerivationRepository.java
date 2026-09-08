package com.godfrey.ai_immigration_document_analyzer.fact.repository;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactDerivation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FactDerivationRepository extends JpaRepository<FactDerivation, Long> {

    List<FactDerivation> findByDerivedFactId(Long derivedFactId);

    List<FactDerivation> findBySourceFactId(Long sourceFactId);
}
