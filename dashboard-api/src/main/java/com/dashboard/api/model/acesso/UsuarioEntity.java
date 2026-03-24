package com.dashboard.api.model.acesso;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "usuarios", schema = "acesso")
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chave_legado", unique = true, length = 80)
    private String chaveLegado;

    @Column(nullable = false, unique = true, length = 80)
    private String login;

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    @Column(name = "algoritmo_hash", nullable = false, length = 20)
    private String algoritmoHash = "bcrypt";

    @Column(name = "senha_alterada_em")
    private Instant senhaAlteradaEm;

    @Column(name = "exige_troca_senha", nullable = false)
    private boolean exigeTrocaSenha;

    @Column(name = "tentativas_falha", nullable = false)
    private int tentativasFalha;

    @Column(name = "bloqueado_ate")
    private Instant bloqueadoAte;

    @Column(name = "identity_source", nullable = false, length = 30)
    private String identitySource = "local";

    @Column(name = "external_subject_id", length = 255)
    private String externalSubjectId;

    @Column(name = "mfa_status", nullable = false, length = 20)
    private String mfaStatus = "disabled";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setor_id", nullable = false)
    private SetorEntity setor;

    @Column(nullable = false)
    private boolean ativo = true;

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
    public String getChaveLegado() { return chaveLegado; }
    public void setChaveLegado(String chaveLegado) { this.chaveLegado = chaveLegado; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public String getAlgoritmoHash() { return algoritmoHash; }
    public void setAlgoritmoHash(String algoritmoHash) { this.algoritmoHash = algoritmoHash; }
    public Instant getSenhaAlteradaEm() { return senhaAlteradaEm; }
    public void setSenhaAlteradaEm(Instant senhaAlteradaEm) { this.senhaAlteradaEm = senhaAlteradaEm; }
    public boolean isExigeTrocaSenha() { return exigeTrocaSenha; }
    public void setExigeTrocaSenha(boolean exigeTrocaSenha) { this.exigeTrocaSenha = exigeTrocaSenha; }
    public int getTentativasFalha() { return tentativasFalha; }
    public void setTentativasFalha(int tentativasFalha) { this.tentativasFalha = tentativasFalha; }
    public Instant getBloqueadoAte() { return bloqueadoAte; }
    public void setBloqueadoAte(Instant bloqueadoAte) { this.bloqueadoAte = bloqueadoAte; }
    public String getIdentitySource() { return identitySource; }
    public void setIdentitySource(String identitySource) { this.identitySource = identitySource; }
    public String getExternalSubjectId() { return externalSubjectId; }
    public void setExternalSubjectId(String externalSubjectId) { this.externalSubjectId = externalSubjectId; }
    public String getMfaStatus() { return mfaStatus; }
    public void setMfaStatus(String mfaStatus) { this.mfaStatus = mfaStatus; }
    public SetorEntity getSetor() { return setor; }
    public void setSetor(SetorEntity setor) { this.setor = setor; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
}
