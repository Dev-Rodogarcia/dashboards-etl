package com.dashboard.api.client.esl;

import com.dashboard.api.exception.EslConflitoEstadoException;
import com.dashboard.api.exception.EslGraphqlOperacaoRecusadaException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class EslGraphqlPayloadNormalizer {

    public JsonNode extrairRecursoMutation(JsonNode resposta, String operacao) {
        validarEnvelope(resposta, operacao);

        JsonNode payloadOperacao = resposta.path("data").path(operacao);
        if (payloadOperacao.isMissingNode() || payloadOperacao.isNull()) {
            recusar(operacao, List.of("O ESL não retornou o resultado da operação."));
        }

        List<String> erros = extrairMensagens(payloadOperacao.path("errors"));
        if (!erros.isEmpty()) {
            recusar(operacao, erros);
        }

        if (!payloadOperacao.path("success").isBoolean() || !payloadOperacao.path("success").asBoolean()) {
            recusar(operacao, List.of("O ESL não confirmou a operação solicitada."));
        }

        JsonNode resource = payloadOperacao.path("resource");
        if (resource.isMissingNode() || resource.isNull()) {
            recusar(operacao, List.of("O ESL confirmou a operação sem retornar o recurso esperado."));
        }
        return resource;
    }

    public JsonNode extrairResultadoQuery(JsonNode resposta, String operacao) {
        validarEnvelope(resposta, operacao);

        JsonNode resultado = resposta.path("data").path(operacao);
        if (resultado.isMissingNode() || resultado.isNull()) {
            recusar(operacao, List.of("O ESL não retornou o resultado da consulta."));
        }
        return resultado;
    }

    private void validarEnvelope(JsonNode resposta, String operacao) {
        if (resposta == null || resposta.isNull()) {
            recusar(operacao, List.of("O ESL retornou uma resposta vazia."));
        }

        List<String> erros = extrairMensagens(resposta.path("errors"));
        if (!erros.isEmpty()) {
            recusar(operacao, erros);
        }
    }

    private List<String> extrairMensagens(JsonNode erros) {
        if (!erros.isArray()) {
            return List.of();
        }

        Set<String> mensagens = new LinkedHashSet<>();
        for (JsonNode erro : erros) {
            String mensagem = erro.isTextual()
                    ? erro.asText()
                    : erro.path("message").asText("");
            String normalizada = normalizarMensagem(mensagem);
            if (!normalizada.isBlank()) {
                mensagens.add(normalizada);
            }
        }
        return new ArrayList<>(mensagens);
    }

    private String normalizarMensagem(String mensagem) {
        if (mensagem == null) {
            return "";
        }
        String semControles = mensagem.replaceAll("[\\r\\n\\t]+", " ").trim();
        return semControles.length() <= 300 ? semControles : semControles.substring(0, 300);
    }

    private void recusar(String operacao, List<String> mensagens) {
        if (ehConflitoDeEstado(operacao, mensagens)) {
            throw new EslConflitoEstadoException(String.join(" ", mensagens));
        }
        throw new EslGraphqlOperacaoRecusadaException(operacao, mensagens);
    }

    private boolean ehConflitoDeEstado(String operacao, List<String> mensagens) {
        boolean operacaoDeColeta = "pickUpdate".equals(operacao) || "pickCancellation".equals(operacao);
        if (!operacaoDeColeta) {
            return false;
        }
        String texto = String.join(" ", mensagens).toLowerCase(java.util.Locale.ROOT);
        return texto.contains("already")
                || texto.contains("já ")
                || texto.contains("ja ")
                || texto.contains("cancelad")
                || texto.contains("canceled")
                || texto.contains("cannot update")
                || texto.contains("não pode atualizar")
                || texto.contains("nao pode atualizar");
    }
}
