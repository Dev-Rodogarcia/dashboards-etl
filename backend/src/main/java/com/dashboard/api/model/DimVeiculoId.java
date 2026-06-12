package com.dashboard.api.model;

import java.io.Serializable;
import java.util.Objects;

public class DimVeiculoId implements Serializable {

    private String placa;
    private String filial;

    public DimVeiculoId() {
    }

    public DimVeiculoId(String placa, String filial) {
        this.placa = placa;
        this.filial = filial;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
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
        if (!(o instanceof DimVeiculoId that)) {
            return false;
        }
        return Objects.equals(placa, that.placa)
                && Objects.equals(filial, that.filial);
    }

    @Override
    public int hashCode() {
        return Objects.hash(placa, filial);
    }
}
