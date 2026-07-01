package com.dashboard.api.dto.manifestos;

import java.util.List;

public record ManifestosMetasImportacaoPreviewResponseDTO(
        String arquivo,
        ManifestosMetasImportacaoTotaisDTO totais,
        boolean podeImportar,
        List<ManifestosMetasImportacaoPreviewLinhaDTO> linhasPreview
) {
}
