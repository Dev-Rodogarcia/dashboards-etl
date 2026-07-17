package com.dashboard.api.dto.esl;

import java.util.List;

public record EslColetaListagemRespostaDTO(
        List<EslColetaListagemItemDTO> itens,
        boolean temProximaPagina,
        String proximoCursor
) {
}
