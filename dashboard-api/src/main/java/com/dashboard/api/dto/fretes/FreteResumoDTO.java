package com.dashboard.api.dto.fretes;

import java.math.BigDecimal;

public record FreteResumoDTO(
        Long id,
        String dataFrete,
        String status,
        String filial,
        String pagador,
        String remetente,
        String destinatario,
        String origemUf,
        String destinoUf,
        BigDecimal valorTotalServico,
        BigDecimal valorFrete,
        BigDecimal pesoTaxado,
        Integer volumes,
        String previsaoEntrega,
        String documentoTipo,
        Integer numeroCte,
        Integer numeroNfse,
        BigDecimal valorIcms,
        BigDecimal valorPis,
        BigDecimal valorCofins
) {}
