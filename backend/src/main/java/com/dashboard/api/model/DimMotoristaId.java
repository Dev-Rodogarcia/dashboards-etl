package com.dashboard.api.model;

import java.io.Serializable;
import java.util.Objects;

public class DimMotoristaId implements Serializable {

    private String nomeMotorista;
    private String filial;

    public DimMotoristaId() {
    }

    public DimMotoristaId(String nomeMotorista, String filial) {
        this.nomeMotorista = nomeMotorista;
        this.filial = filial;
    }

    public String getNomeMotorista() {
        return nomeMotorista;
    }

    public void setNomeMotorista(String nomeMotorista) {
        this.nomeMotorista = nomeMotorista;
    }

    public String getFilial() {
        return filial;
    }

    public void setFilial(String filial) {
        this.filial = filial;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DimMotoristaId that)) {
            return false;
        }
        return Objects.equals(nomeMotorista, that.nomeMotorista)
                && Objects.equals(filial, that.filial);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nomeMotorista, filial);
    }
}
