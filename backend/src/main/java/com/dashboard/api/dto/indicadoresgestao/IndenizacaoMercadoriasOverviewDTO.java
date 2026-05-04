package com.dashboard.api.dto.indicadoresgestao;

import java.math.BigDecimal;

public record IndenizacaoMercadoriasOverviewDTO(
        String updatedAt,
        int totalSinistros,
        BigDecimal valorIndenizadoAbs,
        BigDecimal valorIndenizadoOriginal,
        BigDecimal faturamentoBase,
        double pctIndenizacao
) {
}
