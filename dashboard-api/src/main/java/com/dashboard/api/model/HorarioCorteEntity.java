package com.dashboard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "horarios_corte")
public class HorarioCorteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_operacao", nullable = false)
    private LocalDate dataOperacao;

    @Column(name = "linha_ou_operacao_original", nullable = false, length = 255)
    private String linhaOuOperacaoOriginal;

    @Column(name = "linha_ou_operacao_chave", nullable = false, length = 255)
    private String linhaOuOperacaoChave;

    @Column(name = "filial_canonica", nullable = false, length = 120)
    private String filialCanonica;

    @Column(name = "inicio", nullable = false)
    private LocalTime inicio;

    @Column(name = "manifestado")
    private LocalTime manifestado;

    @Column(name = "sm_gerada")
    private LocalTime smGerada;

    @Column(name = "corte", nullable = false)
    private LocalTime corte;

    @Column(name = "observacao", length = 1000)
    private String observacao;

    @Column(name = "nome_arquivo", nullable = false, length = 255)
    private String nomeArquivo;

    @Column(name = "importado_em", nullable = false)
    private LocalDateTime importadoEm;

    @Column(name = "importado_por", length = 254)
    private String importadoPor;

    public HorarioCorteEntity() {
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDataOperacao() {
        return dataOperacao;
    }

    public void setDataOperacao(LocalDate dataOperacao) {
        this.dataOperacao = dataOperacao;
    }

    public String getLinhaOuOperacaoOriginal() {
        return linhaOuOperacaoOriginal;
    }

    public void setLinhaOuOperacaoOriginal(String linhaOuOperacaoOriginal) {
        this.linhaOuOperacaoOriginal = linhaOuOperacaoOriginal;
    }

    public String getLinhaOuOperacaoChave() {
        return linhaOuOperacaoChave;
    }

    public void setLinhaOuOperacaoChave(String linhaOuOperacaoChave) {
        this.linhaOuOperacaoChave = linhaOuOperacaoChave;
    }

    public String getFilialCanonica() {
        return filialCanonica;
    }

    public void setFilialCanonica(String filialCanonica) {
        this.filialCanonica = filialCanonica;
    }

    public LocalTime getInicio() {
        return inicio;
    }

    public void setInicio(LocalTime inicio) {
        this.inicio = inicio;
    }

    public LocalTime getManifestado() {
        return manifestado;
    }

    public void setManifestado(LocalTime manifestado) {
        this.manifestado = manifestado;
    }

    public LocalTime getSmGerada() {
        return smGerada;
    }

    public void setSmGerada(LocalTime smGerada) {
        this.smGerada = smGerada;
    }

    public LocalTime getCorte() {
        return corte;
    }

    public void setCorte(LocalTime corte) {
        this.corte = corte;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public void setNomeArquivo(String nomeArquivo) {
        this.nomeArquivo = nomeArquivo;
    }

    public LocalDateTime getImportadoEm() {
        return importadoEm;
    }

    public void setImportadoEm(LocalDateTime importadoEm) {
        this.importadoEm = importadoEm;
    }

    public String getImportadoPor() {
        return importadoPor;
    }

    public void setImportadoPor(String importadoPor) {
        this.importadoPor = importadoPor;
    }
}
