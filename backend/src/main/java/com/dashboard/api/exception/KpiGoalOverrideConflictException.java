package com.dashboard.api.exception;

import com.dashboard.api.dto.indicadoresgestao.KpiGoalBranchDTO;

import java.util.List;

public class KpiGoalOverrideConflictException extends RuntimeException {

    private final List<KpiGoalBranchDTO> branches;

    public KpiGoalOverrideConflictException(List<KpiGoalBranchDTO> branches) {
        super("Existem filiais com metas específicas. Remova todas as metas isoladas por filial antes de alterar a meta global.");
        this.branches = List.copyOf(branches);
    }

    public List<KpiGoalBranchDTO> getBranches() {
        return branches;
    }
}
