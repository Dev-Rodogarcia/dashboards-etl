package com.dashboard.api.dto.tracking;

public record TrackingPrevisaoVencidaFilialDTO(
        String filialAtual,
        int vencidas,
        int total
) {
}
