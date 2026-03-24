package com.dashboard.api.model.acesso;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "usuario_permissao_overrides", schema = "acesso",
       uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "permissao_id"}))
public class UsuarioPermissaoOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permissao_id", nullable = false)
    private PermissaoEntity permissao;

    @Column(nullable = false, length = 5)
    private String tipo; // GRANT or DENY

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concedido_por")
    private UsuarioEntity concedidoPor;

    @Column(name = "concedido_em", nullable = false)
    private Instant concedidoEm;

    @PrePersist
    void prePersist() {
        if (this.concedidoEm == null) {
            this.concedidoEm = Instant.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UsuarioEntity getUsuario() { return usuario; }
    public void setUsuario(UsuarioEntity usuario) { this.usuario = usuario; }
    public PermissaoEntity getPermissao() { return permissao; }
    public void setPermissao(PermissaoEntity permissao) { this.permissao = permissao; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public UsuarioEntity getConcedidoPor() { return concedidoPor; }
    public void setConcedidoPor(UsuarioEntity concedidoPor) { this.concedidoPor = concedidoPor; }
    public Instant getConcedidoEm() { return concedidoEm; }
    public void setConcedidoEm(Instant concedidoEm) { this.concedidoEm = concedidoEm; }
}
