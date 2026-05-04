package com.dashboard.api.dto.faturas;

import java.math.BigDecimal;

public record FaturaReconciliacaoDTO(
        String uniqueId,
        String documento,
        String emissao,
        String clientePagador,
        BigDecimal valorOperacional,
        BigDecimal valorFinanceiro,
        String status
) {}
