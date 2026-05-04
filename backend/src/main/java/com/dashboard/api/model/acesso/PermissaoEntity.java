package com.dashboard.api.model.acesso;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "permissoes", schema = "acesso")
public class PermissaoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String chave;

    @Column(name = "chave_legado", unique = true, length = 50)
    private String chaveLegado;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Column(length = 60)
    private String recurso;

    @Column(nullable = false, length = 30)
    private String acao = "read";

    @Column(length = 120)
    private String rota;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @PrePersist
    void prePersist() {
        this.criadoEm = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getChave() { return chave; }
    public void setChave(String chave) { this.chave = chave; }
    public String getChaveLegado() { return chaveLegado; }
    public void setChaveLegado(String chaveLegado) { this.chaveLegado = chaveLegado; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getRecurso() { return recurso; }
    public void setRecurso(String recurso) { this.recurso = recurso; }
    public String getAcao() { return acao; }
    public void setAcao(String acao) { this.acao = acao; }
    public String getRota() { return rota; }
    public void setRota(String rota) { this.rota = rota; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public Instant getCriadoEm() { return criadoEm; }
}
