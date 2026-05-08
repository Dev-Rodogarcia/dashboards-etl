package com.dashboard.api.dto.home;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record HomeComunicadoRequestDTO(
        @NotBlank(message = "O titulo do comunicado e obrigatorio")
        @Size(max = 140, message = "O titulo deve ter no maximo 140 caracteres")
        String titulo,

        @NotBlank(message = "O texto do comunicado e obrigatorio")
        @Size(max = 700, message = "O texto deve ter no maximo 700 caracteres")
        String corpo,

        @NotBlank(message = "A tag do comunicado e obrigatoria")
        @Pattern(regexp = "NOVO|ATENCAO|FIXADO", message = "Tag de comunicado invalida")
        String tag,

        @NotBlank(message = "O publico alvo e obrigatorio")
        @Size(max = 140, message = "O publico alvo deve ter no maximo 140 caracteres")
        String publicoAlvo
) {
}
