package com.dashboard.api.model.acesso;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_logs", schema = "acesso")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "timestamp_utc", nullable = false)
    private Instant timestampUtc;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "usuario_login", length = 80)
    private String usuarioLogin;

    @Column(nullable = false, length = 60)
    private String acao;

    @Column(length = 120)
    private String recurso;

    @Column(name = "detalhes_json", columnDefinition = "NVARCHAR(MAX)")
    private String detalhesJson;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @PrePersist
    void prePersist() {
        if (this.timestampUtc == null) {
            this.timestampUtc = Instant.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Instant getTimestampUtc() { return timestampUtc; }
    public void setTimestampUtc(Instant timestampUtc) { this.timestampUtc = timestampUtc; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getUsuarioLogin() { return usuarioLogin; }
    public void setUsuarioLogin(String usuarioLogin) { this.usuarioLogin = usuarioLogin; }
    public String getAcao() { return acao; }
    public void setAcao(String acao) { this.acao = acao; }
    public String getRecurso() { return recurso; }
    public void setRecurso(String recurso) { this.recurso = recurso; }
    public String getDetalhesJson() { return detalhesJson; }
    public void setDetalhesJson(String detalhesJson) { this.detalhesJson = detalhesJson; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
}
