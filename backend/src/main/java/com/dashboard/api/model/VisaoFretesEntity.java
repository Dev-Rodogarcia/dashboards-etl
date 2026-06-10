package com.dashboard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "fato_fretes_faturamento", schema = "dbo")
public class VisaoFretesEntity {

    @Id
    @Column(name = "frete_id")
    private Long id;

    @Column(name = "data_frete")
    private OffsetDateTime dataFrete;

    @Column(name = "numero_minuta")
    private Long numeroMinuta;

    @Column(name = "criado_em")
    private OffsetDateTime criadoEm;

    @Column(name = "receita_bruta")
    private BigDecimal valorTotal;

    @Column(name = "valor_notas")
    private BigDecimal valorNotas;

    @Column(name = "peso_notas")
    private BigDecimal pesoNotas;

    @Column(name = "valor_frete")
    private BigDecimal subtotal;

    @Column(name = "volumes")
    private Integer volumes;

    @Column(name = "peso_taxado")
    private BigDecimal pesoTaxado;

    @Column(name = "peso_real")
    private BigDecimal pesoReal;

    @Column(name = "peso_cubado")
    private BigDecimal pesoCubado;

    @Column(name = "total_m3")
    private BigDecimal m3Total;

    @Column(name = "total_m3", insertable = false, updatable = false)
    private BigDecimal totalM3;

    @Column(name = "pagador_nome")
    private String pagadorNome;

    @Column(name = "pagador_documento")
    private String pagadorDocumento;

    @Column(name = "remetente_nome")
    private String remetenteNome;

    @Column(name = "remetente_documento")
    private String remetenteDocumento;

    @Column(name = "origem_cidade")
    private String origemCidade;

    @Column(name = "origem_uf")
    private String origemUf;

    @Column(name = "destinatario_nome")
    private String destinatarioNome;

    @Column(name = "destinatario_documento")
    private String destinatarioDocumento;

    @Column(name = "destino_cidade")
    private String destinoCidade;

    @Column(name = "destino_uf")
    private String destinoUf;

    @Column(name = "filial_nome")
    private String filialNome;

    @Column(name = "filial_nome", insertable = false, updatable = false)
    private String filialEmissora;

    @Column(name = "responsavel_regiao_destino")
    private String responsavelRegiaoDestino;

    @Column(name = "filial_apelido")
    private String filialApelido;

    @Column(name = "filial_cnpj")
    private String filialCnpj;

    @Column(name = "tabela_preco_nome")
    private String tabelaPrecoNome;

    @Column(name = "classificacao_nome")
    private String classificacaoNome;

    @Column(name = "centro_custo_nome")
    private String centroCustoNome;

    @Column(name = "usuario_nome")
    private String usuarioNome;

    @Transient
    private LocalDate previsaoEntrega;

    @Transient
    private LocalDate dataFinalizacao;

    @Transient
    private OffsetDateTime finalizacaoPerformance;

    @Transient
    private Integer performanceDiferencaDias;

    @Transient
    private String performanceStatus;

    @Transient
    private String performanceStatusDifDias;

    @Transient
    private String performanceStatusDifDiasOficial;

    @Column(name = "modal")
    private String modal;

    @Column(name = "status_frete")
    private String status;

    @Transient
    private String documentoOficialTipo;

    @Column(name = "is_cortesia")
    private Boolean cortesiaFlag;

    @Column(name = "tipo_frete")
    private String tipoFrete;

    @Column(name = "chave_cte")
    private String chaveCte;

    @Column(name = "numero_cte")
    private Integer numeroCte;

    @Column(name = "serie_cte")
    private Integer serieCte;

    @Column(name = "data_emissao_cte")
    private OffsetDateTime cteEmissao;

    @Column(name = "data_referencia_faturamento")
    private OffsetDateTime dataReferenciaFaturamento;

    @Column(name = "is_elegivel_faturamento")
    private Boolean elegivelFaturamento;

    @Column(name = "cte_id")
    private Long cteId;

    @Column(name = "nfse_number")
    private Integer nfseNumero;

    @Column(name = "nfse_issued_at")
    private LocalDate nfseEmissao;

    @Transient
    private BigDecimal km;

    @Transient
    private BigDecimal valorIcms;

    @Transient
    private BigDecimal valorPis;

    @Transient
    private BigDecimal valorCofins;

    @Column(name = "snapshot_em")
    private LocalDateTime dataExtracao;

