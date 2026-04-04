package com.dashboard.api.dto.indicadoresgestao;

import java.math.BigDecimal;

public record CubagemMercadoriasRowDTO(
        long numeroMinuta,
        String dataFrete,
        String filialEmissora,
        String pagador,
        String destino,
        BigDecimal pesoTaxado,
        BigDecimal pesoReal,
        BigDecimal pesoCubado,
        BigDecimal totalM3,
        boolean cubado
) {
}
