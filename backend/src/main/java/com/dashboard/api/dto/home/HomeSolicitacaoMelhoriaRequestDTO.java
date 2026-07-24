package com.dashboard.api.dto.home;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record HomeSolicitacaoMelhoriaRequestDTO(
        @NotBlank(message = "O tipo da solicitacao e obrigatorio")
        @Pattern(regexp = "MELHORIA|AUTOMACAO|DASHBOARD|CORRECAO|OUTRO", message = "Tipo de solicitacao invalido")
        String tipo,

        @NotBlank(message = "O titulo da solicitacao e obrigatorio")
        @Size(max = 140, message = "O titulo deve ter no maximo 140 caracteres")
        String titulo,

        @NotBlank(message = "Descreva a necessidade")
        @Size(max = 2000, message = "A descricao deve ter no maximo 2000 caracteres")
        String descricao,

        @Size(max = 1000, message = "O resultado esperado deve ter no maximo 1000 caracteres")
        String resultadoEsperado
) {
}
