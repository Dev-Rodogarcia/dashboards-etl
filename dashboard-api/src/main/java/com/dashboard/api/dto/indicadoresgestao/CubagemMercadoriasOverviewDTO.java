package com.dashboard.api.dto.indicadoresgestao;

public record CubagemMercadoriasOverviewDTO(
        String updatedAt,
        int totalFretes,
        int fretesCubados,
        int fretesComPesoReal,
        double pctCubagem
) {
}
