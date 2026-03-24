package com.dashboard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "vw_dim_clientes")
public class DimClienteEntity {

    @Id
    @Column(name = "[Nome]")
    private String nome;

    protected DimClienteEntity() {
    }

    public String getNome() {
        return nome;
    }
}
