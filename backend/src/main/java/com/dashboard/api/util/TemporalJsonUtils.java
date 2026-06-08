package com.dashboard.api.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

public final class TemporalJsonUtils {

    private static final ZoneId ZONA_OPERACIONAL = ZoneId.of(PeriodoOffsetDateTimeHelper.DEFAULT_ZONE_ID);
    private static final DateTimeFormatter ISO_COM_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final DateTimeFormatter SQL_LOCAL_DATE_TIME = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral(' ')
            .append(DateTimeFormatter.ISO_LOCAL_TIME)
            .toFormatter();
    private static final DateTimeFormatter SQL_OFFSET_DATE_TIME = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral(' ')
            .append(DateTimeFormatter.ISO_LOCAL_TIME)
            .appendOffsetId()
            .toFormatter();
    private static final DateTimeFormatter SQL_OFFSET_DATE_TIME_COMPACTO = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral(' ')
            .append(DateTimeFormatter.ISO_LOCAL_TIME)
            .appendOffset("+HHMM", "Z")
            .toFormatter();
    private static final DateTimeFormatter ISO_OFFSET_DATE_TIME_COMPACTO = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .appendOffset("+HHMM", "Z")
            .toFormatter();
    private static final List<DateTimeFormatter> FORMATADORES_COM_OFFSET = List.of(
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ISO_ZONED_DATE_TIME,
            SQL_OFFSET_DATE_TIME,
            SQL_OFFSET_DATE_TIME_COMPACTO,
            ISO_OFFSET_DATE_TIME_COMPACTO
    );
    private static final List<DateTimeFormatter> FORMATADORES_LOCAIS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            SQL_LOCAL_DATE_TIME
    );

    private TemporalJsonUtils() {
    }

    public static String formatarIsoComOffset(LocalDateTime timestamp) {
        LocalDateTime dataHora = timestamp != null
                ? timestamp
                : LocalDateTime.now(ZONA_OPERACIONAL);
        return formatarNoFusoOperacional(dataHora);
    }

    public static String garantirIsoComOffset(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return formatarIsoComOffset(null);
        }

        return resolverOffsetDateTime(timestamp.trim())
                .format(ISO_COM_OFFSET);
    }

    /**
     * @deprecated O nome sugere UTC, mas o contrato real serializa a data no fuso
     * operacional do dashboard com offset explicito. Use {@link #formatarIsoComOffset(LocalDateTime)}.
     */
    @Deprecated(since = "2026-06", forRemoval = false)
    public static String formatarUtc(LocalDateTime timestamp) {
        return formatarIsoComOffset(timestamp);
    }

    /**
     * @deprecated O nome sugere UTC, mas o contrato real normaliza a data para o fuso
     * operacional do dashboard com offset explicito. Use {@link #garantirIsoComOffset(String)}.
     */
    @Deprecated(since = "2026-06", forRemoval = false)
    public static String garantirUtc(String timestamp) {
        return garantirIsoComOffset(timestamp);
    }

    private static String formatarNoFusoOperacional(LocalDateTime dataHora) {
        return dataHora.atZone(ZONA_OPERACIONAL)
                .toOffsetDateTime()
                .format(ISO_COM_OFFSET);
    }

    private static OffsetDateTime resolverOffsetDateTime(String timestamp) {
        return parseOffsetDateTime(timestamp)
                .or(() -> parseLocalDateTime(timestamp)
                        .map(dataHora -> dataHora.atZone(ZONA_OPERACIONAL).toOffsetDateTime()))
                .orElseGet(() -> OffsetDateTime.now(ZONA_OPERACIONAL));
    }

    private static Optional<OffsetDateTime> parseOffsetDateTime(String timestamp) {
        for (DateTimeFormatter formatter : FORMATADORES_COM_OFFSET) {
            try {
                return Optional.of(OffsetDateTime.parse(timestamp, formatter)
                        .atZoneSameInstant(ZONA_OPERACIONAL)
                        .toOffsetDateTime());
            } catch (DateTimeParseException ignored) {
                // Tenta o proximo formato aceito pelo contrato historico da API.
            }
        }
        return Optional.empty();
    }

    private static Optional<LocalDateTime> parseLocalDateTime(String timestamp) {
        for (DateTimeFormatter formatter : FORMATADORES_LOCAIS) {
            try {
                return Optional.of(LocalDateTime.parse(timestamp, formatter));
            } catch (DateTimeParseException ignored) {
                // Tenta o proximo formato aceito pelo contrato historico da API.
            }
        }
        return Optional.empty();
    }
}
