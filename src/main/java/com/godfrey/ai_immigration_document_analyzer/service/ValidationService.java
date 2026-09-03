package com.godfrey.ai_immigration_document_analyzer.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ValidationService {

    private static final Set<String> REQUIRED_STUDENT_VISA_DOCS = Set.of(
            "passport",
            "bank statement",
            "offer letter",
            "accommodation",
            "medical insurance"
    );

    public List<String> validateStudentVisa(List<String> uploadedDocs) {

        if (uploadedDocs == null || uploadedDocs.isEmpty()) {
            return List.copyOf(REQUIRED_STUDENT_VISA_DOCS);
        }

        Set<String> normalizedUploads = uploadedDocs.stream()
                .filter(doc -> doc != null && !doc.isBlank())
                .map(this::normalize)
                .collect(Collectors.toSet());

        return REQUIRED_STUDENT_VISA_DOCS.stream()
                .filter(required -> !normalizedUploads.contains(required))
                .toList();
    }

    private String normalize(String doc) {
        return doc.trim().toLowerCase(Locale.ROOT);
    }
}