package com.dashboard.api.service;

import com.dashboard.api.dto.PaginaDTO;
import com.dashboard.api.model.VisaoHorariosCorteEntity;

import java.time.LocalDate;
import java.util.List;

public interface HorariosCorteRasterDataSource {
    List<VisaoHorariosCorteEntity> findByDataBetween(LocalDate dataInicio, LocalDate dataFim);

    PaginaDTO<VisaoHorariosCorteEntity> findPageByDataBetween(
            LocalDate dataInicio,
            LocalDate dataFim,
            int pagina,
            int tamanhoPagina
    );
}
