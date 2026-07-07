package com.dashboard.api.dto.indicadoresgestao;

import java.util.List;

public record CubagemClientesImportacaoResultadoDTO(
        int totalProcessados,
        int totalImportados,
        int totalErros,
        List<CubagemClientesImportacaoImportadoDTO> listaImportados,
        List<CubagemClientesImportacaoErroDTO> listaErros
) {
}
