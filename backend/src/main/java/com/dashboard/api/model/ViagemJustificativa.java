package com.dashboard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "viagem_justificativas")
@SQLDelete(sql = "UPDATE viagem_justificativas SET ativo = 0 WHERE id = ?")
@SQLRestriction("ativo = 1")
public class ViagemJustificativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cod_solicitacao", nullable = false, unique = true)
    private Long codSolicitacao;

    @Column(name = "justificativa", nullable = false, length = 1000)
    private String justificativa;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "criado_por", nullable = false, length = 255)
    private String criadoPor;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    public Long getId() {
        return id;
    }

    public Long getCodSolicitacao() {
        return codSolicitacao;
    }

    public void setCodSolicitacao(Long codSolicitacao) {
        this.codSolicitacao = codSolicitacao;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(OffsetDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public String getCriadoPor() {
        return criadoPor;
    }

    public void setCriadoPor(String criadoPor) {
        this.criadoPor = criadoPor;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }
}
