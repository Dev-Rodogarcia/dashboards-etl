package com.dashboard.api.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class ManipuladorGlobalExcecoesTest {

    @Test
    void deveRetornar405ParaMetodoHttpNaoSuportado() {
        ManipuladorGlobalExcecoes handler = new ManipuladorGlobalExcecoes();

        var resposta = handler.handleMethodNotSupported(
                new HttpRequestMethodNotSupportedException("GET", List.of("POST"))
        );

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        RespostaErroPadrao body = Objects.requireNonNull(resposta.getBody());
        assertThat(body.status()).isEqualTo(405);
        assertThat(body.erro()).isEqualTo("Method Not Allowed");
    }

    @Test
    void deveRetornar409ParaViolacaoDeIntegridadeSemMascararComo503() {
        ManipuladorGlobalExcecoes handler = new ManipuladorGlobalExcecoes();

        var resposta = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("Violação da restrição UNIQUE KEY")
        );

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        RespostaErroPadrao body = Objects.requireNonNull(resposta.getBody());
        assertThat(body.status()).isEqualTo(409);
        assertThat(body.erro()).isEqualTo("Conflict");
    }

    @Test
    void deveRetornar400ParaParametroObrigatorioAusente() {
        ManipuladorGlobalExcecoes handler = new ManipuladorGlobalExcecoes();

        var resposta = handler.handleMissingServletRequestParameter(
                new MissingServletRequestParameterException("dataInicio", "LocalDate")
        );

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        RespostaErroPadrao body = Objects.requireNonNull(resposta.getBody());
        assertThat(body.status()).isEqualTo(400);
        assertThat(body.erro()).isEqualTo("Bad Request");
        assertThat(body.mensagem()).isEqualTo("Parâmetro obrigatório ausente: dataInicio.");
    }
}
