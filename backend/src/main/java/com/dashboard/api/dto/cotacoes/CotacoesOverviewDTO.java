package com.dashboard.api.dto.cotacoes;

import java.math.BigDecimal;

public record CotacoesOverviewDTO(
    String updatedAt,
    int totalCotacoes,
    BigDecimal valorPotencial,
    BigDecimal valorConvertido,
    BigDecimal freteMedio,
    BigDecimal freteKgMedio,
    double conversaoValor,
    double conversaoQuantidade,
    double reprovacaoPercentual,
    double taxaConversaoCte,
    double taxaConversaoNfse,
    double tempoMedioConversaoHoras
) {}
