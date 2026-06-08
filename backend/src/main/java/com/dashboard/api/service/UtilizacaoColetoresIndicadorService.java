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
import com.dashboard.api.util.TemporalJsonUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class UtilizacaoColetoresIndicadorService {

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
                resumo.pctUtilizacao()
        );
    }

    public List<UtilizacaoColetoresSeriePointDTO> buscarSerie(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        return sqlRepository.buscarUtilizacaoColetoresSerie(filtro, escopoFilialService.escopoAtual());
    }

    public List<UtilizacaoColetoresRankingDTO> buscarRanking(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        List<IndicadoresGestaoAVistaSqlRepository.UtilizacaoColetoresRankingBase> ranking =
                sqlRepository.buscarUtilizacaoColetoresRanking(filtro, escopoFilialService.escopoAtual());

        Set<String> filiaisRanking = new LinkedHashSet<>();
        for (IndicadoresGestaoAVistaSqlRepository.UtilizacaoColetoresRankingBase item : ranking) {
            filiaisRanking.add(item.filial());
        }
        Map<String, BigDecimal> metas = kpiGoalService != null
                ? kpiGoalService.buscarMetasEfetivasPorIndicador(
                        KpiGoalService.COLLECTOR_USAGE,
                        filiaisRanking,
                        filtro.dataInicio()
                )
                : Map.of();

        List<UtilizacaoColetoresRankingDTO> dtos = new ArrayList<>(ranking.size());
        for (IndicadoresGestaoAVistaSqlRepository.UtilizacaoColetoresRankingBase item : ranking) {
            dtos.add(toRankingDto(item, metas.getOrDefault(item.filial(), BigDecimal.valueOf(90))));
        }
        return dtos;
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
                item.pctUtilizacao(),
                meta,
                item.manifestosBipados(),
                item.totalManifestos(),
                item.manifestosDescarregamento(),
                item.manifestosIncompletos()
        );
    }

    private static String updatedAtOuAgora(String updatedAt) {
        return TemporalJsonUtils.garantirUtc(updatedAt);
    }

    private static int inteiro(long valor) {
        if (valor > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) valor;
    }
}
