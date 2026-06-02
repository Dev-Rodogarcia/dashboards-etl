package com.dashboard.api.model.acesso;

public enum StatusSenhaUsuario {
    SEGURA("segura"),
    MIGRAR_NO_LOGIN("migrar_no_login"),
    RESET_OBRIGATORIO("reset_obrigatorio");

    private final String valor;

    StatusSenhaUsuario(String valor) {
        this.valor = valor;
    }

    public String valor() {
        return valor;
    }
}
