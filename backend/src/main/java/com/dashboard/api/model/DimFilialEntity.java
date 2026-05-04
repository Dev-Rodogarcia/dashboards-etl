package com.dashboard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "vw_dim_filiais")
public class DimFilialEntity {

    @Id
    @Column(name = "[NomeFilial]")
    private String nomeFilial;

    protected DimFilialEntity() {
    }

    public String getNomeFilial() {
        return nomeFilial;
    }
}
