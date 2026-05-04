package com.dashboard.api.dto.manifestos;

import java.math.BigDecimal;

public record ManifestoResumoDTO(
        Long numero,
        String identificadorUnico,
        String status,
        String classificacao,
        String filial,
        String dataCriacao,
        String fechamento,
        String motorista,
        String veiculoPlaca,
        String tipoVeiculo,
        BigDecimal totalPesoTaxado,
        BigDecimal totalM3,
        BigDecimal custoTotal,
        BigDecimal valorFrete,
        BigDecimal combustivel,
        BigDecimal pedagio,
        BigDecimal saldoPagar,
        BigDecimal kmTotal,
        Integer itensTotal
) {}
