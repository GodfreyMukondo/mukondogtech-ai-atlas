package com.godfrey.ai_immigration_document_analyzer.requirement.service;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementStatus;

import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * ============================================================================
 * REQUIREMENT LIFECYCLE SERVICE
 * ============================================================================
 *
 * Enforces the Requirement DEFINITION lifecycle (distinct from
 * {@code RequirementEvaluationOutcome}, the lifecycle of applying one
 * definition to one subject - see {@code RequirementStatus}'s own
 * Javadoc):
 *
 * <pre>
 * DRAFT → PUBLISHED → DEPRECATED
 *   ↓         ↓
 *   └──→ RETRACTED
 * </pre>
 *
 * Only the place a Requirement's {@code status} is ever changed - no
 * endpoint accepts a raw status value. {@code PUBLISHED} content is treated
 * as immutable by {@code RequirementAdminService} once evaluation may have
 * relied on it; a substantive rule change is a new row sharing the same
 * {@code requirementKey} against a new {@code RegulatoryVersion}, exactly
 * as the entity's own Javadoc already documents.
 * ============================================================================
 */
@Service
public class RequirementLifecycleService {

    private static final Map<RequirementStatus, Set<RequirementStatus>> ALLOWED_TRANSITIONS = buildTransitions();

    public void assertValidTransition(RequirementStatus from, RequirementStatus to) {

        Set<RequirementStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, EnumSet.noneOf(RequirementStatus.class));

        if (!allowed.contains(to)) {

            throw new IllegalStateException(
                    "Invalid Requirement status transition: " + from + " -> " + to
            );
        }
    }

    private static Map<RequirementStatus, Set<RequirementStatus>> buildTransitions() {

        Map<RequirementStatus, Set<RequirementStatus>> transitions = new EnumMap<>(RequirementStatus.class);

        transitions.put(RequirementStatus.DRAFT, EnumSet.of(
                RequirementStatus.PUBLISHED,
                RequirementStatus.RETRACTED
        ));

        transitions.put(RequirementStatus.PUBLISHED, EnumSet.of(
                RequirementStatus.DEPRECATED,
                RequirementStatus.RETRACTED
        ));

        transitions.put(RequirementStatus.DEPRECATED, EnumSet.noneOf(RequirementStatus.class));

        transitions.put(RequirementStatus.RETRACTED, EnumSet.noneOf(RequirementStatus.class));

        return transitions;
    }
}
