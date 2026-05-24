package com.dashboard.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

final class PerformanceMetricasUtils {

    static final String STATUS_PENDENTE = "Pendente";
    static final String STATUS_EM_TRANSITO = "Em Trânsito";
    static final String STATUS_FINALIZADA = "Finalizada";
    static final String STATUS_CANCELADA = "Cancelada";
    static final String STATUS_EM_TRATATIVA = "Em Tratativa";
    static final String PERFORMANCE_NO_PRAZO = "NO PRAZO";
    static final String PERFORMANCE_FORA_DO_PRAZO = "FORA DO PRAZO";

    private PerformanceMetricasUtils() {
    }

    static String normalizarStatus(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_PENDENTE;
        }

        String normalizado = status.trim().toLowerCase(Locale.ROOT);
        return switch (normalizado) {
            case "finished", "finalizado", "finalizada", "delivered", "entregue" -> STATUS_FINALIZADA;
            case "canceled", "cancelled", "cancelado", "cancelada" -> STATUS_CANCELADA;
            case "in_transit", "em trânsito", "em transito", "manifested", "registrado", "delivering", "em entrega",
                    "in_transfer", "em transferência", "em transferencia" -> STATUS_EM_TRANSITO;
            case "occurrence_treatment", "tratamento de ocorrência", "tratamento de ocorrencia", "em tratativa",
                    "tratativa", "standby", "aguardando" -> STATUS_EM_TRATATIVA;
            default -> STATUS_PENDENTE;
        };
    }

    static Integer diferencaDias(LocalDate dataPrevisaoEntrega, LocalDate dataFinalizacao) {
        if (dataPrevisaoEntrega == null || dataFinalizacao == null) {
            return null;
        }
        return Math.toIntExact(ChronoUnit.DAYS.between(dataPrevisaoEntrega, dataFinalizacao));
    }

    static String performanceStatus(Integer diferencaDias) {
        if (diferencaDias == null) {
            return null;
        }
        return diferencaDias <= 0 ? PERFORMANCE_NO_PRAZO : PERFORMANCE_FORA_DO_PRAZO;
    }

    static String performanceStatusDias(Integer diferencaDias) {
        if (diferencaDias == null) {
            return null;
        }
        return switch (diferencaDias) {
            case 0 -> PERFORMANCE_NO_PRAZO;
            case 1 -> "1 DIA DE ATRASO";
            case 2 -> "2 DIAS DE ATRASO";
            case 3 -> "3 DIAS DE ATRASO";
            case -1 -> "1 DIA ANTES";
            case -2 -> "2 DIAS ANTES";
            case -3 -> "3 DIAS ANTES";
            default -> diferencaDias > 3 ? "ACIMA DE 3 DIAS DE ATRASO" : "ACIMA DE 3 DIAS ANTES";
        };
    }

    static double percentual(long numerador, long denominador) {
        if (denominador <= 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(numerador)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominador), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
