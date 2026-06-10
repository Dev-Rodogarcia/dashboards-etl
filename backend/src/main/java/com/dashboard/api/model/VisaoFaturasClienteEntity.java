package com.dashboard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "fato_gestao_vista_faturas", schema = "dbo")
public class VisaoFaturasClienteEntity {

    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Id
    @Column(name = "unique_id")
    private String uniqueId;

    @Column(name = "filial")
    private String filial;

    @Column(name = "estado")
    private String estado;

    @Column(name = "numero_cte")
    private Long numeroCte;

    @Column(name = "chave_cte")
    private String chaveCte;

    @Column(name = "data_emissao_cte")
    private OffsetDateTime dataEmissaoCte;

    @Column(name = "valor_frete")
    private BigDecimal valorFrete;

    @Column(name = "third_party_ctes_value")
    private BigDecimal valorTerceiros;

    @Column(name = "status_cte")
    private String statusCte;

    @Column(name = "status_cte_result")
    private String resultadoCte;

    @Column(name = "tipo_frete")
    private String tipoFrete;

    @Column(name = "classificacao")
    private String classificacao;

    @Column(name = "pagador_nome")
    private String pagadorNome;

    @Column(name = "pagador_documento")
    private String pagadorDocumento;

    @Column(name = "cliente_cnpj")
    private String clienteCnpj;

    @Column(name = "remetente_nome")
    private String remetenteNome;

    @Column(name = "remetente_documento")
    private String remetenteDocumento;

    @Column(name = "destinatario_nome")
    private String destinatarioNome;

    @Column(name = "destinatario_documento")
    private String destinatarioDocumento;

    @Column(name = "vendedor_nome")
    private String vendedorNome;

    @Column(name = "numero_nfse")
    private Long numeroNfse;

    @Column(name = "serie_nfse")
    private String serieNfse;

    @Column(name = "documento_fatura")
    private String documentoFatura;

    @Column(name = "data_base_prazo")
    private String emissaoFatura;

    @Column(name = "valor_fit_ant")
    private BigDecimal valorFitAnt;

    @Column(name = "valor_fatura")
    private BigDecimal valorFatura;

    @Column(name = "numero_fatura")
    private String numeroFatura;

    @Column(name = "data_emissao_fatura")
    private String dataEmissaoFatura;

    @Column(name = "data_vencimento_fatura")
    private String dataVencimentoFatura;

    @Column(name = "data_baixa_fatura")
    private String dataBaixaFatura;

    @Column(name = "fit_ant_ils_original_due_date")
    private String dataVencimentoOriginal;

    @Column(name = "notas_fiscais")
    private String notasFiscais;

    @Column(name = "pedidos_cliente")
    private String pedidosCliente;

    @Column(name = "snapshot_em")
    private String dataExtracao;

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

    public String getClienteCnpj() {
        return clienteCnpj;
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
        return parseLocalDate(emissaoFatura);
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
        return parseLocalDate(dataEmissaoFatura);
    }

    public LocalDate getDataVencimentoFatura() {
        return parseLocalDate(dataVencimentoFatura);
    }

    public LocalDate getDataBaixaFatura() {
        return parseLocalDate(dataBaixaFatura);
    }

    public LocalDate getDataVencimentoOriginal() {
        return parseLocalDate(dataVencimentoOriginal);
    }

    public String getNotasFiscais() {
        return notasFiscais;
    }

    public String getPedidosCliente() {
        return pedidosCliente;
    }

    public LocalDateTime getDataExtracao() {
        return parseLocalDateTime(dataExtracao);
    }

    private static LocalDate parseLocalDate(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        String texto = valor.trim();
        if (texto.length() >= 10 && texto.charAt(4) == '-' && texto.charAt(7) == '-') {
            return tryParse(texto.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
        }
        if (texto.length() >= 10 && texto.charAt(2) == '/' && texto.charAt(5) == '/') {
            return tryParse(texto.substring(0, 10), DATA_BR);
        }
        if (texto.length() == 8 && texto.chars().allMatch(Character::isDigit)) {
            return tryParse(texto, DateTimeFormatter.BASIC_ISO_DATE);
        }

        return tryParse(texto, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private static LocalDate tryParse(String valor, DateTimeFormatter formatter) {
        try {
            return LocalDate.parse(valor, formatter);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static LocalDateTime parseLocalDateTime(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        String texto = valor.trim();
        if (texto.length() >= 19 && texto.charAt(4) == '-' && texto.charAt(7) == '-') {
            String normalizado = texto.substring(0, 19).replace('T', ' ');
            return tryParseLocalDateTime(normalizado, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        return tryParseLocalDateTime(texto, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private static LocalDateTime tryParseLocalDateTime(String valor, DateTimeFormatter formatter) {
        try {
            return LocalDateTime.parse(valor, formatter);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
