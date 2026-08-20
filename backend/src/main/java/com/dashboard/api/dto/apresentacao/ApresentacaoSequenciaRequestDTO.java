package com.dashboard.api.dto.apresentacao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ApresentacaoSequenciaRequestDTO(
        @NotBlank @Size(max = 80) String nome,
        @NotEmpty @Size(max = 12) List<@NotBlank @Size(max = 60) String> paginas
) { }
