package com.dashboard.api.service;

import com.dashboard.api.model.VisaoHorariosCorteEntity;

import java.time.LocalDate;
import java.util.List;

public interface HorariosCorteRasterDataSource {
    List<VisaoHorariosCorteEntity> findByDataBetween(LocalDate dataInicio, LocalDate dataFim);
}
