package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.indicadoresgestao.HorarioCorteRowDTO;
import com.dashboard.api.dto.indicadoresgestao.HorariosCorteOverviewDTO;
import com.dashboard.api.dto.indicadoresgestao.HorariosCorteSeriePointDTO;
import com.dashboard.api.model.VisaoHorariosCorteEntity;
import com.dashboard.api.repository.VisaoHorariosCorteRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class IndicadoresGestaoAVistaService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final ValidadorPeriodoService validadorPeriodo;
    private final VisaoHorariosCorteRepository repository;
    private final EscopoFilialService escopoFilialService;
    private final HorarioCorteFilialMapperService filialMapperService;

    public IndicadoresGestaoAVistaService(
            ValidadorPeriodoService validadorPeriodo,
            VisaoHorariosCorteRepository repository,
            EscopoFilialService escopoFilialService,
            HorarioCorteFilialMapperService filialMapperService
    ) {
        this.validadorPeriodo = validadorPeriodo;
        this.repository = repository;
        this.escopoFilialService = escopoFilialService;
        this.filialMapperService = filialMapperService;
    }

    public HorariosCorteOverviewDTO buscarHorariosCorteOverview(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        List<HorarioCorteRegistroResolvido> rows = buscarHorariosCorte(filtro);
        if (rows.isEmpty()) {
            return new HorariosCorteOverviewDTO(
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    0,
                    0,
                    0.0,
                    null,
                    null
            );
        }

        int totalProgramado = rows.size();
        int saidasNoHorario = (int) rows.stream()
                .filter(row -> Boolean.TRUE.equals(row.entity().getSaiuNoHorario()))
                .count();
        double pctNoHorario = percentual(saidasNoHorario, totalProgramado);

        HorarioCorteRegistroResolvido ultimaImportacao = rows.stream()
                .filter(row -> row.entity().getImportadoEm() != null)
                .max(Comparator.comparing(row -> row.entity().getImportadoEm()))
                .orElse(null);

        return new HorariosCorteOverviewDTO(
                ConsultaFiltroUtils.latestUpdate(rows, row -> row.entity().getDataExtracao()),
                saidasNoHorario,
                totalProgramado,
                pctNoHorario,
                ultimaImportacao != null ? ultimaImportacao.entity().getImportadoEm().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null,
                ultimaImportacao != null ? ultimaImportacao.entity().getNomeArquivo() : null
        );
    }

    public List<HorariosCorteSeriePointDTO> buscarHorariosCorteSerie(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        Map<String, List<HorarioCorteRegistroResolvido>> agrupado = buscarHorariosCorte(filtro).stream()
                .collect(Collectors.groupingBy(row -> chaveSerie(row.entity().getData(), row.filial())));

        return agrupado.entrySet().stream()
                .map(entry -> {
                    List<HorarioCorteRegistroResolvido> grupo = entry.getValue();
                    HorarioCorteRegistroResolvido amostra = grupo.get(0);
                    int totalProgramado = grupo.size();
                    int saidasNoHorario = (int) grupo.stream()
                            .filter(row -> Boolean.TRUE.equals(row.entity().getSaiuNoHorario()))
                            .count();

                    return new HorariosCorteSeriePointDTO(
                            amostra.entity().getData() != null ? amostra.entity().getData().format(DATE_FMT) : null,
                            amostra.filial(),
                            saidasNoHorario,
                            totalProgramado,
                            percentual(saidasNoHorario, totalProgramado)
                    );
                })
                .sorted(Comparator.comparing(HorariosCorteSeriePointDTO::date, Comparator.nullsLast(String::compareTo))
                        .thenComparing(HorariosCorteSeriePointDTO::filial, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    public List<HorarioCorteRowDTO> buscarHorariosCorteTabela(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 100, 500);

        return buscarHorariosCorte(filtro).stream()
                .sorted(Comparator.comparing((HorarioCorteRegistroResolvido row) -> row.entity().getData(), Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(row -> row.entity().getImportadoEm(), Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(HorarioCorteRegistroResolvido::filial, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(row -> row.entity().getLinhaOuOperacao(), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .limit(limiteAplicado)
                .map(row -> new HorarioCorteRowDTO(
                        Objects.requireNonNullElse(row.entity().getId(), 0L),
                        formatar(row.entity().getData()),
                        row.filial(),
                        row.entity().getLinhaOuOperacao(),
                        formatar(row.entity().getInicio()),
                        formatar(row.entity().getManifestado()),
                        formatar(row.entity().getSmGerada()),
                        formatar(row.entity().getCorte()),
                        formatar(row.entity().getSaidaEfetiva()),
                        formatar(row.entity().getHorarioCorte()),
                        row.entity().getSaiuNoHorario(),
                        row.entity().getAtrasoMinutos(),
                        row.entity().getObservacao(),
                        row.entity().getNomeArquivo(),
                        formatar(row.entity().getImportadoEm()),
                        row.entity().getImportadoPor()
                ))
                .toList();
    }

    private List<HorarioCorteRegistroResolvido> buscarHorariosCorte(FiltroConsultaDTO filtro) {
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        HorarioCorteFilialMapperService.FilialMappingContext mappingContext = filialMapperService.criarContexto();
        return repository.findByDataBetween(filtro.dataInicio(), filtro.dataFim()).stream()
                .map(row -> new HorarioCorteRegistroResolvido(row, resolverFilial(row, mappingContext)))
                .filter(row -> escopo.permiteAlgumaFilial(row.filial()))
                .filter(row -> filtro.corresponde("filiais", row.filial()))
                .toList();
    }

    private String resolverFilial(
            VisaoHorariosCorteEntity row,
            HorarioCorteFilialMapperService.FilialMappingContext mappingContext
    ) {
        String filialAtual = row.getFilial();
        if (filialAtual != null
                && !filialAtual.isBlank()
                && !HorarioCorteFilialMapperService.FILIAL_NAO_MAPEADA.equalsIgnoreCase(filialAtual.trim())) {
            return filialAtual.trim();
        }
        return filialMapperService.mapearFilialCanonica(row.getLinhaOuOperacao(), mappingContext);
    }

    private double percentual(long numerador, long denominador) {
        if (denominador <= 0) {
            return 0.0;
        }
        return BigDecimal.valueOf((numerador * 100.0) / denominador)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private String chaveSerie(LocalDate data, String filial) {
        return formatar(data) + "|" + (filial == null ? "" : filial);
    }

    private String formatar(LocalDate data) {
        return data != null ? data.format(DATE_FMT) : null;
    }

    private String formatar(LocalTime time) {
        return time != null ? time.format(TIME_FMT) : null;
    }

    private String formatar(LocalDateTime dataHora) {
        return dataHora != null ? dataHora.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    private record HorarioCorteRegistroResolvido(
            VisaoHorariosCorteEntity entity,
            String filial
    ) {
    }
}
