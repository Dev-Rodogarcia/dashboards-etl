package com.dashboard.api.model.acesso;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "home_solicitacoes_melhoria", schema = "acesso")
public class HomeSolicitacaoMelhoriaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(nullable = false, length = 140)
    private String titulo;

    @Column(nullable = false, length = 2000)
    private String descricao;

    @Column(name = "resultado_esperado", length = 1000)
    private String resultadoEsperado;

    @Column(name = "local_aplicacao", length = 500)
    private String localAplicacao;

    @Column(nullable = false, length = 20)
    private String status = "ABERTA";

    @Column(name = "solicitante_nome", nullable = false, length = 200)
    private String solicitanteNome;

    @Column(name = "solicitante_email", nullable = false, length = 254)
    private String solicitanteEmail;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "concluido_em")
    private Instant concluidoEm;

    @Column(name = "atualizado_por", length = 120)
    private String atualizadoPor;

    @Column(name = "atualizado_em")
    private Instant atualizadoEm;

    @PrePersist
    void prePersist() {
        if (criadoEm == null) criadoEm = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = Instant.now();
    }

    public Long getId() { return id; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getResultadoEsperado() { return resultadoEsperado; }
    public void setResultadoEsperado(String resultadoEsperado) { this.resultadoEsperado = resultadoEsperado; }
    public String getLocalAplicacao() { return localAplicacao; }
    public void setLocalAplicacao(String localAplicacao) { this.localAplicacao = localAplicacao; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSolicitanteNome() { return solicitanteNome; }
    public void setSolicitanteNome(String solicitanteNome) { this.solicitanteNome = solicitanteNome; }
    public String getSolicitanteEmail() { return solicitanteEmail; }
    public void setSolicitanteEmail(String solicitanteEmail) { this.solicitanteEmail = solicitanteEmail; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getConcluidoEm() { return concluidoEm; }
    public void setConcluidoEm(Instant concluidoEm) { this.concluidoEm = concluidoEm; }
    public String getAtualizadoPor() { return atualizadoPor; }
    public void setAtualizadoPor(String atualizadoPor) { this.atualizadoPor = atualizadoPor; }
}
