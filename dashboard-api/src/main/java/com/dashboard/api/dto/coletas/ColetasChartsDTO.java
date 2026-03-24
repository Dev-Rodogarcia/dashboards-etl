package com.dashboard.api.dto.coletas;

import java.util.List;

public record ColetasChartsDTO(
        List<ColetasStatusDistribuicaoDTO> statusDistribuicao,
        List<ColetasSlaPorFilialDTO> slaPorFilial,
        List<ColetasRegiaoVolumeDTO> regiaoVolume,
        List<ColetasAgingBucketDTO> agingAbertas
) {
}
