package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.indicadoresgestao.IndenizacaoMercadoriasOverviewDTO;
import com.dashboard.api.dto.indicadoresgestao.IndenizacaoMercadoriasRowDTO;
import com.dashboard.api.dto.indicadoresgestao.IndenizacaoMercadoriasSeriePointDTO;
import com.dashboard.api.dto.PaginaDTO;
import com.dashboard.api.repository.IndicadoresGestaoAVistaSqlRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import com.dashboard.api.util.ConsultaLimiteUtils;
import com.dashboard.api.util.IndicadoresGestaoMetricasUtils;
import com.dashboard.api.util.TemporalJsonUtils;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class IndenizacaoMercadoriasIndicadorService {

    private final ValidadorPeriodoService validadorPeriodo;
    private final IndicadoresGestaoAVistaSqlRepository sqlRepository;
    private final EscopoFilialService escopoFilialService;

    public IndenizacaoMercadoriasIndicadorService(
            ValidadorPeriodoService validadorPeriodo,
            IndicadoresGestaoAVistaSqlRepository sqlRepository,
            EscopoFilialService escopoFilialService
    ) {
        this.validadorPeriodo = validadorPeriodo;
        this.sqlRepository = sqlRepository;
        this.escopoFilialService = escopoFilialService;
    }

    public IndenizacaoMercadoriasOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        IndicadoresGestaoAVistaSqlRepository.IndenizacaoResumo resumo =
                sqlRepository.buscarIndenizacaoResumo(filtro, escopoFilialService.escopoAtual());
        return new IndenizacaoMercadoriasOverviewDTO(
                updatedAtOuAgora(resumo.updatedAt()),
                inteiro(resumo.totalSinistros()),
                resumo.valorIndenizadoAbs(),
                resumo.valorIndenizadoOriginal(),
                resumo.faturamentoBase(),
                IndicadoresGestaoMetricasUtils.percentual(resumo.valorIndenizadoAbs(), resumo.faturamentoBase())
        );
    }

    public List<IndenizacaoMercadoriasSeriePointDTO> buscarSerie(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        return sqlRepository.buscarIndenizacaoSerie(filtro, escopoFilialService.escopoAtual());
    }

    public List<IndenizacaoMercadoriasRowDTO> buscarTabela(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 100, 500);
        return sqlRepository.buscarIndenizacaoLinhas(
                filtro,
                escopoFilialService.escopoAtual(),
                0,
                limiteAplicado
        );
    }

    public List<IndenizacaoMercadoriasRowDTO> buscarExportacao(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        return sqlRepository.buscarIndenizacaoExportacao(filtro, escopoFilialService.escopoAtual());
    }

    public PaginaDTO<IndenizacaoMercadoriasRowDTO> buscarTabelaPaginada(
            FiltroConsultaDTO filtro,
            int pagina,
            int tamanhoPagina
    ) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int paginaAplicada = Math.max(1, pagina);
        int tamanhoAplicado = ConsultaLimiteUtils.limitar(tamanhoPagina, 10, 100);
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        long total = sqlRepository.contarIndenizacaoLinhas(filtro, escopo);
        List<IndenizacaoMercadoriasRowDTO> conteudo = sqlRepository.buscarIndenizacaoLinhas(
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
        return TemporalJsonUtils.garantirUtc(updatedAt);
    }

    private static int inteiro(long valor) {
        if (valor > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) valor;
    }
}
