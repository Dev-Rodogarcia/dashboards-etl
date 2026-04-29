package com.dashboard.api.dto.indicadoresgestao;

public record PerformanceEntregaRowDTO(
        long numeroMinuta,
        String dataFrete,
        String filialPerformance,
        String filialEmissora,
        String previsaoEntrega,
        String dataFinalizacao,
        Integer performanceDiferencaDias,
        String performanceStatus
) {
}
