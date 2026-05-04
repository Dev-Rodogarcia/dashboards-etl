package com.dashboard.api.dto.cotacoes;

import java.util.List;

public record CotacoesChartsDTO(
        List<CotacoesFunilDTO> funil,
        List<CotacoesCorredorValiosoDTO> corredoresMaisValiosos,
        List<CotacoesMotivoPerdaDTO> motivosPerda
) {
}
