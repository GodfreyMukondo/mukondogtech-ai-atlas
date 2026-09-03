package com.godfrey.ai_immigration_document_analyzer.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Production-ready date utility class.
 * Handles formatting and safe time operations.
 */
public final class DateUtils {

    private DateUtils() {
        // Prevent instantiation
    }

    private static final DateTimeFormatter DEFAULT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Formats LocalDateTime into readable string.
     */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DEFAULT_FORMAT);
    }

    /**
     * Returns current system timestamp.
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(ZoneId.systemDefault());
    }

    /**
     * Safely parses string into LocalDateTime is future extension point.
     */
    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneId.of("UTC"));
    }
}