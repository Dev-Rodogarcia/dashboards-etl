package com.dashboard.api.repository;

import com.dashboard.api.model.VisaoFretesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

public interface VisaoFretesRepository extends JpaRepository<VisaoFretesEntity, Long>,
        JpaSpecificationExecutor<VisaoFretesEntity> {

    String FRETES_FILTRADOS_SQL = """
            WITH fretes AS (
                SELECT
                    TRY_CONVERT(BIGINT, [ID]) AS id,
                    TRY_CONVERT(datetimeoffset, CONVERT(NVARCHAR(64), [Data frete])) AS data_frete,
                    TRY_CONVERT(BIGINT, [Nº Minuta]) AS numero_minuta,
                    TRY_CONVERT(DECIMAL(18, 2), [Valor Total do Serviço]) AS valor_total,
                    TRY_CONVERT(DECIMAL(18, 2), [Valor Frete]) AS subtotal,
                    TRY_CONVERT(INT, [Volumes]) AS volumes,
                    TRY_CONVERT(DECIMAL(18, 2), [Kg Taxado]) AS peso_taxado,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Pagador]))), '') AS pagador_nome,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Remetente]))), '') AS remetente_nome,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Destinatario]))), '') AS destinatario_nome,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(50), [UF Origem]))), '') AS origem_uf,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(50), [UF Destino]))), '') AS destino_uf,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Destino]))), '') AS destino_cidade,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial]))), '') AS filial_nome,
                    LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial]))), '')) AS filial_nome_normalizada,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial Emissora]))), '') AS filial_emissora,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Responsável pela Região de Destino]))), '') AS responsavel_regiao_destino,
                    LOWER(COALESCE(
                        NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Responsável pela Região de Destino]))), ''),
                        NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial Emissora]))), ''),
                        NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial]))), ''),
                        N'sem_responsavel'
                    )) AS responsavel_destino_key,
                    COALESCE(
                        NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Responsável pela Região de Destino]))), ''),
                        NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial Emissora]))), ''),
                        NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial]))), ''),
                        N'Responsável não informado'
                    ) AS responsavel_destino,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Classificação]))), '') AS classificacao_nome,
                    TRY_CONVERT(date, CONVERT(NVARCHAR(64), [Previsão de Entrega])) AS previsao_entrega,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Status]))), '') AS status,
                    LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Status]))), '')) AS status_normalizado,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Tipo Frete]))), '') AS tipo_frete,
                    LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Tipo Frete]))), '')) AS tipo_frete_normalizado,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Modal]))), '') AS modal,
                    LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(100), [Modal]))), '')) AS modal_normalizado,
                    TRY_CONVERT(INT, [Nº CT-e]) AS numero_cte,
                    TRY_CONVERT(datetimeoffset, CONVERT(NVARCHAR(64), [CT-e Emissão])) AS cte_emissao,
                    TRY_CONVERT(datetimeoffset, CONVERT(NVARCHAR(64), [data_referencia_faturamento])) AS data_referencia_faturamento,
                    TRY_CONVERT(date, CONVERT(NVARCHAR(64), [data_referencia_faturamento])) AS data_referencia_periodo,
                    CASE WHEN TRY_CONVERT(bit, [is_elegivel_faturamento]) = 1 THEN 1 ELSE 0 END AS elegivel_faturamento,
                    TRY_CONVERT(BIGINT, [CT-e ID]) AS cte_id,
                    TRY_CONVERT(INT, [Nº NFS-e]) AS nfse_numero,
                    TRY_CONVERT(DECIMAL(18, 2), [Valor ICMS]) AS valor_icms,
                    TRY_CONVERT(DECIMAL(18, 2), [Valor PIS]) AS valor_pis,
                    TRY_CONVERT(DECIMAL(18, 2), [Valor COFINS]) AS valor_cofins,
                    TRY_CONVERT(datetime2, CONVERT(NVARCHAR(64), [Data de extracao])) AS data_extracao,
                    LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Pagador]))), '')) AS pagador_normalizado,
                    LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(50), [UF Origem]))), '')) AS origem_uf_normalizada,
                    LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(50), [UF Destino]))), '')) AS destino_uf_normalizada
                FROM dbo.vw_fretes_powerbi
            ),
            filtrados AS (
                SELECT *
                FROM fretes
                WHERE data_referencia_periodo >= :dataInicio
                  AND data_referencia_periodo < :dataFimExclusivo
                  AND (:escopoFiliaisVazio = 1 OR filial_nome_normalizada IN (:escopoFiliais))
                  AND (:filiaisVazio = 1 OR filial_nome_normalizada IN (:filiais))
                  AND (:statusVazio = 1 OR status_normalizado IN (:status))
                  AND (:pagadoresVazio = 1 OR pagador_normalizado IN (:pagadores))
                  AND (:responsaveisVazio = 1 OR responsavel_destino_key IN (:responsaveis))
                  AND (:ufOrigemVazio = 1 OR origem_uf_normalizada IN (:ufOrigem))
                  AND (:ufDestinoVazio = 1 OR destino_uf_normalizada IN (:ufDestino))
                  AND (:tiposFreteVazio = 1 OR tipo_frete_normalizado IN (:tiposFrete))
                  AND (:modaisVazio = 1 OR modal_normalizado IN (:modais))
            )
            """;

    List<VisaoFretesEntity> findByDataFreteBetween(OffsetDateTime inicio, OffsetDateTime fim);

    @Query(value = FRETES_FILTRADOS_SQL + """
            SELECT
                MAX(data_extracao) AS updatedAt,
                CAST(COUNT_BIG(1) AS INT) AS totalFretes,
                COALESCE(SUM(CASE WHEN elegivel_faturamento = 1 THEN valor_total ELSE 0 END), 0) AS receitaBruta,
                COALESCE(SUM(CASE WHEN elegivel_faturamento = 1 THEN subtotal ELSE 0 END), 0) AS valorFrete,
                CAST(SUM(CASE WHEN elegivel_faturamento = 1 THEN 1 ELSE 0 END) AS INT) AS fretesFaturamento,
                COALESCE(SUM(peso_taxado), 0) AS pesoTaxadoTotal,
                COALESCE(SUM(volumes), 0) AS volumesTotais,
                CAST(SUM(CASE WHEN cte_id IS NOT NULL THEN 1 ELSE 0 END) AS INT) AS cteEmitidos,
                CAST(SUM(CASE WHEN nfse_numero IS NOT NULL THEN 1 ELSE 0 END) AS INT) AS nfseEmitidas,
                CAST(SUM(CASE
                    WHEN previsao_entrega IS NOT NULL
                     AND previsao_entrega < CAST(SYSDATETIME() AS date)
                     AND COALESCE(status_normalizado, '') <> N'finalizado'
                    THEN 1 ELSE 0 END) AS INT) AS fretesPrevisaoVencida
            FROM filtrados
            """, nativeQuery = true)
    FretesOverviewProjection buscarOverviewAgregado(
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFimExclusivo") LocalDate dataFimExclusivo,
            @Param("escopoFiliais") List<String> escopoFiliais,
            @Param("escopoFiliaisVazio") int escopoFiliaisVazio,
            @Param("filiais") List<String> filiais,
            @Param("filiaisVazio") int filiaisVazio,
            @Param("status") List<String> status,
            @Param("statusVazio") int statusVazio,
            @Param("pagadores") List<String> pagadores,
            @Param("pagadoresVazio") int pagadoresVazio,
            @Param("responsaveis") List<String> responsaveis,
            @Param("responsaveisVazio") int responsaveisVazio,
            @Param("ufOrigem") List<String> ufOrigem,
            @Param("ufOrigemVazio") int ufOrigemVazio,
            @Param("ufDestino") List<String> ufDestino,
            @Param("ufDestinoVazio") int ufDestinoVazio,
            @Param("tiposFrete") List<String> tiposFrete,
            @Param("tiposFreteVazio") int tiposFreteVazio,
            @Param("modais") List<String> modais,
            @Param("modaisVazio") int modaisVazio
    );

    @Query(value = FRETES_FILTRADOS_SQL + """
            SELECT
                CONVERT(VARCHAR(10), data_referencia_periodo, 23) AS date,
                COALESCE(SUM(valor_total), 0) AS receitaBruta,
                COALESCE(SUM(subtotal), 0) AS valorFrete,
                CAST(COUNT_BIG(1) AS INT) AS fretes
            FROM filtrados
            WHERE elegivel_faturamento = 1
              AND data_referencia_periodo IS NOT NULL
            GROUP BY data_referencia_periodo
            ORDER BY data_referencia_periodo
            """, nativeQuery = true)
    List<FretesTrendProjection> buscarSerieTemporalAgregada(
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFimExclusivo") LocalDate dataFimExclusivo,
            @Param("escopoFiliais") List<String> escopoFiliais,
            @Param("escopoFiliaisVazio") int escopoFiliaisVazio,
            @Param("filiais") List<String> filiais,
            @Param("filiaisVazio") int filiaisVazio,
            @Param("status") List<String> status,
            @Param("statusVazio") int statusVazio,
            @Param("pagadores") List<String> pagadores,
            @Param("pagadoresVazio") int pagadoresVazio,
            @Param("responsaveis") List<String> responsaveis,
            @Param("responsaveisVazio") int responsaveisVazio,
            @Param("ufOrigem") List<String> ufOrigem,
            @Param("ufOrigemVazio") int ufOrigemVazio,
            @Param("ufDestino") List<String> ufDestino,
            @Param("ufDestinoVazio") int ufDestinoVazio,
            @Param("tiposFrete") List<String> tiposFrete,
            @Param("tiposFreteVazio") int tiposFreteVazio,
            @Param("modais") List<String> modais,
            @Param("modaisVazio") int modaisVazio
    );

    @Query(value = FRETES_FILTRADOS_SQL + """
            SELECT
                pagador_nome AS cliente,
                COALESCE(SUM(valor_total), 0) AS receita,
                CAST(COUNT_BIG(1) AS INT) AS fretes,
                COALESCE(SUM(valor_total) / NULLIF(COUNT_BIG(1), 0), 0) AS ticketMedio
            FROM filtrados
            WHERE elegivel_faturamento = 1
              AND pagador_nome IS NOT NULL
            GROUP BY pagador_nome
            ORDER BY receita DESC, pagador_nome
            OFFSET 0 ROWS FETCH NEXT :limite ROWS ONLY
            """, nativeQuery = true)
    List<FretesClienteRankingProjection> buscarTopClientesAgregado(
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFimExclusivo") LocalDate dataFimExclusivo,
            @Param("escopoFiliais") List<String> escopoFiliais,
            @Param("escopoFiliaisVazio") int escopoFiliaisVazio,
            @Param("filiais") List<String> filiais,
            @Param("filiaisVazio") int filiaisVazio,
            @Param("status") List<String> status,
            @Param("statusVazio") int statusVazio,
            @Param("pagadores") List<String> pagadores,
            @Param("pagadoresVazio") int pagadoresVazio,
            @Param("responsaveis") List<String> responsaveis,
            @Param("responsaveisVazio") int responsaveisVazio,
            @Param("ufOrigem") List<String> ufOrigem,
            @Param("ufOrigemVazio") int ufOrigemVazio,
            @Param("ufDestino") List<String> ufDestino,
            @Param("ufDestinoVazio") int ufDestinoVazio,
            @Param("tiposFrete") List<String> tiposFrete,
            @Param("tiposFreteVazio") int tiposFreteVazio,
            @Param("modais") List<String> modais,
            @Param("modaisVazio") int modaisVazio,
            @Param("limite") int limite
    );

    @Query(value = FRETES_FILTRADOS_SQL + """
            SELECT
                CASE
                    WHEN cte_id IS NOT NULL THEN N'CT-e'
                    WHEN nfse_numero IS NOT NULL THEN N'NFS-e'
                    ELSE N'Pendente'
                END AS tipoDocumento,
                CAST(COUNT_BIG(1) AS INT) AS total
            FROM filtrados
            GROUP BY CASE
                    WHEN cte_id IS NOT NULL THEN N'CT-e'
                    WHEN nfse_numero IS NOT NULL THEN N'NFS-e'
                    ELSE N'Pendente'
                END
            ORDER BY MIN(CASE
                    WHEN cte_id IS NOT NULL THEN 1
                    WHEN nfse_numero IS NOT NULL THEN 2
                    ELSE 3
                END)
            """, nativeQuery = true)
    List<FretesDocumentMixProjection> buscarMixDocumentalAgregado(
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFimExclusivo") LocalDate dataFimExclusivo,
            @Param("escopoFiliais") List<String> escopoFiliais,
            @Param("escopoFiliaisVazio") int escopoFiliaisVazio,
            @Param("filiais") List<String> filiais,
            @Param("filiaisVazio") int filiaisVazio,
            @Param("status") List<String> status,
            @Param("statusVazio") int statusVazio,
            @Param("pagadores") List<String> pagadores,
            @Param("pagadoresVazio") int pagadoresVazio,
            @Param("responsaveis") List<String> responsaveis,
            @Param("responsaveisVazio") int responsaveisVazio,
            @Param("ufOrigem") List<String> ufOrigem,
            @Param("ufOrigemVazio") int ufOrigemVazio,
            @Param("ufDestino") List<String> ufDestino,
            @Param("ufDestinoVazio") int ufDestinoVazio,
            @Param("tiposFrete") List<String> tiposFrete,
            @Param("tiposFreteVazio") int tiposFreteVazio,
            @Param("modais") List<String> modais,
            @Param("modaisVazio") int modaisVazio
    );

    @Query(value = FRETES_FILTRADOS_SQL + """
            SELECT
                id,
                numero_minuta AS numeroMinuta,
                CONVERT(NVARCHAR(48), data_referencia_faturamento, 127) AS dataReferenciaFaturamento,
                CONVERT(NVARCHAR(48), cte_emissao, 127) AS cteEmissao,
                status,
                filial_nome AS filial,
                pagador_nome AS pagador,
                remetente_nome AS remetente,
                destinatario_nome AS destinatario,
                origem_uf AS origemUf,
                destino_uf AS destinoUf,
                COALESCE(valor_total, 0) AS valorTotalServico,
                COALESCE(subtotal, 0) AS valorFrete,
                COALESCE(peso_taxado, 0) AS pesoTaxado,
                volumes,
                previsao_entrega AS previsaoEntrega,
                CASE
                    WHEN cte_id IS NOT NULL THEN N'CT-e'
                    WHEN nfse_numero IS NOT NULL THEN N'NFS-e'
                    ELSE N'Pendente'
                END AS documentoTipo,
                numero_cte AS numeroCte,
                nfse_numero AS numeroNfse,
                COALESCE(valor_icms, 0) AS valorIcms,
                COALESCE(valor_pis, 0) AS valorPis,
                COALESCE(valor_cofins, 0) AS valorCofins
            FROM filtrados
            ORDER BY data_referencia_faturamento DESC, numero_minuta DESC, id DESC
            OFFSET 0 ROWS FETCH NEXT :limite ROWS ONLY
            """, nativeQuery = true)
    List<FretesTabelaProjection> buscarTabelaPaginada(
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFimExclusivo") LocalDate dataFimExclusivo,
            @Param("escopoFiliais") List<String> escopoFiliais,
            @Param("escopoFiliaisVazio") int escopoFiliaisVazio,
            @Param("filiais") List<String> filiais,
            @Param("filiaisVazio") int filiaisVazio,
            @Param("status") List<String> status,
            @Param("statusVazio") int statusVazio,
            @Param("pagadores") List<String> pagadores,
            @Param("pagadoresVazio") int pagadoresVazio,
            @Param("responsaveis") List<String> responsaveis,
            @Param("responsaveisVazio") int responsaveisVazio,
            @Param("ufOrigem") List<String> ufOrigem,
            @Param("ufOrigemVazio") int ufOrigemVazio,
            @Param("ufDestino") List<String> ufDestino,
            @Param("ufDestinoVazio") int ufDestinoVazio,
            @Param("tiposFrete") List<String> tiposFrete,
            @Param("tiposFreteVazio") int tiposFreteVazio,
            @Param("modais") List<String> modais,
            @Param("modaisVazio") int modaisVazio,
            @Param("limite") int limite
    );

    @Query(value = FRETES_FILTRADOS_SQL + """
            SELECT
                COALESCE(status, N'Sem status') AS status,
                CAST(SUM(CASE
                    WHEN previsao_entrega IS NOT NULL
                     AND previsao_entrega < CAST(SYSDATETIME() AS date)
                     AND COALESCE(status_normalizado, '') <> N'finalizado'
                    THEN 1 ELSE 0 END) AS INT) AS vencidos,
                CAST(SUM(CASE
                    WHEN previsao_entrega IS NULL
                      OR previsao_entrega >= CAST(SYSDATETIME() AS date)
                      OR COALESCE(status_normalizado, '') = N'finalizado'
                    THEN 1 ELSE 0 END) AS INT) AS noPrazo
            FROM filtrados
            GROUP BY COALESCE(status, N'Sem status')
            ORDER BY status
            """, nativeQuery = true)
    List<FretesPrevisaoStatusProjection> buscarPrevisaoPorStatusAgregada(
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFimExclusivo") LocalDate dataFimExclusivo,
            @Param("escopoFiliais") List<String> escopoFiliais,
            @Param("escopoFiliaisVazio") int escopoFiliaisVazio,
            @Param("filiais") List<String> filiais,
            @Param("filiaisVazio") int filiaisVazio,
            @Param("status") List<String> status,
            @Param("statusVazio") int statusVazio,
            @Param("pagadores") List<String> pagadores,
            @Param("pagadoresVazio") int pagadoresVazio,
            @Param("responsaveis") List<String> responsaveis,
            @Param("responsaveisVazio") int responsaveisVazio,
            @Param("ufOrigem") List<String> ufOrigem,
            @Param("ufOrigemVazio") int ufOrigemVazio,
            @Param("ufDestino") List<String> ufDestino,
            @Param("ufDestinoVazio") int ufDestinoVazio,
            @Param("tiposFrete") List<String> tiposFrete,
            @Param("tiposFreteVazio") int tiposFreteVazio,
            @Param("modais") List<String> modais,
            @Param("modaisVazio") int modaisVazio
    );

    @Query(value = FRETES_FILTRADOS_SQL + """
            SELECT
                COALESCE(origem_uf, N'N/A') AS origemUf,
                COALESCE(destino_uf, N'N/A') AS destinoUf,
                COALESCE(SUM(valor_total), 0) AS receita,
                CAST(COUNT_BIG(1) AS INT) AS fretes
            FROM filtrados
            WHERE elegivel_faturamento = 1
            GROUP BY COALESCE(origem_uf, N'N/A'), COALESCE(destino_uf, N'N/A')
            ORDER BY receita DESC, origemUf, destinoUf
            OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY
            """, nativeQuery = true)
    List<FretesRotaProjection> buscarTopRotasPorReceita(
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFimExclusivo") LocalDate dataFimExclusivo,
            @Param("escopoFiliais") List<String> escopoFiliais,
            @Param("escopoFiliaisVazio") int escopoFiliaisVazio,
            @Param("filiais") List<String> filiais,
            @Param("filiaisVazio") int filiaisVazio,
            @Param("status") List<String> status,
            @Param("statusVazio") int statusVazio,
            @Param("pagadores") List<String> pagadores,
            @Param("pagadoresVazio") int pagadoresVazio,
            @Param("responsaveis") List<String> responsaveis,
            @Param("responsaveisVazio") int responsaveisVazio,
            @Param("ufOrigem") List<String> ufOrigem,
            @Param("ufOrigemVazio") int ufOrigemVazio,
            @Param("ufDestino") List<String> ufDestino,
            @Param("ufDestinoVazio") int ufDestinoVazio,
            @Param("tiposFrete") List<String> tiposFrete,
            @Param("tiposFreteVazio") int tiposFreteVazio,
            @Param("modais") List<String> modais,
            @Param("modaisVazio") int modaisVazio
    );

    @Query(value = FRETES_FILTRADOS_SQL + """
            SELECT
                CASE
                    WHEN UPPER(COALESCE(classificacao_nome, N'')) LIKE N'%FTL%' THEN N'FTL'
                    WHEN UPPER(COALESCE(classificacao_nome, N'')) LIKE N'%LTL%' THEN N'LTL'
                    WHEN UPPER(COALESCE(classificacao_nome, N'')) LIKE N'%PTL%' THEN N'PTL'
                    ELSE COALESCE(classificacao_nome, N'Sem classificação')
                END AS nome,
                COALESCE(SUM(valor_total), 0) AS receita,
                CAST(COUNT_BIG(1) AS INT) AS fretes
            FROM filtrados
            WHERE elegivel_faturamento = 1
            GROUP BY CASE
                    WHEN UPPER(COALESCE(classificacao_nome, N'')) LIKE N'%FTL%' THEN N'FTL'
                    WHEN UPPER(COALESCE(classificacao_nome, N'')) LIKE N'%LTL%' THEN N'LTL'
                    WHEN UPPER(COALESCE(classificacao_nome, N'')) LIKE N'%PTL%' THEN N'PTL'
                    ELSE COALESCE(classificacao_nome, N'Sem classificação')
                END
            ORDER BY receita DESC, nome
            OFFSET 0 ROWS FETCH NEXT 8 ROWS ONLY
            """, nativeQuery = true)
    List<FretesFaturamentoGrupoProjection> buscarFaturamentoPorClassificacao(
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFimExclusivo") LocalDate dataFimExclusivo,
            @Param("escopoFiliais") List<String> escopoFiliais,
            @Param("escopoFiliaisVazio") int escopoFiliaisVazio,
            @Param("filiais") List<String> filiais,
            @Param("filiaisVazio") int filiaisVazio,
            @Param("status") List<String> status,
            @Param("statusVazio") int statusVazio,
            @Param("pagadores") List<String> pagadores,
            @Param("pagadoresVazio") int pagadoresVazio,
            @Param("responsaveis") List<String> responsaveis,
            @Param("responsaveisVazio") int responsaveisVazio,
            @Param("ufOrigem") List<String> ufOrigem,
            @Param("ufOrigemVazio") int ufOrigemVazio,
            @Param("ufDestino") List<String> ufDestino,
            @Param("ufDestinoVazio") int ufDestinoVazio,
            @Param("tiposFrete") List<String> tiposFrete,
            @Param("tiposFreteVazio") int tiposFreteVazio,
            @Param("modais") List<String> modais,
            @Param("modaisVazio") int modaisVazio
    );

    @Query(value = FRETES_FILTRADOS_SQL + """
            SELECT
                COALESCE(responsavel_regiao_destino, filial_emissora, filial_nome, N'Responsável não informado') AS nome,
                COALESCE(SUM(valor_total), 0) AS receita,
                CAST(COUNT_BIG(1) AS INT) AS fretes
            FROM filtrados
            WHERE elegivel_faturamento = 1
            GROUP BY COALESCE(responsavel_regiao_destino, filial_emissora, filial_nome, N'Responsável não informado')
            ORDER BY receita DESC, nome
            OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY
            """, nativeQuery = true)
    List<FretesFaturamentoGrupoProjection> buscarFaturamentoPorResponsavelDestino(
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFimExclusivo") LocalDate dataFimExclusivo,
            @Param("escopoFiliais") List<String> escopoFiliais,
            @Param("escopoFiliaisVazio") int escopoFiliaisVazio,
            @Param("filiais") List<String> filiais,
            @Param("filiaisVazio") int filiaisVazio,
            @Param("status") List<String> status,
            @Param("statusVazio") int statusVazio,
            @Param("pagadores") List<String> pagadores,
            @Param("pagadoresVazio") int pagadoresVazio,
            @Param("responsaveis") List<String> responsaveis,
            @Param("responsaveisVazio") int responsaveisVazio,
            @Param("ufOrigem") List<String> ufOrigem,
            @Param("ufOrigemVazio") int ufOrigemVazio,
            @Param("ufDestino") List<String> ufDestino,
            @Param("ufDestinoVazio") int ufDestinoVazio,
            @Param("tiposFrete") List<String> tiposFrete,
            @Param("tiposFreteVazio") int tiposFreteVazio,
            @Param("modais") List<String> modais,
            @Param("modaisVazio") int modaisVazio
    );

    @Query(value = FRETES_FILTRADOS_SQL + """
            SELECT
                COALESCE(origem_uf, N'UF não informada') AS nome,
                COALESCE(SUM(valor_total), 0) AS receita,
                CAST(COUNT_BIG(1) AS INT) AS fretes
            FROM filtrados
            WHERE elegivel_faturamento = 1
            GROUP BY COALESCE(origem_uf, N'UF não informada')
            ORDER BY receita DESC, nome
            OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY
            """, nativeQuery = true)
    List<FretesFaturamentoGrupoProjection> buscarFaturamentoPorUfOrigem(
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFimExclusivo") LocalDate dataFimExclusivo,
            @Param("escopoFiliais") List<String> escopoFiliais,
            @Param("escopoFiliaisVazio") int escopoFiliaisVazio,
            @Param("filiais") List<String> filiais,
            @Param("filiaisVazio") int filiaisVazio,
            @Param("status") List<String> status,
            @Param("statusVazio") int statusVazio,
            @Param("pagadores") List<String> pagadores,
            @Param("pagadoresVazio") int pagadoresVazio,
            @Param("responsaveis") List<String> responsaveis,
            @Param("responsaveisVazio") int responsaveisVazio,
            @Param("ufOrigem") List<String> ufOrigem,
            @Param("ufOrigemVazio") int ufOrigemVazio,
            @Param("ufDestino") List<String> ufDestino,
            @Param("ufDestinoVazio") int ufDestinoVazio,
            @Param("tiposFrete") List<String> tiposFrete,
            @Param("tiposFreteVazio") int tiposFreteVazio,
            @Param("modais") List<String> modais,
            @Param("modaisVazio") int modaisVazio
    );

    @Query(value = FRETES_FILTRADOS_SQL + """
            SELECT
                COALESCE(destino_uf, N'UF não informada') AS nome,
                COALESCE(SUM(valor_total), 0) AS receita,
                CAST(COUNT_BIG(1) AS INT) AS fretes
            FROM filtrados
            WHERE elegivel_faturamento = 1
            GROUP BY COALESCE(destino_uf, N'UF não informada')
            ORDER BY receita DESC, nome
            OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY
            """, nativeQuery = true)
    List<FretesFaturamentoGrupoProjection> buscarFaturamentoPorUfDestino(
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFimExclusivo") LocalDate dataFimExclusivo,
            @Param("escopoFiliais") List<String> escopoFiliais,
            @Param("escopoFiliaisVazio") int escopoFiliaisVazio,
            @Param("filiais") List<String> filiais,
            @Param("filiaisVazio") int filiaisVazio,
            @Param("status") List<String> status,
            @Param("statusVazio") int statusVazio,
            @Param("pagadores") List<String> pagadores,
            @Param("pagadoresVazio") int pagadoresVazio,
            @Param("responsaveis") List<String> responsaveis,
            @Param("responsaveisVazio") int responsaveisVazio,
            @Param("ufOrigem") List<String> ufOrigem,
            @Param("ufOrigemVazio") int ufOrigemVazio,
            @Param("ufDestino") List<String> ufDestino,
            @Param("ufDestinoVazio") int ufDestinoVazio,
            @Param("tiposFrete") List<String> tiposFrete,
            @Param("tiposFreteVazio") int tiposFreteVazio,
            @Param("modais") List<String> modais,
            @Param("modaisVazio") int modaisVazio
    );

    @Query(value = FRETES_FILTRADOS_SQL + """
            SELECT
                COALESCE(destino_cidade, N'Cidade não informada') AS nome,
                COALESCE(SUM(valor_total), 0) AS receita,
                CAST(COUNT_BIG(1) AS INT) AS fretes
            FROM filtrados
            WHERE elegivel_faturamento = 1
            GROUP BY COALESCE(destino_cidade, N'Cidade não informada')
            ORDER BY receita DESC, nome
            OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY
            """, nativeQuery = true)
    List<FretesFaturamentoGrupoProjection> buscarFaturamentoPorCidadeDestino(
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFimExclusivo") LocalDate dataFimExclusivo,
            @Param("escopoFiliais") List<String> escopoFiliais,
            @Param("escopoFiliaisVazio") int escopoFiliaisVazio,
            @Param("filiais") List<String> filiais,
            @Param("filiaisVazio") int filiaisVazio,
            @Param("status") List<String> status,
            @Param("statusVazio") int statusVazio,
            @Param("pagadores") List<String> pagadores,
            @Param("pagadoresVazio") int pagadoresVazio,
            @Param("responsaveis") List<String> responsaveis,
            @Param("responsaveisVazio") int responsaveisVazio,
            @Param("ufOrigem") List<String> ufOrigem,
            @Param("ufOrigemVazio") int ufOrigemVazio,
            @Param("ufDestino") List<String> ufDestino,
            @Param("ufDestinoVazio") int ufDestinoVazio,
            @Param("tiposFrete") List<String> tiposFrete,
            @Param("tiposFreteVazio") int tiposFreteVazio,
            @Param("modais") List<String> modais,
            @Param("modaisVazio") int modaisVazio
    );

    @Query(value = FRETES_FILTRADOS_SQL + """
            SELECT
                COALESCE(filial_nome, N'Filial não informada') AS filial,
                COALESCE(SUM(valor_total), 0) AS realizadoFaturamento
            FROM filtrados
            WHERE elegivel_faturamento = 1
            GROUP BY COALESCE(filial_nome, N'Filial não informada')
            ORDER BY filial
            """, nativeQuery = true)
    List<FretesRealizadoFilialProjection> buscarRealizadoFaturamentoPorFilial(
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFimExclusivo") LocalDate dataFimExclusivo,
            @Param("escopoFiliais") List<String> escopoFiliais,
            @Param("escopoFiliaisVazio") int escopoFiliaisVazio,
            @Param("filiais") List<String> filiais,
            @Param("filiaisVazio") int filiaisVazio,
            @Param("status") List<String> status,
            @Param("statusVazio") int statusVazio,
            @Param("pagadores") List<String> pagadores,
            @Param("pagadoresVazio") int pagadoresVazio,
            @Param("responsaveis") List<String> responsaveis,
            @Param("responsaveisVazio") int responsaveisVazio,
            @Param("ufOrigem") List<String> ufOrigem,
            @Param("ufOrigemVazio") int ufOrigemVazio,
            @Param("ufDestino") List<String> ufDestino,
            @Param("ufDestinoVazio") int ufDestinoVazio,
            @Param("tiposFrete") List<String> tiposFrete,
            @Param("tiposFreteVazio") int tiposFreteVazio,
            @Param("modais") List<String> modais,
            @Param("modaisVazio") int modaisVazio
    );



    @Query(value = """
            SELECT nome, MIN(documento) AS documento
            FROM (
                SELECT
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Pagador]))), '') AS nome,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Pagador Doc]))), '') AS documento,
                    LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Pagador]))), '')) AS nome_normalizado,
                    LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Pagador Doc]))), '')) AS documento_normalizado,
                    LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial]))), '')) AS filial
                FROM dbo.vw_fretes_powerbi
            ) pagadores
            WHERE nome IS NOT NULL
              AND (:escopoFiliaisVazio = 1 OR filial IN (:escopoFiliais))
              AND (:buscaVazia = 1 OR nome_normalizado LIKE :buscaPrefixo OR documento_normalizado LIKE :buscaPrefixo)
            GROUP BY nome
            ORDER BY nome
            OFFSET 0 ROWS FETCH NEXT :limite ROWS ONLY
            """, nativeQuery = true)
    List<PagadorDimProjection> buscarPagadores(
            @Param("buscaPrefixo") String buscaPrefixo,
            @Param("buscaVazia") int buscaVazia,
            @Param("escopoFiliais") List<String> escopoFiliais,
            @Param("escopoFiliaisVazio") int escopoFiliaisVazio,
            @Param("limite") int limite
    );

    @Query(value = """
            SELECT DISTINCT cliente
            FROM (
                SELECT NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Pagador]))), '') AS cliente
                FROM dbo.vw_fretes_powerbi
                UNION ALL
                SELECT NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Remetente]))), '') AS cliente
                FROM dbo.vw_fretes_powerbi
                UNION ALL
                SELECT NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Destinatario]))), '') AS cliente
                FROM dbo.vw_fretes_powerbi
            ) clientes
            WHERE cliente IS NOT NULL
            ORDER BY cliente
            """, nativeQuery = true)
    List<String> findDistinctClientes();

    @Query(value = """
            SELECT DISTINCT cliente
            FROM (
                SELECT
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Pagador]))), '') AS cliente,
                    LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial]))), '')) AS filial
                FROM dbo.vw_fretes_powerbi
                UNION ALL
                SELECT
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Remetente]))), '') AS cliente,
                    LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial]))), '')) AS filial
                FROM dbo.vw_fretes_powerbi
                UNION ALL
                SELECT
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Destinatario]))), '') AS cliente,
                    LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial]))), '')) AS filial
                FROM dbo.vw_fretes_powerbi
            ) clientes
            WHERE cliente IS NOT NULL
              AND filial IN (:filiais)
            ORDER BY cliente
            """, nativeQuery = true)
    List<String> findDistinctClientesByFilialIn(@Param("filiais") List<String> filiais);

    interface FretesOverviewProjection {
        LocalDateTime getUpdatedAt();

        int getTotalFretes();

        BigDecimal getReceitaBruta();

        BigDecimal getValorFrete();

        int getFretesFaturamento();

        BigDecimal getPesoTaxadoTotal();

        int getVolumesTotais();

        int getCteEmitidos();

        int getNfseEmitidas();

        int getFretesPrevisaoVencida();
    }

    interface FretesTrendProjection {
        String getDate();

        BigDecimal getReceitaBruta();

        BigDecimal getValorFrete();

        int getFretes();
    }

    interface FretesClienteRankingProjection {
        String getCliente();

        BigDecimal getReceita();

        int getFretes();

        BigDecimal getTicketMedio();
    }

    interface FretesDocumentMixProjection {
        String getTipoDocumento();

        int getTotal();
    }

    interface FretesTabelaProjection {
        Long getId();

        Long getNumeroMinuta();

        String getDataReferenciaFaturamento();

        String getCteEmissao();

        String getStatus();

        String getFilial();

        String getPagador();

        String getRemetente();

        String getDestinatario();

        String getOrigemUf();

        String getDestinoUf();

        BigDecimal getValorTotalServico();

        BigDecimal getValorFrete();

        BigDecimal getPesoTaxado();

        Integer getVolumes();

        LocalDate getPrevisaoEntrega();

        String getDocumentoTipo();

        Integer getNumeroCte();

        Integer getNumeroNfse();

        BigDecimal getValorIcms();

        BigDecimal getValorPis();

        BigDecimal getValorCofins();
    }

    interface FretesPrevisaoStatusProjection {
        String getStatus();

        int getVencidos();

        int getNoPrazo();
    }

    interface FretesRotaProjection {
        String getOrigemUf();

        String getDestinoUf();

        BigDecimal getReceita();

        int getFretes();
    }

    interface FretesFaturamentoGrupoProjection {
        String getNome();

        BigDecimal getReceita();

        int getFretes();
    }

    interface FretesRealizadoFilialProjection {
        String getFilial();

        BigDecimal getRealizadoFaturamento();
    }

    interface PagadorDimProjection {
        String getNome();

        String getDocumento();
    }
}
