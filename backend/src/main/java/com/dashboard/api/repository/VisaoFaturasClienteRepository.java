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
            SELECT DISTINCT cliente
            FROM (
                SELECT NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Pagador do frete/Nome]))), '') AS cliente
                FROM dbo.vw_faturas_por_cliente_powerbi
                UNION ALL
                SELECT NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Remetente/Nome]))), '') AS cliente
                FROM dbo.vw_faturas_por_cliente_powerbi
                UNION ALL
                SELECT NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Destinatário/Nome]))), '') AS cliente
                FROM dbo.vw_faturas_por_cliente_powerbi
            ) clientes
            WHERE cliente IS NOT NULL
            ORDER BY cliente
            """, nativeQuery = true)
    List<String> findDistinctClientes();

    @Query(value = """
            SELECT DISTINCT cliente
            FROM (
                SELECT
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Pagador do frete/Nome]))), '') AS cliente,
                    LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial]))), '')) AS filial
                FROM dbo.vw_faturas_por_cliente_powerbi
                UNION ALL
                SELECT
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Remetente/Nome]))), '') AS cliente,
                    LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial]))), '')) AS filial
                FROM dbo.vw_faturas_por_cliente_powerbi
                UNION ALL
                SELECT
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Destinatário/Nome]))), '') AS cliente,
                    LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial]))), '')) AS filial
                FROM dbo.vw_faturas_por_cliente_powerbi
            ) clientes
            WHERE cliente IS NOT NULL
              AND filial IN (:filiais)
            ORDER BY cliente
            """, nativeQuery = true)
    List<String> findDistinctClientesByFilialIn(@Param("filiais") List<String> filiais);

    @Query(value = """
            SELECT DISTINCT c.clienteCnpj
            FROM dbo.vw_faturas_por_cliente_powerbi
            CROSS APPLY (VALUES (
                COALESCE(
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Cliente/CNPJ]))), ''),
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Pagador do frete/Documento]))), '')
                )
            )) c(clienteCnpj)
            WHERE c.clienteCnpj IS NOT NULL
            ORDER BY clienteCnpj
            """, nativeQuery = true)
    List<String> findDistinctClienteCnpj();

    @Query(value = """
            SELECT DISTINCT c.clienteCnpj
            FROM dbo.vw_faturas_por_cliente_powerbi
            CROSS APPLY (VALUES (
                COALESCE(
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Cliente/CNPJ]))), ''),
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Pagador do frete/Documento]))), '')
                )
            )) c(clienteCnpj)
            WHERE c.clienteCnpj IS NOT NULL
              AND LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Filial])))) IN (:filiais)
            ORDER BY clienteCnpj
            """, nativeQuery = true)
    List<String> findDistinctClienteCnpjByFilialIn(@Param("filiais") List<String> filiais);
}
