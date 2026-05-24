package com.dashboard.api.dto.performance;

import java.math.BigDecimal;

public record PerformanceOverviewDTO(
        String updatedAt,
        long totalEntregas,
        long finalizadas,
        long noPrazo,
        long foraDoPrazo,
        double performancePercentual,
        long emAtraso,
        BigDecimal pesoTaxadoToneladas,
        double comprovanteAnexadoPercentual,
        BigDecimal valorNfSemComprovante
) {
}
