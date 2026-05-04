package com.dashboard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Immutable
@Table(name = "vw_coletas_powerbi")
public class VisaoColetasEntity {

    @Id
    @Column(name = "[ID]")
    private String id;

    @Column(name = "[Coleta]")
    private Long coleta;

    @Column(name = "[Solicitacao]")
    private LocalDate solicitacao;

    @Column(name = "[Agendamento]")
    private LocalDate agendamento;

    @Column(name = "[Finalizacao]")
    private LocalDate finalizacao;

    @Column(name = "[Status]")
    private String status;

    @Column(name = "[Volumes]")
    private Integer volumes;

    @Column(name = "[Peso Real]")
    private BigDecimal pesoReal;

    @Column(name = "[Peso Taxado]")
    private BigDecimal pesoTaxado;

    @Column(name = "[Valor NF]")
    private BigDecimal valorNf;

    @Column(name = "[Numero Manifesto]")
    private Long numeroManifesto;

    @Column(name = "[Cliente]")
    private String clienteNome;

    @Column(name = "[Cidade]")
    private String cidadeColeta;

    @Column(name = "[UF]")
    private String ufColeta;

    @Column(name = "[Região da Coleta]")
    private String regiaoColeta;

    @Column(name = "[Filial]")
    private String filialNome;

    @Column(name = "[Usuario]")
    private String usuarioNome;

    @Column(name = "[Motivo Cancel.]")
    private String motivoCancelamento;

    @Column(name = "[Data de extracao]")
    private LocalDateTime dataExtracao;

    protected VisaoColetasEntity() {
    }

    public String getId() {
        return id;
    }

    public Long getColeta() {
        return coleta;
    }

    public LocalDate getSolicitacao() {
        return solicitacao;
    }

    public LocalDate getAgendamento() {
        return agendamento;
    }

    public LocalDate getFinalizacao() {
        return finalizacao;
    }

    public String getStatus() {
        return status;
    }

    public Integer getVolumes() {
        return volumes;
    }

    public BigDecimal getPesoReal() {
        return pesoReal;
    }

    public BigDecimal getPesoTaxado() {
        return pesoTaxado;
    }

    public BigDecimal getValorNf() {
        return valorNf;
    }

    public Long getNumeroManifesto() {
        return numeroManifesto;
    }

    public String getClienteNome() {
        return clienteNome;
    }

    public String getCidadeColeta() {
        return cidadeColeta;
    }

    public String getUfColeta() {
        return ufColeta;
    }

    public String getRegiaoColeta() {
        return regiaoColeta;
    }

    public String getFilialNome() {
        return filialNome;
    }

    public String getUsuarioNome() {
        return usuarioNome;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public LocalDateTime getDataExtracao() {
        return dataExtracao;
    }
}
