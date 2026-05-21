package com.dashboard.api.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

public enum DashboardExportDefinition {

    COLETAS(
            "coletas",
            "coletas",
            "[vw_coletas_powerbi]",
            DateMode.LOCAL_DATE,
            "[Solicitacao]",
            List.of("[Filial]"),
            Map.of(
                    "filiais", List.of("[Filial]"),
                    "clientes", List.of("[Cliente]"),
                    "status", List.of("[Status]"),
                    "regioes", List.of("[Região da Coleta]"),
                    "usuarios", List.of("[Usuario]")
            ),
            List.of("[Solicitacao] DESC", "[Coleta] DESC"),
            new DedupConfig("[ID]", List.of("[Data de extracao] DESC", "[Solicitacao] DESC", "[Numero Manifesto] DESC"))
    ),
    FRETES(
            "fretes",
            "fretes",
            "[vw_fretes_powerbi]",
            DateMode.OFFSET_DATE_TIME,
            "[data_referencia_faturamento]",
            List.of("[Filial]"),
            Map.of(
                    "filiais", List.of("[Filial]"),
                    "status", List.of("[Status]"),
                    "pagadores", List.of("[Pagador]"),
                    "ufOrigem", List.of("[UF Origem]"),
                    "ufDestino", List.of("[UF Destino]"),
                    "tiposFrete", List.of("[Tipo Frete]"),
                    "modais", List.of("[Modal]")
            ),
            List.of("[data_referencia_faturamento] DESC", "[Nº Minuta] DESC"),
            null
    ),
    TRACKING(
            "tracking",
            "tracking",
            "[vw_localizacao_cargas_powerbi]",
            DateMode.OFFSET_DATE_TIME,
            "[Data do frete]",
            List.of("[Filial Emissora]", "[Filial Origem]", "[Filial Atual]", "[Filial Destino]"),
            Map.of(
                    "filialEmissora", List.of("[Filial Emissora]"),
                    "filialAtual", List.of("[Filial Atual]"),
                    "filialDestino", List.of("[Filial Destino]"),
                    "regiaoOrigem", List.of("[Região Origem]"),
                    "regiaoDestino", List.of("[Região Destino]"),
                    "statusCarga", List.of("[Status Carga]")
            ),
            List.of("[Data do frete] DESC", "[N° Minuta] DESC"),
            null
    ),
    MANIFESTOS(
            "manifestos",
            "manifestos",
            "[vw_manifestos_powerbi]",
            DateMode.OFFSET_DATE_TIME,
            "[Data criação]",
            List.of("[Filial]"),
            Map.of(
                    "filiais", List.of("[Filial]"),
                    "status", List.of("[Status]"),
                    "motoristas", List.of("[Motorista]"),
                    "veiculos", List.of("[Veículo/Placa]"),
                    "tiposCarga", List.of("[Tipo de carga]"),
                    "tiposContrato", List.of("[Tipo de contrato]")
            ),
            List.of("[Data criação] DESC", "[Número] DESC"),
            null
    ),
    COTACOES(
            "cotacoes",
            "cotacoes",
            "[vw_cotacoes_powerbi]",
            DateMode.OFFSET_DATE_TIME,
            "[Data Cotação]",
            List.of("[Filial]"),
            Map.of(
                    "filiais", List.of("[Filial]"),
                    "clientes", List.of("[Cliente Pagador]"),
                    "ufOrigem", List.of("[UF Origem]"),
                    "ufDestino", List.of("[UF Destino]"),
                    "statusConversao", List.of("[Status Conversão]"),
                    "tabelas", List.of("[Tabela]")
            ),
            List.of("[Data Cotação] DESC", "[N° Cotação] DESC"),
            null
    ),
    CONTAS_A_PAGAR(
            "contas-a-pagar",
            "contas-a-pagar",
            "[vw_contas_a_pagar_powerbi]",
            DateMode.LOCAL_DATE,
            "[Emissão]",
            List.of("[Filial]"),
            Map.of(
                    "filiais", List.of("[Filial]"),
                    "fornecedores", List.of("[Fornecedor/Nome]"),
                    "classificacoes", List.of("[Conta Contábil/Classificação]"),
                    "centrosCusto", List.of("[Centro de custo/Nome]"),
                    "pago", List.of("[Pago]"),
                    "conciliado", List.of("[Conciliado]")
            ),
            List.of("[Emissão] DESC", "[Lançamento a Pagar/N°] DESC"),
            null
    ),
    FATURAS_PROCESSOS(
            "faturas",
            "faturas-processos",
            "[vw_faturas_por_cliente_powerbi]",
            DateMode.OFFSET_DATE_TIME,
            "[CT-e/Data de emissão]",
            List.of("[Filial]"),
            Map.of(
                    "filiais", List.of("[Filial]"),
                    "pagadores", List.of("[Pagador do frete/Nome]"),
                    "clientesCnpj", List.of("[Cliente/CNPJ]", "[Pagador do frete/Documento]")
            ),
            List.of("[CT-e/Data de emissão] DESC", "[ID Único] DESC"),
            new DedupConfig(faturaPorClienteDedupKey(), List.of("[Data da Última Atualização] DESC", "[CT-e/Data de emissão] DESC"))
    ),
    FATURAS_POR_CLIENTE(
            "faturas-por-cliente",
            "faturas-por-cliente",
            "[vw_faturas_por_cliente_powerbi]",
            DateMode.OFFSET_DATE_TIME,
            "[CT-e/Data de emissão]",
            List.of("[Filial]"),
            Map.of(
                    "filiais", List.of("[Filial]"),
                    "pagadores", List.of("[Pagador do frete/Nome]"),
                    "clientesCnpj", List.of("[Cliente/CNPJ]", "[Pagador do frete/Documento]")
            ),
            List.of("[CT-e/Data de emissão] DESC", "[ID Único] DESC"),
            new DedupConfig("[ID Único]", List.of("[Data da Última Atualização] DESC", "[CT-e/Data de emissão] DESC", "[ID Único] ASC"))
    ),
    FATURAS_FINANCEIRO(
            "faturas-financeiro",
            "faturas-titulos-financeiros",
            "[vw_faturas_graphql_powerbi]",
            DateMode.LOCAL_DATE,
            "[Emissão]",
            List.of("[Filial/Nome]"),
            Map.of(
                    "filiais", List.of("[Filial/Nome]"),
                    "pago", List.of("[Pago]")
            ),
            List.of("[Emissão] DESC", "[Fatura/N° Documento] DESC"),
            null
    ),
    ETL_SAUDE(
            "etl-saude",
            "etl-saude",
            "[vw_bi_monitoramento]",
            DateMode.LOCAL_DATE,
            "[Data]",
            List.of(),
            Map.of("status", List.of("[Status]")),
            List.of("[Data] DESC", "[Inicio] DESC"),
            null
    );

