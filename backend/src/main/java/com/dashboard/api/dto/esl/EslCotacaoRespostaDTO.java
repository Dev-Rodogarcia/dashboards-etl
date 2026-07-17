package com.dashboard.api.dto.esl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EslCotacaoRespostaDTO(
        String cotacaoId,
        Long numeroCotacao,
        String referencia,
        LocalDate validade,
        String urlImpressao,
        Integer trechosPendentes,
        List<EslCotacaoTrechoRespostaDTO> trechos,
        BigDecimal valorFreteTotal
) {
}
