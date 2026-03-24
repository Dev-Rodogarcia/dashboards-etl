package com.dashboard.api.dto.etl;

public record EtlExecucaoResumoDTO(
        Long id,
        String inicio,
        String fim,
        Integer duracaoSegundos,
        String data,
        String status,
        Integer totalRegistros,
        String categoriaErro,
        String mensagemErro
) {}
