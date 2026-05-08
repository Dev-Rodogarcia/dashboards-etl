package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
class DashboardExportSqlBuilder {

    private final PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper;

    DashboardExportSqlBuilder(PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper) {
        this.periodoOffsetDateTimeHelper = periodoOffsetDateTimeHelper;
    }

    ExportSql buildSelect(
            DashboardExportDefinition definition,
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo,
            Set<String> filtrosIgnorados
    ) {
        SqlParts parts = buildBase(definition, filtro, escopo, filtrosIgnorados);
        String orderBy = definition.orderBy().isEmpty() ? "" : " ORDER BY " + String.join(", ", definition.orderBy());

        if (definition.dedupConfig() == null) {
            return new ExportSql("SELECT * FROM " + definition.viewName() + " WHERE " + parts.where() + orderBy, parts.params());
        }

        DashboardExportDefinition.DedupConfig dedup = definition.dedupConfig();
        String dedupOrderBy = dedup.orderBy().isEmpty() ? definition.dateColumn() + " DESC" : String.join(", ", dedup.orderBy());
        String sql = """
                SELECT *
                FROM (
                    SELECT base.*, ROW_NUMBER() OVER (PARTITION BY %s ORDER BY %s) AS [__rn]
                    FROM %s base
                    WHERE %s
                ) exportacao
                WHERE [__rn] = 1%s
                """.formatted(dedup.partitionBy(), dedupOrderBy, definition.viewName(), parts.where(), orderBy);
        return new ExportSql(Objects.requireNonNull(sql, "sql"), parts.params());
    }

    ExportSql buildCount(
            DashboardExportDefinition definition,
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo,
            Set<String> filtrosIgnorados
    ) {
        SqlParts parts = buildBase(definition, filtro, escopo, filtrosIgnorados);

        if (definition.dedupConfig() == null) {
            return new ExportSql("SELECT COUNT(1) FROM " + definition.viewName() + " WHERE " + parts.where(), parts.params());
        }

        DashboardExportDefinition.DedupConfig dedup = definition.dedupConfig();
        String dedupOrderBy = dedup.orderBy().isEmpty() ? definition.dateColumn() + " DESC" : String.join(", ", dedup.orderBy());
        String sql = """
                SELECT COUNT(1)
                FROM (
                    SELECT ROW_NUMBER() OVER (PARTITION BY %s ORDER BY %s) AS [__rn]
                    FROM %s base
                    WHERE %s
                ) exportacao
                WHERE [__rn] = 1
                """.formatted(dedup.partitionBy(), dedupOrderBy, definition.viewName(), parts.where());
        return new ExportSql(Objects.requireNonNull(sql, "sql"), parts.params());
    }

    ExportSql buildDistinct(
            DashboardExportDefinition definition,
            String coluna,
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo,
            Set<String> filtrosIgnorados
    ) {
        SqlParts parts = buildBase(definition, filtro, escopo, filtrosIgnorados);
        String sql = """
                SELECT DISTINCT %s AS valor
                FROM %s
                WHERE %s
                  AND %s IS NOT NULL
                  AND LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), %s))) <> ''
                ORDER BY valor
                """.formatted(coluna, definition.viewName(), parts.where(), coluna, coluna);
        return new ExportSql(Objects.requireNonNull(sql, "sql"), parts.params());
    }

    private SqlParts buildBase(
            DashboardExportDefinition definition,
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo,
            Set<String> filtrosIgnorados
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        List<String> where = new ArrayList<>();

        adicionarPeriodo(where, params, definition, filtro.dataInicio(), filtro.dataFim());
        adicionarEscopo(where, params, definition.escopoColumns(), escopo);
        adicionarFiltros(where, params, definition.filtros(), filtro, filtrosIgnorados);
        adicionarStatusProcesso(where, filtro, definition, filtrosIgnorados);

        String whereSql = Objects.requireNonNull(where.isEmpty() ? "1 = 1" : String.join(" AND ", where), "where");
        return new SqlParts(whereSql, params);
    }

