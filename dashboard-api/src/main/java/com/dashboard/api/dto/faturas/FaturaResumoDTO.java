package com.dashboard.api.dto.faturas;

import java.math.BigDecimal;

public record FaturaResumoDTO(
        String uniqueId,
        String documento,
        String emissao,
        String vencimento,
        String filial,
        String clientePagador,
        BigDecimal valorOperacional,
        BigDecimal valorFinanceiro,
        BigDecimal valorPago,
        BigDecimal valorAberto,
        String statusProcesso,
        String statusFinanceiro
) {}
