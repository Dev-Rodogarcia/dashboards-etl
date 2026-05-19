package com.dashboard.api.dto.fretes;

import java.util.List;

public record FretesChartsDTO(
        List<FretesPrevisaoPorStatusDTO> previsaoPorStatus,
        List<FretesOrigemDestinoDTO> topRotasPorReceita,
        List<FretesFaturamentoGrupoDTO> faturamentoPorClassificacao,
        List<FretesFaturamentoGrupoDTO> faturamentoPorResponsavelDestino,
        List<FretesFaturamentoGrupoDTO> faturamentoPorUfOrigem,
        List<FretesFaturamentoGrupoDTO> faturamentoPorUfDestino,
        List<FretesFaturamentoGrupoDTO> faturamentoPorCidadeDestino
) {
}
