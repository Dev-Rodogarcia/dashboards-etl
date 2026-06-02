package com.dashboard.api.util;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.function.Function;
import java.util.List;
import java.util.Objects;

public final class ConsultaFiltroUtils {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private ConsultaFiltroUtils() {
    }

    public static BigDecimal zeroSeNulo(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    public static int zeroSeNulo(Integer valor) {
        return valor != null ? valor : 0;
    }

    public static Long zeroSeNulo(Long valor) {
        return valor != null ? valor : 0L;
    }

    public static String data(LocalDate data) {
        return data.format(DATE_FMT);
    }

    public static String data(OffsetDateTime data) {
        return data.toLocalDate().format(DATE_FMT);
    }

    public static BigDecimal parseBigDecimal(String value) {
        BigDecimal parsed = parseBigDecimalOrNull(value);
        return parsed != null ? parsed : BigDecimal.ZERO;
    }

    public static BigDecimal parseBigDecimalOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalizado = normalizarNumeroDecimal(value);
        if (normalizado.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(normalizado);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String normalizarNumeroDecimal(String value) {
        String texto = value
                .replace('\u00A0', ' ')
                .trim()
                .replaceAll("\\s+", "");
        if (texto.isBlank()) {
            return "";
        }

        int ultimaVirgula = texto.lastIndexOf(',');
        int ultimoPonto = texto.lastIndexOf('.');
        if (ultimaVirgula >= 0 && ultimoPonto >= 0) {
            return ultimaVirgula > ultimoPonto
                    ? texto.replace(".", "").replace(",", ".")
                    : texto.replace(",", "");
        }
        if (ultimaVirgula >= 0) {
            return texto.replace(".", "").replace(",", ".");
        }
        return texto.replace(",", "");
    }

    public static <T> String latestUpdate(List<T> rows, Function<T, LocalDateTime> extractor) {
        return rows.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(LocalDateTime.now())
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
