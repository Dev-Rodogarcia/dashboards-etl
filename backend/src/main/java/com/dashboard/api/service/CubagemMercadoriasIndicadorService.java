package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.indicadoresgestao.CubagemMercadoriasOverviewDTO;
import com.dashboard.api.dto.indicadoresgestao.CubagemMercadoriasRowDTO;
import com.dashboard.api.dto.indicadoresgestao.CubagemMercadoriasSeriePointDTO;
import com.dashboard.api.dto.PaginaDTO;
import com.dashboard.api.repository.IndicadoresGestaoAVistaSqlRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import com.dashboard.api.util.ConsultaLimiteUtils;
import com.dashboard.api.util.IndicadoresGestaoMetricasUtils;
import com.dashboard.api.util.TemporalJsonUtils;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CubagemMercadoriasIndicadorService {

    private final ValidadorPeriodoService validadorPeriodo;
    private final IndicadoresGestaoAVistaSqlRepository sqlRepository;
    private final EscopoFilialService escopoFilialService;

    public CubagemMercadoriasIndicadorService(
            ValidadorPeriodoService validadorPeriodo,
            IndicadoresGestaoAVistaSqlRepository sqlRepository,
            EscopoFilialService escopoFilialService
    ) {
        this.validadorPeriodo = validadorPeriodo;
        this.sqlRepository = sqlRepository;
        this.escopoFilialService = escopoFilialService;
    }

    public CubagemMercadoriasOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        IndicadoresGestaoAVistaSqlRepository.CubagemResumo resumo =
                sqlRepository.buscarCubagemResumo(filtro, escopoFilialService.escopoAtual());
        return new CubagemMercadoriasOverviewDTO(
                updatedAtOuAgora(resumo.updatedAt()),
                inteiro(resumo.totalFretes()),
                inteiro(resumo.fretesCubados()),
                inteiro(resumo.fretesComPesoReal()),
                IndicadoresGestaoMetricasUtils.percentual(resumo.fretesCubados(), resumo.totalFretes())
        );
    }

    public List<CubagemMercadoriasSeriePointDTO> buscarSerie(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        return sqlRepository.buscarCubagemSerie(filtro, escopoFilialService.escopoAtual());
    }

    public List<CubagemMercadoriasRowDTO> buscarTabela(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 100, 500);
        return sqlRepository.buscarCubagemLinhas(
                filtro,
                escopoFilialService.escopoAtual(),
                0,
                limiteAplicado
        );
    }

    public List<CubagemMercadoriasRowDTO> buscarExportacao(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        return sqlRepository.buscarCubagemExportacao(filtro, escopoFilialService.escopoAtual());
    }

    public PaginaDTO<CubagemMercadoriasRowDTO> buscarTabelaPaginada(
            FiltroConsultaDTO filtro,
            int pagina,
            int tamanhoPagina
    ) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int paginaAplicada = Math.max(1, pagina);
        int tamanhoAplicado = ConsultaLimiteUtils.limitar(tamanhoPagina, 10, 100);
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        long total = sqlRepository.contarCubagemLinhas(filtro, escopo);
        List<CubagemMercadoriasRowDTO> conteudo = sqlRepository.buscarCubagemLinhas(
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

    private static String updatedAtOuAgora(String updatedAt) {
        return TemporalJsonUtils.garantirIsoComOffset(updatedAt);
    }

    private static int inteiro(long valor) {
        if (valor > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) valor;
    }
}
