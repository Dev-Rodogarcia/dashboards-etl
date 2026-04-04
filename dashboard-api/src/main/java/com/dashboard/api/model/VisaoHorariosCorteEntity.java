package com.dashboard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Immutable
@Table(name = "vw_horarios_corte_powerbi")
public class VisaoHorariosCorteEntity {

    @Id
    @Column(name = "[ID]")
    private Long id;

    @Column(name = "[Data]")
    private LocalDate data;

    @Column(name = "[Filial]")
    private String filial;

    @Column(name = "[Linha ou Operação]")
    private String linhaOuOperacao;

    @Column(name = "[Início]")
    private LocalTime inicio;

    @Column(name = "[Manifestado]")
    private LocalTime manifestado;

    @Column(name = "[SM Gerada]")
    private LocalTime smGerada;

    @Column(name = "[Corte]")
    private LocalTime corte;

    @Column(name = "[Saída Efetiva]")
    private LocalDateTime saidaEfetiva;

    @Column(name = "[Horário de Corte]")
    private LocalDateTime horarioCorte;

    @Column(name = "[Saiu no Horário]")
    private Boolean saiuNoHorario;

    @Column(name = "[Atraso Minutos]")
    private Integer atrasoMinutos;

    @Column(name = "[Observação]")
    private String observacao;

    @Column(name = "[Nome do Arquivo]")
    private String nomeArquivo;

    @Column(name = "[Importado em]")
    private LocalDateTime importadoEm;

    @Column(name = "[Importado por]")
    private String importadoPor;

    @Column(name = "[Data de extracao]")
    private LocalDateTime dataExtracao;

    protected VisaoHorariosCorteEntity() {
    }

    public Long getId() {
        return id;
    }

    public LocalDate getData() {
        return data;
    }

    public String getFilial() {
        return filial;
    }

    public String getLinhaOuOperacao() {
        return linhaOuOperacao;
    }

    public LocalTime getInicio() {
        return inicio;
    }

    public LocalTime getManifestado() {
        return manifestado;
    }

    public LocalTime getSmGerada() {
        return smGerada;
    }

    public LocalTime getCorte() {
        return corte;
    }

    public LocalDateTime getSaidaEfetiva() {
        return saidaEfetiva;
    }

    public LocalDateTime getHorarioCorte() {
        return horarioCorte;
    }

    public Boolean getSaiuNoHorario() {
        return saiuNoHorario;
    }

    public Integer getAtrasoMinutos() {
        return atrasoMinutos;
    }

    public String getObservacao() {
        return observacao;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public LocalDateTime getImportadoEm() {
        return importadoEm;
    }

    public String getImportadoPor() {
        return importadoPor;
    }

    public LocalDateTime getDataExtracao() {
        return dataExtracao;
    }
}
