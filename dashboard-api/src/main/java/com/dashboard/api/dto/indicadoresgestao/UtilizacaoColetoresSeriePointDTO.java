package com.dashboard.api.dto.indicadoresgestao;

public record UtilizacaoColetoresSeriePointDTO(
        String date,
        String filial,
        int ordensConferencia,
        int manifestosEmitidos,
        int manifestosDescarregamento,
        int totalManifestos,
        double pctUtilizacao
) {
}
