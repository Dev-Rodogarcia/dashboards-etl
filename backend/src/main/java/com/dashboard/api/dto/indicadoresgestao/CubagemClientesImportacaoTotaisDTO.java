package com.dashboard.api.dto.indicadoresgestao;

public record CubagemClientesImportacaoTotaisDTO(
        int totalLinhas,
        int validas,
        int invalidas
) {
}
