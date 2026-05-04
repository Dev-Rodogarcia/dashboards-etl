package com.dashboard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Immutable
@Table(name = "vw_inventario_powerbi")
public class VisaoInventarioEntity {

    @Id
    @Column(name = "[Identificador Único]")
    private String identificadorUnico;

    @Column(name = "[Nº Minuta]")
    private Long numeroMinuta;

    @Column(name = "[N° Ordem]")
    private Long numeroOrdem;

    @Column(name = "[Filial]")
    private String filial;

    @Column(name = "[Filial da Ordem de Conferência]")
    private String filialOrdemConferencia;

    @Column(name = "[Filial Emissora do Frete]")
    private String filialEmissoraFrete;

    @Column(name = "[Tipo]")
    private String tipo;

    @Column(name = "[Data/Hora início]")
    private OffsetDateTime dataHoraInicio;

    @Column(name = "[Data/Hora fim]")
    private OffsetDateTime dataHoraFim;

    @Column(name = "[Status]")
    private String status;

    @Column(name = "[Conferente]")
    private String conferente;

    @Column(name = "[Data de extracao]")
    private LocalDateTime dataExtracao;

    protected VisaoInventarioEntity() {
    }

    public String getIdentificadorUnico() {
        return identificadorUnico;
    }

    public Long getNumeroMinuta() {
        return numeroMinuta;
    }

    public Long getNumeroOrdem() {
        return numeroOrdem;
    }

    public String getFilial() {
        return filial;
    }

    public String getFilialOrdemConferencia() {
        return filialOrdemConferencia;
    }

    public String getFilialEmissoraFrete() {
        return filialEmissoraFrete;
    }

    public String getTipo() {
        return tipo;
    }

    public OffsetDateTime getDataHoraInicio() {
        return dataHoraInicio;
    }

    public OffsetDateTime getDataHoraFim() {
        return dataHoraFim;
    }

    public String getStatus() {
        return status;
    }

    public String getConferente() {
        return conferente;
    }

    public LocalDateTime getDataExtracao() {
        return dataExtracao;
    }
}
