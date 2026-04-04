package com.dashboard.api.dto.indicadoresgestao;

public record PerformanceEntregaOverviewDTO(
        String updatedAt,
        int totalEntregas,
        int entregasNoPrazo,
        int entregasSemDados,
        double pctNoPrazo
) {
}
