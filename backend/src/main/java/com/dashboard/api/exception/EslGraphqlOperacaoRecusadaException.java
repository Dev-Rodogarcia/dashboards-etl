package com.dashboard.api.exception;

import java.util.List;

public class EslGraphqlOperacaoRecusadaException extends RuntimeException {

    private final List<String> mensagens;

    public EslGraphqlOperacaoRecusadaException(String operacao, List<String> mensagens) {
        super(mensagemPadrao(operacao, mensagens));
        this.mensagens = List.copyOf(mensagens);
    }

    public List<String> mensagens() {
        return mensagens;
    }

    private static String mensagemPadrao(String operacao, List<String> mensagens) {
        String detalhe = mensagens.isEmpty()
                ? "O ESL recusou a operação solicitada."
                : String.join(" ", mensagens);
        return "Operação ESL " + operacao + " recusada: " + detalhe;
    }
}
