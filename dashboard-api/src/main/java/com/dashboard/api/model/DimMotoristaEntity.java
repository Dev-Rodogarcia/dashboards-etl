package com.dashboard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "vw_dim_motoristas")
public class DimMotoristaEntity {

    @Id
    @Column(name = "[NomeMotorista]")
    private String nomeMotorista;

    protected DimMotoristaEntity() {
    }

    public String getNomeMotorista() {
        return nomeMotorista;
    }
}
