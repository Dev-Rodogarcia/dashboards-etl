package com.dashboard.api.dto.indicadoresgestao;

public record UtilizacaoColetoresOverviewDTO(
        String updatedAt,
        int ordensConferencia,
        int manifestosEmitidos,
        int manifestosDescarregamento,
        int totalManifestos,
        double pctUtilizacao
) {
}
