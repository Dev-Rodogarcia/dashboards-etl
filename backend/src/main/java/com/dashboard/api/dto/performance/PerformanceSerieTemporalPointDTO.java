package com.dashboard.api.dto.performance;

public record PerformanceSerieTemporalPointDTO(
        String date,
        long total,
        long finalizadas,
        long emTransito,
        long pendentes,
        long canceladas,
        long emTratativa
) {
}
