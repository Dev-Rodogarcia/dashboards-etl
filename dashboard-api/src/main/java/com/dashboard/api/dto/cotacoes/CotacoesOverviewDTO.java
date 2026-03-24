package com.dashboard.api.dto.cotacoes;

import java.math.BigDecimal;

public record CotacoesOverviewDTO(
    String updatedAt,
    int totalCotacoes,
    BigDecimal valorPotencial,
    BigDecimal freteMedio,
    BigDecimal freteKgMedio,
    double taxaConversaoCte,
    double taxaConversaoNfse,
    double taxaReprovacao,
    double tempoMedioConversaoHoras
) {}
