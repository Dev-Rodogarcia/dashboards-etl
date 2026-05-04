package com.dashboard.api.dto.indicadoresgestao;

public record PerformanceEntregaSeriePointDTO(
        String date,
        String filialPerformance,
        int totalEntregas,
        int entregasNoPrazo,
        int entregasForaDoPrazo,
        double pctNoPrazo
) {
}
