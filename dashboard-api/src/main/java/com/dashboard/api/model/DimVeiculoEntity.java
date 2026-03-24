package com.dashboard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "vw_dim_veiculos")
public class DimVeiculoEntity {

    @Id
    @Column(name = "[Placa]")
    private String placa;

    @Column(name = "[TipoVeiculo]")
    private String tipoVeiculo;

    @Column(name = "[Proprietario]")
    private String proprietario;

    protected DimVeiculoEntity() {
    }

    public String getPlaca() {
        return placa;
    }

    public String getTipoVeiculo() {
        return tipoVeiculo;
    }

    public String getProprietario() {
        return proprietario;
    }
}
