package com.dashboard.api.dto.performance;

public record PerformanceHistoricoPointDTO(
        String date,
        double performancePercentual,
        double metaPercentual,
        long finalizadas,
        long noPrazo
) {
}
