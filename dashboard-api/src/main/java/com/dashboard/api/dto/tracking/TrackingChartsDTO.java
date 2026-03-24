package com.dashboard.api.dto.tracking;

import java.util.List;

public record TrackingChartsDTO(
        List<TrackingStatusDistribuicaoDTO> statusDistribuicao,
        List<TrackingPrevisaoVencidaFilialDTO> previsaoVencidaPorFilialAtual,
        List<TrackingValorPorRegiaoDTO> valorPorRegiaoDestino
) {
}
