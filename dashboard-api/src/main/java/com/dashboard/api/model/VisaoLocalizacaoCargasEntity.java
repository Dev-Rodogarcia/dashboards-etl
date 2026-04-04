package com.dashboard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Immutable
@Table(name = "vw_localizacao_cargas_powerbi")
public class VisaoLocalizacaoCargasEntity {

    @Id
    @Column(name = "[N° Minuta]")
    private Long sequenceNumber;

    @Column(name = "[Tipo]")
    private String tipo;

    @Column(name = "[Data do frete]")
    private OffsetDateTime dataFrete;

    @Column(name = "[Volumes]")
    private Integer volumes;

    @Column(name = "[Peso Taxado]")
    private String pesoTaxado;

    @Column(name = "[Valor NF]")
    private String valorNf;

    @Column(name = "[Valor Frete]")
    private BigDecimal valorFrete;

    @Column(name = "[Tipo Serviço]")
    private String tipoServico;

    @Column(name = "[Filial Emissora]")
    private String filialEmissora;

    @Column(name = "[Previsão Entrega/Previsão de entrega]")
    private OffsetDateTime previsaoEntrega;

    @Column(name = "[Região Destino]")
    private String regiaoDestino;

    @Column(name = "[Filial Destino]")
    private String filialDestino;

    @Column(name = "[Responsável pela Região de Destino]")
    private String responsavelRegiaoDestino;

    @Column(name = "[Classificação]")
    private String classificacao;

    @Column(name = "[Status Carga]")
    private String statusCarga;

    @Column(name = "[Filial Atual]")
    private String filialAtual;

    @Column(name = "[Região Origem]")
    private String regiaoOrigem;

    @Column(name = "[Filial Origem]")
    private String filialOrigem;

    @Column(name = "[Data de extracao]")
    private LocalDateTime dataExtracao;

    protected VisaoLocalizacaoCargasEntity() {
    }

    public Long getSequenceNumber() {
        return sequenceNumber;
    }

    public String getTipo() {
        return tipo;
    }

    public OffsetDateTime getDataFrete() {
        return dataFrete;
    }

    public Integer getVolumes() {
        return volumes;
    }

    public String getPesoTaxado() {
        return pesoTaxado;
    }

    public String getValorNf() {
        return valorNf;
    }

    public BigDecimal getValorFrete() {
        return valorFrete;
    }

    public String getTipoServico() {
        return tipoServico;
    }

    public String getFilialEmissora() {
        return filialEmissora;
    }

    public OffsetDateTime getPrevisaoEntrega() {
        return previsaoEntrega;
    }

    public String getRegiaoDestino() {
        return regiaoDestino;
    }

    public String getFilialDestino() {
        return filialDestino;
    }

    public String getResponsavelRegiaoDestino() {
        return responsavelRegiaoDestino;
    }

    public String getClassificacao() {
        return classificacao;
    }

    public String getStatusCarga() {
        return statusCarga;
    }

    public String getFilialAtual() {
        return filialAtual;
    }

    public String getRegiaoOrigem() {
        return regiaoOrigem;
    }

    public String getFilialOrigem() {
        return filialOrigem;
    }

    public LocalDateTime getDataExtracao() {
        return dataExtracao;
    }
}
