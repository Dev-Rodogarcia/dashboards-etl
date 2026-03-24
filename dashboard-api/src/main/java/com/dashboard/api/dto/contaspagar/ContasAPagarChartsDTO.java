package com.dashboard.api.dto.contaspagar;

import java.util.List;

public record ContasAPagarChartsDTO(
        List<ContasAPagarFornecedorDTO> topFornecedores,
        List<ContasAPagarCentroCustoDTO> centroCusto,
        List<ContasAPagarConciliacaoDTO> conciliacao
) {
}