    protected VisaoFretesEntity() {
    }

    public static VisaoFretesEntity criarParaPainel(
            Long id,
            OffsetDateTime dataFrete,
            Long numeroMinuta,
            BigDecimal valorTotal,
            BigDecimal subtotal,
            Integer volumes,
            BigDecimal pesoTaxado,
            String pagadorNome,
            String remetenteNome,
            String destinatarioNome,
            String origemUf,
            String destinoUf,
            String destinoCidade,
            String filialNome,
            String filialEmissora,
            String responsavelRegiaoDestino,
            String classificacaoNome,
            LocalDate previsaoEntrega,
            String status,
            Boolean cortesiaFlag,
            String tipoFrete,
            String modal,
            Integer numeroCte,
            OffsetDateTime cteEmissao,
            OffsetDateTime dataReferenciaFaturamento,
            Boolean elegivelFaturamento,
            Long cteId,
            Integer nfseNumero,
            BigDecimal valorIcms,
            BigDecimal valorPis,
            BigDecimal valorCofins,
            LocalDateTime dataExtracao
    ) {
        VisaoFretesEntity entity = new VisaoFretesEntity();
        entity.id = id;
        entity.dataFrete = dataFrete;
        entity.numeroMinuta = numeroMinuta;
        entity.valorTotal = valorTotal;
        entity.subtotal = subtotal;
        entity.volumes = volumes;
        entity.pesoTaxado = pesoTaxado;
        entity.pagadorNome = pagadorNome;
        entity.remetenteNome = remetenteNome;
        entity.destinatarioNome = destinatarioNome;
        entity.origemUf = origemUf;
        entity.destinoUf = destinoUf;
        entity.destinoCidade = destinoCidade;
        entity.filialNome = filialNome;
        entity.filialEmissora = filialEmissora;
        entity.responsavelRegiaoDestino = responsavelRegiaoDestino;
        entity.classificacaoNome = classificacaoNome;
        entity.previsaoEntrega = previsaoEntrega;
        entity.status = status;
        entity.cortesiaFlag = cortesiaFlag;
        entity.tipoFrete = tipoFrete;
        entity.modal = modal;
        entity.numeroCte = numeroCte;
        entity.cteEmissao = cteEmissao;
        entity.dataReferenciaFaturamento = dataReferenciaFaturamento;
        entity.elegivelFaturamento = elegivelFaturamento;
        entity.cteId = cteId;
        entity.nfseNumero = nfseNumero;
        entity.valorIcms = valorIcms;
        entity.valorPis = valorPis;
        entity.valorCofins = valorCofins;
        entity.dataExtracao = dataExtracao;
        return entity;
    }

    public Long getId() {
        return id;
    }

    public OffsetDateTime getDataFrete() {
        return dataFrete;
    }

    public Long getNumeroMinuta() {
        return numeroMinuta;
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

    public BigDecimal getTotalM3() {
        return totalM3;
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

    public String getCidadeDestino() {
        return destinoCidade;
    }

    public String getDestinoUf() {
        return destinoUf;
    }

    public String getRegiaoDestino() {
        return destinoUf;
    }

    public String getFilialNome() {
        return filialNome;
    }

    public String getFilialEmissora() {
        return filialEmissora;
    }

    public String getResponsavelRegiaoDestino() {
        return responsavelRegiaoDestino;
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

    public LocalDate getDataFinalizacao() {
        return dataFinalizacao;
    }

    public OffsetDateTime getFinalizacaoPerformance() {
        return finalizacaoPerformance;
    }

    public Integer getPerformanceDiferencaDias() {
        return performanceDiferencaDias;
    }

    public String getPerformanceStatus() {
        return performanceStatus;
    }

    public String getPerformanceStatusDifDias() {
        return performanceStatusDifDias;
    }

    public String getPerformanceStatusDifDiasOficial() {
        return performanceStatusDifDiasOficial;
    }

    public String getComprovanteAnexado() {
        return null;
    }

    public String getModal() {
        return modal;
    }

    public String getStatus() {
        return status;
    }

    public String getDocumentoOficialTipo() {
        return documentoOficialTipo;
    }

    public Boolean getCortesiaFlag() {
        return cortesiaFlag;
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

    public OffsetDateTime getDataReferenciaFaturamento() {
        return dataReferenciaFaturamento;
    }

    public Boolean getElegivelFaturamento() {
        return elegivelFaturamento;
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
