package com.dashboard.api.dto.indicadoresgestao;

public record CubagemClientesImportacaoErroDTO(
        int linha,
        String motivo,
        String tipoErro
) {
}
