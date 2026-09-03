package com.godfrey.ai_immigration_document_analyzer.service;

import com.godfrey.ai_immigration_document_analyzer.dto.KnowledgeDocumentResponse;
import com.godfrey.ai_immigration_document_analyzer.dto.KnowledgeDocumentSummaryResponse;
import com.godfrey.ai_immigration_document_analyzer.dto.request.CreateKnowledgeDocumentRequest;
import com.godfrey.ai_immigration_document_analyzer.dto.request.UpdateKnowledgeDocumentRequest;
import com.godfrey.ai_immigration_document_analyzer.dto.response.KnowledgeBaseStatsResponse;
import com.godfrey.ai_immigration_document_analyzer.entity.KnowledgeDocumentStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * ============================================================================
 * KNOWLEDGE BASE SERVICE
 * ============================================================================
 */
public interface KnowledgeBaseService {

    Page<KnowledgeDocumentSummaryResponse> search(
            String search,
            String category,
            KnowledgeDocumentStatus status,
            Pageable pageable
    );

    KnowledgeDocumentResponse getById(
            Long id
    );

    KnowledgeDocumentResponse create(
            CreateKnowledgeDocumentRequest request,
            String username
    );

    KnowledgeDocumentResponse update(
            Long id,
            UpdateKnowledgeDocumentRequest request,
            String username
    );

    void delete(
            Long id
    );

    KnowledgeDocumentResponse markAsIndexed(
            Long id
    );

    KnowledgeBaseStatsResponse getStatistics();
}