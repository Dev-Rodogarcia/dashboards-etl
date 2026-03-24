package com.dashboard.api.dto.cotacoes;

import java.math.BigDecimal;

public record CotacoesCorredorValiosoDTO(
        String trecho,
        BigDecimal valorFrete,
        int cotacoes
) {
}
