package com.dashboard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "vw_dim_planocontas")
public class DimPlanoContasEntity {

    @Id
    @Column(name = "[Descricao]")
    private String descricao;

    @Column(name = "[Classificacao]")
    private String classificacao;

    protected DimPlanoContasEntity() {
    }

    public String getDescricao() {
        return descricao;
    }

    public String getClassificacao() {
        return classificacao;
    }
}
