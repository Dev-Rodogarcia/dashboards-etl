package com.dashboard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Immutable
@Table(name = "vw_sinistros_powerbi")
public class VisaoSinistrosEntity {

    @Id
    @Column(name = "[Identificador Único]")
    private String identificadorUnico;

    @Column(name = "[Nº do Sinistro]")
    private Long numeroSinistro;

    @Column(name = "[Data abertura]")
    private LocalDate dataAbertura;

    @Column(name = "[Data ocorrência]")
    private LocalDate dataOcorrencia;

    @Column(name = "[Hora ocorrência]")
    private LocalTime horaOcorrencia;

    @Column(name = "[Data finalização]")
    private LocalDate dataFinalizacao;

    @Column(name = "[Hora finalização]")
    private LocalTime horaFinalizacao;

    @Column(name = "[Minuta]")
    private Long minuta;

    @Column(name = "[Resultado final]")
    private BigDecimal resultadoFinal;

    @Column(name = "[Ocorrência/Ocorrência]")
    private String ocorrencia;

    @Column(name = "[Tratativa/Solução]")
    private String solucao;

    @Column(name = "[Pessoa/Nome fantasia]")
    private String pessoaNomeFantasia;

    @Column(name = "[Data de extracao]")
    private LocalDateTime dataExtracao;

    protected VisaoSinistrosEntity() {
    }

    public String getIdentificadorUnico() {
        return identificadorUnico;
    }

    public Long getNumeroSinistro() {
        return numeroSinistro;
    }

    public LocalDate getDataAbertura() {
        return dataAbertura;
    }

    public LocalDate getDataOcorrencia() {
        return dataOcorrencia;
    }

    public LocalTime getHoraOcorrencia() {
        return horaOcorrencia;
    }

    public LocalDate getDataFinalizacao() {
        return dataFinalizacao;
    }

    public LocalTime getHoraFinalizacao() {
        return horaFinalizacao;
    }

    public Long getMinuta() {
        return minuta;
    }

    public BigDecimal getResultadoFinal() {
        return resultadoFinal;
    }

    public String getOcorrencia() {
        return ocorrencia;
    }

    public String getSolucao() {
        return solucao;
    }

    public String getPessoaNomeFantasia() {
        return pessoaNomeFantasia;
    }

    public LocalDateTime getDataExtracao() {
        return dataExtracao;
    }
}
