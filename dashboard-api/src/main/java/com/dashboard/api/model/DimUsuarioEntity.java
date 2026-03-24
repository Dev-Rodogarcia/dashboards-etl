package com.dashboard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "vw_dim_usuarios")
public class DimUsuarioEntity {

    @Id
    @Column(name = "[User ID]")
    private String userId;

    @Column(name = "[Nome]")
    private String nome;

    protected DimUsuarioEntity() {
    }

    public String getUserId() {
        return userId;
    }

    public String getNome() {
        return nome;
    }
}
