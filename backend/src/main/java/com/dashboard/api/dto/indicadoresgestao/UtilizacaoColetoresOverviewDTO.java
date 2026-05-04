package com.dashboard.api.dto.indicadoresgestao;

public record UtilizacaoColetoresOverviewDTO(
        String updatedAt,
        int manifestosBipados,
        int manifestosEmitidos,
        int manifestosDescarregamento,
        int totalManifestos,
        int manifestosIncompletos,
        double pctUtilizacao
) {
}
