package com.godfrey.ai_immigration_document_analyzer.requirement.service;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayStatus;

import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * ============================================================================
 * PATHWAY LIFECYCLE SERVICE
 * ============================================================================
 *
 * Enforces the Pathway publishing state machine (Phase 1 spec, section 3):
 *
 * <pre>
 * DRAFT → REVIEW → PUBLISHED → SUPERSEDED
 *   ↓        ↓         ↓
 *   └──→ ARCHIVED ←────┘
 * </pre>
 *
 * This is the only place a Pathway's {@code status} is ever changed - no
 * endpoint accepts a raw status value, mirroring
 * {@code FactLifecycleService}'s guard for Facts. {@code PUBLISHED} content
 * is treated as immutable by {@code PathwayAdminService} (never edited in
 * place); a substantive change to a published pathway is a new row sharing
 * the same {@code pathwayKey}, with the old row transitioned to
 * {@code SUPERSEDED} - this is what "preserve regulatory/version history"
 * means in practice, and it is why {@code PathwayAssessment.pathwayId}
 * always remains reproducible against the exact content that produced it.
 * ============================================================================
 */
@Service
public class PathwayLifecycleService {

    private static final Map<PathwayStatus, Set<PathwayStatus>> ALLOWED_TRANSITIONS = buildTransitions();

    public void assertValidTransition(PathwayStatus from, PathwayStatus to) {

        Set<PathwayStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, EnumSet.noneOf(PathwayStatus.class));

        if (!allowed.contains(to)) {

            throw new IllegalStateException(
                    "Invalid Pathway status transition: " + from + " -> " + to
            );
        }
    }

    private static Map<PathwayStatus, Set<PathwayStatus>> buildTransitions() {

        Map<PathwayStatus, Set<PathwayStatus>> transitions = new EnumMap<>(PathwayStatus.class);

        transitions.put(PathwayStatus.DRAFT, EnumSet.of(
                PathwayStatus.REVIEW,
                PathwayStatus.ARCHIVED
        ));

        transitions.put(PathwayStatus.REVIEW, EnumSet.of(
                PathwayStatus.DRAFT,
                PathwayStatus.PUBLISHED,
                PathwayStatus.ARCHIVED
        ));

        transitions.put(PathwayStatus.PUBLISHED, EnumSet.of(
                PathwayStatus.SUPERSEDED,
                PathwayStatus.ARCHIVED
        ));

        transitions.put(PathwayStatus.SUPERSEDED, EnumSet.noneOf(PathwayStatus.class));

        transitions.put(PathwayStatus.ARCHIVED, EnumSet.noneOf(PathwayStatus.class));

        return transitions;
    }
}
