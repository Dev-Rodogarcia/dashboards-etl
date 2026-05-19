package com.dashboard.api.dto.cotacoes;

import java.util.List;

public record CotacoesChartsDTO(
        List<CotacoesFunilDTO> funil,
        List<CotacoesCorredorValiosoDTO> corredoresMaisValiosos,
        List<CotacoesMotivoPerdaDTO> motivosPerda,
        List<CotacoesAgrupamentoDTO> trechosMaisValiosos,
        List<CotacoesAgrupamentoDTO> trechosPorUfOrigem,
        List<CotacoesAgrupamentoDTO> trechosPorUfDestino,
        List<CotacoesAgrupamentoDTO> conversaoPorTipoOperacao,
        List<CotacoesMotivoPerdaDTO> perdasPorCliente,
        List<CotacoesMotivoPerdaDTO> perdasPorTrecho
) {
}
