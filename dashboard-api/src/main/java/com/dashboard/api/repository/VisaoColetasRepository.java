package com.dashboard.api.repository;

import com.dashboard.api.model.VisaoColetasEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;

public interface VisaoColetasRepository extends JpaRepository<VisaoColetasEntity, String>,
        JpaSpecificationExecutor<VisaoColetasEntity> {

    List<VisaoColetasEntity> findBySolicitacaoBetween(LocalDate inicio, LocalDate fim);
}
