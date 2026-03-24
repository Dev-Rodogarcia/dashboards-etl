package com.dashboard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Immutable
@Table(name = "vw_fretes_powerbi")
public class VisaoFretesEntity {

    @Id
    @Column(name = "[ID]")
    private Long id;

    @Column(name = "[Data frete]")
    private OffsetDateTime dataFrete;

    @Column(name = "[Criado em]")
    private OffsetDateTime criadoEm;

    @Column(name = "[Valor Total do Serviço]")
    private BigDecimal valorTotal;

    @Column(name = "[Valor NF]")
    private BigDecimal valorNotas;

    @Column(name = "[Kg NF]")
    private BigDecimal pesoNotas;

    @Column(name = "[Valor Frete]")
    private BigDecimal subtotal;

    @Column(name = "[Volumes]")
    private Integer volumes;

    @Column(name = "[Kg Taxado]")
    private BigDecimal pesoTaxado;

    @Column(name = "[Kg Real]")
    private BigDecimal pesoReal;

    @Column(name = "[Kg Cubado]")
    private BigDecimal pesoCubado;

    @Column(name = "[M3]")
    private BigDecimal m3Total;

    @Column(name = "[Pagador]")
    private String pagadorNome;

    @Column(name = "[Pagador Doc]")
    private String pagadorDocumento;

    @Column(name = "[Remetente]")
    private String remetenteNome;

    @Column(name = "[Remetente Doc]")
    private String remetenteDocumento;

    @Column(name = "[Origem]")
    private String origemCidade;

    @Column(name = "[UF Origem]")
    private String origemUf;

    @Column(name = "[Destinatario]")
    private String destinatarioNome;

    @Column(name = "[Destinatario Doc]")
    private String destinatarioDocumento;

    @Column(name = "[Destino]")
    private String destinoCidade;

    @Column(name = "[UF Destino]")
    private String destinoUf;

    @Column(name = "[Filial]")
    private String filialNome;

    @Column(name = "[Filial Apelido]")
    private String filialApelido;

    @Column(name = "[Filial CNPJ]")
    private String filialCnpj;

    @Column(name = "[Tabela de Preço]")
    private String tabelaPrecoNome;

    @Column(name = "[Classificação]")
    private String classificacaoNome;

    @Column(name = "[Centro de Custo]")
    private String centroCustoNome;

    @Column(name = "[Usuário]")
    private String usuarioNome;

    @Column(name = "[Previsão de Entrega]")
    private LocalDate previsaoEntrega;

    @Column(name = "[Modal]")
    private String modal;

    @Column(name = "[Status]")
    private String status;

    @Column(name = "[Tipo Frete]")
    private String tipoFrete;

    @Column(name = "[Chave CT-e]")
    private String chaveCte;

    @Column(name = "[Nº CT-e]")
    private Integer numeroCte;

    @Column(name = "[Série]")
    private Integer serieCte;

    @Column(name = "[CT-e Emissão]")
    private OffsetDateTime cteEmissao;

    @Column(name = "[CT-e ID]")
    private Long cteId;

    @Column(name = "[Nº NFS-e]")
    private Integer nfseNumero;

    @Column(name = "[NFS-e/Emissão]")
    private LocalDate nfseEmissao;

    @Column(name = "[KM]")
    private BigDecimal km;

    @Column(name = "[Valor ICMS]")
    private BigDecimal valorIcms;

    @Column(name = "[Valor PIS]")
    private BigDecimal valorPis;

    @Column(name = "[Valor COFINS]")
    private BigDecimal valorCofins;

    @Column(name = "[Data de extracao]")
    private LocalDateTime dataExtracao;

    protected VisaoFretesEntity() {
    }

    public Long getId() {
        return id;
    }

    public OffsetDateTime getDataFrete() {
        return dataFrete;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public BigDecimal getValorNotas() {
        return valorNotas;
    }

    public BigDecimal getPesoNotas() {
        return pesoNotas;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public Integer getVolumes() {
        return volumes;
    }

    public BigDecimal getPesoTaxado() {
        return pesoTaxado;
    }

    public BigDecimal getPesoReal() {
        return pesoReal;
    }

    public BigDecimal getPesoCubado() {
        return pesoCubado;
    }

    public BigDecimal getM3Total() {
        return m3Total;
    }

    public String getPagadorNome() {
        return pagadorNome;
    }

    public String getPagadorDocumento() {
        return pagadorDocumento;
    }

    public String getRemetenteNome() {
        return remetenteNome;
    }

    public String getRemetenteDocumento() {
        return remetenteDocumento;
    }

    public String getOrigemCidade() {
        return origemCidade;
    }

    public String getOrigemUf() {
        return origemUf;
    }

    public String getDestinatarioNome() {
        return destinatarioNome;
    }

    public String getDestinatarioDocumento() {
        return destinatarioDocumento;
    }

    public String getDestinoCidade() {
        return destinoCidade;
    }

    public String getDestinoUf() {
        return destinoUf;
    }

    public String getFilialNome() {
        return filialNome;
    }

    public String getFilialApelido() {
        return filialApelido;
    }

    public String getFilialCnpj() {
        return filialCnpj;
    }

    public String getTabelaPrecoNome() {
        return tabelaPrecoNome;
    }

    public String getClassificacaoNome() {
        return classificacaoNome;
    }

    public String getCentroCustoNome() {
        return centroCustoNome;
    }

    public String getUsuarioNome() {
        return usuarioNome;
    }

    public LocalDate getPrevisaoEntrega() {
        return previsaoEntrega;
    }

    public String getModal() {
        return modal;
    }

    public String getStatus() {
        return status;
    }

    public String getTipoFrete() {
        return tipoFrete;
    }

    public String getChaveCte() {
        return chaveCte;
    }

    public Integer getNumeroCte() {
        return numeroCte;
    }

    public Integer getSerieCte() {
        return serieCte;
    }

    public OffsetDateTime getCteEmissao() {
        return cteEmissao;
    }

    public Long getCteId() {
        return cteId;
    }

    public Integer getNfseNumero() {
        return nfseNumero;
    }

    public LocalDate getNfseEmissao() {
        return nfseEmissao;
    }

    public BigDecimal getKm() {
        return km;
    }

    public BigDecimal getValorIcms() {
        return valorIcms;
    }

    public BigDecimal getValorPis() {
        return valorPis;
    }

    public BigDecimal getValorCofins() {
        return valorCofins;
    }

    public LocalDateTime getDataExtracao() {
        return dataExtracao;
    }
}
