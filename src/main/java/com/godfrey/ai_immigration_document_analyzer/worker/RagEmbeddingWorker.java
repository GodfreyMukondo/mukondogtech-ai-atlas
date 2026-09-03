package com.godfrey.ai_immigration_document_analyzer.worker;

import com.godfrey.ai_immigration_document_analyzer.events.DocumentUploadedEvent;
import com.godfrey.ai_immigration_document_analyzer.repository.DocumentRepository;
import com.godfrey.ai_immigration_document_analyzer.service.EmbeddingService;
import com.godfrey.ai_immigration_document_analyzer.service.VectorDbService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagEmbeddingWorker {

    private final DocumentRepository repository;

    private final EmbeddingService embeddingService;

    private final VectorDbService vectorDb;

    @KafkaListener(
            topics = "document.uploaded",
            groupId = "rag-group"
    )
    public void embed(
            DocumentUploadedEvent event
    ) {

        if (!isValidEvent(event)) {

            log.warn(
                    "Ignoring invalid document.uploaded event received by RAG embedding worker"
            );

            return;
        }

        final Long documentId =
                event.documentId();

        try {

            log.info(
                    "Starting RAG embedding processing | documentId={}",
                    documentId
            );

            Optional<String> extractedText =
                    repository.findExtractedTextById(
                            documentId
                    );

            if (extractedText.isEmpty()) {

                log.warn(
                        "No extracted text found for RAG embedding | documentId={}",
                        documentId
                );

                return;
            }

            final String text =
                    extractedText
                            .get()
                            .trim();

            if (!StringUtils.hasText(text)) {

                log.warn(
                        "Extracted text is empty or blank | documentId={}",
                        documentId
                );

                return;
            }

            log.debug(
                    "Extracted text retrieved for embedding | documentId={} | characters={}",
                    documentId,
                    text.length()
            );

            final List<Double> embedding =
                    embeddingService.embed(
                            text
                    );

            if (embedding == null ||
                    embedding.isEmpty()) {

                log.warn(
                        "Embedding service returned an empty vector | documentId={}",
                        documentId
                );

                return;
            }

            log.debug(
                    "Embedding generated | documentId={} | dimensions={}",
                    documentId,
                    embedding.size()
            );

            vectorDb.store(
                    documentId,
                    embedding,
                    text
            );

            log.info(
                    "RAG embedding stored successfully | documentId={} | dimensions={}",
                    documentId,
                    embedding.size()
            );

        } catch (Exception exception) {

            log.error(
                    "RAG embedding processing failed | documentId={}",
                    documentId,
                    exception
            );

            throw exception;
        }
    }

    private boolean isValidEvent(
            DocumentUploadedEvent event
    ) {

        return event != null
                && event.documentId() != null;
    }
}

