package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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

        assertThat(query.sql()).contains("TRY_CONVERT(datetimeoffset, [data_referencia_faturamento]) >= :inicioOffset AND TRY_CONVERT(datetimeoffset, [data_referencia_faturamento]) < :fimOffset");
        assertThat(query.sql()).contains("ORDER BY [data_referencia_faturamento] DESC");
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
    void buildSelectColetasDeveUsarSolicitacaoComoDataNativa() {
        DashboardExportSqlBuilder.ExportSql query = builder.buildSelect(
                DashboardExportDefinition.COLETAS,
                filtro(Map.of()),
                EscopoFilialService.EscopoFilial.comAcessoTotal(),
                Set.of()
        );

        assertThat(query.sql()).contains("[Solicitacao] BETWEEN :dataInicio AND :dataFim");
        assertThat(query.sql()).doesNotContain("TRY_CONVERT(date, [Solicitacao])");
        assertThat(query.params().getValues()).containsEntry("dataInicio", LocalDate.of(2026, 3, 17));
        assertThat(query.params().getValues()).containsEntry("dataFim", LocalDate.of(2026, 4, 16));
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
        assertThat(query.sql()).contains("LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Pagador do frete/Documento])))) IN (:filtro_clientesCnpj)");
        assertThat(query.sql()).contains("PARTITION BY");
        assertThat(query.sql()).contains("PARTITION BY [ID Único]");
        assertThat(query.params().getValues()).containsKey("filtro_clientesCnpj");
    }

    @Test
    void buildSelectDeveAplicarCodigoTabelaComIgualdadeNumerica() {
        DashboardExportSqlBuilder.ExportSql query = builder.buildSelect(
                DashboardExportDefinition.FRETES,
                filtro(Map.of("tabelaCodigo", List.of("12345"))),
                EscopoFilialService.EscopoFilial.comAcessoTotal(),
                Set.of()
        );

        assertThat(query.sql()).contains("TRY_CONVERT(BIGINT, [ID]) = :filtro_tabelaCodigoNumero");
        assertThat(query.sql()).contains("TRY_CONVERT(BIGINT, [Nº Minuta]) = :filtro_tabelaCodigoNumero");
        assertThat(query.sql()).doesNotContain(":filtro_tabelaCodigoPrefixo");
        assertThat(query.params().getValues()).containsEntry("filtro_tabelaCodigoNumero", 12345L);
    }

    @Test
    void buildSelectNaoDeveGerarLikeAmploParaTextoCurtoTabela() {
        DashboardExportSqlBuilder.ExportSql query = builder.buildSelect(
                DashboardExportDefinition.FRETES,
                filtro(Map.of("tabelaRazaoSocial", List.of("ab"))),
                EscopoFilialService.EscopoFilial.comAcessoTotal(),
                Set.of()
        );

        assertThat(query.sql()).doesNotContain("filtro_tabelaRazaoSocial");
    }

    @Test
    void buildSelectDeveMapearTabelaStatusProcessoCalculado() {
        DashboardExportSqlBuilder.ExportSql query = builder.buildSelect(
                DashboardExportDefinition.FATURAS_PROCESSOS,
                filtro(Map.of("tabelaStatus", List.of("Faturado"))),
                EscopoFilialService.EscopoFilial.comAcessoTotal(),
                Set.of()
        );

        assertThat(query.sql()).contains("[Fatura/N° Documento] IS NOT NULL");
        assertThat(query.sql()).contains("faturado");
    }

    @Test
    void buildSelectDeveAplicarFiltroPorColunaCodigoComIgualdadeNumerica() {
        DashboardExportSqlBuilder.ExportSql query = builder.buildSelect(
                DashboardExportDefinition.FRETES,
                filtro(Map.of("tabelaColuna.id", List.of("12345"))),
                EscopoFilialService.EscopoFilial.comAcessoTotal(),
                Set.of()
        );

        assertThat(query.sql()).contains("TRY_CONVERT(BIGINT, [ID]) = :filtro_tabelaColuna_id");
        assertThat(query.params().getValues()).containsEntry("filtro_tabelaColuna_id", 12345L);
    }

    @Test
    void buildSelectNaoDeveGerarLikeAmploParaTextoCurtoPorColuna() {
        DashboardExportSqlBuilder.ExportSql query = builder.buildSelect(
                DashboardExportDefinition.FRETES,
                filtro(Map.of("tabelaColuna.pagador", List.of("ab"))),
                EscopoFilialService.EscopoFilial.comAcessoTotal(),
                Set.of()
        );

        assertThat(query.sql()).doesNotContain("filtro_tabelaColuna_pagador");
    }

    @Test
    void buildSelectDeveAplicarFiltroPorColunaNumeroComDecimalSeguro() {
        DashboardExportSqlBuilder.ExportSql query = builder.buildSelect(
                DashboardExportDefinition.FRETES,
                filtro(Map.of("tabelaColuna.valorFrete", List.of("123,45"))),
                EscopoFilialService.EscopoFilial.comAcessoTotal(),
                Set.of()
        );

        assertThat(query.sql()).contains("TRY_CONVERT(DECIMAL(19,4), [Valor Frete]) = :filtro_tabelaColuna_valorFrete");
        assertThat(query.params().getValues()).containsEntry("filtro_tabelaColuna_valorFrete", new BigDecimal("123.45"));
    }

    @Test
    void buildSelectDeveAplicarFiltroPorColunaStatusComIn() {
        DashboardExportSqlBuilder.ExportSql query = builder.buildSelect(
                DashboardExportDefinition.FRETES,
                filtro(Map.of("tabelaColuna.status", List.of("Entregue", "Pendente"))),
                EscopoFilialService.EscopoFilial.comAcessoTotal(),
                Set.of()
        );

        assertThat(query.sql()).contains("LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Status])))) IN (:filtro_tabelaColuna_status)");
        assertThat(query.params().getValues()).containsEntry("filtro_tabelaColuna_status", List.of("entregue", "pendente"));
    }

    @Test
    void buildSelectDeveAplicarFiltroEspecialDocumentoTipoFretes() {
        DashboardExportSqlBuilder.ExportSql query = builder.buildSelect(
                DashboardExportDefinition.FRETES,
                filtro(Map.of("tabelaColuna.documentoTipo", List.of("CT-e"))),
                EscopoFilialService.EscopoFilial.comAcessoTotal(),
                Set.of()
        );

        assertThat(query.sql()).contains("CASE WHEN [CT-e ID] IS NOT NULL THEN 'ct-e'");
        assertThat(query.sql()).contains(":filtro_tabelaColuna_documentoTipo");
    }

    @Test
    void buildSelectDeveAplicarFiltroEspecialFinanceiroFaturas() {
        DashboardExportSqlBuilder.ExportSql query = builder.buildSelect(
                DashboardExportDefinition.FATURAS_PROCESSOS,
                filtro(Map.of("tabelaColuna.valorPago", List.of("99.90"))),
                EscopoFilialService.EscopoFilial.comAcessoTotal(),
                Set.of()
        );

        assertThat(query.sql()).contains("[vw_faturas_graphql_powerbi] financeiro");
        assertThat(query.sql()).contains("financeiro.[Valor Pago]");
        assertThat(query.sql()).contains(":filtro_tabelaColuna_valorPago");
    }

    @Test
    void buildSelectTrackingDeveFiltrarPesoComConversaoDecimalSegura() {
        DashboardExportSqlBuilder.ExportSql query = builder.buildSelect(
                DashboardExportDefinition.TRACKING,
                filtro(Map.of("tabelaColuna.pesoTaxadoRaw", List.of("123,45"))),
                EscopoFilialService.EscopoFilial.comAcessoTotal(),
                Set.of()
        );

        assertThat(query.sql()).contains("TRY_CONVERT(DECIMAL(19,4), [Peso Taxado])");
        assertThat(query.sql()).contains("TRY_CONVERT(DECIMAL(19,4), REPLACE(REPLACE(CONVERT(NVARCHAR(50), [Peso Taxado]), '.', ''), ',', '.'))");
        assertThat(query.sql()).contains(":filtro_tabelaColuna_pesoTaxadoRaw");
        assertThat(query.params().getValues()).containsEntry("filtro_tabelaColuna_pesoTaxadoRaw", new BigDecimal("123.45"));
    }

    @Test
    void buildSelectTrackingDeveFiltrarFilialAtualPorTextoECodigoOperacional() {
        DashboardExportSqlBuilder.ExportSql query = builder.buildSelect(
                DashboardExportDefinition.TRACKING,
                filtro(Map.of("filialAtual", List.of("SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA"))),
                EscopoFilialService.EscopoFilial.comAcessoTotal(),
                Set.of()
        );

        assertThat(query.sql()).contains("LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Filial Atual])))) IN (:filtro_filialAtual)");
        assertThat(query.sql()).contains("LOWER(LEFT(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Filial Atual]))), 3)) IN (:filtro_filialAtualCodigos)");
        assertThat(query.params().getValues()).containsEntry("filtro_filialAtual", List.of("spo - rodogarcia transportes rodoviarios ltda"));
        assertThat(query.params().getValues()).containsEntry("filtro_filialAtualCodigos", List.of("spo"));
    }

    @Test
    void buildSelectTrackingDeveAplicarEscopoDeFiliaisPorTextoECodigoOperacional() {
        DashboardExportSqlBuilder.ExportSql query = builder.buildSelect(
                DashboardExportDefinition.TRACKING,
                filtro(Map.of()),
                new EscopoFilialService.EscopoFilial(false, List.of("CWB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA")),
                Set.of()
        );

        assertThat(query.sql()).contains("LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Filial Atual])))) IN (:escopoFiliais)");
        assertThat(query.sql()).contains("LOWER(LEFT(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Filial Atual]))), 3)) IN (:escopoFiliaisCodigos)");
        assertThat(query.params().getValues()).containsEntry("escopoFiliais", List.of("cwb - rodogarcia transportes rodoviarios ltda"));
        assertThat(query.params().getValues()).containsEntry("escopoFiliaisCodigos", List.of("cwb"));
    }

    private static FiltroConsultaDTO filtro(Map<String, List<String>> filtros) {
        return new FiltroConsultaDTO(LocalDate.of(2026, 3, 17), LocalDate.of(2026, 4, 16), filtros);
    }
}
