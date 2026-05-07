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
            WHERE TRY_CONVERT(datetimeoffset, [CT-e/Data de emissão]) >= :inicioInclusivo
              AND TRY_CONVERT(datetimeoffset, [CT-e/Data de emissão]) < :fimExclusivo
            """, nativeQuery = true)
    List<VisaoFaturasClienteEntity> findPowerBiRowsByDataEmissaoCteNaJanela(
            @Param("inicioInclusivo") OffsetDateTime inicioInclusivo,
            @Param("fimExclusivo") OffsetDateTime fimExclusivo
    );

    @Query(value = """
            SELECT DISTINCT LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Cliente/CNPJ]))) AS clienteCnpj
            FROM dbo.vw_faturas_por_cliente_powerbi
            WHERE [Cliente/CNPJ] IS NOT NULL
              AND LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Cliente/CNPJ]))) <> ''
            ORDER BY clienteCnpj
            """, nativeQuery = true)
    List<String> findDistinctClienteCnpj();

    @Query(value = """
            SELECT DISTINCT LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Cliente/CNPJ]))) AS clienteCnpj
            FROM dbo.vw_faturas_por_cliente_powerbi
            WHERE [Cliente/CNPJ] IS NOT NULL
              AND LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Cliente/CNPJ]))) <> ''
              AND LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Filial])))) IN (:filiais)
            ORDER BY clienteCnpj
            """, nativeQuery = true)
    List<String> findDistinctClienteCnpjByFilialIn(@Param("filiais") List<String> filiais);
}
