package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.indicadoresgestao.PerformanceEntregaOverviewDTO;
import com.dashboard.api.dto.indicadoresgestao.PerformanceEntregaRowDTO;
import com.dashboard.api.dto.indicadoresgestao.PerformanceEntregaSeriePointDTO;
import com.dashboard.api.model.VisaoFretesEntity;
import com.dashboard.api.model.VisaoLocalizacaoCargasEntity;
import com.dashboard.api.repository.VisaoFretesRepository;
import com.dashboard.api.repository.VisaoLocalizacaoCargasRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PerformanceEntregaIndicadorService {

    private final ValidadorPeriodoService validadorPeriodo;
    private final VisaoFretesRepository fretesRepository;
    private final VisaoLocalizacaoCargasRepository localizacaoRepository;
    private final EscopoFilialService escopoFilialService;
    private final PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper;

    PerformanceEntregaIndicadorService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoFretesRepository fretesRepository,
            VisaoLocalizacaoCargasRepository localizacaoRepository
    ) {
        this(validadorPeriodo, fretesRepository, localizacaoRepository, escopoSemRestricao(), PeriodoOffsetDateTimeHelper.padrao());
    }

    @Autowired
    public PerformanceEntregaIndicadorService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoFretesRepository fretesRepository,
            VisaoLocalizacaoCargasRepository localizacaoRepository,
            EscopoFilialService escopoFilialService,
            PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper
    ) {
        this.validadorPeriodo = validadorPeriodo;
        this.fretesRepository = fretesRepository;
        this.localizacaoRepository = localizacaoRepository;
        this.escopoFilialService = escopoFilialService;
        this.periodoOffsetDateTimeHelper = periodoOffsetDateTimeHelper;
    }

    public PerformanceEntregaOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        List<PerformanceEntregaRegistro> registros = buscarRegistros(filtro);
        if (registros.isEmpty()) {
            return new PerformanceEntregaOverviewDTO(
                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    0,
                    0,
                    0,
                    0.0
            );
        }

        int totalEntregas = registros.size();
        int entregasNoPrazo = (int) registros.stream()
                .filter(PerformanceEntregaRegistro::noPrazo)
                .count();
        int entregasSemDados = (int) registros.stream()
                .filter(PerformanceEntregaRegistro::semDados)
                .count();

        return new PerformanceEntregaOverviewDTO(
                IndicadoresGestaoMetricasUtils.latestUpdate(registros, PerformanceEntregaRegistro::updatedAt),
                totalEntregas,
                entregasNoPrazo,
                entregasSemDados,
                IndicadoresGestaoMetricasUtils.percentual(entregasNoPrazo, totalEntregas)
        );
    }

    public List<PerformanceEntregaSeriePointDTO> buscarSerie(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        return buscarRegistros(filtro).stream()
                .filter(registro -> registro.dataReferencia() != null)
                .collect(Collectors.groupingBy(registro -> IndicadoresGestaoMetricasUtils.chaveSerie(registro.dataReferencia(), registro.responsavelRegiaoDestino())))
                .values().stream()
                .map(grupo -> {
                    PerformanceEntregaRegistro amostra = grupo.get(0);
                    int totalEntregas = grupo.size();
                    int entregasNoPrazo = (int) grupo.stream().filter(PerformanceEntregaRegistro::noPrazo).count();
                    int entregasSemDados = (int) grupo.stream().filter(PerformanceEntregaRegistro::semDados).count();
                    return new PerformanceEntregaSeriePointDTO(
                            amostra.dataReferencia().toString(),
                            amostra.responsavelRegiaoDestino(),
                            totalEntregas,
                            entregasNoPrazo,
                            entregasSemDados,
                            IndicadoresGestaoMetricasUtils.percentual(entregasNoPrazo, totalEntregas)
                    );
                })
                .sorted(Comparator.comparing(PerformanceEntregaSeriePointDTO::date)
                        .thenComparing(PerformanceEntregaSeriePointDTO::responsavelRegiaoDestino, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    public List<PerformanceEntregaRowDTO> buscarTabela(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 100, 500);

        return buscarRegistros(filtro).stream()
                .sorted(Comparator.comparing(PerformanceEntregaRegistro::dataFinalizacao, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(PerformanceEntregaRegistro::dataFrete, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(PerformanceEntregaRegistro::numeroMinuta, Comparator.reverseOrder()))
                .limit(limiteAplicado)
                .map(registro -> new PerformanceEntregaRowDTO(
                        registro.numeroMinuta(),
                        IndicadoresGestaoMetricasUtils.formatar(registro.dataFrete()),
                        registro.filialEmissora(),
                        registro.responsavelRegiaoDestino(),
                        IndicadoresGestaoMetricasUtils.formatar(registro.previsaoEntrega()),
                        IndicadoresGestaoMetricasUtils.formatar(registro.dataFinalizacao()),
                        registro.performanceDiferencaDias(),
                        registro.performanceStatus()
                ))
                .toList();
    }

    private List<PerformanceEntregaRegistro> buscarRegistros(FiltroConsultaDTO filtro) {
        JanelaOffsetDateTime janela = periodoOffsetDateTimeHelper.criarJanela(filtro.dataInicio(), filtro.dataFim());
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();

        List<VisaoFretesEntity> fretes = fretesRepository.findAll(criarFretesSpecification(filtro, escopo, janela));
        Map<Long, String> responsavelDestinoPorMinuta = localizacaoRepository.findAll(criarLocalizacaoSpecification(janela)).stream()
                .filter(item -> item.getSequenceNumber() != null)
                .collect(Collectors.toMap(
                        VisaoLocalizacaoCargasEntity::getSequenceNumber,
                        item -> primeiroTexto(
                                item.getResponsavelRegiaoDestino(),
                                item.getFilialDestino(),
                                item.getFilialEmissora()
                        ),
                        PerformanceEntregaIndicadorService::preferirTextoMaisCompleto,
                        LinkedHashMap::new
                ));

        Map<Long, PerformanceEntregaRegistro> porMinuta = new LinkedHashMap<>();
        for (VisaoFretesEntity frete : fretes) {
            Long numeroMinuta = frete.getNumeroMinuta();
            if (numeroMinuta == null) {
                continue;
            }

            String filialEmissora = primeiroTexto(frete.getFilialEmissora(), frete.getFilialNome());
            String responsavelRegiaoDestino = primeiroTexto(
                    frete.getResponsavelRegiaoDestino(),
                    responsavelDestinoPorMinuta.get(numeroMinuta),
                    filialEmissora
            );

            PerformanceEntregaRegistro registro = new PerformanceEntregaRegistro(
                    numeroMinuta,
                    frete.getDataFrete(),
                    frete.getPrevisaoEntrega(),
                    frete.getDataFinalizacao(),
                    responsavelRegiaoDestino,
                    filialEmissora,
                    frete.getPerformanceDiferencaDias(),
                    textoOuNulo(frete.getPerformanceStatus()),
                    frete.getDataExtracao()
            );

            porMinuta.merge(numeroMinuta, registro, this::preferirRegistroMaisCompleto);
        }

        return porMinuta.values().stream().toList();
    }

    @NonNull
    private Specification<VisaoFretesEntity> criarFretesSpecification(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo,
            JanelaOffsetDateTime janela
    ) {
        return ConsultaSpecificationUtils.allOf(
                ConsultaSpecificationUtils.greaterThanOrEqualTo("dataFrete", janela.inicioInclusivo()),
                ConsultaSpecificationUtils.lessThan("dataFrete", janela.fimExclusivo()),
                ConsultaSpecificationUtils.escopoFiliais(escopo, "filialEmissora", "filialNome"),
                ConsultaSpecificationUtils.filtroTextoQualquerCampo(filtro, "filiais", "filialEmissora", "filialNome")
        );
    }

    @NonNull
    private Specification<VisaoLocalizacaoCargasEntity> criarLocalizacaoSpecification(JanelaOffsetDateTime janela) {
        return ConsultaSpecificationUtils.allOf(
                ConsultaSpecificationUtils.greaterThanOrEqualTo("dataFrete", janela.inicioInclusivo()),
                ConsultaSpecificationUtils.lessThan("dataFrete", janela.fimExclusivo())
        );
    }

    private PerformanceEntregaRegistro preferirRegistroMaisCompleto(
            PerformanceEntregaRegistro atual,
            PerformanceEntregaRegistro candidato
    ) {
        return pontuacao(candidato) > pontuacao(atual) ? candidato : atual;
    }

    private int pontuacao(PerformanceEntregaRegistro registro) {
        int score = 0;
        if (registro.dataFinalizacao() != null) {
            score += 4;
        }
        if (registro.performanceStatus() != null) {
            score += 3;
        }
        if (registro.responsavelRegiaoDestino() != null) {
            score += 2;
        }
        if (registro.updatedAt() != null) {
            score += 1;
        }
        return score;
    }

    private static String preferirTextoMaisCompleto(String atual, String candidato) {
        if (atual == null || atual.isBlank()) {
            return candidato;
        }
        if (candidato == null || candidato.isBlank()) {
            return atual;
        }
        return candidato.length() > atual.length() ? candidato : atual;
    }

    private static String primeiroTexto(String... valores) {
        for (String valor : valores) {
            if (valor != null && !valor.isBlank()) {
                return valor.trim();
            }
        }
        return null;
    }

    private static String textoOuNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim().toUpperCase(Locale.ROOT);
    }

    private static EscopoFilialService escopoSemRestricao() {
        return new EscopoFilialService(null, null) {
            @Override
            public EscopoFilial escopoAtual() {
                return EscopoFilial.comAcessoTotal();
            }
        };
    }

    private record PerformanceEntregaRegistro(
            long numeroMinuta,
            OffsetDateTime dataFrete,
            LocalDate previsaoEntrega,
            LocalDate dataFinalizacao,
            String responsavelRegiaoDestino,
            String filialEmissora,
            Integer performanceDiferencaDias,
            String performanceStatus,
            LocalDateTime updatedAt
    ) {
        private LocalDate dataReferencia() {
            if (dataFinalizacao != null) {
                return dataFinalizacao;
            }
            return dataFrete != null ? dataFrete.toLocalDate() : null;
        }

        private boolean noPrazo() {
            return "NO PRAZO".equalsIgnoreCase(performanceStatus);
        }

        private boolean semDados() {
            return performanceStatus == null || performanceStatus.isBlank();
        }
    }
}
