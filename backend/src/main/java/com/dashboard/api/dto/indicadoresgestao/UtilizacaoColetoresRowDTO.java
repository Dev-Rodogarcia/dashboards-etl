package com.dashboard.api.dto.indicadoresgestao;

public record UtilizacaoColetoresRowDTO(
        String chave,
        String date,
        String filial,
        String classificacao,
        int manifestosBipados,
        int manifestosEmitidos,
        int manifestosDescarregamento,
        int totalManifestos,
        int manifestosIncompletos,
        double pctUtilizacao
) {
}
