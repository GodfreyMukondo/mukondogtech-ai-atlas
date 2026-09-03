package com.godfrey.ai_immigration_document_analyzer.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * ============================================================================
 * OCR / DOCUMENT TEXT EXTRACTION SERVICE
 * ============================================================================
 *
 * Responsibilities:
 *
 * 1. Extract native text from PDF documents using Apache PDFBox.
 * 2. Extract text from PNG/JPG/JPEG images using Tesseract OCR.
 * 3. Detect scanned/image-only PDFs and fail clearly when OCR rendering is
 *    required but not configured.
 * 4. Validate incoming document content.
 * 5. Sanitize extracted text.
 *
 * Supported document types:
 *
 * - application/pdf
 * - image/png
 * - image/jpeg
 * - .pdf
 * - .png
 * - .jpg
 * - .jpeg
 *
 * PDFBox:
 *     2.0.x
 *
 * Tess4J:
 *     5.8.x
 *
 * IMPORTANT:
 * This implementation intentionally uses PDDocument.load(...)
 * because the project uses Apache PDFBox 2.0.30.
 *
 * PDFBox 3.x uses the Loader API instead.
 */
@Service
@Slf4j
public class OcrService {

    private static final String PDF_MIME_TYPE = "application/pdf";

    private static final String PNG_MIME_TYPE = "image/png";

    private static final String JPEG_MIME_TYPE = "image/jpeg";

    private static final String JPG_MIME_TYPE = "image/jpg";

    private static final String DEFAULT_LANGUAGE = "eng";

    @Value("${ocr.tessdata-path:}")
    private String tessDataPath;

    @Value("${ocr.language:eng}")
    private String language;

    /**
     * Minimum amount of extracted text that is considered meaningful.
     *
     * A PDF containing only a few characters may actually be a scanned PDF.
     */
    @Value("${ocr.pdf.min-text-length:20}")
    private int minimumPdfTextLength;

    /**
     * Whether image OCR is enabled.
     */
    @Value("${ocr.enabled:true}")
    private boolean ocrEnabled;

    private volatile ITesseract tesseract;

    /**
     * ========================================================================
     * INITIALIZATION
     * ========================================================================
     */
    @PostConstruct
    public void initialize() {

        log.info("Initializing document OCR service...");

        if (!ocrEnabled) {

            log.info(
                    "OCR is disabled through configuration."
            );

            return;
        }

        String configuredTessDataPath =
                tessDataPath == null
                        ? ""
                        : tessDataPath.trim();

        String configuredLanguage =
                language == null ||
                        language.isBlank()
                        ? DEFAULT_LANGUAGE
                        : language.trim();

        if (configuredTessDataPath.isBlank()) {

            log.warn(
                    "Tesseract tessdata path is not configured. " +
                            "Image OCR will be unavailable."
            );

            return;
        }

        try {

            Tesseract instance =
                    new Tesseract();

            instance.setDatapath(
                    configuredTessDataPath
            );

            instance.setLanguage(
                    configuredLanguage
            );

            /*
             * Store only after successful configuration.
             */
            this.tesseract =
                    instance;

            log.info(
                    "Tesseract initialized successfully | language={} | tessdata={}",
                    configuredLanguage,
                    configuredTessDataPath
            );

        } catch (Exception ex) {

            this.tesseract = null;

            log.error(
                    "Failed to initialize Tesseract OCR.",
                    ex
            );
        }
    }

    /**
     * ========================================================================
     * PUBLIC API
     * ========================================================================
     *
     * Extract text from document bytes.
     *
     * @param fileBytes document content
     * @param fileName original filename
     * @param mimeType detected/requested MIME type
     *
     * @return sanitized extracted text
     */
    public String extractText(
            byte[] fileBytes,
            String fileName,
            String mimeType
    ) {

        validateInput(
                fileBytes,
                fileName
        );

        String normalizedFileName =
                fileName.trim();

        String normalizedMimeType =
                normalizeMimeType(
                        mimeType
                );

        log.info(
                "Starting document text extraction | fileName={} | mimeType={} | size={} bytes",
                normalizedFileName,
                normalizedMimeType,
                fileBytes.length
        );

        try {

            if (isPdf(
                    normalizedFileName,
                    normalizedMimeType
            )) {

                return extractFromPdf(
                        fileBytes,
                        normalizedFileName
                );
            }

            if (isSupportedImage(
                    normalizedFileName,
                    normalizedMimeType
            )) {

                return extractFromImage(
                        fileBytes,
                        normalizedFileName
                );
            }

            throw new IllegalArgumentException(
                    "Unsupported document type for text extraction: "
                            + normalizedFileName
            );

        } catch (IllegalArgumentException ex) {

            throw ex;

        } catch (Exception ex) {

            log.error(
                    "Document text extraction failed | fileName={} | mimeType={}",
                    normalizedFileName,
                    normalizedMimeType,
                    ex
            );

            throw new IllegalStateException(
                    "Document text extraction failed.",
                    ex
            );
        }
    }

