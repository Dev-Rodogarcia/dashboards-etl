package com.dashboard.api.dto.coletas;

import java.util.List;

public record ColetasChartsDTO(
        List<ColetasStatusDistribuicaoDTO> statusDistribuicao,
        List<ColetasSlaPorFilialDTO> slaPorFilial,
        List<ColetasRegiaoOrigemDTO> regioesOrigem,
        List<ColetasAgingBucketDTO> agingAbertas
) {
}
