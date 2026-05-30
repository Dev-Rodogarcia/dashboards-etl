package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
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
            return new ExportSql("SELECT * FROM " + parts.sourceSql() + " base WHERE " + parts.where() + orderBy, parts.params());
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
                """.formatted(dedup.partitionBy(), dedupOrderBy, parts.sourceSql(), parts.where(), orderBy);
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
            return new ExportSql("SELECT COUNT(1) FROM " + parts.sourceSql() + " base WHERE " + parts.where(), parts.params());
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
                """.formatted(dedup.partitionBy(), dedupOrderBy, parts.sourceSql(), parts.where());
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
                FROM %s base
                WHERE %s
                  AND %s IS NOT NULL
                  AND LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), %s))) <> ''
                ORDER BY valor
                """.formatted(coluna, parts.sourceSql(), parts.where(), coluna, coluna);
        return new ExportSql(Objects.requireNonNull(sql, "sql"), parts.params());
    }

    ExportSql buildDistinctOptions(
            DashboardExportDefinition definition,
            String valueExpression,
            String labelExpression,
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo,
            Set<String> filtrosIgnorados
    ) {
        SqlParts parts = buildBase(definition, filtro, escopo, filtrosIgnorados);
        String sql = """
                SELECT
                    value,
                    MIN(label) AS label
                FROM (
                    SELECT
                        %s AS value,
                        %s AS label
                    FROM %s base
                    WHERE %s
                ) opcoes
                WHERE value IS NOT NULL
                  AND LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), value))) <> ''
                  AND label IS NOT NULL
                  AND LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), label))) <> ''
                GROUP BY value
                ORDER BY label
                """.formatted(
                valueExpression,
                labelExpression,
                parts.sourceSql(),
                parts.where()
        );
        return new ExportSql(Objects.requireNonNull(sql, "sql"), parts.params());
    }

    ExportSql buildFilteredSource(
            DashboardExportDefinition definition,
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo,
            Set<String> filtrosIgnorados
    ) {
        SqlParts parts = buildBase(definition, filtro, escopo, filtrosIgnorados);
        return new ExportSql("FROM " + parts.sourceSql() + " base WHERE " + parts.where(), parts.params());
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
        adicionarEscopo(where, params, definition, escopo);
        adicionarFiltros(where, params, definition, filtro, filtrosIgnorados);
        adicionarStatusProcesso(where, filtro, definition, filtrosIgnorados);
        adicionarFiltrosTabela(where, params, definition, filtro);

        String whereSql = Objects.requireNonNull(where.isEmpty() ? "1 = 1" : String.join(" AND ", where), "where");
        String sourceSql = definition == DashboardExportDefinition.TRACKING
                ? aplicarFiltrosBaseTracking(definition.viewName(), params, filtro, escopo, filtrosIgnorados)
                : definition.viewName();
        return new SqlParts(whereSql, params, sourceSql);
    }

    private String aplicarFiltrosBaseTracking(
            String sourceSql,
            MapSqlParameterSource params,
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo,
            Set<String> filtrosIgnorados
    ) {
        List<String> whereBase = new ArrayList<>();
        whereBase.add("base_raw.[Data do frete] >= :inicioOffset AND base_raw.[Data do frete] < :fimOffset");

        if (!escopo.acessoTotal() && !escopo.filiaisOrdenadas().isEmpty()) {
            String predicadoEscopo = predicadoFilialBaseTracking(
                    "escopoFiliais",
                    "escopoFiliaisCodigos",
                    List.of(
                            "base_raw.[Filial Emissora]",
                            "base_raw.[Filial Origem]",
                            "base_raw.[Filial Atual]",
                            "base_raw.[Localização Atual]",
                            "base_raw.[Filial Destino]"
                    ),
                    params
            );
            if (predicadoEscopo != null) {
                whereBase.add(predicadoEscopo);
            }
        }

        adicionarFiltroBaseTracking(whereBase, params, filtro, filtrosIgnorados, "filialEmissora", "filtro_filialEmissora", "filtro_filialEmissoraCodigos", List.of("base_raw.[Filial Emissora]"), true);
        adicionarFiltroBaseTracking(whereBase, params, filtro, filtrosIgnorados, "filialAtual", "filtro_filialAtual", "filtro_filialAtualCodigos", List.of("base_raw.[Filial Atual]", "base_raw.[Localização Atual]", "base_raw.[Filial Emissora]"), true);
        adicionarFiltroBaseTracking(whereBase, params, filtro, filtrosIgnorados, "filialDestino", "filtro_filialDestino", "filtro_filialDestinoCodigos", List.of("base_raw.[Filial Destino]"), true);
        adicionarFiltroBaseTracking(whereBase, params, filtro, filtrosIgnorados, "regiaoOrigem", "filtro_regiaoOrigem", null, List.of("base_raw.[Região Origem]"), false);
        adicionarFiltroBaseTracking(whereBase, params, filtro, filtrosIgnorados, "regiaoDestino", "filtro_regiaoDestino", null, List.of("base_raw.[Região Destino]"), false);
        adicionarFiltroBaseTracking(whereBase, params, filtro, filtrosIgnorados, "statusCarga", "filtro_statusCarga", null, List.of("base_raw.[Status Carga]"), false);

        String substituicao = whereBase.isEmpty()
                ? ""
                : "AND " + String.join("\n                      AND ", whereBase);
        return sourceSql.replace("/*__TRACKING_BASE_FILTERS__*/", substituicao);
    }

    private void adicionarFiltroBaseTracking(
            List<String> whereBase,
            MapSqlParameterSource params,
            FiltroConsultaDTO filtro,
            Set<String> filtrosIgnorados,
            String chaveFiltro,
            String paramName,
            String codigoParamName,
            List<String> colunas,
            boolean filialFlexivel
    ) {
        if (filtrosIgnorados.contains(chaveFiltro) || !filtro.temFiltro(chaveFiltro) || !params.hasValue(paramName)) {
            return;
        }

        String predicado = filialFlexivel
                ? predicadoFilialBaseTracking(paramName, codigoParamName, colunas, params)
                : predicadoDiretoBaseTracking(paramName, colunas);
        if (predicado != null) {
            whereBase.add(predicado);
        }
    }

    private String predicadoFilialBaseTracking(
            String paramName,
            String codigoParamName,
            List<String> colunas,
            MapSqlParameterSource params
    ) {
        if (!params.hasValue(paramName)) {
            return null;
        }

        List<String> predicados = new ArrayList<>(colunas.stream()
                .map(coluna -> coluna + " IN (:" + paramName + ")")
                .toList());
        if (codigoParamName != null && params.hasValue(codigoParamName)) {
            predicados.addAll(colunas.stream()
                    .map(coluna -> coluna + " IN (:" + codigoParamName + ")")
                    .toList());
        }
        return "(" + String.join(" OR ", predicados) + ")";
    }

    private String predicadoDiretoBaseTracking(String paramName, List<String> colunas) {
        return "(" + String.join(" OR ", colunas.stream()
                .map(coluna -> coluna + " IN (:" + paramName + ")")
                .toList()) + ")";
    }

    private void adicionarPeriodo(
            List<String> where,
            MapSqlParameterSource params,
            DashboardExportDefinition definition,
            LocalDate dataInicio,
            LocalDate dataFim
    ) {
        if (definition.dateMode() == DashboardExportDefinition.DateMode.NATIVE_LOCAL_DATE) {
            where.add(definition.dateColumn() + " BETWEEN :dataInicio AND :dataFim");
            params.addValue("dataInicio", dataInicio);
            params.addValue("dataFim", dataFim);
            return;
        }

        if (definition.dateMode() == DashboardExportDefinition.DateMode.LOCAL_DATE) {
            String colunaData = "TRY_CONVERT(date, " + definition.dateColumn() + ")";
            where.add(colunaData + " BETWEEN :dataInicio AND :dataFim");
            params.addValue("dataInicio", dataInicio);
            params.addValue("dataFim", dataFim);
            return;
        }

        JanelaOffsetDateTime janela = periodoOffsetDateTimeHelper.criarJanela(dataInicio, dataFim);
        if (definition == DashboardExportDefinition.TRACKING) {
            where.add(definition.dateColumn() + " >= :inicioOffset AND " + definition.dateColumn() + " < :fimOffset");
            params.addValue("inicioOffset", janela.inicioInclusivo());
            params.addValue("fimOffset", janela.fimExclusivo());
            return;
        }

        String colunaData = "TRY_CONVERT(datetimeoffset, " + definition.dateColumn() + ")";
        where.add(colunaData + " >= :inicioOffset AND " + colunaData + " < :fimOffset");
        params.addValue("inicioOffset", janela.inicioInclusivo());
        params.addValue("fimOffset", janela.fimExclusivo());
    }

    private void adicionarEscopo(
            List<String> where,
            MapSqlParameterSource params,
            DashboardExportDefinition definition,
            EscopoFilialService.EscopoFilial escopo
    ) {
        List<String> colunas = definition.escopoColumns();
        if (colunas.isEmpty() || escopo.acessoTotal()) {
            return;
        }

        if (escopo.filiaisOrdenadas().isEmpty()) {
            where.add("1 = 0");
            return;
        }

        if (definition == DashboardExportDefinition.TRACKING) {
            adicionarFiltroFilialFlexivelTracking(where, params, "escopoFiliais", "escopoFiliaisCodigos", colunas, escopo.filiaisOrdenadas());
            return;
        }

        List<String> filiais = normalizar(escopo.filiaisOrdenadas());
        params.addValue("escopoFiliais", filiais);
        where.add("(" + String.join(" OR ", colunas.stream()
                .map(coluna -> normalizarSql(coluna) + " IN (:escopoFiliais)")
                .toList()) + ")");
    }

    private void adicionarFiltros(
            List<String> where,
            MapSqlParameterSource params,
            DashboardExportDefinition definition,
            FiltroConsultaDTO filtro,
            Set<String> filtrosIgnorados
    ) {
        Map<String, List<String>> filtrosDefinidos = definition.filtros();
        for (Map.Entry<String, List<String>> entry : filtrosDefinidos.entrySet()) {
            String chave = entry.getKey();
            if (filtrosIgnorados.contains(chave) || !filtro.temFiltro(chave)) {
                continue;
            }

            if (definition == DashboardExportDefinition.TRACKING && chave.startsWith("filial")) {
                String paramName = "filtro_" + chave;
                adicionarFiltroFilialFlexivelTracking(
                        where,
                        params,
                        paramName,
                        paramName + "Codigos",
                        entry.getValue(),
                        filtro.valores(chave)
                );
                continue;
            }

            List<String> valores = normalizar(filtro.valores(chave));
            if (valores.isEmpty()) {
                continue;
            }

            String paramName = "filtro_" + chave;
            params.addValue(paramName, valores);
            if (definition == DashboardExportDefinition.TRACKING) {
                where.add("(" + String.join(" OR ", entry.getValue().stream()
                        .map(coluna -> coluna + " IN (:" + paramName + ")")
                        .toList()) + ")");
            } else if (usaComparacaoDiretaPorChave(definition, chave)) {
                where.add("(" + String.join(" OR ", entry.getValue().stream()
                        .map(coluna -> coluna + " IN (:" + paramName + ")")
                        .toList()) + ")");
            } else {
                where.add("(" + String.join(" OR ", entry.getValue().stream()
                        .map(coluna -> normalizarSql(coluna) + " IN (:" + paramName + ")")
                        .toList()) + ")");
            }
        }
    }

    private boolean usaComparacaoDiretaPorChave(DashboardExportDefinition definition, String chaveFiltro) {
        return definition == DashboardExportDefinition.COTACOES && "usuarios".equals(chaveFiltro);
    }

    private void adicionarFiltroFilialFlexivelTracking(
            List<String> where,
            MapSqlParameterSource params,
            String paramName,
            String codigoParamName,
            List<String> colunas,
            Collection<String> valores
    ) {
        List<String> normalizados = normalizar(valores);
        if (normalizados.isEmpty()) {
            return;
        }

        params.addValue(paramName, normalizados);
        List<String> predicados = new ArrayList<>(colunas.stream()
                .map(coluna -> coluna + " IN (:" + paramName + ")")
                .toList());

        List<String> codigos = codigosFiliais(normalizados);
        if (!codigos.isEmpty()) {
            params.addValue(codigoParamName, codigos);
            predicados.addAll(colunas.stream()
                    .map(coluna -> coluna + " IN (:" + codigoParamName + ")")
                    .toList());
        }

        where.add("(" + String.join(" OR ", predicados) + ")");
    }

    private void adicionarFiltroFilialFlexivel(
            List<String> where,
            MapSqlParameterSource params,
            String paramName,
            String codigoParamName,
            List<String> colunas,
            Collection<String> valores
    ) {
        List<String> normalizados = normalizar(valores);
        if (normalizados.isEmpty()) {
            return;
        }

        params.addValue(paramName, normalizados);
        List<String> predicados = new ArrayList<>(colunas.stream()
                .map(coluna -> normalizarSql(coluna) + " IN (:" + paramName + ")")
                .toList());

        List<String> codigos = codigosFiliais(normalizados);
        if (!codigos.isEmpty()) {
            params.addValue(codigoParamName, codigos);
            predicados.addAll(colunas.stream()
                    .map(coluna -> codigoFilialSql(coluna) + " IN (:" + codigoParamName + ")")
                    .toList());
        }

        where.add("(" + String.join(" OR ", predicados) + ")");
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
        adicionarStatusProcessoCalculado(where, valores);
    }

    private void adicionarStatusProcessoCalculado(List<String> where, List<String> valores) {
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

    private void adicionarFiltrosTabela(
            List<String> where,
            MapSqlParameterSource params,
            DashboardExportDefinition definition,
            FiltroConsultaDTO filtro
    ) {
        TableFilterColumns colunas = colunasTabela(definition);
        adicionarBuscaTabela(where, params, colunas, filtro);
        adicionarCodigoTabela(where, params, colunas.codigo(), filtro);
        adicionarPrefixoTabela(where, params, colunas.placa(), filtro, "tabelaPlaca", "tabelaPlaca");
        adicionarStatusTabela(where, params, definition, colunas.status(), filtro);
        adicionarTextoTabela(where, params, colunas.razaoSocial(), filtro, "tabelaRazaoSocial", "tabelaRazaoSocial", false);
        adicionarTextoTabela(where, params, colunas.origem(), filtro, "tabelaOrigem", "tabelaOrigem", true);
        adicionarTextoTabela(where, params, colunas.destino(), filtro, "tabelaDestino", "tabelaDestino", true);
        adicionarFiltrosColunasTabela(where, params, definition, filtro);
    }

    private void adicionarFiltrosColunasTabela(
            List<String> where,
            MapSqlParameterSource params,
            DashboardExportDefinition definition,
            FiltroConsultaDTO filtro
    ) {
        Map<String, TableColumnFilterDefinition> colunas = colunasVisiveisTabela(definition);
        if (colunas.isEmpty()) {
            return;
        }

        for (Map.Entry<String, List<String>> entry : filtro.filtros().entrySet()) {
            String chaveFiltro = entry.getKey();
            if (!chaveFiltro.startsWith("tabelaColuna.")) {
                continue;
            }

            String chaveColuna = chaveFiltro.substring("tabelaColuna.".length());
            TableColumnFilterDefinition coluna = colunas.get(chaveColuna);
            if (coluna == null) {
                continue;
            }

            adicionarFiltroColunaTabela(where, params, chaveColuna, coluna, entry.getValue());
        }
    }

    private void adicionarFiltroColunaTabela(
            List<String> where,
            MapSqlParameterSource params,
            String chaveColuna,
            TableColumnFilterDefinition coluna,
            Collection<String> valores
    ) {
        if (valores == null || valores.isEmpty() || coluna.expressions().isEmpty()) {
            return;
        }

        String param = "filtro_tabelaColuna_" + chaveColuna.replaceAll("[^A-Za-z0-9]", "_");
        switch (coluna.kind()) {
            case TEXT -> adicionarFiltroTextoColuna(where, params, coluna.expressions(), valores, param, false);
            case UF -> adicionarFiltroTextoColuna(where, params, coluna.expressions(), valores, param, true);
            case CODE -> adicionarFiltroCodigoColuna(where, params, coluna.expressions(), valores, param);
            case NUMERIC_CODE -> adicionarFiltroCodigoNumericoDiretoColuna(where, params, coluna.expressions(), valores, param);
            case NUMBER -> adicionarFiltroNumeroColuna(where, params, coluna.expressions(), valores, param);
            case DATE -> adicionarFiltroDataColuna(where, params, coluna.expressions(), valores, param);
            case STATUS -> adicionarFiltroStatusColuna(where, params, coluna, valores, param);
        }
    }

    private void adicionarFiltroTextoColuna(
            List<String> where,
            MapSqlParameterSource params,
            List<String> expressions,
            Collection<String> valores,
            String param,
            boolean permitirUfExata
    ) {
        String termo = primeiroNormalizado(valores);
        if (termo == null) {
            return;
        }

        if (permitirUfExata && ehUf(termo)) {
            params.addValue(param, termo);
            where.add("(" + String.join(" OR ", expressions.stream()
                    .map(expressao -> normalizarSql(expressao) + " = :" + param)
                    .toList()) + ")");
            return;
        }

        if (termo.length() < 3) {
            return;
        }

        params.addValue(param, "%" + termo + "%");
        where.add("(" + String.join(" OR ", expressions.stream()
                .map(expressao -> normalizarSql(expressao) + " LIKE :" + param)
                .toList()) + ")");
    }

    private void adicionarFiltroCodigoColuna(
            List<String> where,
            MapSqlParameterSource params,
            List<String> expressions,
            Collection<String> valores,
            String param
    ) {
        String termo = primeiroNormalizado(valores);
        if (termo == null) {
            return;
        }

        Long numero = parseLongOrNull(termo);
        if (numero != null) {
            params.addValue(param, numero);
            where.add("(" + String.join(" OR ", expressions.stream()
                    .map(expressao -> "TRY_CONVERT(BIGINT, " + expressao + ") = :" + param)
                    .toList()) + ")");
            return;
        }

        if (termo.length() < 3) {
            return;
        }

        params.addValue(param, termo + "%");
        where.add("(" + String.join(" OR ", expressions.stream()
                .map(expressao -> normalizarSql(expressao) + " LIKE :" + param)
                .toList()) + ")");
    }

    private void adicionarFiltroCodigoNumericoDiretoColuna(
            List<String> where,
            MapSqlParameterSource params,
            List<String> expressions,
            Collection<String> valores,
            String param
    ) {
        Long numero = parseLongOrNull(primeiroNormalizado(valores));
        if (numero == null) {
            return;
        }

        params.addValue(param, numero);
        where.add("(" + String.join(" OR ", expressions.stream()
                .map(expressao -> expressao + " = :" + param)
                .toList()) + ")");
    }

    private void adicionarFiltroNumeroColuna(
            List<String> where,
            MapSqlParameterSource params,
            List<String> expressions,
            Collection<String> valores,
            String param
    ) {
        BigDecimal numero = parseDecimalOrNull(primeiroValor(valores));
        if (numero == null) {
            return;
        }

        params.addValue(param, numero);
        where.add("(" + String.join(" OR ", expressions.stream()
                .map(expressao -> "TRY_CONVERT(DECIMAL(19,4), " + expressao + ") = :" + param)
                .toList()) + ")");
    }

    private void adicionarFiltroDataColuna(
            List<String> where,
            MapSqlParameterSource params,
            List<String> expressions,
            Collection<String> valores,
            String param
    ) {
        String termo = primeiroValor(valores);
        if (termo == null || !ehPrefixoData(termo)) {
            return;
        }

        params.addValue(param, termo + "%");
        where.add("(" + String.join(" OR ", expressions.stream()
                .map(expressao -> dataSql(expressao) + " LIKE :" + param)
                .toList()) + ")");
    }

    private void adicionarFiltroStatusColuna(
            List<String> where,
            MapSqlParameterSource params,
            TableColumnFilterDefinition coluna,
            Collection<String> valores,
            String param
    ) {
        List<String> valoresNormalizados = normalizar(valores);
        if (valoresNormalizados.isEmpty()) {
            return;
        }

        if (coluna.statusProcessoCalculado()) {
            adicionarStatusProcessoCalculado(where, valoresNormalizados);
            return;
        }

        params.addValue(param, valoresNormalizados);
        where.add("(" + String.join(" OR ", coluna.expressions().stream()
                .map(expressao -> normalizarSql(expressao) + " IN (:" + param + ")")
                .toList()) + ")");
    }

    private void adicionarBuscaTabela(
            List<String> where,
            MapSqlParameterSource params,
            TableFilterColumns colunas,
            FiltroConsultaDTO filtro
    ) {
        String termo = primeiroNormalizado(filtro.valores("tabelaBusca"));
        if (termo == null) {
            return;
        }

        List<String> predicados = new ArrayList<>();
        Long numero = parseLongOrNull(termo);
        if (numero != null) {
            String param = "filtro_tabelaBuscaCodigo";
            params.addValue(param, numero);
            predicados.addAll(colunas.codigo().stream()
                    .map(coluna -> "TRY_CONVERT(BIGINT, " + coluna + ") = :" + param)
                    .toList());
        } else if (ehUf(termo)) {
            String param = "filtro_tabelaBuscaUf";
            params.addValue(param, termo);
            predicados.addAll(juntar(colunas.origem(), colunas.destino()).stream()
                    .map(coluna -> normalizarSql(coluna) + " = :" + param)
                    .toList());
        } else if (termo.length() >= 3) {
            String containsParam = "filtro_tabelaBuscaTexto";
            String prefixParam = "filtro_tabelaBuscaPrefixo";
            params.addValue(containsParam, "%" + termo + "%");
            params.addValue(prefixParam, termo + "%");
            predicados.addAll(colunas.buscaTexto().stream()
                    .map(coluna -> normalizarSql(coluna) + " LIKE :" + containsParam)
                    .toList());
            predicados.addAll(juntar(colunas.codigo(), colunas.placa()).stream()
                    .map(coluna -> normalizarSql(coluna) + " LIKE :" + prefixParam)
                    .toList());
        }

        if (!predicados.isEmpty()) {
            where.add("(" + String.join(" OR ", predicados) + ")");
        }
    }

    private void adicionarCodigoTabela(
            List<String> where,
            MapSqlParameterSource params,
            List<String> colunas,
            FiltroConsultaDTO filtro
    ) {
        String termo = primeiroNormalizado(filtro.valores("tabelaCodigo"));
        if (termo == null || colunas.isEmpty()) {
            return;
        }

        List<String> predicados = new ArrayList<>();
        Long numero = parseLongOrNull(termo);
        if (numero != null) {
            String param = "filtro_tabelaCodigoNumero";
            params.addValue(param, numero);
            predicados.addAll(colunas.stream()
                    .map(coluna -> "TRY_CONVERT(BIGINT, " + coluna + ") = :" + param)
                    .toList());
        } else if (termo.length() >= 3) {
            String param = "filtro_tabelaCodigoPrefixo";
            params.addValue(param, termo + "%");
            predicados.addAll(colunas.stream()
                    .map(coluna -> normalizarSql(coluna) + " LIKE :" + param)
                    .toList());
        }

        if (!predicados.isEmpty()) {
            where.add("(" + String.join(" OR ", predicados) + ")");
        }
    }

    private void adicionarPrefixoTabela(
            List<String> where,
            MapSqlParameterSource params,
            List<String> colunas,
            FiltroConsultaDTO filtro,
            String chaveFiltro,
            String nomeParam
    ) {
        String termo = primeiroNormalizado(filtro.valores(chaveFiltro));
        if (termo == null || termo.length() < 3 || colunas.isEmpty()) {
            return;
        }

        String param = "filtro_" + nomeParam;
        params.addValue(param, termo + "%");
        where.add("(" + String.join(" OR ", colunas.stream()
                .map(coluna -> normalizarSql(coluna) + " LIKE :" + param)
                .toList()) + ")");
    }

    private void adicionarStatusTabela(
            List<String> where,
            MapSqlParameterSource params,
            DashboardExportDefinition definition,
            List<String> colunas,
            FiltroConsultaDTO filtro
    ) {
        if (!filtro.temFiltro("tabelaStatus")) {
            return;
        }

        List<String> valores = normalizar(filtro.valores("tabelaStatus"));
        if (valores.isEmpty()) {
            return;
        }

        if (definition.temFiltroStatusProcesso()) {
            adicionarStatusProcessoCalculado(where, valores);
            return;
        }

        if (colunas.isEmpty()) {
            return;
        }

        params.addValue("filtro_tabelaStatus", valores);
        where.add("(" + String.join(" OR ", colunas.stream()
                .map(coluna -> normalizarSql(coluna) + " IN (:filtro_tabelaStatus)")
                .toList()) + ")");
    }

    private void adicionarTextoTabela(
            List<String> where,
            MapSqlParameterSource params,
            List<String> colunas,
            FiltroConsultaDTO filtro,
            String chaveFiltro,
            String nomeParam,
            boolean permitirUfExata
    ) {
        String termo = primeiroNormalizado(filtro.valores(chaveFiltro));
        if (termo == null || colunas.isEmpty()) {
            return;
        }

        String param = "filtro_" + nomeParam;
        if (permitirUfExata && ehUf(termo)) {
            params.addValue(param, termo);
            where.add("(" + String.join(" OR ", colunas.stream()
                    .map(coluna -> normalizarSql(coluna) + " = :" + param)
                    .toList()) + ")");
            return;
        }

        if (termo.length() < 3) {
            return;
        }

        params.addValue(param, "%" + termo + "%");
        where.add("(" + String.join(" OR ", colunas.stream()
                .map(coluna -> normalizarSql(coluna) + " LIKE :" + param)
                .toList()) + ")");
    }

    private String primeiroNormalizado(Collection<String> valores) {
        return normalizar(valores).stream().findFirst().orElse(null);
    }

    private String primeiroValor(Collection<String> valores) {
        if (valores == null) {
            return null;
        }
        return valores.stream()
                .filter(valor -> valor != null && !valor.isBlank())
                .map(String::trim)
                .findFirst()
                .orElse(null);
    }

    private Long parseLongOrNull(String valor) {
        if (valor == null || !valor.matches("\\d+")) {
            return null;
        }
        try {
            return Long.parseLong(valor);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal parseDecimalOrNull(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        String texto = valor.trim();
        String normalizado = texto.contains(",")
                ? texto.replace(".", "").replace(",", ".")
                : texto;
        if (!normalizado.matches("-?\\d+(\\.\\d+)?")) {
            return null;
        }

        try {
            return new BigDecimal(normalizado);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean ehUf(String valor) {
        return valor != null && valor.matches("[a-z]{2}");
    }

    private boolean ehPrefixoData(String valor) {
        return valor != null
                && (valor.matches("\\d{4}")
                || valor.matches("\\d{4}-\\d{2}")
                || valor.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    private String dataSql(String expressao) {
        return "CONVERT(VARCHAR(10), TRY_CONVERT(date, " + expressao + "), 23)";
    }

    private List<String> juntar(List<String> primeira, List<String> segunda) {
        List<String> resultado = new ArrayList<>(primeira);
        resultado.addAll(segunda);
        return resultado;
    }

    private TableFilterColumns colunasTabela(DashboardExportDefinition definition) {
        return switch (definition) {
            case COLETAS -> new TableFilterColumns(
                    List.of("[Cliente]", "[Cidade]", "[UF]", "[Região da Coleta]", "[Status]"),
                    List.of("[ID]", "[Coleta]", "[Numero Manifesto]"),
                    List.of(),
                    List.of("[Status]"),
                    List.of("[Cliente]"),
                    List.of("[Cidade]", "[UF]", "[Região da Coleta]"),
                    List.of()
            );
            case FRETES -> new TableFilterColumns(
                    List.of("[Pagador]", "[Remetente]", "[Destinatario]", "[UF Origem]", "[UF Destino]", "[Status]"),
                    List.of("[ID]", "[Nº Minuta]", "[Nº CT-e]", "[Nº NFS-e]"),
                    List.of(),
                    List.of("[Status]"),
                    List.of("[Pagador]", "[Remetente]", "[Destinatario]"),
                    List.of("[UF Origem]"),
                    List.of("[UF Destino]")
            );
            case TRACKING -> new TableFilterColumns(
                    List.of("[Filial Origem]", "[Filial Destino]", "[Região Origem]", "[Região Destino]", "[Status Carga]"),
                    List.of("[N° Minuta]"),
                    List.of(),
                    List.of("[Status Carga]"),
                    List.of(),
                    List.of("[Filial Origem]", "[Região Origem]"),
                    List.of("[Filial Destino]", "[Região Destino]")
            );
            case MANIFESTOS -> new TableFilterColumns(
                    List.of("[Motorista]", "[Veículo/Placa]", "[Status]"),
                    List.of("[Número]", "[Identificador Único]"),
                    List.of("[Veículo/Placa]"),
                    List.of("[Status]"),
                    List.of(),
                    List.of(),
                    List.of()
            );
            case COTACOES -> new TableFilterColumns(
                    List.of("[Cliente Pagador]", "[Cliente]", "[Trecho]", "[Origem]", "[Destino]", "[UF Origem]", "[UF Destino]", "[Status Conversão]"),
                    List.of("[N° Cotação]"),
                    List.of(),
                    List.of("[Status Conversão]"),
                    List.of("[Cliente Pagador]", "[Cliente]"),
                    List.of("[UF Origem]", "[Cidade Origem]", "[Origem]", "[Trecho]"),
                    List.of("[UF Destino]", "[Cidade Destino]", "[Destino]", "[Trecho]")
            );
            case CONTAS_A_PAGAR -> new TableFilterColumns(
                    List.of("[Fornecedor/Nome]", "[Conta Contábil/Classificação]", "[Centro de custo/Nome]", "[Pago]"),
                    List.of("[Lançamento a Pagar/N°]", "[N° Documento]"),
                    List.of(),
                    List.of("[Pago]"),
                    List.of("[Fornecedor/Nome]"),
                    List.of(),
                    List.of()
            );
            case FATURAS_POR_CLIENTE -> new TableFilterColumns(
                    List.of("[Pagador do frete/Nome]", "[Cliente/CNPJ]", "[Pagador do frete/Documento]"),
                    List.of("[ID Único]", "[Fatura/N° Documento]", "[CT-e/Número]"),
                    List.of(),
                    List.of(),
                    List.of("[Pagador do frete/Nome]", "[Cliente/CNPJ]", "[Pagador do frete/Documento]"),
                    List.of(),
                    List.of()
            );
            default -> TableFilterColumns.empty();
        };
    }

    private Map<String, TableColumnFilterDefinition> colunasVisiveisTabela(DashboardExportDefinition definition) {
        Map<String, TableColumnFilterDefinition> colunas = new LinkedHashMap<>();

        switch (definition) {
            case COLETAS -> {
                put(colunas, "id", codigo("[ID]"));
                put(colunas, "coleta", codigo("[Coleta]"));
                put(colunas, "solicitacao", data("[Solicitacao]"));
                put(colunas, "status", status("[Status]"));
                put(colunas, "filial", texto("[Filial]"));
                put(colunas, "cliente", texto("[Cliente]"));
                put(colunas, "regiaoColeta", texto("[Região da Coleta]"));
                put(colunas, "volumes", numero("[Volumes]"));
                put(colunas, "pesoTaxado", numero("[Peso Taxado]"));
                put(colunas, "valorNf", numero("[Valor NF]"));
                put(colunas, "numeroTentativas", numero("[Nº Tentativas]"));
            }
            case FRETES -> {
                put(colunas, "id", codigo("[ID]"));
                put(colunas, "numeroMinuta", codigoNumericoDireto("[Nº Minuta]"));
                put(colunas, "dataFrete", data("[data_referencia_faturamento]"));
                put(colunas, "status", status("[Status]"));
                put(colunas, "filial", texto("[Filial]"));
                put(colunas, "pagador", texto("[Pagador]"));
                put(colunas, "documentoTipo", texto(documentoTipoFreteSql()));
                put(colunas, "valorFrete", numero("[Valor Frete]"));
                put(colunas, "valorTotalServico", numero("[Valor Total do Serviço]"));
                put(colunas, "pesoTaxado", numero("[Kg Taxado]"));
                put(colunas, "volumes", numero("[Volumes]"));
                put(colunas, "origemUf", uf("[UF Origem]"));
                put(colunas, "destinoUf", uf("[UF Destino]"));
                put(colunas, "previsaoEntrega", data("[Previsão de Entrega]"));
            }
            case TRACKING -> {
                put(colunas, "numeroMinuta", codigo("[N° Minuta]"));
                put(colunas, "dataFrete", data("[Data do frete]"));
                put(colunas, "statusCarga", status("[Status Carga]"));
                put(colunas, "filialEmissora", texto("[Filial Emissora]"));
                put(colunas, "filialAtual", texto("[Filial Atual]"));
                put(colunas, "regiaoDestino", texto("[Região Destino]"));
                put(colunas, "pesoTaxadoRaw", numero(trackingPesoTaxadoSql()));
                put(colunas, "valorFrete", numero("[Valor Frete]"));
                put(colunas, "previsaoEntrega", data("[Previsão Entrega/Previsão de entrega]"));
            }
            case MANIFESTOS -> {
                put(colunas, "numero", codigo("[Número]"));
                put(colunas, "status", status("[Status]"));
                put(colunas, "filial", texto("[Filial]"));
                put(colunas, "motorista", texto("[Motorista]"));
                put(colunas, "veiculoPlaca", codigo("[Veículo/Placa]"));
                put(colunas, "dataCriacao", data("[Data criação]"));
                put(colunas, "totalPesoTaxado", numero("[Total peso taxado]"));
                put(colunas, "totalM3", numero("[Total M3]"));
                put(colunas, "custoTotal", numero("[Custo total]"));
                put(colunas, "valorFrete", numero("[Valor frete]"));
                put(colunas, "kmTotal", numero("[KM Total]"));
            }
            case COTACOES -> {
                put(colunas, "numeroCotacao", codigo("[N° Cotação]"));
                put(colunas, "dataCotacao", data("[Data Cotação]"));
                put(colunas, "filial", texto("[Filial]"));
                put(colunas, "clientePagador", texto("[Cliente Pagador]"));
                put(colunas, "trecho", texto("[Trecho]"));
                put(colunas, "valorFrete", numero("[Valor frete]"));
                put(colunas, "statusConversao", status("[Status Conversão]"));
                put(colunas, "motivoPerda", texto("[Motivo Perda]"));
                put(colunas, "tipoOperacao", texto("[Tipo de operação]"));
                put(colunas, "volumes", numero("[Volume]"));
                put(colunas, "pesoTaxado", numero("[Peso taxado]"));
                put(colunas, "fretePorKg", numero("CASE WHEN TRY_CONVERT(decimal(18,6), [Peso taxado]) > 0 THEN TRY_CONVERT(decimal(18,6), [Valor frete]) / TRY_CONVERT(decimal(18,6), [Peso taxado]) ELSE 0 END"));
                put(colunas, "minFreteKg", numero("[Min. Frete/KG]"));
                put(colunas, "valorNf", numero("[Valor NF]"));
                put(colunas, "percentualNf", numero("CASE WHEN TRY_CONVERT(decimal(18,6), [Valor NF]) > 0 THEN (TRY_CONVERT(decimal(18,6), [Valor frete]) * 100) / TRY_CONVERT(decimal(18,6), [Valor NF]) ELSE 0 END"));
                put(colunas, "tabela", texto("[Tabela]"));
                put(colunas, "origem", texto("[Origem]"));
                put(colunas, "destino", texto("[Destino]"));
            }
            case CONTAS_A_PAGAR -> {
                put(colunas, "lancamentoNumero", codigo("[Lançamento a Pagar/N°]"));
                put(colunas, "emissao", data("[Emissão]"));
                put(colunas, "filial", texto("[Filial]"));
                put(colunas, "fornecedor", texto("[Fornecedor/Nome]"));
                put(colunas, "classificacao", texto("[Conta Contábil/Classificação]"));
                put(colunas, "centroCusto", texto("[Centro de custo/Nome]"));
                put(colunas, "valorAPagar", numero("[Valor a pagar]"));
                put(colunas, "valorPago", numero("[Valor pago]"));
                put(colunas, "statusPagamento", status("[Pago]"));
            }
            case FATURAS_POR_CLIENTE -> {
                put(colunas, "idUnico", codigo("[ID Único]"));
                put(colunas, "documentoFatura", codigo("[Fatura/N° Documento]"));
                put(colunas, "emissao", data("COALESCE([Fatura/Emissão], [CT-e/Data de emissão])"));
                put(colunas, "vencimento", data("[Parcelas/Vencimento]"));
                put(colunas, "baixa", data("[Fatura/Baixa]"));
                put(colunas, "filial", texto("[Filial]"));
                put(colunas, "clientePagador", texto("[Pagador do frete/Nome]"));
                put(colunas, "clienteCnpj", texto("[Cliente/CNPJ]", "[Pagador do frete/Documento]"));
                put(colunas, "numeroCte", codigo("[CT-e/Número]"));
                put(colunas, "valorFaturado", numero(valorOperacionalFaturaSql()));
                put(colunas, "statusProcesso", statusProcesso());
            }
            default -> {
            }
        }

        return colunas;
    }

    private void put(
            Map<String, TableColumnFilterDefinition> colunas,
            String chave,
            TableColumnFilterDefinition definition
    ) {
        colunas.put(chave, definition);
    }

    private TableColumnFilterDefinition texto(String... expressions) {
        return coluna(TableColumnKind.TEXT, expressions);
    }

    private TableColumnFilterDefinition uf(String... expressions) {
        return coluna(TableColumnKind.UF, expressions);
    }

    private TableColumnFilterDefinition codigo(String... expressions) {
        return coluna(TableColumnKind.CODE, expressions);
    }

    private TableColumnFilterDefinition codigoNumericoDireto(String... expressions) {
        return coluna(TableColumnKind.NUMERIC_CODE, expressions);
    }

    private TableColumnFilterDefinition numero(String... expressions) {
        return coluna(TableColumnKind.NUMBER, expressions);
    }

    private TableColumnFilterDefinition data(String... expressions) {
        return coluna(TableColumnKind.DATE, expressions);
    }

    private TableColumnFilterDefinition status(String... expressions) {
        return coluna(TableColumnKind.STATUS, expressions);
    }

    private TableColumnFilterDefinition statusProcesso() {
        return new TableColumnFilterDefinition(TableColumnKind.STATUS, List.of("[Fatura/N° Documento]"), true);
    }

    private TableColumnFilterDefinition coluna(TableColumnKind kind, String... expressions) {
        return new TableColumnFilterDefinition(kind, List.of(expressions), false);
    }

    private String documentoTipoFreteSql() {
        return "(CASE WHEN [CT-e ID] IS NOT NULL THEN 'ct-e' WHEN [Nº NFS-e] IS NOT NULL THEN 'nfs-e' ELSE 'pendente' END)";
    }

    private String trackingPesoTaxadoSql() {
        return """
                COALESCE(
                    TRY_CONVERT(DECIMAL(19,4), [Peso Taxado]),
                    TRY_CONVERT(DECIMAL(19,4), REPLACE(CONVERT(NVARCHAR(50), [Peso Taxado]), ',', '.')),
                    TRY_CONVERT(DECIMAL(19,4), REPLACE(REPLACE(CONVERT(NVARCHAR(50), [Peso Taxado]), '.', ''), ',', '.')),
                    0
                )
                """;
    }

    private String valorOperacionalFaturaSql() {
        return "COALESCE([Fatura/Valor], [Fatura/Valor Total], [Frete/Valor dos CT-es])";
    }

    private String normalizarSql(String coluna) {
        return "LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), " + coluna + "))))";
    }

    private String codigoFilialSql(String coluna) {
        return "LOWER(LEFT(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), " + coluna + "))), 3))";
    }

    private List<String> normalizar(Collection<String> valores) {
        return valores.stream()
                .filter(valor -> valor != null && !valor.isBlank())
                .map(valor -> valor.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private List<String> codigosFiliais(Collection<String> valores) {
        return valores.stream()
                .filter(valor -> valor != null && !valor.isBlank())
                .map(valor -> valor.trim().toLowerCase(Locale.ROOT))
                .map(this::codigoFilial)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private String codigoFilial(String valor) {
        String[] partes = valor.split("\\s*[-–—]\\s*");
        if (partes.length == 0) {
            return null;
        }

        String codigo = partes[0].trim();
        if ("sem_map".equals(codigo) && partes.length > 1) {
            codigo = partes[1].trim();
        }
        if (codigo.length() < 3) {
            return null;
        }
        return codigo.substring(0, 3);
    }

    record ExportSql(@NonNull String sql, @NonNull MapSqlParameterSource params) {
    }

    private record SqlParts(@NonNull String where, @NonNull MapSqlParameterSource params, @NonNull String sourceSql) {
    }

    private record TableFilterColumns(
            List<String> buscaTexto,
            List<String> codigo,
            List<String> placa,
            List<String> status,
            List<String> razaoSocial,
            List<String> origem,
            List<String> destino
    ) {
        static TableFilterColumns empty() {
            return new TableFilterColumns(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        }
    }

    private record TableColumnFilterDefinition(
            TableColumnKind kind,
            List<String> expressions,
            boolean statusProcessoCalculado
    ) {
    }

    private enum TableColumnKind {
        TEXT,
        UF,
        CODE,
        NUMERIC_CODE,
        NUMBER,
        DATE,
        STATUS
    }
}
