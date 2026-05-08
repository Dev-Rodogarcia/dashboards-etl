package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.PaginaDTO;
import com.dashboard.api.dto.coletas.ColetaResumoDTO;
import com.dashboard.api.dto.contaspagar.ContaPagarResumoDTO;
import com.dashboard.api.dto.cotacoes.CotacaoResumoDTO;
import com.dashboard.api.dto.etl.EtlExecucaoResumoDTO;
import com.dashboard.api.dto.faturas.FaturaResumoDTO;
import com.dashboard.api.dto.faturascliente.FaturaPorClienteResumoDTO;
import com.dashboard.api.dto.fretes.FreteResumoDTO;
import com.dashboard.api.dto.manifestos.ManifestoResumoDTO;
import com.dashboard.api.dto.tracking.TrackingResumoDTO;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardTabelaPaginadaService {

    private static final int PAGINA_PADRAO = 1;
    private static final int TAMANHO_PADRAO = 10;
    private static final int TAMANHO_MAXIMO = 100;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final DashboardExportSqlBuilder sqlBuilder;
    private final ValidadorPeriodoService validadorPeriodo;
    private final EscopoFilialService escopoFilialService;

    public DashboardTabelaPaginadaService(
            NamedParameterJdbcTemplate jdbcTemplate,
            DashboardExportSqlBuilder sqlBuilder,
            ValidadorPeriodoService validadorPeriodo,
            EscopoFilialService escopoFilialService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlBuilder = sqlBuilder;
        this.validadorPeriodo = validadorPeriodo;
        this.escopoFilialService = escopoFilialService;
    }

    public PaginaDTO<ColetaResumoDTO> buscarColetas(FiltroConsultaDTO filtro, int pagina, int tamanhoPagina) {
        return buscarPagina(DashboardExportDefinition.COLETAS, filtro, pagina, tamanhoPagina, this::mapearColeta);
    }

    public PaginaDTO<FreteResumoDTO> buscarFretes(FiltroConsultaDTO filtro, int pagina, int tamanhoPagina) {
        return buscarPagina(DashboardExportDefinition.FRETES, filtro, pagina, tamanhoPagina, this::mapearFrete);
    }

    public PaginaDTO<TrackingResumoDTO> buscarTracking(FiltroConsultaDTO filtro, int pagina, int tamanhoPagina) {
        return buscarPagina(DashboardExportDefinition.TRACKING, filtro, pagina, tamanhoPagina, this::mapearTracking);
    }

    public PaginaDTO<ManifestoResumoDTO> buscarManifestos(FiltroConsultaDTO filtro, int pagina, int tamanhoPagina) {
        return buscarPagina(DashboardExportDefinition.MANIFESTOS, filtro, pagina, tamanhoPagina, this::mapearManifesto);
    }

    public PaginaDTO<CotacaoResumoDTO> buscarCotacoes(FiltroConsultaDTO filtro, int pagina, int tamanhoPagina) {
        return buscarPagina(DashboardExportDefinition.COTACOES, filtro, pagina, tamanhoPagina, this::mapearCotacao);
    }

    public PaginaDTO<ContaPagarResumoDTO> buscarContasAPagar(FiltroConsultaDTO filtro, int pagina, int tamanhoPagina) {
        return buscarPagina(DashboardExportDefinition.CONTAS_A_PAGAR, filtro, pagina, tamanhoPagina, this::mapearContaPagar);
    }

    public PaginaDTO<FaturaResumoDTO> buscarFaturas(FiltroConsultaDTO filtro, int pagina, int tamanhoPagina) {
        PaginaDTO<Map<String, Object>> paginaBruta = buscarPaginaBruta(
                DashboardExportDefinition.FATURAS_PROCESSOS,
                filtro,
                pagina,
                tamanhoPagina
        );
        Map<String, TituloFinanceiro> titulos = buscarTitulosFinanceiros(filtro, paginaBruta.conteudo());
        List<FaturaResumoDTO> conteudo = paginaBruta.conteudo().stream()
                .map(row -> mapearFatura(row, titulos))
                .toList();
        return copiarPagina(paginaBruta, conteudo);
    }

    public PaginaDTO<FaturaPorClienteResumoDTO> buscarFaturasPorCliente(FiltroConsultaDTO filtro, int pagina, int tamanhoPagina) {
        return buscarPagina(DashboardExportDefinition.FATURAS_POR_CLIENTE, filtro, pagina, tamanhoPagina, this::mapearFaturaPorCliente);
    }

    public PaginaDTO<EtlExecucaoResumoDTO> buscarEtlSaude(FiltroConsultaDTO filtro, int pagina, int tamanhoPagina) {
        return buscarPagina(DashboardExportDefinition.ETL_SAUDE, filtro, pagina, tamanhoPagina, this::mapearEtlSaude);
    }

    private <T> PaginaDTO<T> buscarPagina(
            DashboardExportDefinition definition,
            FiltroConsultaDTO filtro,
            int paginaSolicitada,
            int tamanhoSolicitado,
            Function<Map<String, Object>, T> mapper
    ) {
        PaginaDTO<Map<String, Object>> pagina = buscarPaginaBruta(definition, filtro, paginaSolicitada, tamanhoSolicitado);
        return copiarPagina(pagina, pagina.conteudo().stream().map(mapper).toList());
    }

    private PaginaDTO<Map<String, Object>> buscarPaginaBruta(
            DashboardExportDefinition definition,
            FiltroConsultaDTO filtro,
            int paginaSolicitada,
            int tamanhoSolicitado
    ) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        int pagina = Math.max(PAGINA_PADRAO, paginaSolicitada);
        int tamanho = ConsultaLimiteUtils.limitar(tamanhoSolicitado, TAMANHO_PADRAO, TAMANHO_MAXIMO);
        long offset = (long) (pagina - 1) * tamanho;
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();

        DashboardExportSqlBuilder.ExportSql countQuery = sqlBuilder.buildCount(definition, filtro, escopo, Set.of());
        String countSql = Objects.requireNonNull(countQuery.sql(), "countSql");
        MapSqlParameterSource countParams = Objects.requireNonNull(countQuery.params(), "countParams");
        Long total = jdbcTemplate.queryForObject(countSql, countParams, Long.class);

        DashboardExportSqlBuilder.ExportSql selectQuery = sqlBuilder.buildSelect(definition, filtro, escopo, Set.of());
        MapSqlParameterSource params = Objects.requireNonNull(selectQuery.params(), "selectParams");
        params.addValue("offsetTabela", offset);
        params.addValue("tamanhoTabela", tamanho);

        String selectSql = Objects.requireNonNull(selectQuery.sql(), "selectSql");
        String sql = selectSql + " OFFSET :offsetTabela ROWS FETCH NEXT :tamanhoTabela ROWS ONLY";
        List<Map<String, Object>> conteudo = jdbcTemplate.queryForList(sql, params);
        long totalSeguro = total != null ? total : 0L;
        int totalPaginas = totalSeguro == 0 ? 0 : (int) Math.ceil(totalSeguro / (double) tamanho);

        return new PaginaDTO<>(conteudo, totalSeguro, totalPaginas, pagina, tamanho);
    }

    private <T> PaginaDTO<T> copiarPagina(PaginaDTO<?> origem, List<T> conteudo) {
        return new PaginaDTO<>(
                conteudo,
                origem.totalElementos(),
                origem.totalPaginas(),
                origem.paginaAtual(),
                origem.tamanhoPagina()
        );
    }

    private ColetaResumoDTO mapearColeta(Map<String, Object> row) {
        return new ColetaResumoDTO(
                texto(row, "ID"),
                longo(row, "Coleta"),
                texto(row, "Solicitacao"),
                texto(row, "Agendamento"),
                texto(row, "Finalizacao"),
                texto(row, "Status"),
                inteiro(row, "Volumes"),
                decimal(row, "Peso Taxado"),
                decimal(row, "Valor NF"),
                longo(row, "Numero Manifesto"),
                texto(row, "Cliente"),
                texto(row, "Cidade"),
                texto(row, "UF"),
                texto(row, "Região da Coleta", "Regiao da Coleta"),
                texto(row, "Filial"),
                texto(row, "Usuario", "Usuário"),
                texto(row, "Motivo Cancel."),
                inteiro(row, "Nº Tentativas", "N° Tentativas")
        );
    }

    private FreteResumoDTO mapearFrete(Map<String, Object> row) {
        return new FreteResumoDTO(
                longo(row, "ID"),
                texto(row, "Data frete"),
                texto(row, "Status"),
                texto(row, "Filial"),
                texto(row, "Pagador"),
                texto(row, "Remetente"),
                texto(row, "Destinatario", "Destinatário"),
                texto(row, "UF Origem"),
                texto(row, "UF Destino"),
                decimal(row, "Valor Total do Serviço", "Valor Total do Servico"),
                decimal(row, "Valor Frete"),
                decimal(row, "Peso Taxado", "Kg Taxado"),
                inteiro(row, "Volumes"),
                texto(row, "Previsão de Entrega", "Previsao de Entrega"),
                documentoTipoFrete(row),
                inteiro(row, "Nº CT-e", "N° CT-e"),
                inteiro(row, "Nº NFS-e", "N° NFS-e"),
                decimal(row, "Valor ICMS"),
                decimal(row, "Valor PIS"),
                decimal(row, "Valor COFINS")
        );
    }

    private TrackingResumoDTO mapearTracking(Map<String, Object> row) {
        return new TrackingResumoDTO(
                longo(row, "N° Minuta", "Nº Minuta"),
                texto(row, "Data do frete"),
                texto(row, "Tipo"),
                inteiro(row, "Volumes"),
                decimal(row, "Peso Taxado"),
                decimal(row, "Valor NF"),
                decimal(row, "Valor Frete"),
                texto(row, "Filial Emissora"),
                texto(row, "Filial Origem"),
                texto(row, "Filial Atual"),
                texto(row, "Filial Destino"),
                texto(row, "Região Origem", "Regiao Origem"),
                texto(row, "Região Destino", "Regiao Destino"),
                texto(row, "Classificação", "Classificacao"),
                texto(row, "Status Carga"),
                texto(row, "Previsão Entrega/Previsão de entrega", "Previsao Entrega/Previsao de entrega")
        );
    }

    private ManifestoResumoDTO mapearManifesto(Map<String, Object> row) {
        return new ManifestoResumoDTO(
                longo(row, "Número", "Numero"),
                texto(row, "Identificador Único", "Identificador Unico"),
                texto(row, "Status"),
                texto(row, "Classificação", "Classificacao"),
                texto(row, "Filial"),
                texto(row, "Data criação", "Data criacao"),
                texto(row, "Fechamento"),
                texto(row, "Motorista"),
                texto(row, "Veículo/Placa", "Veiculo/Placa"),
                texto(row, "Tipo Veículo", "Tipo Veiculo"),
                decimal(row, "Total peso taxado"),
                decimal(row, "Total M3"),
                decimal(row, "Custo total"),
                decimal(row, "Valor frete"),
                decimal(row, "Combustível", "Combustivel"),
                decimal(row, "Pedágio", "Pedagio"),
                decimal(row, "Saldo a pagar"),
                decimal(row, "KM Total"),
                inteiro(row, "Itens/Total")
        );
    }

    private CotacaoResumoDTO mapearCotacao(Map<String, Object> row) {
        return new CotacaoResumoDTO(
                longo(row, "N° Cotação", "Nº Cotação", "N° Cotacao", "Nº Cotacao"),
                texto(row, "Data Cotação", "Data Cotacao"),
                texto(row, "Filial"),
                texto(row, "Solicitante"),
                texto(row, "Cliente Pagador"),
                texto(row, "Cliente"),
                texto(row, "Trecho"),
                decimal(row, "Peso taxado"),
                decimal(row, "Valor NF"),
                decimal(row, "Valor frete"),
                texto(row, "Tabela"),
                texto(row, "Status Conversão", "Status Conversao"),
                texto(row, "Motivo Perda"),
                texto(row, "CT-e/Data de emissão", "CT-e/Data de emissao"),
                texto(row, "Nfse/Data de emissão", "Nfse/Data de emissao")
        );
    }

    private ContaPagarResumoDTO mapearContaPagar(Map<String, Object> row) {
        return new ContaPagarResumoDTO(
                longo(row, "Lançamento a Pagar/N°", "Lancamento a Pagar/N°", "Lançamento a Pagar/Nº"),
                texto(row, "N° Documento", "Nº Documento"),
                texto(row, "Emissão", "Emissao"),
                texto(row, "Tipo"),
                texto(row, "Filial"),
                texto(row, "Fornecedor/Nome"),
                decimal(row, "Valor"),
                decimal(row, "Valor pago"),
                decimal(row, "Valor a pagar"),
                texto(row, "Conta Contábil/Classificação", "Conta Contabil/Classificacao"),
                texto(row, "Conta Contábil/Descrição", "Conta Contabil/Descricao"),
                texto(row, "Centro de custo/Nome"),
                texto(row, "Baixa/Data liquidação", "Baixa/Data liquidacao"),
                texto(row, "Pago"),
                texto(row, "Conciliado")
        );
    }

    private FaturaResumoDTO mapearFatura(Map<String, Object> row, Map<String, TituloFinanceiro> titulos) {
        String documento = documentoFaturaOperacional(row);
        String uniqueId = texto(row, "ID Único", "ID Unico");
        TituloFinanceiro titulo = documento != null ? titulos.get(documento) : null;
        BigDecimal valorFinanceiro = titulo != null ? titulo.valor() : BigDecimal.ZERO;
        BigDecimal valorPago = titulo != null ? titulo.valorPago() : BigDecimal.ZERO;
        BigDecimal valorAberto = titulo != null ? titulo.valorAPagar() : BigDecimal.ZERO;

        return new FaturaResumoDTO(
                uniqueId,
                documento != null ? documento : uniqueId,
                emissaoFaturaOperacional(row),
                titulo != null ? titulo.vencimento() : texto(row, "Parcelas/Vencimento"),
                texto(row, "Filial"),
                texto(row, "Pagador do frete/Nome"),
                valorOperacionalFatura(row),
                valorFinanceiro,
                valorPago,
                valorAberto,
                statusProcesso(row),
                titulo != null ? titulo.status() : "Sem titulo"
        );
    }

    private FaturaPorClienteResumoDTO mapearFaturaPorCliente(Map<String, Object> row) {
        return new FaturaPorClienteResumoDTO(
                texto(row, "ID Único", "ID Unico"),
                documentoFaturaOperacional(row),
                emissaoFaturaOperacional(row),
                vencimentoFaturaOperacional(row),
                baixaFaturaOperacional(row),
                texto(row, "Filial"),
                texto(row, "Pagador do frete/Nome"),
                clienteCnpjOperacional(row),
                longo(row, "CT-e/Número", "CT-e/Numero"),
                valorOperacionalFatura(row),
                statusProcesso(row)
        );
    }

    private EtlExecucaoResumoDTO mapearEtlSaude(Map<String, Object> row) {
        return new EtlExecucaoResumoDTO(
                longo(row, "Id", "ID"),
                texto(row, "Inicio"),
                texto(row, "Fim"),
                inteiro(row, "Duracao (s)", "Duração (s)"),
                texto(row, "Data"),
                texto(row, "Status"),
                inteiro(row, "Total Registros"),
                texto(row, "Categoria Erro"),
                texto(row, "Mensagem Erro")
        );
    }

    private Map<String, TituloFinanceiro> buscarTitulosFinanceiros(FiltroConsultaDTO filtro, List<Map<String, Object>> rows) {
        List<String> documentos = rows.stream()
                .map(row -> texto(row, "Fatura/N° Documento", "Fatura/Nº Documento"))
                .filter(Objects::nonNull)
                .filter(documento -> !documento.isBlank())
                .distinct()
                .toList();

        if (documentos.isEmpty()) {
            return Map.of();
        }

        MapSqlParameterSource params = new MapSqlParameterSource("documentos", documentos);
        StringBuilder sql = new StringBuilder("""
                SELECT [Fatura/N° Documento], [Vencimento], [Valor], [Valor Pago], [Valor a Pagar], [Status]
                FROM [vw_faturas_graphql_powerbi]
                WHERE [Fatura/N° Documento] IN (:documentos)
                """);

        List<String> pagos = normalizar(filtro.valores("pago"));
        if (!pagos.isEmpty()) {
            params.addValue("pago", pagos);
            sql.append(" AND LOWER(LTRIM(RTRIM(CONVERT(NVARCHAR(MAX), [Pago])))) IN (:pago)");
        }

        String consultaTitulos = Objects.requireNonNull(sql.toString(), "consultaTitulos");
        return jdbcTemplate.queryForList(consultaTitulos, params).stream()
                .map(this::mapearTitulo)
                .filter(titulo -> titulo.documento() != null)
                .collect(Collectors.toMap(
                        TituloFinanceiro::documento,
                        Function.identity(),
                        (atual, ignorado) -> atual,
                        LinkedHashMap::new
                ));
    }

    private TituloFinanceiro mapearTitulo(Map<String, Object> row) {
        return new TituloFinanceiro(
                texto(row, "Fatura/N° Documento", "Fatura/Nº Documento"),
                texto(row, "Vencimento"),
                decimal(row, "Valor"),
                decimal(row, "Valor Pago"),
                decimal(row, "Valor a Pagar"),
                texto(row, "Status")
        );
    }

    private String documentoTipoFrete(Map<String, Object> row) {
        if (valor(row, "CT-e ID") != null) {
            return "CT-e";
        }
        if (valor(row, "Nº NFS-e", "N° NFS-e") != null) {
            return "NFS-e";
        }
        return "Pendente";
    }

    private BigDecimal valorOperacionalFatura(Map<String, Object> row) {
        Object valorFitAnt = viewFaturasClienteDeslocada(row)
                ? valor(row, "Fatura/Valor Total")
                : valor(row, "Fatura/Valor");
        if (valorFitAnt != null) {
            return decimalObjeto(valorFitAnt);
        }
        Object valorFatura = viewFaturasClienteDeslocada(row)
                ? valor(row, "Fatura/Número", "Fatura/Numero")
                : valor(row, "Fatura/Valor Total");
        if (valorFatura != null) {
            return decimalObjeto(valorFatura);
        }
        return decimal(row, "Frete/Valor dos CT-es");
    }

    private String statusProcesso(Map<String, Object> row) {
        String documento = texto(row, "Fatura/N° Documento", "Fatura/Nº Documento");
        if (ehStatusProcesso(documento)) {
            return documento;
        }
        return documento != null && !documento.isBlank() ? "Faturado" : "Aguardando Faturamento";
    }

    private boolean viewFaturasClienteDeslocada(Map<String, Object> row) {
        return ehStatusProcesso(texto(row, "Fatura/N° Documento", "Fatura/Nº Documento"));
    }

    private boolean ehStatusProcesso(String valor) {
        return valor != null
                && (valor.equalsIgnoreCase("Faturado")
                || valor.equalsIgnoreCase("Aguardando Faturamento"));
    }

    private String documentoFaturaOperacional(Map<String, Object> row) {
        return viewFaturasClienteDeslocada(row)
                ? texto(row, "Fatura/Emissão", "Fatura/Emissao")
                : texto(row, "Fatura/N° Documento", "Fatura/Nº Documento");
    }

    private String emissaoFaturaOperacional(Map<String, Object> row) {
        if (viewFaturasClienteDeslocada(row)) {
            return texto(row, "Fatura/Valor", "CT-e/Data de emissão", "CT-e/Data de emissao");
        }
        return texto(row, "Fatura/Emissão", "Fatura/Emissao", "Fatura/Emissão Fatura", "Fatura/Emissao Fatura", "CT-e/Data de emissão", "CT-e/Data de emissao");
    }

    private String vencimentoFaturaOperacional(Map<String, Object> row) {
        return viewFaturasClienteDeslocada(row)
                ? texto(row, "Fatura/Baixa")
                : texto(row, "Parcelas/Vencimento");
    }

    private String baixaFaturaOperacional(Map<String, Object> row) {
        return viewFaturasClienteDeslocada(row)
                ? texto(row, "Fatura/Data Vencimento Original")
                : texto(row, "Fatura/Baixa");
    }

    private String clienteCnpjOperacional(Map<String, Object> row) {
        return viewFaturasClienteDeslocada(row)
                ? texto(row, "Remetente/Nome", "Pagador do frete/Documento")
                : texto(row, "Cliente/CNPJ", "Pagador do frete/Documento");
    }

    private Object valor(Map<String, Object> row, String... colunas) {
        for (String coluna : colunas) {
            Object valor = row.get(coluna);
            if (valor != null) {
                return valor;
            }
        }
        return null;
    }

    private String texto(Map<String, Object> row, String... colunas) {
        Object valor = valor(row, colunas);
        return valor != null ? valor.toString() : null;
    }

    private Long longo(Map<String, Object> row, String... colunas) {
        Object valor = valor(row, colunas);
        if (valor == null) {
            return null;
        }
        if (valor instanceof Number numero) {
            return numero.longValue();
        }
        String texto = valor.toString().trim();
        if (texto.isEmpty()) {
            return null;
        }
        return new BigDecimal(texto.replace(",", ".")).longValue();
    }

    private Integer inteiro(Map<String, Object> row, String... colunas) {
        Object valor = valor(row, colunas);
        if (valor == null) {
            return null;
        }
        if (valor instanceof Number numero) {
            return numero.intValue();
        }
        String texto = valor.toString().trim();
        if (texto.isEmpty()) {
            return null;
        }
        return new BigDecimal(texto.replace(",", ".")).intValue();
    }

    private BigDecimal decimal(Map<String, Object> row, String... colunas) {
        return decimalObjeto(valor(row, colunas));
    }

    private BigDecimal decimalObjeto(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (valor instanceof BigDecimal decimal) {
            return decimal.setScale(2, RoundingMode.HALF_UP);
        }
        if (valor instanceof Number numero) {
            return BigDecimal.valueOf(numero.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        return ConsultaFiltroUtils.parseBigDecimal(valor.toString()).setScale(2, RoundingMode.HALF_UP);
    }

    private List<String> normalizar(Collection<String> valores) {
        return valores.stream()
                .filter(valor -> valor != null && !valor.isBlank())
                .map(valor -> valor.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private record TituloFinanceiro(
            String documento,
            String vencimento,
            BigDecimal valor,
            BigDecimal valorPago,
            BigDecimal valorAPagar,
            String status
    ) {
    }
}
