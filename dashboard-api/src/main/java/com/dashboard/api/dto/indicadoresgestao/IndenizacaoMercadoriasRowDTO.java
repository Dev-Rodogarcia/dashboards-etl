package com.dashboard.api.dto.indicadoresgestao;

import java.math.BigDecimal;

public record IndenizacaoMercadoriasRowDTO(
        long numeroSinistro,
        String dataFinalizacao,
        String filial,
        Long minuta,
        BigDecimal resultadoFinalOriginal,
        BigDecimal resultadoFinalAbs,
        String causaRaiz,
        String solucao,
        double pctSobreFaturamentoFilial
) {
}
