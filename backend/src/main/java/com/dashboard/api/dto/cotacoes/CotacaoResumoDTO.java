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
        BigDecimal valorFrete,
        String statusConversao,
        String motivoPerda,
        String tipoOperacao,
        Integer volumes,
        BigDecimal pesoTaxado,
        BigDecimal fretePorKg,
        BigDecimal minFreteKg,
        BigDecimal valorNf,
        BigDecimal percentualNf,
        String tabela,
        String origem,
        String destino,
        String cteDataEmissao,
        String nfseDataEmissao
) {}
