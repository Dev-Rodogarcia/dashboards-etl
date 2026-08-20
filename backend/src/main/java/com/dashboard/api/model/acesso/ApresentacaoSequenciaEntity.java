package com.dashboard.api.model.acesso;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "apresentacao_sequencias", schema = "acesso")
public class ApresentacaoSequenciaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "usuario_id", nullable = false) private UsuarioEntity usuario;
    @Column(nullable = false, length = 80) private String nome;
    @Column(nullable = false) private boolean ativo = true;
    @Column(name = "criado_em", nullable = false, updatable = false) private Instant criadoEm;
    @Column(name = "atualizado_em", nullable = false) private Instant atualizadoEm;
    @OneToMany(mappedBy = "sequencia", cascade = CascadeType.ALL, orphanRemoval = true) @OrderBy("ordem ASC") private List<ApresentacaoSequenciaItemEntity> itens = new ArrayList<>();
    @PrePersist void prePersist() { Instant agora = Instant.now(); criadoEm = agora; atualizadoEm = agora; }
    @PreUpdate void preUpdate() { atualizadoEm = Instant.now(); }
    public Long getId() { return id; }
    public UsuarioEntity getUsuario() { return usuario; }
    public void setUsuario(UsuarioEntity usuario) { this.usuario = usuario; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public List<ApresentacaoSequenciaItemEntity> getItens() { return itens; }
    public void substituirItens(List<String> paginas) { itens.clear(); for (int i = 0; i < paginas.size(); i++) { ApresentacaoSequenciaItemEntity item = new ApresentacaoSequenciaItemEntity(); item.setSequencia(this); item.setPagina(paginas.get(i)); item.setOrdem(i + 1); itens.add(item); } }
}
