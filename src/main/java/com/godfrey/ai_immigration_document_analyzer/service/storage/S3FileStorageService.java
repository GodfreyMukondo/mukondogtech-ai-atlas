package com.godfrey.ai_immigration_document_analyzer.service.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

/**
 * ============================================================================
 * S3 FILE STORAGE SERVICE
 * ============================================================================
 *
 * Responsible exclusively for storing immigration documents in Amazon S3.
 *
 * Responsibilities:
 *
 * - Validate upload metadata
 * - Generate collision-resistant S3 object keys
 * - Upload files
 * - Download files for OCR
 * - Delete files
 * - Prevent path traversal
 * - Avoid storing physical local upload paths in the database
 *
 * Database FILE_PATH contains the S3 object key.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3FileStorageService {

    private static final long DEFAULT_MAX_FILE_SIZE =
            10L * 1024L * 1024L;

    private static final String DOCUMENT_PREFIX =
            "documents";

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${file.max-size:10485760}")
    private long maxFileSize;

    /**
     * Upload a multipart file to S3.
     *
     * @return S3 object key
     */
    public String saveFile(
            MultipartFile file,
            Long userId
    ) {

        validateFile(
                file,
                userId
        );

        final String originalFileName =
                sanitizeFileName(
                        file.getOriginalFilename()
                );

        final String extension =
                extractExtension(
                        originalFileName
                );

        final String objectKey =
                generateObjectKey(
                        userId,
                        extension
                );

        final String contentType =
                normalizeMimeType(
                        file.getContentType()
                );

        try {

            PutObjectRequest request =
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .contentType(contentType)
                            .contentLength(file.getSize())
                            .metadata(
                                    java.util.Map.of(
                                            "original-filename",
                                            encodeMetadata(
                                                    originalFileName
                                            ),
                                            "user-id",
                                            String.valueOf(userId)
                                    )
                            )
                            .build();

            s3Client.putObject(
                    request,
                    RequestBody.fromInputStream(
                            file.getInputStream(),
                            file.getSize()
                    )
            );

            log.info(
                    "Document uploaded to S3 | bucket={} | key={} | " +
                            "userId={} | size={} | contentType={}",
                    bucketName,
                    objectKey,
                    userId,
                    file.getSize(),
                    contentType
            );

            return objectKey;

        } catch (IOException ex) {

            log.error(
                    "Unable to read uploaded file | userId={} | key={}",
                    userId,
                    objectKey,
                    ex
            );

            throw new IllegalStateException(
                    "Unable to read uploaded document.",
                    ex
            );

        } catch (S3Exception ex) {

            log.error(
                    "S3 upload failed | bucket={} | key={} | userId={} | " +
                            "statusCode={} | awsError={}",
                    bucketName,
                    objectKey,
                    userId,
                    ex.statusCode(),
                    ex.awsErrorDetails() != null
                            ? ex.awsErrorDetails().errorMessage()
                            : "unknown",
                    ex
            );

            throw new IllegalStateException(
                    "Unable to store document in secure storage.",
                    ex
            );
        }
    }

    /**
     * Download an S3 object into memory.
     *
     * Suitable for the current document size limit of 10 MB.
     */
    public byte[] downloadFile(
            String objectKey
    ) {

        validateObjectKey(
                objectKey
        );

        try {

            GetObjectRequest request =
                    GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .build();

            byte[] content =
                    s3Client.getObjectAsBytes(
                            request
                    ).asByteArray();

            log.debug(
                    "Document downloaded from S3 | bucket={} | key={} | bytes={}",
                    bucketName,
                    objectKey,
                    content.length
            );

            return content;

        } catch (NoSuchKeyException ex) {

            log.warn(
                    "S3 document does not exist | bucket={} | key={}",
                    bucketName,
                    objectKey
            );

            throw new IllegalArgumentException(
                    "Stored document could not be found.",
                    ex
            );

        } catch (S3Exception ex) {

            log.error(
                    "S3 download failed | bucket={} | key={} | statusCode={}",
                    bucketName,
                    objectKey,
                    ex.statusCode(),
                    ex
            );

            throw new IllegalStateException(
                    "Unable to retrieve stored document.",
                    ex
            );
        }
    }

    /**
     * Delete an S3 object.
     */
    public void deleteFile(
            String objectKey
    ) {

        validateObjectKey(
                objectKey
        );

        try {

            DeleteObjectRequest request =
                    DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .build();

            s3Client.deleteObject(
                    request
            );

            log.info(
                    "Document deleted from S3 | bucket={} | key={}",
                    bucketName,
                    objectKey
            );

        } catch (S3Exception ex) {

            log.error(
                    "S3 deletion failed | bucket={} | key={} | statusCode={}",
                    bucketName,
                    objectKey,
                    ex.statusCode(),
                    ex
            );

            throw new IllegalStateException(
                    "Unable to delete stored document.",
                    ex
            );
        }
    }

    /**
     * Generate a private S3 object key.
     *
     * Example:
     *
     * documents/25/550e8400-e29b-41d4-a716-446655440000.pdf
     */
    private String generateObjectKey(
            Long userId,
            String extension
    ) {

        return DOCUMENT_PREFIX
                + "/"
                + userId
                + "/"
                + UUID.randomUUID()
                + "."
                + extension;
    }

    private void validateFile(
            MultipartFile file,
            Long userId
    ) {

        if (userId == null ||
                userId <= 0) {

            throw new IllegalArgumentException(
                    "Invalid user ID."
            );
        }

        if (file == null) {

            throw new IllegalArgumentException(
                    "Uploaded file cannot be null."
            );
        }

        if (file.isEmpty()) {

            throw new IllegalArgumentException(
                    "Uploaded file cannot be empty."
            );
        }

        if (file.getSize() <= 0) {

            throw new IllegalArgumentException(
                    "Uploaded file has an invalid size."
            );
        }

        final long effectiveMaxSize =
                maxFileSize > 0
                        ? maxFileSize
                        : DEFAULT_MAX_FILE_SIZE;

        if (file.getSize() > effectiveMaxSize) {

            throw new IllegalArgumentException(
                    "File exceeds the maximum allowed size."
            );
        }

        String fileName =
                file.getOriginalFilename();

        if (fileName == null ||
                fileName.isBlank()) {

            throw new IllegalArgumentException(
                    "Uploaded file must have a filename."
            );
        }

        String extension =
                extractExtension(fileName);

        if (!isAllowedExtension(extension)) {

            throw new IllegalArgumentException(
                    "Unsupported file type. " +
                            "Supported formats: PDF, PNG, JPG and JPEG."
            );
        }
    }

    private boolean isAllowedExtension(
            String extension
    ) {

        return extension.equals("pdf")
                || extension.equals("png")
                || extension.equals("jpg")
                || extension.equals("jpeg");
    }

    private String extractExtension(
            String fileName
    ) {

        if (fileName == null ||
                fileName.isBlank()) {

            return "";
        }

        String normalized =
                fileName
                        .trim()
                        .replace("\\", "/");

        int slash =
                normalized.lastIndexOf('/');

        if (slash >= 0) {

            normalized =
                    normalized.substring(
                            slash + 1
                    );
        }

        int dot =
                normalized.lastIndexOf('.');

        if (dot < 0 ||
                dot == normalized.length() - 1) {

            return "";
        }

        return normalized
                .substring(dot + 1)
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String sanitizeFileName(
            String fileName
    ) {

        if (fileName == null ||
                fileName.isBlank()) {

            throw new IllegalArgumentException(
                    "Invalid filename."
            );
        }

        String normalized =
                fileName
                        .trim()
                        .replace("\\", "/");

        int slash =
                normalized.lastIndexOf('/');

        if (slash >= 0) {

            normalized =
                    normalized.substring(
                            slash + 1
                    );
        }

        normalized =
                normalized.replaceAll(
                        "[\\p{Cntrl}]",
                        ""
                );

        if (normalized.isBlank()) {

            throw new IllegalArgumentException(
                    "Invalid filename."
            );
        }

        return normalized;
    }

    private String normalizeMimeType(
            String contentType
    ) {

        if (contentType == null ||
                contentType.isBlank()) {

            return "application/octet-stream";
        }

        return contentType
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private void validateObjectKey(
            String objectKey
    ) {

        if (objectKey == null ||
                objectKey.isBlank()) {

            throw new IllegalArgumentException(
                    "Invalid storage object key."
            );
        }

        String normalized =
                objectKey.trim();

        if (normalized.startsWith("/")
                || normalized.contains("..")
                || normalized.contains("\\")
                || !normalized.startsWith(
                DOCUMENT_PREFIX + "/"
        )) {

            throw new SecurityException(
                    "Invalid storage object key."
            );
        }
    }

    /**
     * S3 user metadata must remain ASCII-safe.
     */
    private String encodeMetadata(
            String value
    ) {

        return new String(
                value.getBytes(
                        StandardCharsets.UTF_8
                ),
                StandardCharsets.ISO_8859_1
        );
    }
}