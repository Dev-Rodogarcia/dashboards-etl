package com.dashboard.api.model.acesso;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "usuario_papel_vinculos", schema = "acesso",
       uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "papel_id"}))
public class UsuarioPapelVinculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "papel_id", nullable = false)
    private PapelEntity papel;

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
    public PapelEntity getPapel() { return papel; }
    public void setPapel(PapelEntity papel) { this.papel = papel; }
    public UsuarioEntity getConcedidoPor() { return concedidoPor; }
    public void setConcedidoPor(UsuarioEntity concedidoPor) { this.concedidoPor = concedidoPor; }
    public Instant getConcedidoEm() { return concedidoEm; }
    public void setConcedidoEm(Instant concedidoEm) { this.concedidoEm = concedidoEm; }
}
