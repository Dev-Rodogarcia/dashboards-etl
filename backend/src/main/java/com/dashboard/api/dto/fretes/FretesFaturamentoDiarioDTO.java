package com.dashboard.api.dto.fretes;

import java.math.BigDecimal;

public record FretesFaturamentoDiarioDTO(
        int totalDiasUteisMes,
        int diasUteisDecorridos,
        int diasUteisRestantes,
        BigDecimal metaDiariaBase,
        BigDecimal faturamentoDiarioReal,
        BigDecimal metaDiariaDinamica,
        BigDecimal faturamentoFaltante,
        BigDecimal tendenciaFaturamento,
        BigDecimal tendenciaPercentual
) {
}
