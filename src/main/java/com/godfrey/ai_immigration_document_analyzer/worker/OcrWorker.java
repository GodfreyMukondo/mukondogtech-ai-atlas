package com.godfrey.ai_immigration_document_analyzer.worker;

import com.godfrey.ai_immigration_document_analyzer.events.DocumentUploadedEvent;
import com.godfrey.ai_immigration_document_analyzer.repository.DocumentRepository;
import com.godfrey.ai_immigration_document_analyzer.service.OcrService;
import com.godfrey.ai_immigration_document_analyzer.service.storage.S3FileStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class OcrWorker {


    private final DocumentRepository repository;

    private final OcrService ocrService;

    private final S3FileStorageService s3FileStorageService;

    @KafkaListener(
            topics = "document.uploaded",
            groupId = "ocr-group"
    )
    @Transactional
    public void process(DocumentUploadedEvent event) {

        if (!isValidEvent(event)) {

            log.warn(
                    "Ignoring invalid document.uploaded event received by OCR worker"
            );

            return;
        }

        final Long documentId = event.documentId();

        final String filePath = event.filePath();

        try {

            log.info(
                    "Starting OCR processing | documentId={} | filePath={}",
                    documentId,
                    filePath
            );

            final byte[] fileBytes =
                    s3FileStorageService.downloadFile(
                            filePath
                    );

            if (fileBytes == null || fileBytes.length == 0) {

                throw new IllegalStateException(
                        "Downloaded document is empty."
                );
            }

            log.debug(
                    "Document downloaded from S3 | documentId={} | bytes={}",
                    documentId,
                    fileBytes.length
            );

            final String fileName =
                    extractFileName(filePath);

            final String mimeType =
                    determineMimeType(fileName);

            log.debug(
                    "Document metadata resolved | documentId={} | fileName={} | mimeType={}",
                    documentId,
                    fileName,
                    mimeType
            );

            final String extractedText =
                    ocrService.extractText(
                            fileBytes,
                            fileName,
                            mimeType
                    );

            final String normalizedText =
                    StringUtils.hasText(extractedText)
                            ? extractedText.trim()
                            : "";

            if (normalizedText.isEmpty()) {

                log.warn(
                        "OCR completed but no text was extracted | documentId={} | fileName={}",
                        documentId,
                        fileName
                );

            } else {

                log.info(
                        "OCR text extraction completed | documentId={} | characters={}",
                        documentId,
                        normalizedText.length()
                );
            }

            final int updatedRows =
                    repository.updateExtractedText(
                            documentId,
                            normalizedText
                    );

            if (updatedRows <= 0) {

                log.warn(
                        "OCR text was extracted but no document was updated | documentId={}",
                        documentId
                );

                return;
            }

            log.info(
                    "OCR processing completed successfully | documentId={} | characters={} | updatedRows={}",
                    documentId,
                    normalizedText.length(),
                    updatedRows
            );

        } catch (Exception exception) {

            log.error(
                    "OCR processing failed | documentId={} | filePath={}",
                    documentId,
                    filePath,
                    exception
            );

            throw exception;
        }
    }

    private boolean isValidEvent(
            DocumentUploadedEvent event
    ) {

        return event != null
                && event.documentId() != null
                && StringUtils.hasText(
                event.filePath()
        );
    }

    private String extractFileName(
            String filePath
    ) {

        if (!StringUtils.hasText(filePath)) {

            throw new IllegalArgumentException(
                    "S3 file path cannot be empty."
            );
        }

        final String normalizedPath =
                filePath
                        .trim()
                        .replace("\\", "/");

        final int separator =
                normalizedPath.lastIndexOf('/');

        final String fileName =
                separator >= 0
                        ? normalizedPath.substring(
                        separator + 1
                )
                        : normalizedPath;

        if (!StringUtils.hasText(fileName)) {

            throw new IllegalArgumentException(
                    "Unable to determine filename from S3 object key."
            );
        }

        return fileName;
    }

    private String determineMimeType(
            String fileName
    ) {

        if (!StringUtils.hasText(fileName)) {

            throw new IllegalArgumentException(
                    "Filename cannot be empty."
            );
        }

        final String normalizedFileName =
                fileName
                        .trim()
                        .toLowerCase(Locale.ROOT);

        if (normalizedFileName.endsWith(".pdf")) {

            return "application/pdf";
        }

        if (normalizedFileName.endsWith(".png")) {

            return "image/png";
        }

        if (normalizedFileName.endsWith(".jpg")
                || normalizedFileName.endsWith(".jpeg")) {

            return "image/jpeg";
        }

        throw new IllegalArgumentException(
                "Unsupported document type: " + fileName
        );
    }


}
