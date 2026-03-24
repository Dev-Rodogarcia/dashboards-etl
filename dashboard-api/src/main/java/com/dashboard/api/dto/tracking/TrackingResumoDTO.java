package com.dashboard.api.dto.tracking;

import java.math.BigDecimal;

public record TrackingResumoDTO(
        Long numeroMinuta,
        String dataFrete,
        String tipo,
        Integer volumes,
        BigDecimal pesoTaxado,
        BigDecimal valorNf,
        BigDecimal valorFrete,
        String filialEmissora,
        String filialOrigem,
        String filialAtual,
        String filialDestino,
        String regiaoOrigem,
        String regiaoDestino,
        String classificacao,
        String statusCarga,
        String previsaoEntrega
) {}
