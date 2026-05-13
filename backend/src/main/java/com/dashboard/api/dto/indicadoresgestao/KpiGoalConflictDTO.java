package com.dashboard.api.dto.indicadoresgestao;

import java.util.List;

public record KpiGoalConflictDTO(
        String mensagem,
        List<KpiGoalBranchDTO> branches
) {
}