    private final String dashboardId;
    private final String nomeArquivo;
    private final String viewName;
    private final DateMode dateMode;
    private final String dateColumn;
    private final List<String> escopoColumns;
    private final Map<String, List<String>> filtros;
    private final List<String> orderBy;
    private final DedupConfig dedupConfig;

    DashboardExportDefinition(
            String dashboardId,
            String nomeArquivo,
            String viewName,
            DateMode dateMode,
            String dateColumn,
            List<String> escopoColumns,
            Map<String, List<String>> filtros,
            List<String> orderBy,
            DedupConfig dedupConfig
    ) {
        this.dashboardId = dashboardId;
        this.nomeArquivo = nomeArquivo;
        this.viewName = viewName;
        this.dateMode = dateMode;
        this.dateColumn = dateColumn;
        this.escopoColumns = List.copyOf(escopoColumns);
        this.filtros = Map.copyOf(filtros);
        this.orderBy = List.copyOf(orderBy);
        this.dedupConfig = dedupConfig;
    }

    String dashboardId() {
        return dashboardId;
    }

    public String nomeArquivo() {
        return nomeArquivo;
    }

    String viewName() {
        return viewName;
    }

    DateMode dateMode() {
        return dateMode;
    }

    String dateColumn() {
        return dateColumn;
    }

    List<String> escopoColumns() {
        return escopoColumns;
    }

    Map<String, List<String>> filtros() {
        return filtros;
    }

    List<String> orderBy() {
        return orderBy;
    }

    DedupConfig dedupConfig() {
        return dedupConfig;
    }

    boolean temFiltroStatusProcesso() {
        return Set.of(FATURAS_PROCESSOS, FATURAS_POR_CLIENTE).contains(this);
    }

    private static String faturaPorClienteDedupKey() {
        return """
                CASE
                    WHEN [Fatura/N° Documento] IS NOT NULL
                     AND LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Fatura/N° Documento]))) <> ''
                    THEN CONCAT(
                        'fatura|',
                        LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Fatura/N° Documento]))))
                    )
                    ELSE CONCAT(
                        'linha|',
                        LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [ID Único]))))
                    )
                END
                """;
    }

    enum DateMode {
        LOCAL_DATE,
        OFFSET_DATE_TIME
    }

    record DedupConfig(String partitionBy, List<String> orderBy) {
    }
}
