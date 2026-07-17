package com.dashboard.api.dto.esl;

public enum EslMotivoCancelamento {
    DESISTENCIA("Desistência"),
    VOLUME_INCORRETO("Volume Incorreto"),
    DUPLICIDADE("Duplicidade"),
    OUTROS("Outros");

    private final String valorEsl;

    EslMotivoCancelamento(String valorEsl) {
        this.valorEsl = valorEsl;
    }

    public String valorEsl() {
        return valorEsl;
    }
}
