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
@Table(name = "vw_faturas_graphql_powerbi")
public class VisaoFaturasGraphqlEntity {

    @Id
    @Column(name = "[ID]")
    private Long id;

    @Column(name = "[Fatura/N° Documento]")
    private String documento;

    @Column(name = "[Emissão]")
    private LocalDate emissao;

    @Column(name = "[Vencimento]")
    private LocalDate vencimento;

    @Column(name = "[Vencimento Original]")
    private LocalDate vencimentoOriginal;

    @Column(name = "[Valor]")
    private BigDecimal valor;

    @Column(name = "[Valor Pago]")
    private BigDecimal valorPago;

    @Column(name = "[Valor a Pagar]")
    private BigDecimal valorAPagar;

    @Column(name = "[Valor Desconto]")
    private BigDecimal valorDesconto;

    @Column(name = "[Valor Juros]")
    private BigDecimal valorJuros;

    @Column(name = "[Pago]")
    private String pago;

    @Column(name = "[Status]")
    private String status;

    @Column(name = "[Tipo]")
    private String tipo;

    @Column(name = "[Filial/Nome]")
    private String filialNome;

    @Column(name = "[Filial/CNPJ]")
    private String filialCnpj;

    @Column(name = "[Método Pagamento]")
    private String metodoPagamento;

    @Column(name = "[Data de extracao]")
    private LocalDateTime dataExtracao;

    protected VisaoFaturasGraphqlEntity() {
    }

    public Long getId() {
        return id;
    }

    public String getDocumento() {
        return documento;
    }

    public LocalDate getEmissao() {
        return emissao;
    }

    public LocalDate getVencimento() {
        return vencimento;
    }

    public LocalDate getVencimentoOriginal() {
        return vencimentoOriginal;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public BigDecimal getValorPago() {
        return valorPago;
    }

    public BigDecimal getValorAPagar() {
        return valorAPagar;
    }

    public BigDecimal getValorDesconto() {
        return valorDesconto;
    }

    public BigDecimal getValorJuros() {
        return valorJuros;
    }

    public String getPago() {
        return pago;
    }

    public String getStatus() {
        return status;
    }

    public String getTipo() {
        return tipo;
    }

    public String getFilialNome() {
        return filialNome;
    }

    public String getFilialCnpj() {
        return filialCnpj;
    }

    public String getMetodoPagamento() {
        return metodoPagamento;
    }

    public LocalDateTime getDataExtracao() {
        return dataExtracao;
    }
}
