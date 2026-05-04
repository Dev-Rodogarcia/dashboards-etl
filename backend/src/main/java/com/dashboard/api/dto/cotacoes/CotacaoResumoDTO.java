package com.dashboard.api.dto.cotacoes;

import java.math.BigDecimal;

public record CotacaoResumoDTO(
        Long numeroCotacao,
        String dataCotacao,
        String filial,
        String solicitante,
        String clientePagador,
        String cliente,
        String trecho,
        BigDecimal pesoTaxado,
        BigDecimal valorNf,
        BigDecimal valorFrete,
        String tabela,
        String statusConversao,
        String motivoPerda,
        String cteDataEmissao,
        String nfseDataEmissao
) {}
