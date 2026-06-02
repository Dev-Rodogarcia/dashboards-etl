package com.dashboard.api.repository;

import com.dashboard.api.model.VisaoMonitoramentoEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VisaoMonitoramentoRepository extends JpaRepository<VisaoMonitoramentoEntity, Long>,
        JpaSpecificationExecutor<VisaoMonitoramentoEntity> {

    List<VisaoMonitoramentoEntity> findByDataBetween(LocalDate inicio, LocalDate fim);
}
