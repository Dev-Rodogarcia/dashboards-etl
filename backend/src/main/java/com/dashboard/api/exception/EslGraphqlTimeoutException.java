package com.dashboard.api.exception;

public class EslGraphqlTimeoutException extends RuntimeException {

    public EslGraphqlTimeoutException(Throwable cause) {
        super("A operação no ESL excedeu o tempo limite de 6 segundos.", cause);
    }
}
