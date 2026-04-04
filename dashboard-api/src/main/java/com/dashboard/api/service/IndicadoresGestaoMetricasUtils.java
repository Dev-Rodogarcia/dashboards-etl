package com.dashboard.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

final class IndicadoresGestaoMetricasUtils {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private IndicadoresGestaoMetricasUtils() {
    }

    static double percentual(long numerador, long denominador) {
        return percentual(numerador, denominador, 1);
    }

    static double percentual(long numerador, long denominador, int casasDecimais) {
        if (denominador <= 0) {
            return 0.0;
        }
        return BigDecimal.valueOf((numerador * 100.0) / denominador)
                .setScale(casasDecimais, RoundingMode.HALF_UP)
                .doubleValue();
    }

    static double percentual(BigDecimal numerador, BigDecimal denominador) {
        if (numerador == null || denominador == null || denominador.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        return numerador.multiply(BigDecimal.valueOf(100))
                .divide(denominador, 3, RoundingMode.HALF_UP)
                .doubleValue();
    }

    static BigDecimal zero(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    static BigDecimal abs(BigDecimal valor) {
        return zero(valor).abs();
    }

    static String formatar(LocalDate data) {
        return data != null ? data.atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    static String formatar(LocalDateTime dataHora) {
        return dataHora != null ? dataHora.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    static String formatar(OffsetDateTime dataHora) {
        return dataHora != null ? dataHora.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : null;
    }

    static String chaveSerie(LocalDate data, String agrupador) {
        return (data != null ? data.format(DATE_FMT) : "") + "|" + Objects.toString(agrupador, "");
    }

    static <T> String latestUpdate(Collection<T> rows, Function<T, LocalDateTime> extractor) {
        return rows.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(LocalDateTime.now())
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    static String textoOuPadrao(String valor, String padrao) {
        return valor == null || valor.isBlank() ? padrao : valor;
    }

    static <T> T primeira(List<T> valores) {
        return valores.isEmpty() ? null : valores.get(0);
    }
}
