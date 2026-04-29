package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.PaginaDTO;
import com.dashboard.api.dto.indicadoresgestao.CubagemMercadoriasOverviewDTO;
import com.dashboard.api.dto.indicadoresgestao.CubagemMercadoriasRowDTO;
import com.dashboard.api.dto.indicadoresgestao.CubagemMercadoriasSeriePointDTO;
import com.dashboard.api.model.VisaoFretesEntity;
import com.dashboard.api.repository.VisaoFretesRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CubagemMercadoriasIndicadorService {

    private static final String STATUS_CANCELADO = "cancelado";

    private final ValidadorPeriodoService validadorPeriodo;
    private final VisaoFretesRepository fretesRepository;
    private final EscopoFilialService escopoFilialService;
    private final PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper;
    private final Set<String> remetenteDocsExcluidos;

    CubagemMercadoriasIndicadorService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoFretesRepository fretesRepository
    ) {
        this(validadorPeriodo, fretesRepository, escopoSemRestricao(), PeriodoOffsetDateTimeHelper.padrao(), "");
    }

    @Autowired
    public CubagemMercadoriasIndicadorService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoFretesRepository fretesRepository,
            EscopoFilialService escopoFilialService,
            PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper,
            @Value("${dashboard.indicadores.cubagem.remetente-docs-excluidos:}") String remetenteDocsExcluidosConfigurados
    ) {
        this.validadorPeriodo = validadorPeriodo;
        this.fretesRepository = fretesRepository;
        this.escopoFilialService = escopoFilialService;
        this.periodoOffsetDateTimeHelper = periodoOffsetDateTimeHelper;
        this.remetenteDocsExcluidos = normalizarDocumentosConfigurados(remetenteDocsExcluidosConfigurados);
    }

    public CubagemMercadoriasOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        List<CubagemRegistro> registros = buscarRegistros(filtro);
        if (registros.isEmpty()) {
            return new CubagemMercadoriasOverviewDTO(
                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    0,
                    0,
                    0,
                    0.0
            );
        }

        int totalFretes = registros.size();
        int fretesCubados = (int) registros.stream().filter(CubagemRegistro::cubado).count();
        int fretesComPesoReal = (int) registros.stream()
                .filter(registro -> IndicadoresGestaoMetricasUtils.zero(registro.pesoReal()).compareTo(BigDecimal.ZERO) > 0)
                .count();

        return new CubagemMercadoriasOverviewDTO(
                IndicadoresGestaoMetricasUtils.latestUpdate(registros, CubagemRegistro::updatedAt),
                totalFretes,
                fretesCubados,
                fretesComPesoReal,
                IndicadoresGestaoMetricasUtils.percentual(fretesCubados, totalFretes)
        );
    }

    public List<CubagemMercadoriasSeriePointDTO> buscarSerie(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        return buscarRegistros(filtro).stream()
                .filter(registro -> registro.dataFrete() != null)
                .collect(Collectors.groupingBy(registro -> IndicadoresGestaoMetricasUtils.chaveSerie(registro.dataFrete().toLocalDate(), registro.filialEmissora())))
                .values().stream()
                .map(grupo -> {
                    CubagemRegistro amostra = grupo.get(0);
                    int totalFretes = grupo.size();
                    int fretesCubados = (int) grupo.stream().filter(CubagemRegistro::cubado).count();
                    return new CubagemMercadoriasSeriePointDTO(
                            amostra.dataFrete().toLocalDate().toString(),
                            amostra.filialEmissora(),
                            totalFretes,
                            fretesCubados,
                            IndicadoresGestaoMetricasUtils.percentual(fretesCubados, totalFretes)
                    );
                })
                .sorted(Comparator.comparing(CubagemMercadoriasSeriePointDTO::date)
                        .thenComparing(CubagemMercadoriasSeriePointDTO::filial, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    public List<CubagemMercadoriasRowDTO> buscarTabela(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 100, 500);

        return buscarRegistros(filtro).stream()
                .sorted(Comparator.comparing(CubagemRegistro::dataFrete, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(CubagemRegistro::numeroMinuta, Comparator.reverseOrder()))
                .limit(limiteAplicado)
                .map(registro -> new CubagemMercadoriasRowDTO(
                        registro.numeroMinuta(),
                        IndicadoresGestaoMetricasUtils.formatar(registro.dataFrete()),
                        registro.filialEmissora(),
                        registro.pagador(),
                        registro.remetenteDocumento(),
                        registro.destino(),
                        IndicadoresGestaoMetricasUtils.zero(registro.pesoTaxado()),
                        IndicadoresGestaoMetricasUtils.zero(registro.pesoReal()),
                        IndicadoresGestaoMetricasUtils.zero(registro.pesoCubado()),
                        IndicadoresGestaoMetricasUtils.zero(registro.totalM3()),
                        registro.cubado()
                ))
                .toList();
    }

    public List<CubagemMercadoriasRowDTO> buscarExportacao(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        return buscarRegistros(filtro).stream()
                .sorted(Comparator.comparing(CubagemRegistro::dataFrete, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(CubagemRegistro::numeroMinuta, Comparator.reverseOrder()))
                .map(registro -> new CubagemMercadoriasRowDTO(
                        registro.numeroMinuta(),
                        IndicadoresGestaoMetricasUtils.formatar(registro.dataFrete()),
                        registro.filialEmissora(),
                        registro.pagador(),
                        registro.remetenteDocumento(),
                        registro.destino(),
                        IndicadoresGestaoMetricasUtils.zero(registro.pesoTaxado()),
                        IndicadoresGestaoMetricasUtils.zero(registro.pesoReal()),
                        IndicadoresGestaoMetricasUtils.zero(registro.pesoCubado()),
                        IndicadoresGestaoMetricasUtils.zero(registro.totalM3()),
                        registro.cubado()
                ))
                .toList();
    }

    public PaginaDTO<CubagemMercadoriasRowDTO> buscarTabelaPaginada(FiltroConsultaDTO filtro, int pagina, int tamanhoPagina) {
        return PaginacaoListaUtils.paginar(buscarExportacao(filtro), pagina, tamanhoPagina);
    }

    private List<CubagemRegistro> buscarRegistros(FiltroConsultaDTO filtro) {
        JanelaOffsetDateTime janela = periodoOffsetDateTimeHelper.criarJanela(filtro.dataInicio(), filtro.dataFim());
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();

        Map<Long, CubagemRegistro> porMinuta = new LinkedHashMap<>();
        for (VisaoFretesEntity frete : fretesRepository.findAll(criarSpecification(filtro, escopo, janela))) {
            Long numeroMinuta = frete.getNumeroMinuta();
            if (numeroMinuta == null || statusCancelado(frete.getStatus()) || remetenteDocumentoExcluido(frete.getRemetenteDocumento())) {
                continue;
            }

            CubagemRegistro registro = new CubagemRegistro(
                    numeroMinuta,
                    frete.getDataFrete(),
                    textoOuPadrao(frete.getFilialEmissora(), frete.getFilialNome()),
                    frete.getPagadorNome(),
                    normalizarDocumento(frete.getRemetenteDocumento()),
                    frete.getDestinoCidade(),
                    frete.getPesoTaxado(),
                    frete.getPesoReal(),
                    frete.getTotalM3(),
                    frete.getPesoCubado(),
                    frete.getDataExtracao()
            );

            porMinuta.merge(numeroMinuta, registro, this::preferirRegistroMaisAtual);
        }

        return porMinuta.values().stream().toList();
    }

    @NonNull
    private Specification<VisaoFretesEntity> criarSpecification(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo,
            JanelaOffsetDateTime janela
    ) {
        return ConsultaSpecificationUtils.allOf(
                ConsultaSpecificationUtils.greaterThanOrEqualTo("dataFrete", janela.inicioInclusivo()),
                ConsultaSpecificationUtils.lessThan("dataFrete", janela.fimExclusivo()),
                (root, query, cb) -> cb.or(
                        cb.isNull(root.get("status")),
                        cb.notEqual(cb.lower(root.get("status")), STATUS_CANCELADO)
                ),
                ConsultaSpecificationUtils.escopoFiliais(escopo, "filialEmissora", "filialNome"),
                ConsultaSpecificationUtils.filtroTextoQualquerCampo(filtro, "filiais", "filialEmissora", "filialNome")
        );
    }

    private CubagemRegistro preferirRegistroMaisAtual(CubagemRegistro atual, CubagemRegistro candidato) {
        if (atual.updatedAt() == null) {
            return candidato;
        }
        if (candidato.updatedAt() == null) {
            return atual;
        }
        return candidato.updatedAt().isAfter(atual.updatedAt()) ? candidato : atual;
    }

    private boolean remetenteDocumentoExcluido(String remetenteDocumento) {
        String documentoNormalizado = normalizarDocumento(remetenteDocumento);
        return documentoNormalizado != null && remetenteDocsExcluidos.contains(documentoNormalizado);
    }

    private static String textoOuPadrao(String valor, String fallback) {
        if (valor != null && !valor.isBlank()) {
            return valor.trim();
        }
        return fallback != null && !fallback.isBlank() ? fallback.trim() : "Não informado";
    }

    private static boolean statusCancelado(String status) {
        return status != null && STATUS_CANCELADO.equalsIgnoreCase(status.trim());
    }

    private static Set<String> normalizarDocumentosConfigurados(String remetenteDocsExcluidosConfigurados) {
        if (remetenteDocsExcluidosConfigurados == null || remetenteDocsExcluidosConfigurados.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(remetenteDocsExcluidosConfigurados.split("[,;\\r\\n]+"))
                .map(CubagemMercadoriasIndicadorService::normalizarDocumento)
                .filter(documento -> documento != null && !documento.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String normalizarDocumento(String documento) {
        if (documento == null) {
            return null;
        }
        String normalizado = documento.replaceAll("[^0-9A-Za-z]", "");
        return normalizado.isBlank() ? null : normalizado;
    }

    private static EscopoFilialService escopoSemRestricao() {
        return new EscopoFilialService(null, null) {
            @Override
            public EscopoFilial escopoAtual() {
                return EscopoFilial.comAcessoTotal();
            }
        };
    }

    private record CubagemRegistro(
            long numeroMinuta,
            java.time.OffsetDateTime dataFrete,
            String filialEmissora,
            String pagador,
            String remetenteDocumento,
            String destino,
            BigDecimal pesoTaxado,
            BigDecimal pesoReal,
            BigDecimal totalM3,
            BigDecimal pesoCubado,
            LocalDateTime updatedAt
    ) {
        private boolean cubado() {
            return IndicadoresGestaoMetricasUtils.zero(totalM3).compareTo(BigDecimal.ZERO) > 0
                    || IndicadoresGestaoMetricasUtils.zero(pesoCubado).compareTo(BigDecimal.ZERO) > 0;
        }
    }
}
