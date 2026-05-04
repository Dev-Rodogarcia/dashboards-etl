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
@Table(name = "vw_faturas_por_cliente_powerbi")
public class VisaoFaturasClienteEntity {

    @Id
    @Column(name = "[ID Único]")
    private String uniqueId;

    @Column(name = "[Filial]")
    private String filial;

    @Column(name = "[Estado]")
    private String estado;

    @Column(name = "[CT-e/Número]")
    private Long numeroCte;

    @Column(name = "[CT-e/Chave]")
    private String chaveCte;

    @Column(name = "[CT-e/Data de emissão]")
    private OffsetDateTime dataEmissaoCte;

    @Column(name = "[Frete/Valor dos CT-es]")
    private BigDecimal valorFrete;

    @Column(name = "[Terceiros/Valor CT-es]")
    private BigDecimal valorTerceiros;

    @Column(name = "[CT-e/Status]")
    private String statusCte;

    @Column(name = "[CT-e/Resultado]")
    private String resultadoCte;

    @Column(name = "[Tipo]")
    private String tipoFrete;

    @Column(name = "[Classificação]")
    private String classificacao;

    @Column(name = "[Pagador do frete/Nome]")
    private String pagadorNome;

    @Column(name = "[Pagador do frete/Documento]")
    private String pagadorDocumento;

    @Column(name = "[Remetente/Nome]")
    private String remetenteNome;

    @Column(name = "[Remetente/Documento]")
    private String remetenteDocumento;

    @Column(name = "[Destinatário/Nome]")
    private String destinatarioNome;

    @Column(name = "[Destinatário/Documento]")
    private String destinatarioDocumento;

    @Column(name = "[Vendedor/Nome]")
    private String vendedorNome;

    @Column(name = "[NFS-e/Número]")
    private Long numeroNfse;

    @Column(name = "[NFS-e/Série]")
    private String serieNfse;

    @Column(name = "[Fatura/N° Documento]")
    private String documentoFatura;

    @Column(name = "[Fatura/Emissão]")
    private LocalDate emissaoFatura;

    @Column(name = "[Fatura/Valor]")
    private BigDecimal valorFitAnt;

    @Column(name = "[Fatura/Valor Total]")
    private BigDecimal valorFatura;

    @Column(name = "[Fatura/Número]")
    private String numeroFatura;

    @Column(name = "[Fatura/Emissão Fatura]")
    private LocalDate dataEmissaoFatura;

    @Column(name = "[Parcelas/Vencimento]")
    private LocalDate dataVencimentoFatura;

    @Column(name = "[Fatura/Baixa]")
    private LocalDate dataBaixaFatura;

    @Column(name = "[Fatura/Data Vencimento Original]")
    private LocalDate dataVencimentoOriginal;

    @Column(name = "[Notas Fiscais]")
    private String notasFiscais;

    @Column(name = "[Pedidos/Cliente]")
    private String pedidosCliente;

    @Column(name = "[Data da Última Atualização]")
    private LocalDateTime dataExtracao;

    protected VisaoFaturasClienteEntity() {
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public String getFilial() {
        return filial;
    }

    public String getEstado() {
        return estado;
    }

    public Long getNumeroCte() {
        return numeroCte;
    }

    public String getChaveCte() {
        return chaveCte;
    }

    public OffsetDateTime getDataEmissaoCte() {
        return dataEmissaoCte;
    }

    public BigDecimal getValorFrete() {
        return valorFrete;
    }

    public BigDecimal getValorTerceiros() {
        return valorTerceiros;
    }

    public String getStatusCte() {
        return statusCte;
    }

    public String getResultadoCte() {
        return resultadoCte;
    }

    public String getTipoFrete() {
        return tipoFrete;
    }

    public String getClassificacao() {
        return classificacao;
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

    public String getDestinatarioNome() {
        return destinatarioNome;
    }

    public String getDestinatarioDocumento() {
        return destinatarioDocumento;
    }

    public String getVendedorNome() {
        return vendedorNome;
    }

    public Long getNumeroNfse() {
        return numeroNfse;
    }

    public String getSerieNfse() {
        return serieNfse;
    }

    public String getDocumentoFatura() {
        return documentoFatura;
    }

    public LocalDate getEmissaoFatura() {
        return emissaoFatura;
    }

    public BigDecimal getValorFitAnt() {
        return valorFitAnt;
    }

    public BigDecimal getValorFatura() {
        return valorFatura;
    }

    public String getNumeroFatura() {
        return numeroFatura;
    }

    public LocalDate getDataEmissaoFatura() {
        return dataEmissaoFatura;
    }

    public LocalDate getDataVencimentoFatura() {
        return dataVencimentoFatura;
    }

    public LocalDate getDataBaixaFatura() {
        return dataBaixaFatura;
    }

    public LocalDate getDataVencimentoOriginal() {
        return dataVencimentoOriginal;
    }

    public String getNotasFiscais() {
        return notasFiscais;
    }

    public String getPedidosCliente() {
        return pedidosCliente;
    }

    public LocalDateTime getDataExtracao() {
        return dataExtracao;
    }
}
