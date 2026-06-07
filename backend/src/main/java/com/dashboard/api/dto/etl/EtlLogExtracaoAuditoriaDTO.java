package com.dashboard.api.dto.etl;

import java.time.LocalDateTime;

public record EtlLogExtracaoAuditoriaDTO(
        Long id,
        String entidade,
        LocalDateTime timestampInicio,
        LocalDateTime timestampFim,
        String statusFinal,
        Integer registrosExtraidos,
        Integer paginasProcessadas,
        Integer noopCount,
        String mensagem
) {}
