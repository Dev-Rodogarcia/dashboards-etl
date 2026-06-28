package com.dashboard.api.exception;

import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;
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
    void deveRetornar504ParaTimeoutDeConsulta() {
        ManipuladorGlobalExcecoes handler = new ManipuladorGlobalExcecoes();

        var resposta = handler.handleQueryTimeout(new QueryTimeoutException("timeout na view"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        RespostaErroPadrao body = Objects.requireNonNull(resposta.getBody());
        assertThat(body.status()).isEqualTo(504);
        assertThat(body.erro()).isEqualTo("Gateway Timeout");
    }

    @Test
    void deveRetornar503ParaFalhaDeServicoHttpExterno() {
        ManipuladorGlobalExcecoes handler = new ManipuladorGlobalExcecoes();

        var resposta = handler.handleExternalHttpFailure(
                new ResourceAccessException("Connection refused")
        );

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        RespostaErroPadrao body = Objects.requireNonNull(resposta.getBody());
        assertThat(body.status()).isEqualTo(503);
        assertThat(body.erro()).isEqualTo("Service Unavailable");
        assertThat(body.mensagem()).isEqualTo("Serviço de integração temporariamente indisponível.");
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

    @Test
    void deveRetornar400ParaCorpoInvalidoPorValidacao() throws Exception {
        ManipuladorGlobalExcecoes handler = new ManipuladorGlobalExcecoes();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "loginRequest");
        bindingResult.addError(new FieldError("loginRequest", "email", "não deve estar em branco"));
        MethodParameter parameter = new MethodParameter(
                ManipuladorGlobalExcecoesTest.class.getDeclaredMethod("endpointComValidacao", Object.class),
                0
        );

        var resposta = handler.handleMethodArgumentNotValid(
                new MethodArgumentNotValidException(parameter, bindingResult)
        );

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        RespostaErroPadrao body = Objects.requireNonNull(resposta.getBody());
        assertThat(body.status()).isEqualTo(400);
        assertThat(body.erro()).isEqualTo("Bad Request");
        assertThat(body.mensagem()).isEqualTo("Dados inválidos na requisição.");
    }

    @Test
    void devePreservarStatusDeResponseStatusException() {
        ManipuladorGlobalExcecoes handler = new ManipuladorGlobalExcecoes();

        var resposta = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filial Atual é obrigatória.")
        );

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        RespostaErroPadrao body = Objects.requireNonNull(resposta.getBody());
        assertThat(body.status()).isEqualTo(400);
        assertThat(body.erro()).isEqualTo("Bad Request");
        assertThat(body.mensagem()).isEqualTo("Filial Atual é obrigatória.");
    }

    @SuppressWarnings("unused")
    private void endpointComValidacao(Object request) {
    }
}
