package com.dashboard.api.dto.etl;

import java.util.List;

public record EtlSaudeChartsDTO(
        List<EtlCategoriaErroDTO> categoriasErro
) {
}
