package com.dashboard.api.dto.esl;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados internos derivados da sessão e da configuração de filial. Não é um
 * contrato HTTP para o React.
 */
public record EslContextoOperacionalDTO(
        @NotBlank String documentoCorporacao,
        Long idCorporacao,
        @NotBlank String nomeSolicitante,
        @NotBlank @Email String emailSolicitante,
        @Size(max = 40) String telefoneSolicitante,
        @Size(max = 160) String departamento
) {
}
