package com.dashboard.api.repository;

import com.dashboard.api.model.VisaoFaturasGraphqlEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;

public interface VisaoFaturasGraphqlRepository extends JpaRepository<VisaoFaturasGraphqlEntity, Long>,
        JpaSpecificationExecutor<VisaoFaturasGraphqlEntity> {

    List<VisaoFaturasGraphqlEntity> findByEmissaoBetween(LocalDate inicio, LocalDate fim);
}
