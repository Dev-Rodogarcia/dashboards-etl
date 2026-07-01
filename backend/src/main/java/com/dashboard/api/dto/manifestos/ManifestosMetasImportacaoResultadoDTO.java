package com.dashboard.api.dto.manifestos;

import java.util.List;

public record ManifestosMetasImportacaoResultadoDTO(
        int totalProcessados,
        int totalImportados,
        int totalErros,
        List<ManifestosMetasImportacaoImportadoDTO> listaImportados,
        List<ManifestosMetasImportacaoErroDTO> listaErros
) {
}
