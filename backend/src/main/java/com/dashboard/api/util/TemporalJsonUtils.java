package com.dashboard.api.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.regex.Pattern;

public final class TemporalJsonUtils {

    private static final Pattern OFFSET_SUFFIX = Pattern.compile(".*(?:Z|[+-]\\d{2}:?\\d{2})$");

    private TemporalJsonUtils() {
    }

    public static String formatarUtc(LocalDateTime timestamp) {
        Instant instante = timestamp != null
                ? timestamp.toInstant(ZoneOffset.UTC)
                : Instant.now();
        return instante.toString();
    }

    public static String garantirUtc(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return Instant.now().toString();
        }

        String normalizado = timestamp.trim().replaceFirst("^(\\d{4}-\\d{2}-\\d{2})\\s+", "$1T");
        return OFFSET_SUFFIX.matcher(normalizado).matches()
                ? normalizado
                : normalizado + "Z";
    }
}
