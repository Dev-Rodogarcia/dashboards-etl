package com.dashboard.api.repository;

import com.dashboard.api.model.VisaoFaturasClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface VisaoFaturasClienteRepository extends JpaRepository<VisaoFaturasClienteEntity, String>,
        JpaSpecificationExecutor<VisaoFaturasClienteEntity> {

    List<VisaoFaturasClienteEntity> findByDataEmissaoCteGreaterThanEqualAndDataEmissaoCteLessThan(
            OffsetDateTime inicioInclusivo,
            OffsetDateTime fimExclusivo
    );

    @Query(value = """
            SELECT *
            FROM dbo.vw_faturas_por_cliente_powerbi
            WHERE [CT-e/Data de emissão] >= :inicioInclusivo
              AND [CT-e/Data de emissão] < :fimExclusivo
            """, nativeQuery = true)
    List<VisaoFaturasClienteEntity> findPowerBiRowsByDataEmissaoCteNaJanela(
            @Param("inicioInclusivo") OffsetDateTime inicioInclusivo,
            @Param("fimExclusivo") OffsetDateTime fimExclusivo
    );
}
