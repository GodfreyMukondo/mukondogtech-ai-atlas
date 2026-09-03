package com.godfrey.ai_immigration_document_analyzer.controller;


import com.godfrey.ai_immigration_document_analyzer.dto.KnowledgeDocumentResponse;
import com.godfrey.ai_immigration_document_analyzer.dto.KnowledgeDocumentSummaryResponse;
import com.godfrey.ai_immigration_document_analyzer.dto.request.CreateKnowledgeDocumentRequest;
import com.godfrey.ai_immigration_document_analyzer.dto.request.UpdateKnowledgeDocumentRequest;
import com.godfrey.ai_immigration_document_analyzer.dto.response.KnowledgeBaseStatsResponse;
import com.godfrey.ai_immigration_document_analyzer.entity.KnowledgeDocumentStatus;

import com.godfrey.ai_immigration_document_analyzer.service.KnowledgeBaseService;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

/**
 * ============================================================================
 * KNOWLEDGE BASE ADMIN CONTROLLER
 * ============================================================================
 *
 * Base URL:
 *
 *     /api/admin/knowledge-base
 *
 * All endpoints require ROLE_ADMIN.
 * ============================================================================
 */
@RestController
@RequestMapping("/api/admin/knowledge-base")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * =========================================================================
     * LIST / SEARCH DOCUMENTS
     * =========================================================================
     *
     * GET /api/admin/knowledge-base
     */
    @GetMapping
    public ResponseEntity<Page<KnowledgeDocumentSummaryResponse>>
    search(
            @RequestParam(required = false)
            String search,

            @RequestParam(required = false)
            String category,

            @RequestParam(required = false)
            KnowledgeDocumentStatus status,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size,

            Authentication authentication
    ) {

        int safePage =
                Math.max(page, 0);

        int safeSize =
                Math.min(
                        Math.max(size, 1),
                        100
                );

        Pageable pageable =
                PageRequest.of(
                        safePage,
                        safeSize,
                        Sort.by(
                                Sort.Direction.DESC,
                                "updatedAt"
                        )
                );

        log.debug(
                "Knowledge base search | user={} | search={} | category={} | status={} | page={} | size={}",
                authentication.getName(),
                search,
                category,
                status,
                safePage,
                safeSize
        );

        return ResponseEntity.ok(
                knowledgeBaseService.search(
                        search,
                        category,
                        status,
                        pageable
                )
        );
    }

    /**
     * =========================================================================
     * STATISTICS
     * =========================================================================
     *
     * GET /api/admin/knowledge-base/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<KnowledgeBaseStatsResponse>
    getStatistics(
            Authentication authentication
    ) {

        log.debug(
                "Knowledge base statistics requested | user={}",
                authentication.getName()
        );

        return ResponseEntity.ok(
                knowledgeBaseService.getStatistics()
        );
    }

    /**
     * =========================================================================
     * GET DOCUMENT
     * =========================================================================
     *
     * GET /api/admin/knowledge-base/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<KnowledgeDocumentResponse>
    getById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                knowledgeBaseService.getById(id)
        );
    }

    /**
     * =========================================================================
     * CREATE DOCUMENT
     * =========================================================================
     *
     * POST /api/admin/knowledge-base
     */
    @PostMapping
    public ResponseEntity<KnowledgeDocumentResponse>
    create(
            @Valid
            @RequestBody
            CreateKnowledgeDocumentRequest request,

            Authentication authentication
    ) {

        KnowledgeDocumentResponse response =
                knowledgeBaseService.create(
                        request,
                        authentication.getName()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * =========================================================================
     * UPDATE DOCUMENT
     * =========================================================================
     *
     * PUT /api/admin/knowledge-base/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<KnowledgeDocumentResponse>
    update(
            @PathVariable Long id,

            @Valid
            @RequestBody
            UpdateKnowledgeDocumentRequest request,

            Authentication authentication
    ) {

        return ResponseEntity.ok(
                knowledgeBaseService.update(
                        id,
                        request,
                        authentication.getName()
                )
        );
    }

    /**
     * =========================================================================
     * DELETE DOCUMENT
     * =========================================================================
     *
     * DELETE /api/admin/knowledge-base/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void>
    delete(
            @PathVariable Long id
    ) {

        knowledgeBaseService.delete(id);

        return ResponseEntity.noContent()
                .build();
    }

    /**
     * =========================================================================
     * MARK AI INDEXED
     * =========================================================================
     *
     * POST /api/admin/knowledge-base/{id}/index
     */
    @PostMapping("/{id}/index")
    public ResponseEntity<KnowledgeDocumentResponse>
    markAsIndexed(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                knowledgeBaseService.markAsIndexed(id)
        );
    }
}