package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.PaginaDTO;
import com.dashboard.api.dto.indicadoresgestao.IndenizacaoMercadoriasOverviewDTO;
import com.dashboard.api.dto.indicadoresgestao.IndenizacaoMercadoriasRowDTO;
import com.dashboard.api.dto.indicadoresgestao.IndenizacaoMercadoriasSeriePointDTO;
import com.dashboard.api.model.VisaoFretesEntity;
import com.dashboard.api.model.VisaoSinistrosEntity;
import com.dashboard.api.repository.VisaoFretesRepository;
import com.dashboard.api.repository.VisaoSinistrosRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
        BigDecimal valorIndenizadoOriginal = contexto.registros().stream()
                .map(IndenizacaoRegistro::valorAPagarCliente)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorIndenizadoAbs = IndicadoresGestaoMetricasUtils.abs(valorIndenizadoOriginal)
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
                .collect(Collectors.groupingBy(registro -> chaveMensal(primeiroDiaMes(registro.dataAbertura()), registro.filial())))
                .values().stream()
                .map(grupo -> {
                    IndenizacaoRegistro amostra = grupo.get(0);
                    LocalDate mesRef = primeiroDiaMes(amostra.dataAbertura());
                    BigDecimal valorIndenizadoOriginal = grupo.stream()
                            .map(IndenizacaoRegistro::valorAPagarCliente)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .setScale(2, RoundingMode.HALF_UP);
                    BigDecimal valorIndenizadoAbs = IndicadoresGestaoMetricasUtils.abs(valorIndenizadoOriginal)
                            .setScale(2, RoundingMode.HALF_UP);
                    BigDecimal faturamentoBase = contexto.faturamentoMensalPorFilial().getOrDefault(chaveMensal(mesRef, amostra.filial()), BigDecimal.ZERO)
                            .setScale(2, RoundingMode.HALF_UP);
                    BigDecimal faturamentoPeriodoFilial = contexto.faturamentoPeriodoPorFilial().getOrDefault(amostra.filial(), BigDecimal.ZERO)
                            .setScale(2, RoundingMode.HALF_UP);
                    return new IndenizacaoMercadoriasSeriePointDTO(
                            mesRef.toString(),
                            amostra.filial(),
                            grupo.size(),
                            valorIndenizadoOriginal,
                            valorIndenizadoAbs,
                            faturamentoBase,
                            faturamentoPeriodoFilial,
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
                        .thenComparing(IndenizacaoRegistro::valorAPagarClienteAbs, Comparator.reverseOrder()))
                .limit(limiteAplicado)
                .map(registro -> toRow(registro, contexto.faturamentoPeriodoPorFilial()))
                .toList();
    }

    public List<IndenizacaoMercadoriasRowDTO> buscarExportacao(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        IndenizacaoContexto contexto = buscarContexto(filtro);
        return contexto.registros().stream()
                .sorted(Comparator.comparing(IndenizacaoRegistro::dataAbertura, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(IndenizacaoRegistro::valorAPagarClienteAbs, Comparator.reverseOrder()))
                .map(registro -> toRow(registro, contexto.faturamentoPeriodoPorFilial()))
                .toList();
    }

    public PaginaDTO<IndenizacaoMercadoriasRowDTO> buscarTabelaPaginada(FiltroConsultaDTO filtro, int pagina, int tamanhoPagina) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int paginaAplicada = Math.max(1, pagina);
        int tamanhoAplicado = ConsultaLimiteUtils.limitar(tamanhoPagina, 10, 100);
        JanelaOffsetDateTime janela = periodoOffsetDateTimeHelper.criarJanela(filtro.dataInicio(), filtro.dataFim());
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        FaturamentoBase faturamentoBase = calcularFaturamentoBase(consultarFretes(filtro, escopo, janela), filtro);

        Page<VisaoSinistrosEntity> paginaSinistros = sinistrosRepository.findAll(
                criarSinistrosSpecification(filtro, escopo),
                PageRequest.of(
                        paginaAplicada - 1,
                        tamanhoAplicado,
                        Sort.by(
                                Sort.Order.desc("dataAbertura"),
                                Sort.Order.desc("valorAPagarCliente"),
                                Sort.Order.desc("numeroSinistro")
                        )
                )
        );

        Map<Long, IndenizacaoRegistro> porSinistro = new LinkedHashMap<>();
        for (VisaoSinistrosEntity sinistro : paginaSinistros.getContent()) {
            IndenizacaoRegistro registro = criarRegistroSinistro(sinistro, filtro, escopo);
            if (registro != null && dataNoPeriodo(registro.dataAbertura(), filtro.dataInicio(), filtro.dataFim())) {
                porSinistro.merge(registro.numeroSinistro(), registro, this::preferirRegistroMaisAtual);
            }
        }

        return new PaginaDTO<>(
                porSinistro.values().stream()
                        .map(registro -> toRow(registro, faturamentoBase.faturamentoPeriodoPorFilial()))
                        .toList(),
                paginaSinistros.getTotalElements(),
                paginaSinistros.getTotalPages(),
                paginaAplicada,
                tamanhoAplicado
        );
    }

    private IndenizacaoContexto buscarContexto(FiltroConsultaDTO filtro) {
        JanelaOffsetDateTime janela = periodoOffsetDateTimeHelper.criarJanela(filtro.dataInicio(), filtro.dataFim());
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();

        List<VisaoFretesEntity> fretes = consultarFretes(filtro, escopo, janela);
        FaturamentoBase faturamentoBase = calcularFaturamentoBase(fretes, filtro);

        List<VisaoSinistrosEntity> sinistros = consultarSinistros(filtro, escopo);
        Map<Long, IndenizacaoRegistro> porSinistro = new LinkedHashMap<>();
        LocalDateTime updatedAt = faturamentoBase.updatedAt() != null ? faturamentoBase.updatedAt() : LocalDateTime.now();

        for (VisaoSinistrosEntity sinistro : sinistros) {
            IndenizacaoRegistro registro = criarRegistroSinistro(sinistro, filtro, escopo);
            if (registro != null) {
                porSinistro.merge(registro.numeroSinistro(), registro, this::preferirRegistroMaisAtual);
                updatedAt = dataMaisRecente(updatedAt, sinistro.getDataExtracao());
            }
        }

        return new IndenizacaoContexto(
                porSinistro.values().stream()
                        .filter(registro -> dataNoPeriodo(registro.dataAbertura(), filtro.dataInicio(), filtro.dataFim()))
                        .toList(),
                faturamentoBase.faturamentoPeriodoPorFilial(),
                faturamentoBase.faturamentoMensalPorFilial(),
                updatedAt.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }

    private FaturamentoBase calcularFaturamentoBase(List<VisaoFretesEntity> fretes, FiltroConsultaDTO filtro) {
        Map<String, BigDecimal> faturamentoPeriodoPorFilial = new LinkedHashMap<>();
        Map<String, BigDecimal> faturamentoMensalPorFilial = new LinkedHashMap<>();
        LocalDateTime updatedAtFretes = null;

        for (VisaoFretesEntity frete : fretes) {
            if (!IndicadoresGestaoMetricasUtils.freteComValorOperacionalElegivel(frete)) {
                continue;
            }

            String filial = primeiroTexto(frete.getFilialEmissora(), frete.getFilialNome());
            if (filial == null || !filtro.corresponde("filiais", filial)) {
                continue;
            }

            BigDecimal valorTotal = IndicadoresGestaoMetricasUtils.zero(frete.getValorTotal());
            faturamentoPeriodoPorFilial.merge(filial, valorTotal, BigDecimal::add);
            if (frete.getDataFrete() != null) {
                LocalDate mesRef = primeiroDiaMes(frete.getDataFrete().toLocalDate());
                faturamentoMensalPorFilial.merge(chaveMensal(mesRef, filial), valorTotal, BigDecimal::add);
            }
            updatedAtFretes = dataMaisRecente(updatedAtFretes, frete.getDataExtracao());
        }

        return new FaturamentoBase(faturamentoPeriodoPorFilial, faturamentoMensalPorFilial, updatedAtFretes);
    }

    private IndenizacaoRegistro criarRegistroSinistro(
            VisaoSinistrosEntity sinistro,
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo
    ) {
        Long numeroSinistro = sinistro.getNumeroSinistro();
        if (numeroSinistro == null) {
            return null;
        }

        String filial = primeiroTexto(sinistro.getPessoaNomeFantasia(), "Não mapeada");
        if (!escopo.permiteAlgumaFilial(filial) || !filtro.corresponde("filiais", filial)) {
            return null;
        }

        BigDecimal valorAPagarCliente = IndicadoresGestaoMetricasUtils.zero(sinistro.getValorAPagarCliente());
        return new IndenizacaoRegistro(
                numeroSinistro,
                sinistro.getDataAbertura(),
                filial,
                sinistro.getMinuta(),
                valorAPagarCliente,
                IndicadoresGestaoMetricasUtils.abs(valorAPagarCliente),
                sinistro.getOcorrenciaDescricao(),
                sinistro.getSolucao(),
                sinistro.getDataExtracao()
        );
    }

    private IndenizacaoMercadoriasRowDTO toRow(
            IndenizacaoRegistro registro,
            Map<String, BigDecimal> faturamentoPeriodoPorFilial
    ) {
        BigDecimal faturamentoFilial = faturamentoPeriodoPorFilial.getOrDefault(registro.filial(), BigDecimal.ZERO);
        return new IndenizacaoMercadoriasRowDTO(
                registro.numeroSinistro(),
                IndicadoresGestaoMetricasUtils.formatar(registro.dataAbertura()),
                registro.filial(),
                registro.minuta(),
                registro.valorAPagarCliente().setScale(2, RoundingMode.HALF_UP),
                registro.valorAPagarClienteAbs().setScale(2, RoundingMode.HALF_UP),
                registro.causaRaiz(),
                registro.solucao(),
                IndicadoresGestaoMetricasUtils.percentual(registro.valorAPagarClienteAbs(), faturamentoFilial)
        );
    }

    private List<VisaoFretesEntity> consultarFretes(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo,
            JanelaOffsetDateTime janela
    ) {
        return fretesRepository.findAll(criarFretesSpecification(filtro, escopo, janela));
    }

    private List<VisaoSinistrosEntity> consultarSinistros(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo
    ) {
        return sinistrosRepository.findAll(criarSinistrosSpecification(filtro, escopo));
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
    private Specification<VisaoSinistrosEntity> criarSinistrosSpecification(
            FiltroConsultaDTO filtro,
            EscopoFilialService.EscopoFilial escopo
    ) {
        return ConsultaSpecificationUtils.allOf(
                ConsultaSpecificationUtils.between("dataAbertura", filtro.dataInicio(), filtro.dataFim()),
                ConsultaSpecificationUtils.escopoFiliais(escopo, "pessoaNomeFantasia"),
                ConsultaSpecificationUtils.filtroTexto(filtro, "filiais", "pessoaNomeFantasia")
        );
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

    private static String chaveMensal(LocalDate data, String filial) {
        return IndicadoresGestaoMetricasUtils.chaveSerie(data, filial);
    }

    private static LocalDateTime dataMaisRecente(LocalDateTime atual, LocalDateTime candidato) {
        if (candidato == null) {
            return atual;
        }
        if (atual == null || candidato.isAfter(atual)) {
            return candidato;
        }
        return atual;
    }

    private static LocalDate primeiroDiaMes(LocalDate data) {
        return data != null ? data.withDayOfMonth(1) : null;
    }

    private static boolean dataNoPeriodo(LocalDate data, LocalDate inicio, LocalDate fim) {
        return data != null && !data.isBefore(inicio) && !data.isAfter(fim);
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

    private record IndenizacaoRegistro(
            long numeroSinistro,
            LocalDate dataAbertura,
            String filial,
            Long minuta,
            BigDecimal valorAPagarCliente,
            BigDecimal valorAPagarClienteAbs,
            String causaRaiz,
            String solucao,
            LocalDateTime updatedAt
    ) {
    }

    private record IndenizacaoContexto(
            List<IndenizacaoRegistro> registros,
            Map<String, BigDecimal> faturamentoPeriodoPorFilial,
            Map<String, BigDecimal> faturamentoMensalPorFilial,
            String updatedAt
    ) {
    }

    private record FaturamentoBase(
            Map<String, BigDecimal> faturamentoPeriodoPorFilial,
            Map<String, BigDecimal> faturamentoMensalPorFilial,
            LocalDateTime updatedAt
    ) {
    }
}
