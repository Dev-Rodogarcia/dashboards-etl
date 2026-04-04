package com.dashboard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Immutable
@Table(name = "vw_manifestos_powerbi")
public class VisaoManifestosEntity {

    @EmbeddedId
    private VisaoManifestosId id;

    @Column(name = "[Status]")
    private String status;

    @Column(name = "[Classificação]")
    private String classificacao;

    @Column(name = "[Filial]")
    private String filial;

    @Column(name = "[Filial Emissora]")
    private String filialEmissora;

    @Column(name = "[Data criação]")
    private OffsetDateTime dataCriacao;

    @Column(name = "[Saída]")
    private OffsetDateTime saida;

    @Column(name = "[Fechamento]")
    private OffsetDateTime fechamento;

    @Column(name = "[Chegada]")
    private OffsetDateTime chegada;

    @Column(name = "[Veículo/Placa]")
    private String veiculoPlaca;

    @Column(name = "[Tipo Veículo]")
    private String tipoVeiculo;

    @Column(name = "[Motorista]")
    private String motorista;

    @Column(name = "[KM viagem]")
    private Integer kmViagem;

    @Column(name = "[Total peso taxado]")
    private BigDecimal totalPesoTaxado;

    @Column(name = "[Total M3]")
    private BigDecimal totalM3;

    @Column(name = "[Valor NF]")
    private BigDecimal valorNf;

    @Column(name = "[Custo total]")
    private BigDecimal custoTotal;

    @Column(name = "[Valor frete]")
    private BigDecimal valorFrete;

    @Column(name = "[Combustível]")
    private BigDecimal combustivel;

    @Column(name = "[Pedágio]")
    private BigDecimal pedagio;

    @Column(name = "[Serviços motorista/Total]")
    private BigDecimal servicosMotorista;

    @Column(name = "[Despesa operacional]")
    private BigDecimal despesaOperacional;

    @Column(name = "[Saldo a pagar]")
    private BigDecimal saldoPagar;

    @Column(name = "[KM Total]")
    private BigDecimal kmTotal;

    @Column(name = "[Capacidade Lotação Kg]")
    private BigDecimal capacidadeKg;

    @Column(name = "[Veículo/Peso Cubado]")
    private BigDecimal veiculoPesoCubado;

    @Column(name = "[Tipo de contrato]")
    private String tipoContrato;

    @Column(name = "[Tipo de carga]")
    private String tipoCarga;

    @Column(name = "[Itens/Total]")
    private Integer itensTotal;

    @Column(name = "[Local de Descarregamento]")
    private String localDescarregamento;

    @Column(name = "[Data de extracao]")
    private LocalDateTime dataExtracao;

    protected VisaoManifestosEntity() {
    }

    public VisaoManifestosId getId() {
        return id;
    }

    public Long getNumero() {
        return id != null ? id.getNumero() : null;
    }

    public String getIdentificadorUnico() {
        return id != null ? id.getIdentificadorUnico() : null;
    }

    public String getStatus() {
        return status;
    }

    public String getClassificacao() {
        return classificacao;
    }

    public String getFilial() {
        return filial;
    }

    public String getFilialEmissora() {
        return filialEmissora;
    }

    public OffsetDateTime getDataCriacao() {
        return dataCriacao;
    }

    public OffsetDateTime getSaida() {
        return saida;
    }

    public OffsetDateTime getFechamento() {
        return fechamento;
    }

    public OffsetDateTime getChegada() {
        return chegada;
    }

    public String getVeiculoPlaca() {
        return veiculoPlaca;
    }

    public String getTipoVeiculo() {
        return tipoVeiculo;
    }

    public String getMotorista() {
        return motorista;
    }

    public Integer getKmViagem() {
        return kmViagem;
    }

    public BigDecimal getTotalPesoTaxado() {
        return totalPesoTaxado;
    }

    public BigDecimal getTotalM3() {
        return totalM3;
    }

    public BigDecimal getValorNf() {
        return valorNf;
    }

    public BigDecimal getCustoTotal() {
        return custoTotal;
    }

    public BigDecimal getValorFrete() {
        return valorFrete;
    }

    public BigDecimal getCombustivel() {
        return combustivel;
    }

    public BigDecimal getPedagio() {
        return pedagio;
    }

    public BigDecimal getServicosMotorista() {
        return servicosMotorista;
    }

    public BigDecimal getDespesaOperacional() {
        return despesaOperacional;
    }

    public BigDecimal getSaldoPagar() {
        return saldoPagar;
    }

    public BigDecimal getKmTotal() {
        return kmTotal;
    }

    public BigDecimal getCapacidadeKg() {
        return capacidadeKg;
    }

    public BigDecimal getVeiculoPesoCubado() {
        return veiculoPesoCubado;
    }

    public String getTipoContrato() {
        return tipoContrato;
    }

    public String getTipoCarga() {
        return tipoCarga;
    }

    public Integer getItensTotal() {
        return itensTotal;
    }

    public String getLocalDescarregamento() {
        return localDescarregamento;
    }

    public LocalDateTime getDataExtracao() {
        return dataExtracao;
    }
}
