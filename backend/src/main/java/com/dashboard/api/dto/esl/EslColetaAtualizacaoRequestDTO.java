package com.dashboard.api.dto.esl;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record EslColetaAtualizacaoRequestDTO(
        LocalDate dataSolicitacao,
        LocalTime horaSolicitacao,
        LocalDate dataAgendada,
        LocalTime horaInicial,
        LocalTime horaFinal,
        @Email(message = "O e-mail de notificação é inválido")
        @Size(max = 160, message = "O e-mail de notificação deve ter no máximo 160 caracteres")
        String emailNotificacao,
        @Size(max = 40, message = "O telefone de notificação deve ter no máximo 40 caracteres")
        String telefoneNotificacao,
        @Size(max = 2_000, message = "As observações devem ter no máximo 2000 caracteres")
        String observacoes
) {
}
