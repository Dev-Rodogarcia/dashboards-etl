package com.dashboard.api.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

public final class RespostaErroHttpWriter {

    private RespostaErroHttpWriter() {
    }

    public static void escrever(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            HttpStatus status,
            String erro,
            String mensagem
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(response.getWriter(), new RespostaErroPadrao(
                LocalDateTime.now(),
                status.value(),
                erro,
                mensagem
        ));
    }
}
