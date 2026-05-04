package com.dashboard.api.model.acesso;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "setores", schema = "acesso")
public class SetorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String chave;

    @Column(nullable = false, unique = true, length = 120)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Column(nullable = false)
    private boolean sistema;

    @Column(nullable = false)
    private boolean ativo = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "setor_filiais_permitidas",
            schema = "acesso",
            joinColumns = @JoinColumn(name = "setor_id")
    )
    @Column(name = "filial_nome", nullable = false, length = 120)
    private Set<String> filiaisPermitidas = new LinkedHashSet<>();

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.criadoEm = now;
        this.atualizadoEm = now;
    }

    @PreUpdate
    void preUpdate() {
        this.atualizadoEm = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getChave() { return chave; }
    public void setChave(String chave) { this.chave = chave; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public boolean isSistema() { return sistema; }
    public void setSistema(boolean sistema) { this.sistema = sistema; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public Set<String> getFiliaisPermitidas() { return filiaisPermitidas; }
    public void setFiliaisPermitidas(Set<String> filiaisPermitidas) {
        this.filiaisPermitidas = filiaisPermitidas != null ? new LinkedHashSet<>(filiaisPermitidas) : new LinkedHashSet<>();
    }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
}
