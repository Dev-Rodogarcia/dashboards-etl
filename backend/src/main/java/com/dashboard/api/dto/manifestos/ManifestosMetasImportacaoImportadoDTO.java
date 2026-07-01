package com.dashboard.api.dto.manifestos;

import java.math.BigDecimal;

public record ManifestosMetasImportacaoImportadoDTO(
        int linha,
        int ano,
        int mes,
        String branchId,
        String contractTypeKey,
        String classificationKey,
        BigDecimal costGoal
) {
}
