package com.dashboard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@IdClass(DimVeiculoId.class)
@Table(name = "vw_dim_veiculos")
public class DimVeiculoEntity {

    @Id
    @Column(name = "[Placa]")
    private String placa;

    @Id
    @Column(name = "[Filial]")
    private String filial;

    @Column(name = "[TipoVeiculo]")
    private String tipoVeiculo;

    @Column(name = "[Proprietario]")
    private String proprietario;

    protected DimVeiculoEntity() {
    }

    public String getPlaca() {
        return placa;
    }

    public String getFilial() {
        return filial;
    }

    public String getTipoVeiculo() {
        return tipoVeiculo;
    }

    public String getProprietario() {
        return proprietario;
    }
}
