package com.dashboard.api.dto.indicadoresgestao;

import java.util.List;

public record CubagemClientesImportacaoPreviewResponseDTO(
        String nomeArquivo,
        CubagemClientesImportacaoTotaisDTO totais,
        boolean podeImportar,
        List<CubagemClientesImportacaoPreviewLinhaDTO> linhasPreview
) {
}
