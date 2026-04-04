package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.indicadoresgestao.IndenizacaoMercadoriasOverviewDTO;
import com.dashboard.api.dto.indicadoresgestao.IndenizacaoMercadoriasRowDTO;
import com.dashboard.api.dto.indicadoresgestao.IndenizacaoMercadoriasSeriePointDTO;
import com.dashboard.api.model.VisaoFretesEntity;
import com.dashboard.api.model.VisaoSinistrosEntity;
import com.dashboard.api.repository.VisaoFretesRepository;
import com.dashboard.api.repository.VisaoSinistrosRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class IndenizacaoMercadoriasIndicadorService {

    private final ValidadorPeriodoService validadorPeriodo;
    private final VisaoSinistrosRepository sinistrosRepository;
    private final VisaoFretesRepository fretesRepository;
    private final EscopoFilialService escopoFilialService;
    private final PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper;

    IndenizacaoMercadoriasIndicadorService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoSinistrosRepository sinistrosRepository,
            VisaoFretesRepository fretesRepository
    ) {
        this(validadorPeriodo, sinistrosRepository, fretesRepository, escopoSemRestricao(), PeriodoOffsetDateTimeHelper.padrao());
    }

    @Autowired
    public IndenizacaoMercadoriasIndicadorService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoSinistrosRepository sinistrosRepository,
            VisaoFretesRepository fretesRepository,
            EscopoFilialService escopoFilialService,
            PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper
    ) {
        this.validadorPeriodo = validadorPeriodo;
        this.sinistrosRepository = sinistrosRepository;
        this.fretesRepository = fretesRepository;
        this.escopoFilialService = escopoFilialService;
        this.periodoOffsetDateTimeHelper = periodoOffsetDateTimeHelper;
    }

    public IndenizacaoMercadoriasOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        IndenizacaoContexto contexto = buscarContexto(filtro);
        BigDecimal valorIndenizadoAbs = contexto.registros().stream()
                .map(IndenizacaoRegistro::resultadoFinalAbs)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorIndenizadoOriginal = contexto.registros().stream()
                .map(IndenizacaoRegistro::resultadoFinalOriginal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal faturamentoBase = contexto.faturamentoPeriodoPorFilial().values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new IndenizacaoMercadoriasOverviewDTO(
                contexto.updatedAt(),
                contexto.registros().size(),
                valorIndenizadoAbs,
                valorIndenizadoOriginal,
                faturamentoBase,
                IndicadoresGestaoMetricasUtils.percentual(valorIndenizadoAbs, faturamentoBase)
        );
    }

    public List<IndenizacaoMercadoriasSeriePointDTO> buscarSerie(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        IndenizacaoContexto contexto = buscarContexto(filtro);
        return contexto.registros().stream()
                .filter(registro -> registro.dataAbertura() != null)
                .collect(Collectors.groupingBy(registro -> IndicadoresGestaoMetricasUtils.chaveSerie(registro.dataAbertura(), registro.filial())))
                .values().stream()
                .map(grupo -> {
                    IndenizacaoRegistro amostra = grupo.get(0);
                    BigDecimal valorIndenizadoAbs = grupo.stream()
                            .map(IndenizacaoRegistro::resultadoFinalAbs)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .setScale(2, RoundingMode.HALF_UP);
                    BigDecimal faturamentoBase = contexto.faturamentoDiarioPorFilial().getOrDefault(chaveDiaria(amostra.dataAbertura(), amostra.filial()), BigDecimal.ZERO)
                            .setScale(2, RoundingMode.HALF_UP);
                    return new IndenizacaoMercadoriasSeriePointDTO(
                            amostra.dataAbertura().toString(),
                            amostra.filial(),
                            grupo.size(),
                            valorIndenizadoAbs,
                            faturamentoBase,
                            IndicadoresGestaoMetricasUtils.percentual(valorIndenizadoAbs, faturamentoBase)
                    );
                })
                .sorted(Comparator.comparing(IndenizacaoMercadoriasSeriePointDTO::date)
                        .thenComparing(IndenizacaoMercadoriasSeriePointDTO::filial, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    public List<IndenizacaoMercadoriasRowDTO> buscarTabela(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 100, 500);

        IndenizacaoContexto contexto = buscarContexto(filtro);
        return contexto.registros().stream()
                .sorted(Comparator.comparing(IndenizacaoRegistro::dataAbertura, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(IndenizacaoRegistro::resultadoFinalAbs, Comparator.reverseOrder()))
                .limit(limiteAplicado)
                .map(registro -> {
                    BigDecimal faturamentoFilial = contexto.faturamentoPeriodoPorFilial().getOrDefault(registro.filial(), BigDecimal.ZERO);
                    return new IndenizacaoMercadoriasRowDTO(
                            registro.numeroSinistro(),
                            IndicadoresGestaoMetricasUtils.formatar(registro.dataAbertura()),
                            registro.filial(),
                            registro.minuta(),
                            registro.resultadoFinalOriginal().setScale(2, RoundingMode.HALF_UP),
                            registro.resultadoFinalAbs().setScale(2, RoundingMode.HALF_UP),
                            registro.ocorrencia(),
                            registro.solucao(),
                            IndicadoresGestaoMetricasUtils.percentual(registro.resultadoFinalAbs(), faturamentoFilial)
                    );
                })
                .toList();
    }

    private IndenizacaoContexto buscarContexto(FiltroConsultaDTO filtro) {
        JanelaOffsetDateTime janela = periodoOffsetDateTimeHelper.criarJanela(filtro.dataInicio(), filtro.dataFim());
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();

        List<VisaoFretesEntity> fretes = fretesRepository.findAll(criarFretesSpecification(filtro, escopo, janela));
        Map<Long, FreteResumo> fretePorMinuta = new LinkedHashMap<>();
        Map<String, BigDecimal> faturamentoPeriodoPorFilial = new LinkedHashMap<>();
        Map<String, BigDecimal> faturamentoDiarioPorFilial = new LinkedHashMap<>();

        for (VisaoFretesEntity frete : fretes) {
            String filial = primeiroTexto(frete.getFilialEmissora(), frete.getFilialNome());
            if (filial == null) {
                continue;
            }

            BigDecimal valorTotal = IndicadoresGestaoMetricasUtils.zero(frete.getValorTotal());
            faturamentoPeriodoPorFilial.merge(filial, valorTotal, BigDecimal::add);
            if (frete.getDataFrete() != null) {
                faturamentoDiarioPorFilial.merge(chaveDiaria(frete.getDataFrete().toLocalDate(), filial), valorTotal, BigDecimal::add);
            }

            if (frete.getNumeroMinuta() == null) {
                continue;
            }

            FreteResumo resumo = new FreteResumo(
                    frete.getNumeroMinuta(),
                    filial,
                    frete.getDataExtracao()
            );
            fretePorMinuta.merge(resumo.numeroMinuta(), resumo, this::preferirFreteMaisAtual);
        }

        List<VisaoSinistrosEntity> sinistros = sinistrosRepository.findAll(criarSinistrosSpecification(filtro));
        Map<Long, IndenizacaoRegistro> porSinistro = new LinkedHashMap<>();
        LocalDateTime updatedAt = fretes.stream()
                .map(VisaoFretesEntity::getDataExtracao)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(LocalDateTime.now());

        for (VisaoSinistrosEntity sinistro : sinistros) {
            Long numeroSinistro = sinistro.getNumeroSinistro();
            if (numeroSinistro == null) {
                continue;
            }

            Long minuta = sinistro.getMinuta();
            String filial = primeiroTexto(
                    fretePorMinuta.containsKey(minuta) ? fretePorMinuta.get(minuta).filial() : null,
                    sinistro.getPessoaNomeFantasia(),
                    "Não mapeada"
            );
            if (!escopo.permiteAlgumaFilial(filial) || !filtro.corresponde("filiais", filial)) {
                continue;
            }

            LocalDate dataAbertura = primeiraData(sinistro.getDataAbertura(), sinistro.getDataFinalizacao(), sinistro.getDataOcorrencia());
            BigDecimal resultadoOriginal = IndicadoresGestaoMetricasUtils.zero(sinistro.getResultadoFinal());
            IndenizacaoRegistro registro = new IndenizacaoRegistro(
                    numeroSinistro,
                    dataAbertura,
                    filial,
                    minuta,
                    resultadoOriginal,
                    IndicadoresGestaoMetricasUtils.abs(resultadoOriginal),
                    sinistro.getOcorrencia(),
                    sinistro.getSolucao(),
                    sinistro.getDataExtracao()
            );
            porSinistro.merge(numeroSinistro, registro, this::preferirRegistroMaisAtual);

            if (sinistro.getDataExtracao() != null && sinistro.getDataExtracao().isAfter(updatedAt)) {
                updatedAt = sinistro.getDataExtracao();
            }
        }

        return new IndenizacaoContexto(
                porSinistro.values().stream().toList(),
                faturamentoPeriodoPorFilial,
                faturamentoDiarioPorFilial,
                updatedAt.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
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
    private Specification<VisaoSinistrosEntity> criarSinistrosSpecification(FiltroConsultaDTO filtro) {
        return ConsultaSpecificationUtils.allOf(
                ConsultaSpecificationUtils.between("dataAbertura", filtro.dataInicio(), filtro.dataFim())
        );
    }

    private FreteResumo preferirFreteMaisAtual(FreteResumo atual, FreteResumo candidato) {
        if (atual.updatedAt() == null) {
            return candidato;
        }
        if (candidato.updatedAt() == null) {
            return atual;
        }
        return candidato.updatedAt().isAfter(atual.updatedAt()) ? candidato : atual;
    }

    private IndenizacaoRegistro preferirRegistroMaisAtual(IndenizacaoRegistro atual, IndenizacaoRegistro candidato) {
        if (atual.updatedAt() == null) {
            return candidato;
        }
        if (candidato.updatedAt() == null) {
            return atual;
        }
        return candidato.updatedAt().isAfter(atual.updatedAt()) ? candidato : atual;
    }

    private static String chaveDiaria(LocalDate data, String filial) {
        return IndicadoresGestaoMetricasUtils.chaveSerie(data, filial);
    }

    private static LocalDate primeiraData(LocalDate... datas) {
        for (LocalDate data : datas) {
            if (data != null) {
                return data;
            }
        }
        return null;
    }

    private static String primeiroTexto(String... valores) {
        for (String valor : valores) {
            if (valor != null && !valor.isBlank()) {
                return valor.trim();
            }
        }
        return null;
    }

    private static EscopoFilialService escopoSemRestricao() {
        return new EscopoFilialService(null, null) {
            @Override
            public EscopoFilial escopoAtual() {
                return EscopoFilial.comAcessoTotal();
            }
        };
    }

    private record FreteResumo(
            long numeroMinuta,
            String filial,
            LocalDateTime updatedAt
    ) {
    }

    private record IndenizacaoRegistro(
            long numeroSinistro,
            LocalDate dataAbertura,
            String filial,
            Long minuta,
            BigDecimal resultadoFinalOriginal,
            BigDecimal resultadoFinalAbs,
            String ocorrencia,
            String solucao,
            LocalDateTime updatedAt
    ) {
    }

    private record IndenizacaoContexto(
            List<IndenizacaoRegistro> registros,
            Map<String, BigDecimal> faturamentoPeriodoPorFilial,
            Map<String, BigDecimal> faturamentoDiarioPorFilial,
            String updatedAt
    ) {
    }
}
