package com.dashboard.api.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RespostaErroPadrao(
    LocalDateTime timestamp,
    int status,
    String erro,
    String mensagem,
    String codigo
) {
    public RespostaErroPadrao(LocalDateTime timestamp, int status, String erro, String mensagem) {
        this(timestamp, status, erro, mensagem, null);
    }
}
