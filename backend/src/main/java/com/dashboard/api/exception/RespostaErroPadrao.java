package com.dashboard.api.exception;

import java.time.LocalDateTime;

public record RespostaErroPadrao(
    LocalDateTime timestamp,
    int status,
    String erro,
    String mensagem
) {}
