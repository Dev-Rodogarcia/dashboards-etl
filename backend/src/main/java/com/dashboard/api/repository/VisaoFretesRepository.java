package com.dashboard.api.repository;

import com.dashboard.api.model.VisaoFretesEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisaoFretesRepository extends JpaRepository<VisaoFretesEntity, Long>,
        JpaSpecificationExecutor<VisaoFretesEntity> {

    String FRETES_FILTRADOS_SQL = """
            WITH fretes AS (
                SELECT
                    frete_id AS id,
                    data_frete,
                    numero_minuta,
                    receita_bruta AS valor_total,
                    valor_frete AS subtotal,
                    volumes,
                    peso_taxado,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), pagador_nome))), '') AS pagador_nome,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), remetente_nome))), '') AS remetente_nome,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), destinatario_nome))), '') AS destinatario_nome,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(50), origem_uf))), '') AS origem_uf,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(50), destino_uf))), '') AS destino_uf,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), destino_cidade))), '') AS destino_cidade,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), filial_nome))), '') AS filial_nome,
                    LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), filial_nome))), '')) AS filial_nome_normalizada,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), filial_nome))), '') AS filial_emissora,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), responsavel_regiao_destino))), '') AS responsavel_regiao_destino,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), responsavel_regiao_destino_key))), '') AS responsavel_destino_key,
                    COALESCE(
                        NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), responsavel_regiao_destino))), ''),
                        NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), filial_nome))), ''),
                        N'Responsável não informado'
                    ) AS responsavel_destino,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), classificacao_nome))), '') AS classificacao_nome,
                    CAST(NULL AS date) AS previsao_entrega,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(100), status_frete))), '') AS status,
                    LOWER(COALESCE(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(100), status_frete_norm))), ''),
                                   NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(100), status_frete))), ''))) AS status_normalizado,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(100), tipo_frete))), '') AS tipo_frete,
                    LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(100), tipo_frete))), '')) AS tipo_frete_normalizado,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(100), modal))), '') AS modal,
                    LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(100), modal))), '')) AS modal_normalizado,
                    numero_cte,
                    data_emissao_cte AS cte_emissao,
                    data_referencia_faturamento,
                    data_referencia_faturamento_date AS data_referencia_periodo,
                    CONVERT(INT, is_elegivel_faturamento) AS elegivel_faturamento,
                    cte_id,
                    nfse_number AS nfse_numero,
                    CAST(0 AS DECIMAL(18, 2)) AS valor_icms,
                    CAST(0 AS DECIMAL(18, 2)) AS valor_pis,
                    CAST(0 AS DECIMAL(18, 2)) AS valor_cofins,
                    snapshot_em AS data_extracao,
                    LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), pagador_nome))), '')) AS pagador_normalizado,
                    LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(50), origem_uf))), '')) AS origem_uf_normalizada,
                    LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(50), destino_uf))), '')) AS destino_uf_normalizada
                FROM dbo.fato_fretes_faturamento
                WHERE excluido_na_origem = 0
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

    @Query(value = """
            SELECT MAX(data)
            FROM dbo.dim_calendario
            WHERE data < :dataReferencia
              AND is_dia_util = 1
            """, nativeQuery = true)
    LocalDate buscarUltimoDiaUtilFechado(@Param("dataReferencia") LocalDate dataReferencia);

    @Query(value = """
            SELECT CAST(COUNT_BIG(1) AS INT)
            FROM dbo.dim_calendario
            WHERE data >= :dataInicio
              AND data <= :dataFim
              AND is_dia_util = 1
            """, nativeQuery = true)
    Integer contarDiasUteisCalendario(
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim
    );

    @Query(value = FRETES_FILTRADOS_SQL + """
            SELECT
                MAX(data_extracao) AS updatedAt,
                CAST(COUNT_BIG(1) AS INT) AS totalFretes,
                COALESCE(SUM(valor_total), 0) AS receitaBruta,
                COALESCE(SUM(subtotal), 0) AS valorFrete,
                CAST(COALESCE(SUM(elegivel_faturamento), 0) AS INT) AS fretesFaturamento,
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
                CAST(COALESCE(SUM(elegivel_faturamento), 0) AS INT) AS fretes
            FROM filtrados
            WHERE data_referencia_periodo IS NOT NULL
            GROUP BY data_referencia_periodo
            HAVING COALESCE(SUM(valor_total), 0) <> 0
                OR COALESCE(SUM(subtotal), 0) <> 0
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
                CAST(COALESCE(SUM(elegivel_faturamento), 0) AS INT) AS fretes,
                COALESCE(SUM(valor_total) / NULLIF(SUM(elegivel_faturamento), 0), 0) AS ticketMedio
            FROM filtrados
            WHERE pagador_nome IS NOT NULL
            GROUP BY pagador_nome
            HAVING COALESCE(SUM(valor_total), 0) <> 0
                OR COALESCE(SUM(subtotal), 0) <> 0
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
                CAST(COALESCE(SUM(elegivel_faturamento), 0) AS INT) AS fretes
            FROM filtrados
            GROUP BY COALESCE(origem_uf, N'N/A'), COALESCE(destino_uf, N'N/A')
            HAVING COALESCE(SUM(valor_total), 0) <> 0
                OR COALESCE(SUM(subtotal), 0) <> 0
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
                CAST(COALESCE(SUM(elegivel_faturamento), 0) AS INT) AS fretes
            FROM filtrados
            GROUP BY CASE
                    WHEN UPPER(COALESCE(classificacao_nome, N'')) LIKE N'%FTL%' THEN N'FTL'
                    WHEN UPPER(COALESCE(classificacao_nome, N'')) LIKE N'%LTL%' THEN N'LTL'
                    WHEN UPPER(COALESCE(classificacao_nome, N'')) LIKE N'%PTL%' THEN N'PTL'
                    ELSE COALESCE(classificacao_nome, N'Sem classificação')
                END
            HAVING COALESCE(SUM(valor_total), 0) <> 0
                OR COALESCE(SUM(subtotal), 0) <> 0
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
                CAST(COALESCE(SUM(elegivel_faturamento), 0) AS INT) AS fretes
            FROM filtrados
            GROUP BY COALESCE(responsavel_regiao_destino, filial_emissora, filial_nome, N'Responsável não informado')
            HAVING COALESCE(SUM(valor_total), 0) <> 0
                OR COALESCE(SUM(subtotal), 0) <> 0
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
                CAST(COALESCE(SUM(elegivel_faturamento), 0) AS INT) AS fretes
            FROM filtrados
            GROUP BY COALESCE(origem_uf, N'UF não informada')
            HAVING COALESCE(SUM(valor_total), 0) <> 0
                OR COALESCE(SUM(subtotal), 0) <> 0
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
                CAST(COALESCE(SUM(elegivel_faturamento), 0) AS INT) AS fretes
            FROM filtrados
            GROUP BY COALESCE(destino_uf, N'UF não informada')
            HAVING COALESCE(SUM(valor_total), 0) <> 0
                OR COALESCE(SUM(subtotal), 0) <> 0
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
                CAST(COALESCE(SUM(elegivel_faturamento), 0) AS INT) AS fretes
            FROM filtrados
            GROUP BY COALESCE(destino_cidade, N'Cidade não informada')
            HAVING COALESCE(SUM(valor_total), 0) <> 0
                OR COALESCE(SUM(subtotal), 0) <> 0
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
            GROUP BY COALESCE(filial_nome, N'Filial não informada')
            HAVING COALESCE(SUM(valor_total), 0) <> 0
                OR COALESCE(SUM(subtotal), 0) <> 0
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
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), pagador_nome))), '') AS nome,
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), pagador_documento))), '') AS documento,
                    LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), pagador_nome))), '')) AS nome_normalizado,
                    LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), pagador_documento))), '')) AS documento_normalizado,
                    LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), filial_nome))), '')) AS filial
                FROM dbo.fato_fretes_faturamento
                WHERE excluido_na_origem = 0
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
                SELECT NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), pagador_nome))), '') AS cliente
                FROM dbo.fato_fretes_faturamento
                WHERE excluido_na_origem = 0
                UNION ALL
                SELECT NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), remetente_nome))), '') AS cliente
                FROM dbo.fato_fretes_faturamento
                WHERE excluido_na_origem = 0
                UNION ALL
                SELECT NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), destinatario_nome))), '') AS cliente
                FROM dbo.fato_fretes_faturamento
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
                    LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), filial_nome))), '')) AS filial
                FROM dbo.fato_fretes_faturamento
                WHERE excluido_na_origem = 0
                UNION ALL
                SELECT
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), remetente_nome))), '') AS cliente,
                    LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), filial_nome))), '')) AS filial
                FROM dbo.fato_fretes_faturamento
                WHERE excluido_na_origem = 0
                UNION ALL
                SELECT
                    NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), destinatario_nome))), '') AS cliente,
                    LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), filial_nome))), '')) AS filial
                FROM dbo.fato_fretes_faturamento
                WHERE excluido_na_origem = 0
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
