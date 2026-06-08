package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.fretes.FreteResumoDTO;
import com.dashboard.api.dto.fretes.FretesChartsDTO;
import com.dashboard.api.dto.fretes.FretesClienteRankingDTO;
import com.dashboard.api.dto.fretes.FretesDocumentMixDTO;
import com.dashboard.api.dto.fretes.FretesFaturamentoDiarioDTO;
import com.dashboard.api.dto.fretes.FretesFaturamentoGrupoDTO;
import com.dashboard.api.dto.fretes.FretesGoalSummaryDTO;
import com.dashboard.api.dto.fretes.FretesOrigemDestinoDTO;
import com.dashboard.api.dto.fretes.FretesOverviewDTO;
import com.dashboard.api.dto.fretes.FretesPrevisaoPorStatusDTO;
import com.dashboard.api.dto.fretes.FretesTrendPointDTO;
import com.dashboard.api.filter.DashboardQueryFilters;
import com.dashboard.api.repository.VisaoFretesRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import com.dashboard.api.util.ConsultaFiltroUtils;
import com.dashboard.api.util.ConsultaLimiteUtils;
import com.dashboard.api.util.PeriodoOffsetDateTimeHelper;
import com.dashboard.api.util.TemporalJsonUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FretesService {

    private static final Logger log = LoggerFactory.getLogger(FretesService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ValidadorPeriodoService validadorPeriodo;
    private final VisaoFretesRepository repository;
    private final EscopoFilialService escopoFilialService;
    private final FretesGoalService fretesGoalService;

    FretesService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoFretesRepository repository
    ) {
        this(validadorPeriodo, repository, escopoSemRestricao(), PeriodoOffsetDateTimeHelper.padrao(), null);
    }

    @Autowired
    public FretesService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoFretesRepository repository,
            EscopoFilialService escopoFilialService,
            PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper,
            FretesGoalService fretesGoalService
    ) {
        this.validadorPeriodo = validadorPeriodo;
        this.repository = repository;
        this.escopoFilialService = escopoFilialService;
        this.fretesGoalService = fretesGoalService;
    }

    FretesService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoFretesRepository repository,
            EscopoFilialService escopoFilialService,
            PeriodoOffsetDateTimeHelper periodoOffsetDateTimeHelper
    ) {
        this(validadorPeriodo, repository, escopoFilialService, periodoOffsetDateTimeHelper, null);
    }

    public FretesOverviewDTO buscarOverview(LocalDate dataInicio, LocalDate dataFim) {
        return buscarOverview(new FiltroConsultaDTO(dataInicio, dataFim, Map.of()));
    }

    public FretesOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        FretesConsulta consulta = consulta(filtro);
        VisaoFretesRepository.FretesOverviewProjection overview = buscarOverviewAgregado(consulta);
        int totalFretes = overview != null ? overview.getTotalFretes() : 0;
        FretesGoalSummaryDTO metas = buscarResumoMetas(filtro);

        if (totalFretes == 0) {
            return new FretesOverviewDTO(
                    TemporalJsonUtils.formatarIsoComOffset(null),
                    0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, 0, 0.0, 0.0, 0,
                    metas.metaFaturamento(),
                    metas.percentualAtingimentoFaturamento(),
                    calcularFaturamentoDiario(filtro.dataFim(), metas.metaFaturamento(), BigDecimal.ZERO)
            );
        }

        BigDecimal receitaBruta = zero(overview.getReceitaBruta()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorFrete = zero(overview.getValorFrete()).setScale(2, RoundingMode.HALF_UP);
        int fretesFaturamento = overview.getFretesFaturamento();
        BigDecimal ticketMedio = fretesFaturamento == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : receitaBruta.divide(BigDecimal.valueOf(fretesFaturamento), 2, RoundingMode.HALF_UP);

        log.info("Overview fretes calculado: totalFretes={}, periodo={} a {}", totalFretes, filtro.dataInicio(), filtro.dataFim());

        return new FretesOverviewDTO(
                formatarAtualizacao(overview.getUpdatedAt()),
                totalFretes,
                receitaBruta,
                valorFrete,
                ticketMedio,
                zero(overview.getPesoTaxadoTotal()).setScale(2, RoundingMode.HALF_UP),
                overview.getVolumesTotais(),
                percentual(overview.getCteEmitidos(), totalFretes),
                percentual(overview.getNfseEmitidas(), totalFretes),
                overview.getFretesPrevisaoVencida(),
                metas.metaFaturamento(),
                metas.percentualAtingimentoFaturamento(),
                calcularFaturamentoDiario(filtro.dataFim(), metas.metaFaturamento(), receitaBruta)
        );
    }

    public FretesGoalSummaryDTO buscarResumoMetas(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        return buscarResumoMetas(filtro, buscarRealizadosPorFilial(filtro));
    }

    public List<FretesTrendPointDTO> buscarSerieTemporal(LocalDate dataInicio, LocalDate dataFim) {
        return buscarSerieTemporal(new FiltroConsultaDTO(dataInicio, dataFim, Map.of()));
    }

    public List<FretesTrendPointDTO> buscarSerieTemporal(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        return buscarSerieTemporalAgregada(consulta(filtro)).stream()
                .map(row -> new FretesTrendPointDTO(
                        row.getDate(),
                        zero(row.getReceitaBruta()).setScale(2, RoundingMode.HALF_UP),
                        zero(row.getValorFrete()).setScale(2, RoundingMode.HALF_UP),
                        row.getFretes()
                ))
                .toList();
    }

    public List<FretesClienteRankingDTO> buscarTopClientes(LocalDate dataInicio, LocalDate dataFim, int limite) {
        return buscarTopClientes(new FiltroConsultaDTO(dataInicio, dataFim, Map.of()), limite);
    }

    public List<FretesClienteRankingDTO> buscarTopClientes(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 10, 50);

        return buscarTopClientesAgregado(consulta(filtro), limiteAplicado).stream()
                .map(row -> new FretesClienteRankingDTO(
                        row.getCliente(),
                        zero(row.getReceita()).setScale(2, RoundingMode.HALF_UP),
                        row.getFretes(),
                        zero(row.getTicketMedio()).setScale(2, RoundingMode.HALF_UP)
                ))
                .toList();
    }

    public List<FretesDocumentMixDTO> buscarMixDocumental(LocalDate dataInicio, LocalDate dataFim) {
        return buscarMixDocumental(new FiltroConsultaDTO(dataInicio, dataFim, Map.of()));
    }

    public List<FretesDocumentMixDTO> buscarMixDocumental(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        Map<String, Integer> totais = buscarMixDocumentalAgregado(consulta(filtro)).stream()
                .collect(java.util.stream.Collectors.toMap(
                        VisaoFretesRepository.FretesDocumentMixProjection::getTipoDocumento,
                        VisaoFretesRepository.FretesDocumentMixProjection::getTotal
                ));
        return List.of(
                new FretesDocumentMixDTO("CT-e", totais.getOrDefault("CT-e", 0)),
                new FretesDocumentMixDTO("NFS-e", totais.getOrDefault("NFS-e", 0)),
                new FretesDocumentMixDTO("Pendente", totais.getOrDefault("Pendente", 0))
        );
    }

    public List<FreteResumoDTO> buscarTabela(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 100, 200);

        return buscarTabelaPaginada(consulta(filtro), limiteAplicado).stream()
                .map(row -> new FreteResumoDTO(
                        row.getId(),
                        row.getNumeroMinuta(),
                        row.getDataReferenciaFaturamento(),
                        origemDataFaturamento(row.getCteEmissao()),
                        row.getStatus(),
                        row.getFilial(),
                        row.getPagador(),
                        row.getRemetente(),
                        row.getDestinatario(),
                        row.getOrigemUf(),
                        row.getDestinoUf(),
                        zero(row.getValorTotalServico()).setScale(2, RoundingMode.HALF_UP),
                        zero(row.getValorFrete()).setScale(2, RoundingMode.HALF_UP),
                        zero(row.getPesoTaxado()).setScale(2, RoundingMode.HALF_UP),
                        row.getVolumes(),
                        row.getPrevisaoEntrega() != null ? row.getPrevisaoEntrega().toString() : null,
                        row.getDocumentoTipo(),
                        row.getNumeroCte(),
                        row.getNumeroNfse(),
                        zero(row.getValorIcms()).setScale(2, RoundingMode.HALF_UP),
                        zero(row.getValorPis()).setScale(2, RoundingMode.HALF_UP),
                        zero(row.getValorCofins()).setScale(2, RoundingMode.HALF_UP)
                ))
                .toList();
    }

    public FretesChartsDTO buscarGraficos(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        FretesConsulta consulta = consulta(filtro);

        return new FretesChartsDTO(
                buscarPrevisaoPorStatusAgregada(consulta).stream()
                        .map(row -> new FretesPrevisaoPorStatusDTO(row.getStatus(), row.getVencidos(), row.getNoPrazo()))
                        .toList(),
                buscarTopRotasPorReceita(consulta).stream()
                        .map(row -> new FretesOrigemDestinoDTO(
                                row.getOrigemUf(),
                                row.getDestinoUf(),
                                zero(row.getReceita()).setScale(2, RoundingMode.HALF_UP),
                                row.getFretes()
                        ))
                        .toList(),
                mapearGrupos(buscarFaturamentoPorClassificacao(consulta)),
                mapearGrupos(buscarFaturamentoPorResponsavelDestino(consulta)),
                mapearGrupos(buscarFaturamentoPorUfOrigem(consulta)),
                mapearGrupos(buscarFaturamentoPorUfDestino(consulta)),
                mapearGrupos(buscarFaturamentoPorCidadeDestino(consulta))
        );
    }

    private FretesGoalSummaryDTO buscarResumoMetas(
            FiltroConsultaDTO filtro,
            Collection<FretesGoalService.FretesBranchRealizado> realizados
    ) {
        if (fretesGoalService == null) {
            return fallbackResumoMetas(filtro, realizados);
        }
        try {
            return fretesGoalService.buscarResumo(
                    filtro.dataInicio(),
                    filtro.dataFim(),
                    realizados,
                    filtro.valores("filiais")
            );
        } catch (RuntimeException ex) {
            log.warn("Não foi possível carregar metas de fretes. O relatório seguirá sem metas configuradas. Motivo: {}", ex.getMessage());
            return fallbackResumoMetas(filtro, realizados);
        }
    }

    private FretesGoalSummaryDTO fallbackResumoMetas(
            FiltroConsultaDTO filtro,
            Collection<FretesGoalService.FretesBranchRealizado> realizados
    ) {
        BigDecimal realizadoFaturamento = realizados.stream()
                .map(FretesGoalService.FretesBranchRealizado::realizadoFaturamento)
                .map(this::zero)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        return new FretesGoalSummaryDTO(
                filtro.dataInicio().format(DATE_FMT),
                filtro.dataFim().format(DATE_FMT),
                BigDecimal.ZERO,
                realizadoFaturamento,
                0.0,
                List.of()
        );
    }

    private FretesFaturamentoDiarioDTO calcularFaturamentoDiario(
            LocalDate referencia,
            BigDecimal metaFaturamento,
            BigDecimal realizadoFaturamento
    ) {
        BigDecimal meta = ConsultaFiltroUtils.zeroSeNulo(metaFaturamento).setScale(2, RoundingMode.HALF_UP);
        BigDecimal realizado = ConsultaFiltroUtils.zeroSeNulo(realizadoFaturamento).setScale(2, RoundingMode.HALF_UP);
        int totalDiasUteisMes = Math.max(1, contarDiasUteis(
                referencia.withDayOfMonth(1),
                referencia.withDayOfMonth(referencia.lengthOfMonth())
        ));
        int diasUteisDecorridos = Math.max(1, contarDiasUteis(referencia.withDayOfMonth(1), referencia));
        int diasUteisRestantes = Math.max(0, totalDiasUteisMes - diasUteisDecorridos);
        int divisorDiasRestantes = Math.max(1, diasUteisRestantes);

        BigDecimal metaDiariaBase = dividir(meta, totalDiasUteisMes);
        BigDecimal faturamentoDiarioReal = dividir(realizado, diasUteisDecorridos);
        BigDecimal faturamentoFaltante = meta.subtract(realizado).setScale(2, RoundingMode.HALF_UP);
        BigDecimal metaDiariaDinamica = dividir(faturamentoFaltante, divisorDiasRestantes);
        BigDecimal tendenciaFaturamento = faturamentoDiarioReal
                .multiply(BigDecimal.valueOf(totalDiasUteisMes))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal tendenciaPercentual = meta.compareTo(BigDecimal.ZERO) > 0
                ? tendenciaFaturamento.divide(meta, 6, RoundingMode.HALF_UP).subtract(BigDecimal.ONE)
                : BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);

        return new FretesFaturamentoDiarioDTO(
                totalDiasUteisMes,
                diasUteisDecorridos,
                diasUteisRestantes,
                metaDiariaBase,
                faturamentoDiarioReal,
                metaDiariaDinamica,
                faturamentoFaltante,
                tendenciaFaturamento,
                tendenciaPercentual
        );
    }

    private int contarDiasUteis(LocalDate inicio, LocalDate fim) {
        if (inicio.isAfter(fim)) {
            return 0;
        }

        int total = 0;
        for (LocalDate data = inicio; !data.isAfter(fim); data = data.plusDays(1)) {
            DayOfWeek dia = data.getDayOfWeek();
            if (dia != DayOfWeek.SATURDAY && dia != DayOfWeek.SUNDAY) {
                total++;
            }
        }
        return total;
    }

    private BigDecimal dividir(BigDecimal valor, int divisor) {
        if (divisor <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return valor.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP);
    }

    private double percentual(long valor, int total) {
        if (total == 0) {
            return 0.0;
        }

        return BigDecimal.valueOf(valor)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private String origemDataFaturamento(String cteEmissao) {
        return cteEmissao != null ? "CT-e Emissão" : "Data do Frete";
    }

    private BigDecimal zero(BigDecimal valor) {
        return ConsultaFiltroUtils.zeroSeNulo(valor);
    }

    private String formatarAtualizacao(LocalDateTime updatedAt) {
        return TemporalJsonUtils.formatarIsoComOffset(updatedAt);
    }

    private List<FretesFaturamentoGrupoDTO> mapearGrupos(
            List<VisaoFretesRepository.FretesFaturamentoGrupoProjection> rows
    ) {
        return rows.stream()
                .map(row -> new FretesFaturamentoGrupoDTO(
                        row.getNome(),
                        zero(row.getReceita()).setScale(2, RoundingMode.HALF_UP),
                        row.getFretes()
                ))
                .toList();
    }

    private List<FretesGoalService.FretesBranchRealizado> buscarRealizadosPorFilial(FiltroConsultaDTO filtro) {
        return buscarRealizadoFaturamentoPorFilial(consulta(filtro)).stream()
                .map(row -> new FretesGoalService.FretesBranchRealizado(
                        row.getFilial(),
                        zero(row.getRealizadoFaturamento()).setScale(2, RoundingMode.HALF_UP)
                ))
                .toList();
    }

    private FretesConsulta consulta(FiltroConsultaDTO filtro) {
        return new FretesConsulta(
                filtro.dataInicio(),
                filtro.dataFim().plusDays(1),
                DashboardQueryFilters.escopo(escopoFilialService.escopoAtual()),
                DashboardQueryFilters.of(filtro.valores("filiais")),
                DashboardQueryFilters.of(filtro.valores("status")),
                DashboardQueryFilters.of(filtro.valores("pagadores")),
                DashboardQueryFilters.of(filtro.valores("responsaveis")),
                DashboardQueryFilters.of(filtro.valores("ufOrigem")),
                DashboardQueryFilters.of(filtro.valores("ufDestino")),
                DashboardQueryFilters.of(filtro.valores("tiposFrete")),
                DashboardQueryFilters.of(filtro.valores("modais"))
        );
    }

    private VisaoFretesRepository.FretesOverviewProjection buscarOverviewAgregado(FretesConsulta consulta) {
        return repository.buscarOverviewAgregado(
                consulta.dataInicio(), consulta.dataFimExclusivo(),
                consulta.escopo().valores(), consulta.escopo().vazio(),
                consulta.filiais().valores(), consulta.filiais().vazio(),
                consulta.status().valores(), consulta.status().vazio(),
                consulta.pagadores().valores(), consulta.pagadores().vazio(),
                consulta.responsaveis().valores(), consulta.responsaveis().vazio(),
                consulta.ufOrigem().valores(), consulta.ufOrigem().vazio(),
                consulta.ufDestino().valores(), consulta.ufDestino().vazio(),
                consulta.tiposFrete().valores(), consulta.tiposFrete().vazio(),
                consulta.modais().valores(), consulta.modais().vazio()
        );
    }

    private List<VisaoFretesRepository.FretesTrendProjection> buscarSerieTemporalAgregada(FretesConsulta consulta) {
        return repository.buscarSerieTemporalAgregada(
                consulta.dataInicio(), consulta.dataFimExclusivo(),
                consulta.escopo().valores(), consulta.escopo().vazio(),
                consulta.filiais().valores(), consulta.filiais().vazio(),
                consulta.status().valores(), consulta.status().vazio(),
                consulta.pagadores().valores(), consulta.pagadores().vazio(),
                consulta.responsaveis().valores(), consulta.responsaveis().vazio(),
                consulta.ufOrigem().valores(), consulta.ufOrigem().vazio(),
                consulta.ufDestino().valores(), consulta.ufDestino().vazio(),
                consulta.tiposFrete().valores(), consulta.tiposFrete().vazio(),
                consulta.modais().valores(), consulta.modais().vazio()
        );
    }

    private List<VisaoFretesRepository.FretesClienteRankingProjection> buscarTopClientesAgregado(
            FretesConsulta consulta,
            int limite
    ) {
        return repository.buscarTopClientesAgregado(
                consulta.dataInicio(), consulta.dataFimExclusivo(),
                consulta.escopo().valores(), consulta.escopo().vazio(),
                consulta.filiais().valores(), consulta.filiais().vazio(),
                consulta.status().valores(), consulta.status().vazio(),
                consulta.pagadores().valores(), consulta.pagadores().vazio(),
                consulta.responsaveis().valores(), consulta.responsaveis().vazio(),
                consulta.ufOrigem().valores(), consulta.ufOrigem().vazio(),
                consulta.ufDestino().valores(), consulta.ufDestino().vazio(),
                consulta.tiposFrete().valores(), consulta.tiposFrete().vazio(),
                consulta.modais().valores(), consulta.modais().vazio(),
                limite
        );
    }

    private List<VisaoFretesRepository.FretesDocumentMixProjection> buscarMixDocumentalAgregado(FretesConsulta consulta) {
        return repository.buscarMixDocumentalAgregado(
                consulta.dataInicio(), consulta.dataFimExclusivo(),
                consulta.escopo().valores(), consulta.escopo().vazio(),
                consulta.filiais().valores(), consulta.filiais().vazio(),
                consulta.status().valores(), consulta.status().vazio(),
                consulta.pagadores().valores(), consulta.pagadores().vazio(),
                consulta.responsaveis().valores(), consulta.responsaveis().vazio(),
                consulta.ufOrigem().valores(), consulta.ufOrigem().vazio(),
                consulta.ufDestino().valores(), consulta.ufDestino().vazio(),
                consulta.tiposFrete().valores(), consulta.tiposFrete().vazio(),
                consulta.modais().valores(), consulta.modais().vazio()
        );
    }

    private List<VisaoFretesRepository.FretesTabelaProjection> buscarTabelaPaginada(FretesConsulta consulta, int limite) {
        return repository.buscarTabelaPaginada(
                consulta.dataInicio(), consulta.dataFimExclusivo(),
                consulta.escopo().valores(), consulta.escopo().vazio(),
                consulta.filiais().valores(), consulta.filiais().vazio(),
                consulta.status().valores(), consulta.status().vazio(),
                consulta.pagadores().valores(), consulta.pagadores().vazio(),
                consulta.responsaveis().valores(), consulta.responsaveis().vazio(),
                consulta.ufOrigem().valores(), consulta.ufOrigem().vazio(),
                consulta.ufDestino().valores(), consulta.ufDestino().vazio(),
                consulta.tiposFrete().valores(), consulta.tiposFrete().vazio(),
                consulta.modais().valores(), consulta.modais().vazio(),
                limite
        );
    }

    private List<VisaoFretesRepository.FretesPrevisaoStatusProjection> buscarPrevisaoPorStatusAgregada(FretesConsulta consulta) {
        return repository.buscarPrevisaoPorStatusAgregada(
                consulta.dataInicio(), consulta.dataFimExclusivo(),
                consulta.escopo().valores(), consulta.escopo().vazio(),
                consulta.filiais().valores(), consulta.filiais().vazio(),
                consulta.status().valores(), consulta.status().vazio(),
                consulta.pagadores().valores(), consulta.pagadores().vazio(),
                consulta.responsaveis().valores(), consulta.responsaveis().vazio(),
                consulta.ufOrigem().valores(), consulta.ufOrigem().vazio(),
                consulta.ufDestino().valores(), consulta.ufDestino().vazio(),
                consulta.tiposFrete().valores(), consulta.tiposFrete().vazio(),
                consulta.modais().valores(), consulta.modais().vazio()
        );
    }

    private List<VisaoFretesRepository.FretesRotaProjection> buscarTopRotasPorReceita(FretesConsulta consulta) {
        return repository.buscarTopRotasPorReceita(
                consulta.dataInicio(), consulta.dataFimExclusivo(),
                consulta.escopo().valores(), consulta.escopo().vazio(),
                consulta.filiais().valores(), consulta.filiais().vazio(),
                consulta.status().valores(), consulta.status().vazio(),
                consulta.pagadores().valores(), consulta.pagadores().vazio(),
                consulta.responsaveis().valores(), consulta.responsaveis().vazio(),
                consulta.ufOrigem().valores(), consulta.ufOrigem().vazio(),
                consulta.ufDestino().valores(), consulta.ufDestino().vazio(),
                consulta.tiposFrete().valores(), consulta.tiposFrete().vazio(),
                consulta.modais().valores(), consulta.modais().vazio()
        );
    }

    private List<VisaoFretesRepository.FretesFaturamentoGrupoProjection> buscarFaturamentoPorClassificacao(FretesConsulta consulta) {
        return repository.buscarFaturamentoPorClassificacao(
                consulta.dataInicio(), consulta.dataFimExclusivo(),
                consulta.escopo().valores(), consulta.escopo().vazio(),
                consulta.filiais().valores(), consulta.filiais().vazio(),
                consulta.status().valores(), consulta.status().vazio(),
                consulta.pagadores().valores(), consulta.pagadores().vazio(),
                consulta.responsaveis().valores(), consulta.responsaveis().vazio(),
                consulta.ufOrigem().valores(), consulta.ufOrigem().vazio(),
                consulta.ufDestino().valores(), consulta.ufDestino().vazio(),
                consulta.tiposFrete().valores(), consulta.tiposFrete().vazio(),
                consulta.modais().valores(), consulta.modais().vazio()
        );
    }

    private List<VisaoFretesRepository.FretesFaturamentoGrupoProjection> buscarFaturamentoPorResponsavelDestino(FretesConsulta consulta) {
        return repository.buscarFaturamentoPorResponsavelDestino(
                consulta.dataInicio(), consulta.dataFimExclusivo(),
                consulta.escopo().valores(), consulta.escopo().vazio(),
                consulta.filiais().valores(), consulta.filiais().vazio(),
                consulta.status().valores(), consulta.status().vazio(),
                consulta.pagadores().valores(), consulta.pagadores().vazio(),
                consulta.responsaveis().valores(), consulta.responsaveis().vazio(),
                consulta.ufOrigem().valores(), consulta.ufOrigem().vazio(),
                consulta.ufDestino().valores(), consulta.ufDestino().vazio(),
                consulta.tiposFrete().valores(), consulta.tiposFrete().vazio(),
                consulta.modais().valores(), consulta.modais().vazio()
        );
    }

    private List<VisaoFretesRepository.FretesFaturamentoGrupoProjection> buscarFaturamentoPorUfOrigem(FretesConsulta consulta) {
        return repository.buscarFaturamentoPorUfOrigem(
                consulta.dataInicio(), consulta.dataFimExclusivo(),
                consulta.escopo().valores(), consulta.escopo().vazio(),
                consulta.filiais().valores(), consulta.filiais().vazio(),
                consulta.status().valores(), consulta.status().vazio(),
                consulta.pagadores().valores(), consulta.pagadores().vazio(),
                consulta.responsaveis().valores(), consulta.responsaveis().vazio(),
                consulta.ufOrigem().valores(), consulta.ufOrigem().vazio(),
                consulta.ufDestino().valores(), consulta.ufDestino().vazio(),
                consulta.tiposFrete().valores(), consulta.tiposFrete().vazio(),
                consulta.modais().valores(), consulta.modais().vazio()
        );
    }

    private List<VisaoFretesRepository.FretesFaturamentoGrupoProjection> buscarFaturamentoPorUfDestino(FretesConsulta consulta) {
        return repository.buscarFaturamentoPorUfDestino(
                consulta.dataInicio(), consulta.dataFimExclusivo(),
                consulta.escopo().valores(), consulta.escopo().vazio(),
                consulta.filiais().valores(), consulta.filiais().vazio(),
                consulta.status().valores(), consulta.status().vazio(),
                consulta.pagadores().valores(), consulta.pagadores().vazio(),
                consulta.responsaveis().valores(), consulta.responsaveis().vazio(),
                consulta.ufOrigem().valores(), consulta.ufOrigem().vazio(),
                consulta.ufDestino().valores(), consulta.ufDestino().vazio(),
                consulta.tiposFrete().valores(), consulta.tiposFrete().vazio(),
                consulta.modais().valores(), consulta.modais().vazio()
        );
    }

    private List<VisaoFretesRepository.FretesFaturamentoGrupoProjection> buscarFaturamentoPorCidadeDestino(FretesConsulta consulta) {
        return repository.buscarFaturamentoPorCidadeDestino(
                consulta.dataInicio(), consulta.dataFimExclusivo(),
                consulta.escopo().valores(), consulta.escopo().vazio(),
                consulta.filiais().valores(), consulta.filiais().vazio(),
                consulta.status().valores(), consulta.status().vazio(),
                consulta.pagadores().valores(), consulta.pagadores().vazio(),
                consulta.responsaveis().valores(), consulta.responsaveis().vazio(),
                consulta.ufOrigem().valores(), consulta.ufOrigem().vazio(),
                consulta.ufDestino().valores(), consulta.ufDestino().vazio(),
                consulta.tiposFrete().valores(), consulta.tiposFrete().vazio(),
                consulta.modais().valores(), consulta.modais().vazio()
        );
    }

    private List<VisaoFretesRepository.FretesRealizadoFilialProjection> buscarRealizadoFaturamentoPorFilial(FretesConsulta consulta) {
        return repository.buscarRealizadoFaturamentoPorFilial(
                consulta.dataInicio(), consulta.dataFimExclusivo(),
                consulta.escopo().valores(), consulta.escopo().vazio(),
                consulta.filiais().valores(), consulta.filiais().vazio(),
                consulta.status().valores(), consulta.status().vazio(),
                consulta.pagadores().valores(), consulta.pagadores().vazio(),
                consulta.responsaveis().valores(), consulta.responsaveis().vazio(),
                consulta.ufOrigem().valores(), consulta.ufOrigem().vazio(),
                consulta.ufDestino().valores(), consulta.ufDestino().vazio(),
                consulta.tiposFrete().valores(), consulta.tiposFrete().vazio(),
                consulta.modais().valores(), consulta.modais().vazio()
        );
    }

    private record FretesConsulta(
            LocalDate dataInicio,
            LocalDate dataFimExclusivo,
            DashboardQueryFilters.ParametroLista escopo,
            DashboardQueryFilters.ParametroLista filiais,
            DashboardQueryFilters.ParametroLista status,
            DashboardQueryFilters.ParametroLista pagadores,
            DashboardQueryFilters.ParametroLista responsaveis,
            DashboardQueryFilters.ParametroLista ufOrigem,
            DashboardQueryFilters.ParametroLista ufDestino,
            DashboardQueryFilters.ParametroLista tiposFrete,
            DashboardQueryFilters.ParametroLista modais
    ) {
    }

    private static EscopoFilialService escopoSemRestricao() {
        return new EscopoFilialService(null, null) {
            @Override
            public EscopoFilial escopoAtual() {
                return EscopoFilial.comAcessoTotal();
            }
        };
    }
}
