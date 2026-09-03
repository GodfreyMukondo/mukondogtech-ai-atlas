package com.godfrey.ai_immigration_document_analyzer.repository;

import com.godfrey.ai_immigration_document_analyzer.entity.VisaRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VisaRuleRepository extends JpaRepository<VisaRule, Long> {

    List<VisaRule> findByCountry(String country);

    List<VisaRule> findByVisaType(String visaType);

    Optional<VisaRule> findByCountryAndVisaType(String country, String visaType);

    Optional<VisaRule> findByCountryIgnoreCaseAndVisaTypeIgnoreCase(
            String country,
            String visaType
    );

    List<VisaRule> findByCountryIgnoreCase(String country);

    List<VisaRule> findByVisaTypeIgnoreCase(String visaType);

    List<VisaRule> findByCountryContainingIgnoreCase(String keyword);

    // Native query needed: Oracle's UPPER() can't take a CLOB directly
    @Query(value = "SELECT * FROM visa_rule v WHERE DBMS_LOB.INSTR(UPPER(v.requirements), UPPER(:keyword)) > 0",
            nativeQuery = true)
    List<VisaRule> findByRequirementsContainingIgnoreCase(@Param("keyword") String keyword);
}