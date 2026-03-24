package com.dashboard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Immutable
@Table(name = "vw_bi_monitoramento")
public class VisaoMonitoramentoEntity {

    @Id
    @Column(name = "[Id]")
    private Long id;

    @Column(name = "[Inicio]")
    private LocalDateTime inicio;

    @Column(name = "[Fim]")
    private LocalDateTime fim;

    @Column(name = "[Duracao (s)]")
    private Integer duracaoSegundos;

    @Column(name = "[Data]")
    private LocalDate data;

    @Column(name = "[Status]")
    private String status;

    @Column(name = "[Total Registros]")
    private Integer totalRegistros;

    @Column(name = "[Categoria Erro]")
    private String categoriaErro;

    @Column(name = "[Mensagem Erro]")
    private String mensagemErro;

    protected VisaoMonitoramentoEntity() {
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public LocalDateTime getFim() {
        return fim;
    }

    public Integer getDuracaoSegundos() {
        return duracaoSegundos;
    }

    public LocalDate getData() {
        return data;
    }

    public String getStatus() {
        return status;
    }

    public Integer getTotalRegistros() {
        return totalRegistros;
    }

    public String getCategoriaErro() {
        return categoriaErro;
    }

    public String getMensagemErro() {
        return mensagemErro;
    }
}
