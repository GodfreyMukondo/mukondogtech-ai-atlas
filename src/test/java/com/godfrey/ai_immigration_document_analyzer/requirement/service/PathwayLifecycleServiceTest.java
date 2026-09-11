package com.godfrey.ai_immigration_document_analyzer.requirement.service;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayStatus;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link PathwayLifecycleService} - the guarded Pathway
 * publishing state machine (Phase 1 spec, section 3).
 */
class PathwayLifecycleServiceTest {

    private final PathwayLifecycleService lifecycleService = new PathwayLifecycleService();

    // =========================================================================
    // VALID TRANSITIONS
    // =========================================================================

    @Test
    void draftToReviewIsAllowed() {
        assertThatCode(() -> lifecycleService.assertValidTransition(PathwayStatus.DRAFT, PathwayStatus.REVIEW))
                .doesNotThrowAnyException();
    }

    @Test
    void reviewToDraftIsAllowed() {
        assertThatCode(() -> lifecycleService.assertValidTransition(PathwayStatus.REVIEW, PathwayStatus.DRAFT))
                .doesNotThrowAnyException();
    }

    @Test
    void reviewToPublishedIsAllowed() {
        assertThatCode(() -> lifecycleService.assertValidTransition(PathwayStatus.REVIEW, PathwayStatus.PUBLISHED))
                .doesNotThrowAnyException();
    }

    @Test
    void publishedToSupersededIsAllowed() {
        assertThatCode(() -> lifecycleService.assertValidTransition(PathwayStatus.PUBLISHED, PathwayStatus.SUPERSEDED))
                .doesNotThrowAnyException();
    }

    @Test
    void publishedToArchivedIsAllowed() {
        assertThatCode(() -> lifecycleService.assertValidTransition(PathwayStatus.PUBLISHED, PathwayStatus.ARCHIVED))
                .doesNotThrowAnyException();
    }

    @Test
    void draftToArchivedIsAllowed() {
        // Abandoning a draft without ever publishing it.
        assertThatCode(() -> lifecycleService.assertValidTransition(PathwayStatus.DRAFT, PathwayStatus.ARCHIVED))
                .doesNotThrowAnyException();
    }

    // =========================================================================
    // INVALID TRANSITIONS - PREVENT INVALID PUBLICATION STATES
    // =========================================================================

    @Test
    void draftCannotJumpDirectlyToPublished() {

        assertThatThrownBy(() -> lifecycleService.assertValidTransition(PathwayStatus.DRAFT, PathwayStatus.PUBLISHED))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void supersededIsTerminal() {

        assertThatThrownBy(() -> lifecycleService.assertValidTransition(PathwayStatus.SUPERSEDED, PathwayStatus.PUBLISHED))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void archivedIsTerminal() {

        assertThatThrownBy(() -> lifecycleService.assertValidTransition(PathwayStatus.ARCHIVED, PathwayStatus.DRAFT))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void publishedCannotRevertToDraft() {

        // Published content is immutable by design - it cannot be pulled
        // back into a directly-editable status.
        assertThatThrownBy(() -> lifecycleService.assertValidTransition(PathwayStatus.PUBLISHED, PathwayStatus.DRAFT))
                .isInstanceOf(IllegalStateException.class);
    }
}
