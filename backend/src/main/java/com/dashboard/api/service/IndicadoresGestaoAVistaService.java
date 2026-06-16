package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.indicadoresgestao.HorarioCorteRowDTO;
import com.dashboard.api.dto.indicadoresgestao.HorariosCorteOverviewDTO;
import com.dashboard.api.dto.indicadoresgestao.HorariosCorteSeriePointDTO;
import com.dashboard.api.dto.PaginaDTO;
import com.dashboard.api.model.VisaoHorariosCorteEntity;
import com.dashboard.api.repository.HorariosCorteRasterDataSource;
import com.dashboard.api.service.acesso.EscopoFilialService;
import com.dashboard.api.util.ConsultaLimiteUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class IndicadoresGestaoAVistaService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final ValidadorPeriodoService validadorPeriodo;
    private final HorariosCorteRasterDataSource rasterSqlRepository;
    private final EscopoFilialService escopoFilialService;
    private final HorarioCorteFilialMapperService filialMapperService;

    public IndicadoresGestaoAVistaService(
            ValidadorPeriodoService validadorPeriodo,
            HorariosCorteRasterDataSource rasterSqlRepository,
            EscopoFilialService escopoFilialService,
            HorarioCorteFilialMapperService filialMapperService
    ) {
        this.validadorPeriodo = validadorPeriodo;
        this.rasterSqlRepository = rasterSqlRepository;
        this.escopoFilialService = escopoFilialService;
        this.filialMapperService = filialMapperService;
    }

    public HorariosCorteOverviewDTO buscarHorariosCorteOverview(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        HorariosCorteRasterDataSource.HorariosCorteResumo resumo = rasterSqlRepository.buscarResumoPorPeriodo(
                filtro.dataInicio(),
                filtro.dataFim(),
                escopo,
                filtro.valores("filiais")
        );
        int totalProgramado = inteiro(resumo.totalProgramado());
        int saidasNoHorario = inteiro(resumo.saidasNoHorario());
        double pctNoHorario = percentual(saidasNoHorario, totalProgramado);

        return new HorariosCorteOverviewDTO(
                updatedAtOuAgora(resumo.updatedAt()),
                saidasNoHorario,
                totalProgramado,
                pctNoHorario,
                resumo.ultimaImportacaoEm(),
                resumo.ultimaImportacaoArquivo()
        );
    }

    public List<HorariosCorteSeriePointDTO> buscarHorariosCorteSerie(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        return rasterSqlRepository.buscarSeriePorPeriodo(
                filtro.dataInicio(),
                filtro.dataFim(),
                escopoFilialService.escopoAtual(),
                filtro.valores("filiais")
        );
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
                        row.entity().getOrigemSm(),
                        row.entity().getDestinoSm(),
                        row.entity().getOrigemDestino(),
                        row.entity().getOrigem(),
                        row.entity().getOrdem(),
                        row.entity().getDestino(),
                        row.entity().getHorarioCorteSm(),
                        row.entity().getPrevisaoChegadaDestino(),
                        row.entity().getTransitTime(),
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

    public List<HorarioCorteRowDTO> buscarHorariosCorteExportacao(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        return buscarHorariosCorte(filtro).stream()
                .sorted(Comparator.comparing((HorarioCorteRegistroResolvido row) -> row.entity().getData(), Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(row -> row.entity().getImportadoEm(), Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(HorarioCorteRegistroResolvido::filial, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(row -> row.entity().getLinhaOuOperacao(), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(row -> new HorarioCorteRowDTO(
                        Objects.requireNonNullElse(row.entity().getId(), 0L),
                        formatar(row.entity().getData()),
                        row.filial(),
                        row.entity().getLinhaOuOperacao(),
                        row.entity().getOrigemSm(),
                        row.entity().getDestinoSm(),
                        row.entity().getOrigemDestino(),
                        row.entity().getOrigem(),
                        row.entity().getOrdem(),
                        row.entity().getDestino(),
                        row.entity().getHorarioCorteSm(),
                        row.entity().getPrevisaoChegadaDestino(),
                        row.entity().getTransitTime(),
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

    public PaginaDTO<HorarioCorteRowDTO> buscarHorariosCorteTabelaPaginada(FiltroConsultaDTO filtro, int pagina, int tamanhoPagina) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        PaginaDTO<VisaoHorariosCorteEntity> paginaFonte = rasterSqlRepository.findPageByDataBetween(
                filtro.dataInicio(),
                filtro.dataFim(),
                pagina,
                tamanhoPagina
        );
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        HorarioCorteFilialMapperService.FilialMappingContext mappingContext = filialMapperService.criarContextoRasterPadrao();

        List<HorarioCorteRowDTO> conteudo = paginaFonte.conteudo().stream()
                .map(row -> new HorarioCorteRegistroResolvido(row, resolverFilial(row, mappingContext)))
                .filter(row -> escopo.permiteAlgumaFilial(row.filial()))
                .filter(row -> filtro.corresponde("filiais", row.filial()))
                .map(row -> new HorarioCorteRowDTO(
                        Objects.requireNonNullElse(row.entity().getId(), 0L),
                        formatar(row.entity().getData()),
                        row.filial(),
                        row.entity().getLinhaOuOperacao(),
                        row.entity().getOrigemSm(),
                        row.entity().getDestinoSm(),
                        row.entity().getOrigemDestino(),
                        row.entity().getOrigem(),
                        row.entity().getOrdem(),
                        row.entity().getDestino(),
                        row.entity().getHorarioCorteSm(),
                        row.entity().getPrevisaoChegadaDestino(),
                        row.entity().getTransitTime(),
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

        return new PaginaDTO<>(
                conteudo,
                paginaFonte.totalElementos(),
                paginaFonte.totalPaginas(),
                paginaFonte.paginaAtual(),
                paginaFonte.tamanhoPagina()
        );
    }

    private List<HorarioCorteRegistroResolvido> buscarHorariosCorte(FiltroConsultaDTO filtro) {
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        HorarioCorteFilialMapperService.FilialMappingContext mappingContext = filialMapperService.criarContextoRasterPadrao();
        return rasterSqlRepository.findByDataBetween(filtro.dataInicio(), filtro.dataFim()).stream()
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

    private String updatedAtOuAgora(String updatedAt) {
        return updatedAt != null && !updatedAt.isBlank()
                ? updatedAt
                : LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private int inteiro(long valor) {
        if (valor > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (valor < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) valor;
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
