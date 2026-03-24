package com.dashboard.api.dto.manifestos;

import java.math.BigDecimal;

public record ManifestosRankingMotoristaDTO(
        String motorista,
        int manifestos,
        BigDecimal km,
        BigDecimal custoTotal
) {
}
