package com.dashboard.api.dto.esl;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record EslColetaCriacaoRequestDTO(
        @NotBlank(message = "O documento do cliente é obrigatório")
        @Size(max = 20, message = "O documento do cliente deve ter no máximo 20 caracteres")
        String documentoCliente,
        @Size(max = 120, message = "A referência deve ter no máximo 120 caracteres")
        String referencia,
        @NotBlank(message = "O documento do local de coleta é obrigatório")
        @Size(max = 20, message = "O documento do local de coleta deve ter no máximo 20 caracteres")
        String documentoLocalColeta,
        @NotNull(message = "A data agendada é obrigatória")
        LocalDate dataAgendada,
        @NotNull(message = "A hora inicial é obrigatória")
        LocalTime horaInicial,
        @NotNull(message = "A hora final é obrigatória")
        LocalTime horaFinal,
        @Email(message = "O e-mail de notificação é inválido")
        @Size(max = 160, message = "O e-mail de notificação deve ter no máximo 160 caracteres")
        String emailNotificacao,
        @Size(max = 40, message = "O telefone de notificação deve ter no máximo 40 caracteres")
        String telefoneNotificacao,
        @Size(max = 2_000, message = "As observações devem ter no máximo 2000 caracteres")
        String observacoes,
        @NotEmpty(message = "Informe ao menos um item para a coleta")
        List<@Valid EslColetaItemRequestDTO> itens
) {
}
