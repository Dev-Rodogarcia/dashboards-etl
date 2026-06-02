package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.indicadoresgestao.UtilizacaoColetoresOverviewDTO;
import com.dashboard.api.dto.indicadoresgestao.UtilizacaoColetoresRankingDTO;
import com.dashboard.api.dto.indicadoresgestao.UtilizacaoColetoresRowDTO;
import com.dashboard.api.dto.indicadoresgestao.UtilizacaoColetoresSeriePointDTO;
import com.dashboard.api.dto.PaginaDTO;
import com.dashboard.api.repository.IndicadoresGestaoAVistaSqlRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import com.dashboard.api.util.ConsultaLimiteUtils;
import com.dashboard.api.util.IndicadoresGestaoMetricasUtils;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class UtilizacaoColetoresIndicadorService {

    private static final String[] FILIAIS_PADRAO = {
            "AGU - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
            "CAS - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
            "CPQ - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
            "CWB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
            "NHB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
            "REC - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
            "RJR - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
            "SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA"
    };

    private final ValidadorPeriodoService validadorPeriodo;
    private final IndicadoresGestaoAVistaSqlRepository sqlRepository;
    private final EscopoFilialService escopoFilialService;
    private final KpiGoalService kpiGoalService;

    public UtilizacaoColetoresIndicadorService(
            ValidadorPeriodoService validadorPeriodo,
            IndicadoresGestaoAVistaSqlRepository sqlRepository,
            EscopoFilialService escopoFilialService,
            KpiGoalService kpiGoalService
    ) {
        this.validadorPeriodo = validadorPeriodo;
        this.sqlRepository = sqlRepository;
        this.escopoFilialService = escopoFilialService;
        this.kpiGoalService = kpiGoalService;
    }

    public UtilizacaoColetoresOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        IndicadoresGestaoAVistaSqlRepository.UtilizacaoColetoresResumo resumo =
                sqlRepository.buscarUtilizacaoColetoresResumo(filtro, escopoFilialService.escopoAtual());
        return new UtilizacaoColetoresOverviewDTO(
                updatedAtOuAgora(resumo.updatedAt()),
                inteiro(resumo.manifestosBipados()),
                inteiro(resumo.manifestosEmitidos()),
                inteiro(resumo.manifestosDescarregamento()),
                inteiro(resumo.totalManifestos()),
                inteiro(resumo.manifestosIncompletos()),
                IndicadoresGestaoMetricasUtils.percentual(resumo.manifestosBipados(), resumo.totalManifestos())
        );
    }

    public List<UtilizacaoColetoresSeriePointDTO> buscarSerie(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        return sqlRepository.buscarUtilizacaoColetoresSerie(filtro, escopoFilialService.escopoAtual());
    }

    public List<UtilizacaoColetoresRankingDTO> buscarRanking(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        List<IndicadoresGestaoAVistaSqlRepository.UtilizacaoColetoresRankingBase> rankingFiltrado =
                sqlRepository.buscarUtilizacaoColetoresRanking(filtro, escopoFilialService.escopoAtual()).stream()
                        .filter(this::deveExibirNoRankingColetores)
                        .toList();

        Set<String> filiaisRanking = rankingFiltrado.stream()
                .map(IndicadoresGestaoAVistaSqlRepository.UtilizacaoColetoresRankingBase::filial)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Map<String, BigDecimal> metas = kpiGoalService != null
                ? kpiGoalService.buscarMetasEfetivasPorIndicador(KpiGoalService.COLLECTOR_USAGE, filiaisRanking)
                : Map.of();

        return rankingFiltrado.stream()
                .map(item -> toRankingDto(item, metas.getOrDefault(item.filial(), BigDecimal.valueOf(90))))
                .sorted(Comparator.comparingDouble(UtilizacaoColetoresRankingDTO::utilization)
                        .thenComparing(UtilizacaoColetoresRankingDTO::manifestosBipaveis, Comparator.reverseOrder())
                        .thenComparing(UtilizacaoColetoresRankingDTO::branchName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<UtilizacaoColetoresRowDTO> buscarTabela(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 100, 500);
        return sqlRepository.buscarUtilizacaoColetoresLinhas(
                filtro,
                escopoFilialService.escopoAtual(),
                0,
                limiteAplicado
        );
    }

    public List<UtilizacaoColetoresRowDTO> buscarExportacao(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        return sqlRepository.buscarUtilizacaoColetoresExportacao(filtro, escopoFilialService.escopoAtual());
    }

    public PaginaDTO<UtilizacaoColetoresRowDTO> buscarTabelaPaginada(
            FiltroConsultaDTO filtro,
            int pagina,
            int tamanhoPagina
    ) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int paginaAplicada = Math.max(1, pagina);
        int tamanhoAplicado = ConsultaLimiteUtils.limitar(tamanhoPagina, 10, 100);
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        long total = sqlRepository.contarUtilizacaoColetoresLinhas(filtro, escopo);
        List<UtilizacaoColetoresRowDTO> conteudo = sqlRepository.buscarUtilizacaoColetoresLinhas(
                filtro,
                escopo,
                (paginaAplicada - 1) * tamanhoAplicado,
                tamanhoAplicado
        );

        return new PaginaDTO<>(
                conteudo,
                total,
                total == 0 ? 0 : (int) Math.ceil(total / (double) tamanhoAplicado),
                paginaAplicada,
                tamanhoAplicado
        );
    }

    private UtilizacaoColetoresRankingDTO toRankingDto(
            IndicadoresGestaoAVistaSqlRepository.UtilizacaoColetoresRankingBase item,
            BigDecimal meta
    ) {
        return new UtilizacaoColetoresRankingDTO(
                item.filial(),
                item.filial(),
                IndicadoresGestaoMetricasUtils.percentual(item.manifestosBipados(), item.totalManifestos()),
                meta,
                item.manifestosBipados(),
                item.totalManifestos(),
                item.manifestosDescarregamento(),
                item.manifestosIncompletos()
        );
    }

    private boolean deveExibirNoRankingColetores(
            IndicadoresGestaoAVistaSqlRepository.UtilizacaoColetoresRankingBase item
    ) {
        return item.manifestosBipados() > 0 || filialOperacional(item.filial());
    }

    private boolean filialOperacional(String filial) {
        if (filial == null || filial.isBlank()) {
            return false;
        }
        String normalizada = normalizarTexto(filial);
        for (String filialPadrao : FILIAIS_PADRAO) {
            if (normalizada.equals(normalizarTexto(filialPadrao))) {
                return true;
            }
        }
        return false;
    }

    private String normalizarTexto(String valor) {
        String texto = Objects.toString(valor, "").trim().toLowerCase(Locale.ROOT);
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return semAcento.replaceAll("\\s+", " ");
    }

    private static String updatedAtOuAgora(String updatedAt) {
        return updatedAt != null && !updatedAt.isBlank()
                ? updatedAt
                : LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private static int inteiro(long valor) {
        if (valor > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) valor;
    }
}
