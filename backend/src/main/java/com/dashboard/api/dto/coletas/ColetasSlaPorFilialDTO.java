package com.dashboard.api.dto.coletas;

public record ColetasSlaPorFilialDTO(
        String filial,
        double slaPct,
        int total
) {
}
