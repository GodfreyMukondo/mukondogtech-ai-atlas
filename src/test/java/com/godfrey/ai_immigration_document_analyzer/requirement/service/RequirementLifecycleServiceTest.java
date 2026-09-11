package com.godfrey.ai_immigration_document_analyzer.requirement.service;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementStatus;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit tests for {@link RequirementLifecycleService} - the guarded Requirement definition state machine. */
class RequirementLifecycleServiceTest {

    private final RequirementLifecycleService lifecycleService = new RequirementLifecycleService();

    @Test
    void draftToPublishedIsAllowed() {
        assertThatCode(() -> lifecycleService.assertValidTransition(RequirementStatus.DRAFT, RequirementStatus.PUBLISHED))
                .doesNotThrowAnyException();
    }

    @Test
    void draftToRetractedIsAllowed() {
        assertThatCode(() -> lifecycleService.assertValidTransition(RequirementStatus.DRAFT, RequirementStatus.RETRACTED))
                .doesNotThrowAnyException();
    }

    @Test
    void publishedToDeprecatedIsAllowed() {
        assertThatCode(() -> lifecycleService.assertValidTransition(RequirementStatus.PUBLISHED, RequirementStatus.DEPRECATED))
                .doesNotThrowAnyException();
    }

    @Test
    void publishedToRetractedIsAllowed() {
        assertThatCode(() -> lifecycleService.assertValidTransition(RequirementStatus.PUBLISHED, RequirementStatus.RETRACTED))
                .doesNotThrowAnyException();
    }

    @Test
    void deprecatedIsTerminal() {

        assertThatThrownBy(() -> lifecycleService.assertValidTransition(RequirementStatus.DEPRECATED, RequirementStatus.PUBLISHED))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void retractedIsTerminal() {

        assertThatThrownBy(() -> lifecycleService.assertValidTransition(RequirementStatus.RETRACTED, RequirementStatus.DRAFT))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void draftCannotJumpDirectlyToDeprecated() {

        assertThatThrownBy(() -> lifecycleService.assertValidTransition(RequirementStatus.DRAFT, RequirementStatus.DEPRECATED))
                .isInstanceOf(IllegalStateException.class);
    }
}
