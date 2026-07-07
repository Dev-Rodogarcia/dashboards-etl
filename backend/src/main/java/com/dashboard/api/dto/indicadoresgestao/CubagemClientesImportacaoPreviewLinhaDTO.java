package com.dashboard.api.dto.indicadoresgestao;

import java.util.List;

public record CubagemClientesImportacaoPreviewLinhaDTO(
        int linha,
        String clienteCnpj,
        String razaoSocial,
        String nomeFantasia,
        String cidadeUf,
        String status,
        List<String> mensagens
) {
}
