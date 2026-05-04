package com.dashboard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class VisaoManifestosId implements Serializable {

    @Column(name = "[Número]")
    private Long numero;

    @Column(name = "[Identificador Único]")
    private String identificadorUnico;

    protected VisaoManifestosId() {
    }

    public VisaoManifestosId(Long numero, String identificadorUnico) {
        this.numero = numero;
        this.identificadorUnico = identificadorUnico;
    }

    public Long getNumero() {
        return numero;
    }

    public String getIdentificadorUnico() {
        return identificadorUnico;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VisaoManifestosId that)) {
            return false;
        }
        return Objects.equals(numero, that.numero)
                && Objects.equals(identificadorUnico, that.identificadorUnico);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numero, identificadorUnico);
    }
}
