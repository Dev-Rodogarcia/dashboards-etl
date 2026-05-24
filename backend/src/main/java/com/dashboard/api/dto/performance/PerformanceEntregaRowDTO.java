package com.dashboard.api.dto.performance;

import java.math.BigDecimal;

public record PerformanceEntregaRowDTO(
        long numeroMinuta,
        String status,
        String dataPrevisaoEntrega,
        String dataFinalizacao,
        String responsavelRegiaoDestino,
        String filialEmissora,
        String regiaoDestino,
        String cidadeDestino,
        BigDecimal pesoTaxado,
        BigDecimal valorNotaFiscal,
        boolean comprovanteAnexado,
        String performanceStatus,
        String performanceStatusDias
) {
}
