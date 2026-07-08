package com.dashboard.api.dto.etl;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record EtlTabelaAuditoriaResumoDTO(
        String tabelaAlvo,
        long qtdExtracoes,
        long qtdSucessos,
        long qtdFalhas,
        long totalRegistrosGravados,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime primeiraExtracao,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime ultimaExtracao,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime menorDataNegocio,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime maiorDataNegocio
) {}
