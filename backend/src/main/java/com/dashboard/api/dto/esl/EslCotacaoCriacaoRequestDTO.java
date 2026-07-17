package com.dashboard.api.dto.esl;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record EslCotacaoCriacaoRequestDTO(
        @NotBlank(message = "O documento do cliente é obrigatório")
        @Size(max = 20, message = "O documento do cliente deve ter no máximo 20 caracteres")
        String documentoCliente,
        @NotNull(message = "A validade da cotação é obrigatória")
        LocalDate validade,
        @Size(max = 120, message = "A referência deve ter no máximo 120 caracteres")
        String referencia,
        @Size(max = 2_000, message = "As observações devem ter no máximo 2000 caracteres")
        String observacoes,
        @NotEmpty(message = "Informe ao menos um trecho para a cotação")
        List<@Valid EslCotacaoTrechoRequestDTO> trechos
) {
}
