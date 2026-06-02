package com.dashboard.api.util;

import com.dashboard.api.model.VisaoFretesEntity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Function;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class IndicadoresGestaoMetricasUtils {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final BigDecimal VALOR_MINIMO_OPERACIONAL = new BigDecimal("0.01");
    private static final Set<String> DOCUMENTOS_FILIAIS_OPERACIONAIS = Set.of(
            "51863654000180",
            "51863654000260",
            "60960473000162",
            "60960473000243",
            "60960473000596",
            "60960473000677",
            "60960473000758",
            "60960473000839",
            "60960473001134",
            "60960473001304",
            "60960473001568"
    );

    private IndicadoresGestaoMetricasUtils() {
    }

    public static double percentual(long numerador, long denominador) {
        return percentual(numerador, denominador, 1);
    }

    public static double percentual(long numerador, long denominador, int casasDecimais) {
        if (denominador <= 0) {
            return 0.0;
        }
        return BigDecimal.valueOf((numerador * 100.0) / denominador)
                .setScale(casasDecimais, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public static double percentual(BigDecimal numerador, BigDecimal denominador) {
        if (numerador == null || denominador == null || denominador.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        return numerador.multiply(BigDecimal.valueOf(100))
                .divide(denominador, 3, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public static BigDecimal zero(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    public static BigDecimal abs(BigDecimal valor) {
        return zero(valor).abs();
    }

    public static String formatar(LocalDate data) {
        return data != null ? data.atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    public static String formatar(LocalDateTime dataHora) {
        return dataHora != null ? dataHora.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    public static String formatar(OffsetDateTime dataHora) {
        return dataHora != null ? dataHora.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : null;
    }

    public static String chaveSerie(LocalDate data, String agrupador) {
        return (data != null ? data.format(DATE_FMT) : "") + "|" + Objects.toString(agrupador, "");
    }

    public static <T> String latestUpdate(Collection<T> rows, Function<T, LocalDateTime> extractor) {
        return rows.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(LocalDateTime.now())
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public static String textoOuPadrao(String valor, String padrao) {
        return valor == null || valor.isBlank() ? padrao : valor;
    }

    public static <T> T primeira(List<T> valores) {
        return valores.isEmpty() ? null : valores.get(0);
    }

    public static boolean freteOperacionalElegivel(VisaoFretesEntity frete) {
        return frete != null
                && !Boolean.TRUE.equals(frete.getCortesiaFlag())
                && !freteInternoSemDocumento(frete)
                && !freteSemDocumentoSemValor(frete)
                && !freteSubstitutoPendenteSemDocumento(frete);
    }

    public static boolean freteComValorOperacionalElegivel(VisaoFretesEntity frete) {
        return freteOperacionalElegivel(frete)
                && zero(frete.getValorTotal()).compareTo(VALOR_MINIMO_OPERACIONAL) > 0;
    }

    private static boolean freteInternoSemDocumento(VisaoFretesEntity frete) {
        return !documentoFiscalEmitido(frete.getDocumentoOficialTipo())
                && DOCUMENTOS_FILIAIS_OPERACIONAIS.contains(normalizarDocumento(frete.getPagadorDocumento()));
    }

    private static boolean freteSemDocumentoSemValor(VisaoFretesEntity frete) {
        return !documentoFiscalEmitido(frete.getDocumentoOficialTipo())
                && zero(frete.getValorTotal()).compareTo(VALOR_MINIMO_OPERACIONAL) <= 0;
    }

    private static boolean freteSubstitutoPendenteSemDocumento(VisaoFretesEntity frete) {
        return !documentoFiscalEmitido(frete.getDocumentoOficialTipo())
                && contem(frete.getTipoFrete(), "SUBSTITUTE")
                && contem(frete.getStatus(), "PENDENTE");
    }

    private static boolean documentoFiscalEmitido(String tipoDocumento) {
        if (tipoDocumento == null || tipoDocumento.isBlank()) {
            return false;
        }
        String normalizado = tipoDocumento.trim().toUpperCase(Locale.ROOT);
        return "CT-E".equals(normalizado) || "NFS-E".equals(normalizado);
    }

    private static boolean contem(String valor, String trecho) {
        return valor != null && valor.trim().toUpperCase(Locale.ROOT).contains(trecho);
    }

    private static String normalizarDocumento(String documento) {
        if (documento == null) {
            return "";
        }
        return documento.replaceAll("[^0-9A-Za-z]", "");
    }
}
