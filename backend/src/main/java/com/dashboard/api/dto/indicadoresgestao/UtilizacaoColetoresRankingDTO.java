package com.dashboard.api.dto.indicadoresgestao;

import java.math.BigDecimal;

public record UtilizacaoColetoresRankingDTO(
        String branchId,
        String branchName,
        double utilization,
        BigDecimal goal,
        int ordensConferencia,
        int manifestosBipaveis,
        int descarregamentos,
        int ordensIncompletas
) {
}
