package com.dashboard.api.dto.manifestos;

public record ManifestosMetasImportacaoTotaisDTO(
        int totalLinhas,
        int validas,
        int invalidas
) {
}
