package com.dashboard.api.dto.cotacoes;

import java.math.BigDecimal;

public record CotacoesAgrupamentoDTO(
        String nome,
        BigDecimal valorPotencial,
        BigDecimal valorConvertido,
        int cotacoes,
        int convertidas,
        int reprovadas
) {
}