    private void adicionarPeriodo(
            List<String> where,
            MapSqlParameterSource params,
            DashboardExportDefinition definition,
            LocalDate dataInicio,
            LocalDate dataFim
    ) {
        if (definition.dateMode() == DashboardExportDefinition.DateMode.LOCAL_DATE) {
            String colunaData = "TRY_CONVERT(date, " + definition.dateColumn() + ")";
            where.add(colunaData + " BETWEEN :dataInicio AND :dataFim");
            params.addValue("dataInicio", dataInicio);
            params.addValue("dataFim", dataFim);
            return;
        }

        JanelaOffsetDateTime janela = periodoOffsetDateTimeHelper.criarJanela(dataInicio, dataFim);
        String colunaData = "TRY_CONVERT(datetimeoffset, " + definition.dateColumn() + ")";
        where.add(colunaData + " >= :inicioOffset AND " + colunaData + " < :fimOffset");
        params.addValue("inicioOffset", janela.inicioInclusivo());
        params.addValue("fimOffset", janela.fimExclusivo());
    }

    private void adicionarEscopo(
            List<String> where,
            MapSqlParameterSource params,
            List<String> colunas,
            EscopoFilialService.EscopoFilial escopo
    ) {
        if (colunas.isEmpty() || escopo.acessoTotal()) {
            return;
        }

        List<String> filiais = normalizar(escopo.filiaisOrdenadas());
        if (filiais.isEmpty()) {
            where.add("1 = 0");
            return;
        }

        params.addValue("escopoFiliais", filiais);
        where.add("(" + String.join(" OR ", colunas.stream()
                .map(coluna -> normalizarSql(coluna) + " IN (:escopoFiliais)")
                .toList()) + ")");
    }

    private void adicionarFiltros(
            List<String> where,
            MapSqlParameterSource params,
            Map<String, List<String>> filtrosDefinidos,
            FiltroConsultaDTO filtro,
            Set<String> filtrosIgnorados
    ) {
        for (Map.Entry<String, List<String>> entry : filtrosDefinidos.entrySet()) {
            String chave = entry.getKey();
            if (filtrosIgnorados.contains(chave) || !filtro.temFiltro(chave)) {
                continue;
            }

            List<String> valores = normalizar(filtro.valores(chave));
            if (valores.isEmpty()) {
                continue;
            }

            String paramName = "filtro_" + chave;
            params.addValue(paramName, valores);
            where.add("(" + String.join(" OR ", entry.getValue().stream()
                    .map(coluna -> normalizarSql(coluna) + " IN (:" + paramName + ")")
                    .toList()) + ")");
        }
    }

    private void adicionarStatusProcesso(
            List<String> where,
            FiltroConsultaDTO filtro,
            DashboardExportDefinition definition,
            Set<String> filtrosIgnorados
    ) {
        if (!definition.temFiltroStatusProcesso()
                || filtrosIgnorados.contains("statusProcesso")
                || !filtro.temFiltro("statusProcesso")) {
            return;
        }

        List<String> valores = normalizar(filtro.valores("statusProcesso"));
        List<String> permitidos = new ArrayList<>();
        String documentoNormalizado = normalizarSql("[Fatura/N° Documento]");
        String documentoPreenchido = "([Fatura/N° Documento] IS NOT NULL AND LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Fatura/N° Documento]))) <> '')";
        String statusFaturado = documentoNormalizado + " = 'faturado'";
        String statusAguardando = documentoNormalizado + " = 'aguardando faturamento'";
        String documentoNaoEhStatus = "(" + documentoNormalizado + " NOT IN ('faturado', 'aguardando faturamento'))";

        if (valores.contains("faturado")) {
            permitidos.add("(" + statusFaturado + " OR (" + documentoPreenchido + " AND " + documentoNaoEhStatus + "))");
        }
        if (valores.contains("aguardando faturamento")) {
            permitidos.add("(" + statusAguardando + " OR [Fatura/N° Documento] IS NULL OR LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Fatura/N° Documento]))) = '')");
        }
        where.add(permitidos.isEmpty() ? "1 = 0" : "(" + String.join(" OR ", permitidos) + ")");
    }

    private String normalizarSql(String coluna) {
        return "LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), " + coluna + "))))";
    }

    private List<String> normalizar(Collection<String> valores) {
        return valores.stream()
                .filter(valor -> valor != null && !valor.isBlank())
                .map(valor -> valor.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    record ExportSql(@NonNull String sql, @NonNull MapSqlParameterSource params) {
    }

    private record SqlParts(@NonNull String where, @NonNull MapSqlParameterSource params) {
    }
}
