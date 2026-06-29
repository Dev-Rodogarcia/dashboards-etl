package com.dashboard.api.dto.integracoes;

import java.time.LocalDate;

public record IntegracaoEvolucaoDiariaDTO(
        LocalDate data,
        Integer total,
        Integer sucessos,
        Integer erros
) {
}
