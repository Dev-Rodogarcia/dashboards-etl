package com.dashboard.api.dto.fretes;

import java.math.BigDecimal;

public record FretesFaturamentoGrupoDTO(
        String nome,
        BigDecimal receita,
        int fretes
) {
}
