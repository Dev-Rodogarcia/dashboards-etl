package com.dashboard.api.repository;

import com.dashboard.api.model.HorarioCorteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface HorarioCorteRepository extends JpaRepository<HorarioCorteEntity, Long> {
    Optional<HorarioCorteEntity> findByDataOperacaoAndLinhaOuOperacaoChave(LocalDate dataOperacao, String linhaOuOperacaoChave);
}
