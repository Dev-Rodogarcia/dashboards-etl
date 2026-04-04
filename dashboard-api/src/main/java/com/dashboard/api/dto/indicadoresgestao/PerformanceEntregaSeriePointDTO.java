package com.dashboard.api.dto.indicadoresgestao;

public record PerformanceEntregaSeriePointDTO(
        String date,
        String responsavelRegiaoDestino,
        int totalEntregas,
        int entregasNoPrazo,
        int entregasSemDados,
        double pctNoPrazo
) {
}
