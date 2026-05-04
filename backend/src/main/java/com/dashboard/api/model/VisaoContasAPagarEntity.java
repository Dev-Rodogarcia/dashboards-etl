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
@Table(name = "vw_contas_a_pagar_powerbi")
public class VisaoContasAPagarEntity {

    @Id
    @Column(name = "[Lançamento a Pagar/N°]")
    private Long sequenceCode;

    @Column(name = "[N° Documento]")
    private String documentoNumero;

    @Column(name = "[Emissão]")
    private LocalDate emissao;

    @Column(name = "[Tipo]")
    private String tipoLancamento;

    @Column(name = "[Valor]")
    private BigDecimal valor;

    @Column(name = "[Valor a pagar]")
    private BigDecimal valorAPagar;

    @Column(name = "[Pago]")
    private String pago;

    @Column(name = "[Valor pago]")
    private BigDecimal valorPago;

    @Column(name = "[Fornecedor/Nome]")
    private String fornecedorNome;

    @Column(name = "[Filial]")
    private String filial;

    @Column(name = "[Conta Contábil/Classificação]")
    private String classificacaoContabil;

    @Column(name = "[Conta Contábil/Descrição]")
    private String descricaoContabil;

    @Column(name = "[Centro de custo/Nome]")
    private String centroCustoNome;

    @Column(name = "[Baixa/Data liquidação]")
    private LocalDate dataLiquidacao;

    @Column(name = "[Conciliado]")
    private String conciliado;

    @Column(name = "[Data de extracao]")
    private LocalDateTime dataExtracao;

    protected VisaoContasAPagarEntity() {
    }

    public Long getSequenceCode() {
        return sequenceCode;
    }

    public String getDocumentoNumero() {
        return documentoNumero;
    }

    public LocalDate getEmissao() {
        return emissao;
    }

    public String getTipoLancamento() {
        return tipoLancamento;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public BigDecimal getValorAPagar() {
        return valorAPagar;
    }

    public String getPago() {
        return pago;
    }

    public BigDecimal getValorPago() {
        return valorPago;
    }

    public String getFornecedorNome() {
        return fornecedorNome;
    }

    public String getFilial() {
        return filial;
    }

    public String getClassificacaoContabil() {
        return classificacaoContabil;
    }

    public String getDescricaoContabil() {
        return descricaoContabil;
    }

    public String getCentroCustoNome() {
        return centroCustoNome;
    }

    public LocalDate getDataLiquidacao() {
        return dataLiquidacao;
    }

    public String getConciliado() {
        return conciliado;
    }

    public LocalDateTime getDataExtracao() {
        return dataExtracao;
    }
}
