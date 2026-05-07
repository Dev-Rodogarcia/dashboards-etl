package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardExportSqlBuilderTest {

    private final DashboardExportSqlBuilder builder = new DashboardExportSqlBuilder(PeriodoOffsetDateTimeHelper.padrao());

    @Test
    void buildSelectDeveAplicarDatasOffsetFiltrosEscopoSemLimite() {
        FiltroConsultaDTO filtro = filtro(Map.of(
                "status", List.of("Entregue"),
                "pagadores", List.of("ACME")
        ));

        DashboardExportSqlBuilder.ExportSql query = builder.buildSelect(
                DashboardExportDefinition.FRETES,
                filtro,
                new EscopoFilialService.EscopoFilial(false, List.of("SP")),
                Set.of()
        );

        assertThat(query.sql()).contains("TRY_CONVERT(datetimeoffset, [Data frete]) >= :inicioOffset AND TRY_CONVERT(datetimeoffset, [Data frete]) < :fimOffset");
        assertThat(query.sql()).contains("LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Filial])))) IN (:escopoFiliais)");
        assertThat(query.sql()).contains("LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Status])))) IN (:filtro_status)");
        assertThat(query.sql()).contains("LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Pagador])))) IN (:filtro_pagadores)");
        assertThat(query.sql()).doesNotContainIgnoringCase("limit");
        assertThat(query.sql()).doesNotContainIgnoringCase("top ");
        assertThat(query.params().getValues()).containsKeys("inicioOffset", "fimOffset", "escopoFiliais", "filtro_status", "filtro_pagadores");
    }

    @Test
    void buildCountDeveAplicarDatasLocalDateComBetween() {
        DashboardExportSqlBuilder.ExportSql query = builder.buildCount(
                DashboardExportDefinition.CONTAS_A_PAGAR,
                filtro(Map.of("pago", List.of("Sim"))),
                EscopoFilialService.EscopoFilial.comAcessoTotal(),
                Set.of()
        );

        assertThat(query.sql()).contains("TRY_CONVERT(date, [Emissão]) BETWEEN :dataInicio AND :dataFim");
        assertThat(query.params().getValues()).containsEntry("dataInicio", LocalDate.of(2026, 3, 17));
        assertThat(query.params().getValues()).containsEntry("dataFim", LocalDate.of(2026, 4, 16));
    }

    @Test
    void buildSelectDeveAplicarDeduplicacaoEmViewsConfiguradas() {
        DashboardExportSqlBuilder.ExportSql query = builder.buildSelect(
                DashboardExportDefinition.COLETAS,
                filtro(Map.of()),
                EscopoFilialService.EscopoFilial.comAcessoTotal(),
                Set.of()
        );

        assertThat(query.sql()).contains("ROW_NUMBER() OVER (PARTITION BY [ID]");
        assertThat(query.sql()).contains("WHERE [__rn] = 1");
    }

    @Test
    void buildDistinctDeveIgnorarProprioFiltroDeStatusFretes() {
        DashboardExportSqlBuilder.ExportSql query = builder.buildDistinct(
                DashboardExportDefinition.FRETES,
                "[Status]",
                filtro(Map.of("status", List.of("Entregue"), "pagadores", List.of("ACME"))),
                EscopoFilialService.EscopoFilial.comAcessoTotal(),
                Set.of("status")
        );

        assertThat(query.sql()).doesNotContain(":filtro_status");
        assertThat(query.sql()).contains(":filtro_pagadores");
    }

    @Test
    void buildSelectDeveMapearStatusProcessoCalculado() {
        DashboardExportSqlBuilder.ExportSql query = builder.buildSelect(
                DashboardExportDefinition.FATURAS_PROCESSOS,
                filtro(Map.of("statusProcesso", List.of("Faturado"))),
                EscopoFilialService.EscopoFilial.comAcessoTotal(),
                Set.of()
        );

        assertThat(query.sql()).contains("[Fatura/N° Documento] IS NOT NULL");
        assertThat(query.sql()).contains("LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Fatura/N° Documento]))) <> ''");
    }

    @Test
    void buildSelectDeveFiltrarFaturasPorClientePorCnpj() {
        DashboardExportSqlBuilder.ExportSql query = builder.buildSelect(
                DashboardExportDefinition.FATURAS_POR_CLIENTE,
                filtro(Map.of("clientesCnpj", List.of("12.345.678/0001-90"))),
                EscopoFilialService.EscopoFilial.comAcessoTotal(),
                Set.of()
        );

        assertThat(query.sql()).contains("LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Cliente/CNPJ])))) IN (:filtro_clientesCnpj)");
        assertThat(query.params().getValues()).containsKey("filtro_clientesCnpj");
    }

    private static FiltroConsultaDTO filtro(Map<String, List<String>> filtros) {
        return new FiltroConsultaDTO(LocalDate.of(2026, 3, 17), LocalDate.of(2026, 4, 16), filtros);
    }
}
