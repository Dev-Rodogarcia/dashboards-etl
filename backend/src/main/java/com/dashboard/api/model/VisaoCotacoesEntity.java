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
@Table(name = "vw_cotacoes_powerbi")
public class VisaoCotacoesEntity {

    @Id
    @Column(name = "[N° Cotação]")
    private Long sequenceCode;

    @Column(name = "[Data Cotação]")
    private OffsetDateTime dataCotacao;

    @Column(name = "[Filial]")
    private String filial;

    @Column(name = "[Solicitante]")
    private String solicitante;

    @Column(name = "[Cliente Pagador]")
    private String clientePagador;

    @Column(name = "[CNPJ/CPF Cliente]")
    private String clienteDoc;

    @Column(name = "[Cliente]")
    private String cliente;

    @Column(name = "[Cidade Origem]")
    private String cidadeOrigem;

    @Column(name = "[UF Origem]")
    private String ufOrigem;

    @Column(name = "[Cidade Destino]")
    private String cidadeDestino;

    @Column(name = "[UF Destino]")
    private String ufDestino;

    @Column(name = "[Trecho]")
    private String trecho;

    @Column(name = "[Volume]")
    private Integer volume;

    @Column(name = "[Peso taxado]")
    private BigDecimal pesoTaxado;

    @Column(name = "[Valor NF]")
    private BigDecimal valorNf;

    @Column(name = "[Valor frete]")
    private BigDecimal valorFrete;

    @Column(name = "[Tabela]")
    private String tabela;

    @Column(name = "[Tipo de operação]")
    private String tipoOperacao;

    @Column(name = "[Status Conversão]")
    private String statusConversao;

    @Column(name = "[Motivo Perda]")
    private String motivoPerda;

    @Column(name = "[CT-e/Data de emissão]")
    private OffsetDateTime cteEmissao;

    @Column(name = "[Nfse/Data de emissão]")
    private OffsetDateTime nfseEmissao;

    @Column(name = "[Data de extracao]")
    private LocalDateTime dataExtracao;

    protected VisaoCotacoesEntity() {
    }

    public Long getSequenceCode() {
        return sequenceCode;
    }

    public OffsetDateTime getDataCotacao() {
        return dataCotacao;
    }

    public String getFilial() {
        return filial;
    }

    public String getSolicitante() {
        return solicitante;
    }

    public String getClientePagador() {
        return clientePagador;
    }

    public String getClienteDoc() {
        return clienteDoc;
    }

    public String getCliente() {
        return cliente;
    }

    public String getCidadeOrigem() {
        return cidadeOrigem;
    }

    public String getUfOrigem() {
        return ufOrigem;
    }

    public String getCidadeDestino() {
        return cidadeDestino;
    }

    public String getUfDestino() {
        return ufDestino;
    }

    public String getTrecho() {
        return trecho;
    }

    public Integer getVolume() {
        return volume;
    }

    public BigDecimal getPesoTaxado() {
        return pesoTaxado;
    }

    public BigDecimal getValorNf() {
        return valorNf;
    }

    public BigDecimal getValorFrete() {
        return valorFrete;
    }

    public String getTabela() {
        return tabela;
    }

    public String getTipoOperacao() {
        return tipoOperacao;
    }

    public String getStatusConversao() {
        return statusConversao;
    }

    public String getMotivoPerda() {
        return motivoPerda;
    }

    public OffsetDateTime getCteEmissao() {
        return cteEmissao;
    }

    public OffsetDateTime getNfseEmissao() {
        return nfseEmissao;
    }

    public LocalDateTime getDataExtracao() {
        return dataExtracao;
    }
}
