package com.dashboard.api.repository;

import com.dashboard.api.model.VisaoHorariosCorteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface VisaoHorariosCorteRepository extends JpaRepository<VisaoHorariosCorteEntity, Long> {
    List<VisaoHorariosCorteEntity> findByDataBetween(LocalDate inicio, LocalDate fim);
}
