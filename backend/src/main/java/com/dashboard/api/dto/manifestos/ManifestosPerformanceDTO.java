package com.dashboard.api.dto.manifestos;

import java.math.BigDecimal;
import java.util.List;

public record ManifestosPerformanceDTO(
        String updatedAt,
        int totalDiasUteis,
        KpisManifestosDTO kpis,
        GaugeMetricDTO remuneracao,
        GaugeMetricDTO aproveitamento,
        GaugeMetricDTO efetividade,
        List<StatusSazonalDTO> statusSazonal,
        List<CustoContratoDTO> custosContrato,
        List<TipoVeiculoDTO> tiposVeiculo,
        ManifestosCustosEvolucaoDTO custosEvolucao
) {

    public record KpisManifestosDTO(
            long totalManifestos,
            long emTransito,
            long pendentes,
            long encerrados,
            BigDecimal kmTotal,
            BigDecimal custoTotal,
            BigDecimal custoPorKg,
            BigDecimal custoPorKm,
            BigDecimal receitaPorKg,
            BigDecimal receitaPorKm
    ) {
    }

    public record GaugeMetricDTO(
            BigDecimal global,
            BigDecimal distribuicao,
            BigDecimal transferencia,
            BigDecimal cargaFechada
    ) {
    }

    public record StatusSazonalDTO(
            String data,
            long encerrado,
            long emTransito,
            long pendente
    ) {
    }

    public record CustoContratoDTO(
            String tipoContrato,
            BigDecimal custoTotal
    ) {
    }

    public record TipoVeiculoDTO(
            String tipo,
            long quantidade,
            Double aproveitamentoMedio,
            Double mediaEventos
    ) {
    }
}
