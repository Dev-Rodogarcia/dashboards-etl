package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.coletas.ColetasTrendPointDTO;
import com.dashboard.api.dto.contaspagar.ContasAPagarMensalTrendDTO;
import com.dashboard.api.dto.contaspagar.ContasAPagarOverviewDTO;
import com.dashboard.api.dto.executivo.ExecutivoOverviewDTO;
import com.dashboard.api.dto.executivo.ExecutivoTrendPointDTO;
import com.dashboard.api.dto.faturas.FaturasMensalTrendDTO;
import com.dashboard.api.dto.faturas.FaturasOverviewDTO;
import com.dashboard.api.dto.fretes.FretesOverviewDTO;
import com.dashboard.api.dto.fretes.FretesTrendPointDTO;
import com.dashboard.api.dto.manifestos.ManifestosOverviewDTO;
import com.dashboard.api.dto.tracking.TrackingOverviewDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExecutivoService {

    private static final Logger log = LoggerFactory.getLogger(ExecutivoService.class);
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final FretesService fretesService;
    private final FaturasService faturasService;
    private final ContasAPagarService contasAPagarService;
    private final ColetasService coletasService;
    private final TrackingService trackingService;
    private final ManifestosService manifestosService;

    public ExecutivoService(
            FretesService fretesService,
            FaturasService faturasService,
            ContasAPagarService contasAPagarService,
            ColetasService coletasService,
            TrackingService trackingService,
            ManifestosService manifestosService) {
        this.fretesService = fretesService;
        this.faturasService = faturasService;
        this.contasAPagarService = contasAPagarService;
        this.coletasService = coletasService;
        this.trackingService = trackingService;
        this.manifestosService = manifestosService;
    }

    public ExecutivoOverviewDTO buscarOverview(LocalDate dataInicio, LocalDate dataFim) {
        return buscarOverview(new FiltroConsultaDTO(dataInicio, dataFim, Map.of()));
    }

    public ExecutivoOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        log.info("Calculando overview executivo: periodo={} a {}", filtro.dataInicio(), filtro.dataFim());

        FretesOverviewDTO fretes = fretesService.buscarOverview(filtro);
        FaturasOverviewDTO faturas = faturasService.buscarOverview(filtro);
        ContasAPagarOverviewDTO contasAPagar = contasAPagarService.buscarOverview(filtro);
        var coletas = coletasService.buscarOverview(filtro);
        TrackingOverviewDTO tracking = trackingService.buscarOverview(filtro);
        ManifestosOverviewDTO manifestos = manifestosService.buscarOverview(filtro);

        return new ExecutivoOverviewDTO(
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                fretes.receitaBruta(),
                faturas.valorFaturado(),
                faturas.saldoAberto(),
                contasAPagar.saldoAberto(),
                coletas.totalColetas() - coletas.finalizadas(),
                tracking.previsaoVencida(),
                manifestos.ocupacaoPesoMediaPct()
        );
    }

    public List<ExecutivoTrendPointDTO> buscarSerie(FiltroConsultaDTO filtro) {
        Map<YearMonth, ExecutivoTrendAccumulator> acc = new HashMap<>();

        for (FretesTrendPointDTO ponto : fretesService.buscarSerieTemporal(filtro)) {
            YearMonth chave = YearMonth.parse(ponto.date().substring(0, 7), MONTH_FMT);
            acc.computeIfAbsent(chave, ignored -> new ExecutivoTrendAccumulator()).receitaOperacional =
                    acc.getOrDefault(chave, new ExecutivoTrendAccumulator()).receitaOperacional.add(ponto.receitaBruta());
        }

        for (FaturasMensalTrendDTO ponto : faturasService.buscarMensal(filtro)) {
            YearMonth chave = YearMonth.parse(ponto.month(), MONTH_FMT);
            ExecutivoTrendAccumulator item = acc.computeIfAbsent(chave, ignored -> new ExecutivoTrendAccumulator());
            item.valorFaturado = item.valorFaturado.add(ponto.faturado());
            item.saldoAReceber = item.saldoAReceber.add(ponto.saldoAberto());
        }

        for (ContasAPagarMensalTrendDTO ponto : contasAPagarService.buscarSerie(filtro)) {
            YearMonth chave = YearMonth.parse(ponto.month(), MONTH_FMT);
            ExecutivoTrendAccumulator item = acc.computeIfAbsent(chave, ignored -> new ExecutivoTrendAccumulator());
            item.saldoAPagar = item.saldoAPagar.add(ponto.aberto());
        }

        for (ColetasTrendPointDTO ponto : coletasService.buscarSerieTemporal(filtro)) {
            YearMonth chave = YearMonth.parse(ponto.date().substring(0, 7), MONTH_FMT);
            ExecutivoTrendAccumulator item = acc.computeIfAbsent(chave, ignored -> new ExecutivoTrendAccumulator());
            item.backlogColetas += Math.max(0, ponto.total() - ponto.finalizadas() - ponto.canceladas());
        }

        return acc.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new ExecutivoTrendPointDTO(
                        entry.getKey().format(MONTH_FMT),
                        entry.getValue().receitaOperacional,
                        entry.getValue().valorFaturado,
                        entry.getValue().saldoAReceber,
                        entry.getValue().saldoAPagar,
                        entry.getValue().backlogColetas
                ))
                .toList();
    }

    private static final class ExecutivoTrendAccumulator {
        private BigDecimal receitaOperacional = BigDecimal.ZERO;
        private BigDecimal valorFaturado = BigDecimal.ZERO;
        private BigDecimal saldoAReceber = BigDecimal.ZERO;
        private BigDecimal saldoAPagar = BigDecimal.ZERO;
        private int backlogColetas = 0;
    }
}
