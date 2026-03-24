package com.dashboard.api.dto.fretes;

public record FretesPrevisaoPorStatusDTO(
        String status,
        int vencidos,
        int noPrazo
) {
}
