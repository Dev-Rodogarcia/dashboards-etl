package com.dashboard.api.dto.manifestos;

import java.math.BigDecimal;
import java.util.List;

public record ManifestosMetasImportacaoPreviewLinhaDTO(
        int linha,
        Integer ano,
        Integer mes,
        String branchId,
        String contractType,
        String contractTypeKey,
        String classificationKey,
        BigDecimal costGoal,
        String status,
        List<String> mensagens
) {
}
