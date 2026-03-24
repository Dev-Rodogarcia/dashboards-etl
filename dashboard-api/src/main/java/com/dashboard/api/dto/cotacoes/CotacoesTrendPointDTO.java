package com.dashboard.api.dto.cotacoes;

public record CotacoesTrendPointDTO(
        String date,
        int cotacoes,
        int convertidas,
        int reprovadas
) {}
