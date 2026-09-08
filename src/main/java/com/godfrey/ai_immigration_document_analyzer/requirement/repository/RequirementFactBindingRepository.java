package com.godfrey.ai_immigration_document_analyzer.requirement.repository;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementFactBinding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequirementFactBindingRepository extends JpaRepository<RequirementFactBinding, Long> {

    List<RequirementFactBinding> findByRequirementId(Long requirementId);

    /** Which Requirements declare a dependency on this Fact key - the query surface a future Next-Best-Action reads (not built yet). */
    List<RequirementFactBinding> findByFactKey(String factKey);
}
