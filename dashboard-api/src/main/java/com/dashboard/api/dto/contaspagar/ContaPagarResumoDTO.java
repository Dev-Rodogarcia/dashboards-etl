package com.dashboard.api.dto.contaspagar;

import java.math.BigDecimal;

public record ContaPagarResumoDTO(
        Long lancamentoNumero,
        String documentoNumero,
        String emissao,
        String tipo,
        String filial,
        String fornecedor,
        BigDecimal valor,
        BigDecimal valorPago,
        BigDecimal valorAPagar,
        String classificacao,
        String descricaoContabil,
        String centroCusto,
        String dataLiquidacao,
        String statusPagamento,
        String conciliado
) {}
