package com.dashboard.api.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.UncheckedIOException;
import java.sql.SQLException;
import java.time.LocalDateTime;

@RestControllerAdvice
public class ManipuladorGlobalExcecoes {

    private static final Logger log = LoggerFactory.getLogger(ManipuladorGlobalExcecoes.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RespostaErroPadrao> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Requisição inválida: {}", ex.getMessage());

        RespostaErroPadrao resposta = new RespostaErroPadrao(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resposta);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<RespostaErroPadrao> handleIllegalState(IllegalStateException ex) {
        log.warn("Conflito de regra de negócio: {}", ex.getMessage());

        RespostaErroPadrao resposta = new RespostaErroPadrao(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(resposta);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RespostaErroPadrao> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Acesso negado: {}", ex.getMessage());

        RespostaErroPadrao resposta = new RespostaErroPadrao(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                "Acesso negado ao recurso."
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(resposta);
    }

    @ExceptionHandler({
        jakarta.persistence.QueryTimeoutException.class,
        org.springframework.dao.QueryTimeoutException.class
    })
    public ResponseEntity<RespostaErroPadrao> handleQueryTimeout(Exception ex) {
        log.warn("Timeout na consulta ao banco de dados: {}", ex.getMessage());

        RespostaErroPadrao resposta = new RespostaErroPadrao(
                LocalDateTime.now(),
                HttpStatus.REQUEST_TIMEOUT.value(),
                "Request Timeout",
                "A consulta excedeu o tempo limite. Reduza o período ou os filtros e tente novamente."
        );

        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(resposta);
    }

    @ExceptionHandler({SQLException.class, DataAccessException.class})
    public ResponseEntity<RespostaErroPadrao> handleDatabaseFailure(Exception ex) {
        log.error("Falha no acesso ao banco de dados: {}", ex.getMessage(), ex);

        RespostaErroPadrao resposta = new RespostaErroPadrao(
                LocalDateTime.now(),
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Service Unavailable",
                "Serviço de dados temporariamente indisponível."
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(resposta);
    }

    @ExceptionHandler(UncheckedIOException.class)
    public ResponseEntity<RespostaErroPadrao> handleUncheckedIo(UncheckedIOException ex) {
        log.error("Falha de I/O não tratada (possível arquivo de configuração corrompido): {}", ex.getMessage(), ex);

        RespostaErroPadrao resposta = new RespostaErroPadrao(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Erro interno no servidor."
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resposta);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespostaErroPadrao> handleGeneric(Exception ex) {
        log.error("Erro interno não tratado: {}", ex.getMessage(), ex);

        RespostaErroPadrao resposta = new RespostaErroPadrao(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Erro interno no servidor."
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resposta);
    }
}
