package com.dashboard.api.dto.indicadoresgestao;

import java.math.BigDecimal;

public record IndenizacaoMercadoriasSeriePointDTO(
        String date,
        String filial,
        int totalSinistros,
        BigDecimal valorIndenizadoAbs,
        BigDecimal faturamentoBase,
        double pctIndenizacao
) {
}
