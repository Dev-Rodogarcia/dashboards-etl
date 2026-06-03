package com.dashboard.api.repository;

import com.dashboard.api.model.VisaoFaturasClienteEntity;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisaoFaturasClienteRepository extends JpaRepository<VisaoFaturasClienteEntity, String>,
        JpaSpecificationExecutor<VisaoFaturasClienteEntity> {

    List<VisaoFaturasClienteEntity> findByDataEmissaoCteGreaterThanEqualAndDataEmissaoCteLessThan(
            OffsetDateTime inicioInclusivo,
            OffsetDateTime fimExclusivo
    );

    @Query(value = """
            SELECT *
            FROM [ETL_SISTEMA].dbo.fato_gestao_vista_faturas
            WHERE data_emissao_cte >= :inicioInclusivo
              AND data_emissao_cte < :fimExclusivo
              AND excluido_na_origem = 0
            """, nativeQuery = true)
    List<VisaoFaturasClienteEntity> findPowerBiRowsByDataEmissaoCteNaJanela(
            @Param("inicioInclusivo") OffsetDateTime inicioInclusivo,
            @Param("fimExclusivo") OffsetDateTime fimExclusivo
    );

    @Query(value = """
            SELECT DISTINCT cliente
            FROM (
                SELECT NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), pagador_nome))), '') AS cliente
                FROM [ETL_SISTEMA].dbo.fato_gestao_vista_faturas
                WHERE excluido_na_origem = 0
                UNION ALL
                SELECT NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), remetente_nome))), '') AS cliente
                FROM [ETL_SISTEMA].dbo.fato_gestao_vista_faturas
                WHERE excluido_na_origem = 0
                UNION ALL
                SELECT NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), destinatario_nome))), '') AS cliente
                FROM [ETL_SISTEMA].dbo.fato_gestao_vista_faturas
                WHERE excluido_na_origem = 0
            ) clientes
            WHERE cliente IS NOT NULL
            ORDER BY cliente
            """, nativeQuery = true)
    List<String> findDistinctClientes();

    @Query(value = """
            SELECT DISTINCT cliente
            FROM (
                SELECT
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), pagador_nome))), '') AS cliente,
                    LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), filial))), '')) AS filial
                FROM [ETL_SISTEMA].dbo.fato_gestao_vista_faturas
                WHERE excluido_na_origem = 0
                UNION ALL
                SELECT
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), remetente_nome))), '') AS cliente,
                    LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), filial))), '')) AS filial
                FROM [ETL_SISTEMA].dbo.fato_gestao_vista_faturas
                WHERE excluido_na_origem = 0
                UNION ALL
                SELECT
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), destinatario_nome))), '') AS cliente,
                    LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), filial))), '')) AS filial
                FROM [ETL_SISTEMA].dbo.fato_gestao_vista_faturas
                WHERE excluido_na_origem = 0
            ) clientes
            WHERE cliente IS NOT NULL
              AND filial IN (:filiais)
            ORDER BY cliente
            """, nativeQuery = true)
    List<String> findDistinctClientesByFilialIn(@Param("filiais") List<String> filiais);

    @Query(value = """
            SELECT DISTINCT c.clienteCnpj
            FROM [ETL_SISTEMA].dbo.fato_gestao_vista_faturas
            CROSS APPLY (VALUES (
                COALESCE(
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), cliente_cnpj))), ''),
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), pagador_documento))), '')
                )
            )) c(clienteCnpj)
            WHERE c.clienteCnpj IS NOT NULL
              AND excluido_na_origem = 0
            ORDER BY clienteCnpj
            """, nativeQuery = true)
    List<String> findDistinctClienteCnpj();

    @Query(value = """
            SELECT DISTINCT c.clienteCnpj
            FROM [ETL_SISTEMA].dbo.fato_gestao_vista_faturas
            CROSS APPLY (VALUES (
                COALESCE(
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), cliente_cnpj))), ''),
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), pagador_documento))), '')
                )
            )) c(clienteCnpj)
            WHERE c.clienteCnpj IS NOT NULL
              AND excluido_na_origem = 0
              AND LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), filial)))) IN (:filiais)
            ORDER BY clienteCnpj
            """, nativeQuery = true)
    List<String> findDistinctClienteCnpjByFilialIn(@Param("filiais") List<String> filiais);
}
