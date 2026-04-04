package com.dashboard.api.dto.indicadoresgestao;

public record PerformanceEntregaRowDTO(
        long numeroMinuta,
        String dataFrete,
        String filialEmissora,
        String responsavelRegiaoDestino,
        String previsaoEntrega,
        String dataFinalizacao,
        Integer performanceDiferencaDias,
        String performanceStatus
) {
}
