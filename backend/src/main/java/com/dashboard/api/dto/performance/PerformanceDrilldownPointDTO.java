package com.dashboard.api.dto.performance;

public record PerformanceDrilldownPointDTO(
        String nome,
        String nivel,
        long noPrazo,
        long foraDoPrazo,
        long emAtraso,
        long total
) {
}
