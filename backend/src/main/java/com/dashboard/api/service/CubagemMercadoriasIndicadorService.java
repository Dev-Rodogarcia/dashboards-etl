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
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CubagemMercadoriasIndicadorService {

    private static final String PAGADOR_DOCS_EXCLUIDOS_PADRAO = """
            44699346000103;07668944000180;13190609000546;13190609000384;13190609000627;46928552000165;\
            14675270007381;56643018010390;14675270000450;14675270000298;05396883001510;05396883000386;\
            51602373000173;43829282000651;43829282000147;43829282000490;03944724000696;03944724000777;\
            03944724000262;03944724000939;03944724000858;44381747000102;01459630000272;43996693003061;\
            43996693000631;43996693000208;43996693002766;43996693002928;43996693002847;43996693000801;\
            43996693000127;92599901000160;33064262000250;08862530000827;08862530000231;08862530000150;\
            33064262000179;08862530000746;08862530001122;08862530001203
            """;

    private final ValidadorPeriodoService validadorPeriodo;
    private final IndicadoresGestaoAVistaSqlRepository sqlRepository;
    private final EscopoFilialService escopoFilialService;
    private final Set<String> pagadorDocsExcluidos;

    public CubagemMercadoriasIndicadorService(
            ValidadorPeriodoService validadorPeriodo,
            IndicadoresGestaoAVistaSqlRepository sqlRepository,
            EscopoFilialService escopoFilialService,
            @Value("${dashboard.indicadores.cubagem.pagador-docs-excluidos:}") String pagadorDocsExcluidosConfigurados
    ) {
        this.validadorPeriodo = validadorPeriodo;
        this.sqlRepository = sqlRepository;
        this.escopoFilialService = escopoFilialService;
        this.pagadorDocsExcluidos = normalizarDocumentosConfigurados(textoConfiguracaoOuPadrao(pagadorDocsExcluidosConfigurados));
    }

    public CubagemMercadoriasOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());

        IndicadoresGestaoAVistaSqlRepository.CubagemResumo resumo =
                sqlRepository.buscarCubagemResumo(filtro, escopoFilialService.escopoAtual(), pagadorDocsExcluidos);
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
        return sqlRepository.buscarCubagemSerie(filtro, escopoFilialService.escopoAtual(), pagadorDocsExcluidos);
    }

    public List<CubagemMercadoriasRowDTO> buscarTabela(FiltroConsultaDTO filtro, int limite) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        int limiteAplicado = ConsultaLimiteUtils.limitar(limite, 100, 500);
        return sqlRepository.buscarCubagemLinhas(
                filtro,
                escopoFilialService.escopoAtual(),
                pagadorDocsExcluidos,
                0,
                limiteAplicado
        );
    }

    public List<CubagemMercadoriasRowDTO> buscarExportacao(FiltroConsultaDTO filtro) {
        validadorPeriodo.validar(filtro.dataInicio(), filtro.dataFim());
        return sqlRepository.buscarCubagemExportacao(filtro, escopoFilialService.escopoAtual(), pagadorDocsExcluidos);
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
        long total = sqlRepository.contarCubagemLinhas(filtro, escopo, pagadorDocsExcluidos);
        List<CubagemMercadoriasRowDTO> conteudo = sqlRepository.buscarCubagemLinhas(
                filtro,
                escopo,
                pagadorDocsExcluidos,
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

    private static Set<String> normalizarDocumentosConfigurados(String docsConfigurados) {
        if (docsConfigurados == null || docsConfigurados.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(docsConfigurados.split("[,;\\r\\n]+"))
                .map(CubagemMercadoriasIndicadorService::normalizarDocumento)
                .filter(documento -> documento != null && !documento.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String textoConfiguracaoOuPadrao(String configuracao) {
        return configuracao == null || configuracao.isBlank() ? PAGADOR_DOCS_EXCLUIDOS_PADRAO : configuracao;
    }

    private static String normalizarDocumento(String documento) {
        if (documento == null) {
            return null;
        }
        String normalizado = documento.replaceAll("[^0-9A-Za-z]", "");
        return normalizado.isBlank() ? null : normalizado;
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
