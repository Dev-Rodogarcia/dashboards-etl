package com.dashboard.api.dto.esl;

import java.time.LocalDate;

public record EslColetaListagemItemDTO(
        String coletaId,
        Long numeroColeta,
        String status,
        LocalDate dataSolicitacao,
        String horaSolicitacao,
        LocalDate dataAgendada,
        String horaInicial,
        String horaFinal,
        String referencia,
        String motivoCancelamento,
        String observacoes
) {
}
