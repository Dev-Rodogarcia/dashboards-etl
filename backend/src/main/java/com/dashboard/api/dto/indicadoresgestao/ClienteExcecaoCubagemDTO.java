package com.dashboard.api.dto.indicadoresgestao;

import java.time.OffsetDateTime;

public record ClienteExcecaoCubagemDTO(
        String clienteCnpj,
        String razaoSocial,
        String nomeFantasia,
        String cidadeUf,
        String atualizadoPor,
        OffsetDateTime dataAtualizacao
) {
}
