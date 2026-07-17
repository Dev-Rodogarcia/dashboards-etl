package com.dashboard.api.dto.esl;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record EslColetaListagemRequestDTO(
        @NotNull(message = "A data de solicitação é obrigatória")
        LocalDate dataSolicitacao,
        @Size(max = 500, message = "O cursor deve ter no máximo 500 caracteres")
        String cursor,
        @Min(value = 1, message = "O tamanho da página deve ser maior que zero")
        @Max(value = 100, message = "O tamanho da página deve ser de no máximo 100")
        Integer tamanhoPagina
) {
    public int tamanhoPaginaNormalizado() {
        return tamanhoPagina == null ? 50 : tamanhoPagina;
    }
}
