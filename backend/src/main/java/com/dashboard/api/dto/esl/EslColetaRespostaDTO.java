package com.dashboard.api.dto.esl;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EslColetaRespostaDTO(
        String coletaId,
        Long numeroColeta,
        String status,
        LocalDate dataSolicitacao,
        String horaSolicitacao,
        LocalDate dataAgendada,
        String horaInicial,
        String horaFinal,
        String referencia,
        String observacoes,
        String motivoCancelamento,
        BigDecimal valorNotasFiscais,
        Integer quantidadeVolumes,
        BigDecimal pesoNotasFiscais,
        BigDecimal pesoTaxado
) {
}
