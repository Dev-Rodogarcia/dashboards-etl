package com.dashboard.api.dto.coletas;

public record ColetasHistoricoPerformanceDTO(
        String date,
        double performancePercentual,
        double metaPercentual,
        long finalizadas,
        long noPrazo,
        long foraDoPrazo
) {
}
