package com.dashboard.api.dto.etl;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record EtlLogExtracaoAuditoriaDTO(
        Long id,
        String entidade,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestampInicio,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestampFim,
        String statusFinal,
        Integer registrosExtraidos,
        Integer paginasProcessadas,
        Integer noopCount,
        String mensagem
) {}
