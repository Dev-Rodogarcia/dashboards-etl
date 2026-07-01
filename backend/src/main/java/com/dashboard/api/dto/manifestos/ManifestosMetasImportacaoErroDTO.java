package com.dashboard.api.dto.manifestos;

public record ManifestosMetasImportacaoErroDTO(
        int linha,
        String motivo,
        String tipoErro
) {
}
