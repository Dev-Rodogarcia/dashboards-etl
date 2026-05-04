package com.dashboard.api.dto.manifestos;

import java.util.List;

public record ManifestosChartsDTO(
        List<ManifestosCustoPorFilialDTO> custoPorFilial,
        List<ManifestosRankingMotoristaDTO> rankingMotorista,
        List<ManifestosComposicaoCustoDTO> composicaoCusto,
        List<ManifestosOcupacaoScatterDTO> ocupacaoScatter
) {
}
