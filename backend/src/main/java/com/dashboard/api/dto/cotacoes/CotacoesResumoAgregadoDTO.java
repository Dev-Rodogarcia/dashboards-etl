package com.dashboard.api.dto.cotacoes;

import java.math.BigDecimal;

public record CotacoesResumoAgregadoDTO(
        String id,
        String entidade,
        int totalCotacoes,
        int ganhas,
        int emAberto,
        double taxaConversao,
        BigDecimal freteCotado,
        BigDecimal freteGanho,
        BigDecimal volumeM3
) {
}
