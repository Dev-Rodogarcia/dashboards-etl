package com.dashboard.api.repository;

import com.dashboard.api.model.VisaoMonitoramentoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;

public interface VisaoMonitoramentoRepository extends JpaRepository<VisaoMonitoramentoEntity, Long>,
        JpaSpecificationExecutor<VisaoMonitoramentoEntity> {

    List<VisaoMonitoramentoEntity> findByDataBetween(LocalDate inicio, LocalDate fim);
}
