package com.godfrey.ai_immigration_document_analyzer.service;

import com.godfrey.ai_immigration_document_analyzer.entity.VisaRule;
import com.godfrey.ai_immigration_document_analyzer.exception.VisaRuleNotFoundException;
import com.godfrey.ai_immigration_document_analyzer.repository.VisaRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisaRuleService {

    private final VisaRuleRepository repository;

    @Transactional(readOnly = true)
    public VisaRule getRule(String country, String visaType) {

        validateInputs(country, visaType);

        String normalizedCountry = normalize(country);
        String normalizedVisaType = normalize(visaType);

        log.debug("Fetching visa rule | country={} | visaType={}",
                normalizedCountry, normalizedVisaType);

        return repository
                .findByCountryIgnoreCaseAndVisaTypeIgnoreCase(country, visaType)
                .orElseThrow(() -> {
                    log.warn("Visa rule not found | country={} | visaType={}",
                            normalizedCountry, normalizedVisaType);

                    return new VisaRuleNotFoundException(
                            normalizedCountry,
                            normalizedVisaType
                    );
                });
    }

    // ================= VALIDATION =================

    private void validateInputs(String country, String visaType) {
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Country cannot be empty");
        }

        if (visaType == null || visaType.isBlank()) {
            throw new IllegalArgumentException("Visa type cannot be empty");
        }
    }

    // ================= NORMALIZATION =================

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
    }
}