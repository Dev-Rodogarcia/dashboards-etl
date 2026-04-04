package com.dashboard.api.dto.indicadoresgestao;

public record CubagemMercadoriasSeriePointDTO(
        String date,
        String filial,
        int totalFretes,
        int fretesCubados,
        double pctCubagem
) {
}
