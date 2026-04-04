package com.dashboard.api.dto.indicadoresgestao;

import java.math.BigDecimal;

public record IndenizacaoMercadoriasRowDTO(
        long numeroSinistro,
        String dataAbertura,
        String filial,
        Long minuta,
        BigDecimal resultadoFinalOriginal,
        BigDecimal resultadoFinalAbs,
        String ocorrencia,
        String solucao,
        double pctSobreFaturamentoFilial
) {
}
