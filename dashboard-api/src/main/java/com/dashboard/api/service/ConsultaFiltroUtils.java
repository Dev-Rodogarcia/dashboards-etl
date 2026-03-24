package com.dashboard.api.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

final class ConsultaFiltroUtils {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private ConsultaFiltroUtils() {
    }

    static BigDecimal zeroSeNulo(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    static int zeroSeNulo(Integer valor) {
        return valor != null ? valor : 0;
    }

    static Long zeroSeNulo(Long valor) {
        return valor != null ? valor : 0L;
    }

    static String data(LocalDate data) {
        return data.format(DATE_FMT);
    }

    static String data(OffsetDateTime data) {
        return data.toLocalDate().format(DATE_FMT);
    }

    static BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }

        try {
            return new BigDecimal(value.trim().replace(",", "."));
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    static <T> String latestUpdate(List<T> rows, Function<T, LocalDateTime> extractor) {
        return rows.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(LocalDateTime.now())
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
