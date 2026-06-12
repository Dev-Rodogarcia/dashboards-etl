package com.dashboard.api.dto.manifestos;

import java.math.BigDecimal;
import java.util.List;

public record ManifestosCustosEvolucaoDTO(
        boolean orcamentoAplicavel,
        boolean orcamentoConfigurado,
        String observacao,
        int totalDiasUteis,
        int diasUteisDecorridos,
        int diasUteisRestantes,
        BigDecimal orcamentoCusto,
        BigDecimal custoReal,
        BigDecimal limiteDiarioBase,
        BigDecimal custoMedioDiarioReal,
        BigDecimal saldoOrcamentario,
        BigDecimal limiteDiarioDinamico,
        BigDecimal tendenciaCusto,
        BigDecimal consumoOrcamento,
        List<CustoDiarioDTO> serieDiaria
) {
    public record CustoDiarioDTO(
            String data,
            BigDecimal custoReal
    ) {
    }
}
