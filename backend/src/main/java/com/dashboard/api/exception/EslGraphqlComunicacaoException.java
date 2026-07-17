package com.dashboard.api.exception;

public class EslGraphqlComunicacaoException extends RuntimeException {

    public EslGraphqlComunicacaoException(Throwable cause) {
        super("Não foi possível comunicar com o ESL.", cause);
    }
}
