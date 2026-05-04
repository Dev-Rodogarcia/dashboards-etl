package com.dashboard.api.dto.fretes;

import java.util.List;

public record FretesChartsDTO(
        List<FretesPrevisaoPorStatusDTO> previsaoPorStatus,
        List<FretesOrigemDestinoDTO> topRotasPorReceita
) {
}
