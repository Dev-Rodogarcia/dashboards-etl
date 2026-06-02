package com.dashboard.api.repository;

import com.dashboard.api.model.HorarioCorteEntity;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HorarioCorteRepository extends JpaRepository<HorarioCorteEntity, Long> {
    Optional<HorarioCorteEntity> findByDataOperacaoAndLinhaOuOperacaoChave(LocalDate dataOperacao, String linhaOuOperacaoChave);
}