    /**
     * ========================================================================
     * PDF EXTRACTION
     * ========================================================================
     *
     * Extracts native text from a PDF using PDFBox 2.x.
     *
     * NOTE:
     * PDFBox does not perform OCR.
     *
     * If the PDF is scanned and contains no text layer, this method will
     * return an empty/very small result.
     */
    private String extractFromPdf(
            byte[] fileBytes,
            String fileName
    ) throws IOException {

        log.info(
                "Extracting native PDF text | fileName={}",
                fileName
        );

        try (
                PDDocument document =
                        PDDocument.load(
                                new ByteArrayInputStream(
                                        fileBytes
                                )
                        )
        ) {

            if (document.isEncrypted()) {

                log.warn(
                        "PDF is encrypted and may not be readable | fileName={}",
                        fileName
                );
            }

            PDFTextStripper stripper =
                    new PDFTextStripper();

            /*
             * Prevent unnecessarily huge extraction buffers.
             */
            stripper.setSortByPosition(true);

            String extractedText =
                    stripper.getText(
                            document
                    );

            String sanitized =
                    sanitize(
                            extractedText
                    );

            log.info(
                    "PDF text extraction completed | fileName={} | characters={}",
                    fileName,
                    sanitized.length()
            );

            /*
             * A scanned PDF usually has little or no native text.
             */
            if (sanitized.length() <
                    minimumPdfTextLength) {

                log.warn(
                        "PDF contains little or no extractable native text. " +
                                "It may be a scanned PDF | fileName={} | characters={}",
                        fileName,
                        sanitized.length()
                );
            }

            return sanitized;
        }
    }

    /**
     * ========================================================================
     * IMAGE OCR
     * ========================================================================
     */
    private String extractFromImage(
            byte[] fileBytes,
            String fileName
    ) throws Exception {

        if (!ocrEnabled) {

            throw new IllegalStateException(
                    "OCR processing is disabled."
            );
        }

        ITesseract ocrEngine =
                this.tesseract;

        if (ocrEngine == null) {

            throw new IllegalStateException(
                    "Tesseract OCR is not configured. " +
                            "Configure TESSDATA_PATH and OCR_LANGUAGE."
            );
        }

        log.info(
                "Starting image OCR | fileName={}",
                fileName
        );

        BufferedImage image =
                ImageIO.read(
                        new ByteArrayInputStream(
                                fileBytes
                        )
                );

        if (image == null) {

            throw new IllegalArgumentException(
                    "The uploaded file is not a valid readable image."
            );
        }

        if (image.getWidth() <= 0 ||
                image.getHeight() <= 0) {

            throw new IllegalArgumentException(
                    "Image dimensions are invalid."
            );
        }

        log.debug(
                "Image loaded | fileName={} | width={} | height={}",
                fileName,
                image.getWidth(),
                image.getHeight()
        );

        String extractedText =
                ocrEngine.doOCR(
                        image
                );

        String sanitized =
                sanitize(
                        extractedText
                );

        log.info(
                "Image OCR completed | fileName={} | characters={}",
                fileName,
                sanitized.length()
        );

        return sanitized;
    }

    /**
     * ========================================================================
     * DOCUMENT TYPE DETECTION
     * ========================================================================
     */
    private boolean isPdf(
            String fileName,
            String mimeType
    ) {

        return PDF_MIME_TYPE.equals(
                mimeType
        )
                ||
                hasExtension(
                        fileName,
                        ".pdf"
                );
    }

    private boolean isSupportedImage(
            String fileName,
            String mimeType
    ) {

        if (PNG_MIME_TYPE.equals(mimeType)
                || JPEG_MIME_TYPE.equals(mimeType)
                || JPG_MIME_TYPE.equals(mimeType)) {

            return true;
        }

        return hasExtension(
                fileName,
                ".png",
                ".jpg",
                ".jpeg"
        );
    }

    /**
     * ========================================================================
     * MIME TYPE NORMALIZATION
     * ========================================================================
     */
    private String normalizeMimeType(
            String mimeType
    ) {

        if (mimeType == null ||
                mimeType.isBlank()) {

            return "";
        }

        return mimeType
                .trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }

    /**
     * ========================================================================
     * FILE EXTENSION
     * ========================================================================
     */
    private boolean hasExtension(
            String fileName,
            String... extensions
    ) {

        if (fileName == null ||
                fileName.isBlank()) {

            return false;
        }

        String normalized =
                fileName
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .trim();

        for (String extension :
                extensions) {

            if (normalized.endsWith(
                    extension
            )) {

                return true;
            }
        }

        return false;
    }

    /**
     * ========================================================================
     * TEXT SANITIZATION
     * ========================================================================
     */
    private String sanitize(
            String text
    ) {

        if (text == null) {

            return "";
        }

        return text
                /*
                 * Remove NULL characters.
                 */
                .replace(
                        "\u0000",
                        ""
                )

                /*
                 * Normalize Windows line endings.
                 */
                .replace(
                        "\r\n",
                        "\n"
                )

                /*
                 * Normalize old Mac line endings.
                 */
                .replace(
                        "\r",
                        "\n"
                )

                /*
                 * Remove trailing spaces from lines.
                 */
                .replaceAll(
                        "(?m)[ \\t]+$",
                        ""
                )

                /*
                 * Prevent excessive blank lines.
                 */
                .replaceAll(
                        "\\n{3,}",
                        "\n\n"
                )

                .trim();
    }

    /**
     * ========================================================================
     * INPUT VALIDATION
     * ========================================================================
     */
    private void validateInput(
            byte[] fileBytes,
            String fileName
    ) {

        if (fileBytes == null ||
                fileBytes.length == 0) {

            throw new IllegalArgumentException(
                    "Document content cannot be empty."
            );
        }

        if (fileName == null ||
                fileName.isBlank()) {

            throw new IllegalArgumentException(
                    "Document filename cannot be empty."
            );
        }
    }
}