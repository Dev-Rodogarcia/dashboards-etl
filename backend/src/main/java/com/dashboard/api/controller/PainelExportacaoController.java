package com.dashboard.api.controller;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.TotalRegistrosDTO;
import com.dashboard.api.service.CubagemMercadoriasIndicadorService;
import com.dashboard.api.service.DashboardExportDefinition;
import com.dashboard.api.service.DashboardExportService;
import com.dashboard.api.service.IndenizacaoMercadoriasIndicadorService;
import com.dashboard.api.service.IndicadoresGestaoAVistaService;
import com.dashboard.api.service.PerformanceEntregaIndicadorService;
import com.dashboard.api.service.UtilizacaoColetoresIndicadorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/painel")
public class PainelExportacaoController {

    private final DashboardExportService dashboardExportService;
    private final PerformanceEntregaIndicadorService performanceEntregaService;
    private final UtilizacaoColetoresIndicadorService utilizacaoColetoresService;
    private final CubagemMercadoriasIndicadorService cubagemMercadoriasService;
    private final IndenizacaoMercadoriasIndicadorService indenizacaoMercadoriasService;
    private final IndicadoresGestaoAVistaService horariosCorteService;

    public PainelExportacaoController(
            DashboardExportService dashboardExportService,
            PerformanceEntregaIndicadorService performanceEntregaService,
            UtilizacaoColetoresIndicadorService utilizacaoColetoresService,
            CubagemMercadoriasIndicadorService cubagemMercadoriasService,
            IndenizacaoMercadoriasIndicadorService indenizacaoMercadoriasService,
            IndicadoresGestaoAVistaService horariosCorteService
    ) {
        this.dashboardExportService = dashboardExportService;
        this.performanceEntregaService = performanceEntregaService;
        this.utilizacaoColetoresService = utilizacaoColetoresService;
        this.cubagemMercadoriasService = cubagemMercadoriasService;
        this.indenizacaoMercadoriasService = indenizacaoMercadoriasService;
        this.horariosCorteService = horariosCorteService;
    }

    @GetMapping("/coletas/exportacao")
    @PreAuthorize("@acessoSeguranca.podeAcessar('coletas')")
    public ResponseEntity<byte[]> exportarColetas(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return exportar(DashboardExportDefinition.COLETAS, dataInicio, dataFim, params);
    }

    @GetMapping("/coletas/tabela/total")
    @PreAuthorize("@acessoSeguranca.podeAcessar('coletas')")
    public ResponseEntity<TotalRegistrosDTO> totalColetas(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return total(DashboardExportDefinition.COLETAS, dataInicio, dataFim, params);
    }

    @GetMapping("/fretes/exportacao")
    @PreAuthorize("@acessoSeguranca.podeAcessar('fretes')")
    public ResponseEntity<byte[]> exportarFretes(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return exportar(DashboardExportDefinition.FRETES, dataInicio, dataFim, params);
    }

    @GetMapping("/fretes/tabela/total")
    @PreAuthorize("@acessoSeguranca.podeAcessar('fretes')")
    public ResponseEntity<TotalRegistrosDTO> totalFretes(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return total(DashboardExportDefinition.FRETES, dataInicio, dataFim, params);
    }

    @GetMapping("/tracking/exportacao")
    @PreAuthorize("@acessoSeguranca.podeAcessar('tracking')")
    public ResponseEntity<byte[]> exportarTracking(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return exportar(DashboardExportDefinition.TRACKING, dataInicio, dataFim, params);
    }

    @GetMapping("/tracking/tabela/total")
    @PreAuthorize("@acessoSeguranca.podeAcessar('tracking')")
    public ResponseEntity<TotalRegistrosDTO> totalTracking(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return total(DashboardExportDefinition.TRACKING, dataInicio, dataFim, params);
    }

    @GetMapping("/manifestos/exportacao")
    @PreAuthorize("@acessoSeguranca.podeAcessar('manifestos')")
    public ResponseEntity<byte[]> exportarManifestos(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return exportar(DashboardExportDefinition.MANIFESTOS, dataInicio, dataFim, params);
    }

    @GetMapping("/manifestos/tabela/total")
    @PreAuthorize("@acessoSeguranca.podeAcessar('manifestos')")
    public ResponseEntity<TotalRegistrosDTO> totalManifestos(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return total(DashboardExportDefinition.MANIFESTOS, dataInicio, dataFim, params);
    }

    @GetMapping("/cotacoes/exportacao")
    @PreAuthorize("@acessoSeguranca.podeAcessar('cotacoes')")
    public ResponseEntity<byte[]> exportarCotacoes(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return exportar(DashboardExportDefinition.COTACOES, dataInicio, dataFim, params);
    }

    @GetMapping("/cotacoes/tabela/total")
    @PreAuthorize("@acessoSeguranca.podeAcessar('cotacoes')")
    public ResponseEntity<TotalRegistrosDTO> totalCotacoes(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return total(DashboardExportDefinition.COTACOES, dataInicio, dataFim, params);
    }

    @GetMapping("/contas-a-pagar/exportacao")
    @PreAuthorize("@acessoSeguranca.podeAcessar('contasAPagar')")
    public ResponseEntity<byte[]> exportarContasAPagar(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return exportar(DashboardExportDefinition.CONTAS_A_PAGAR, dataInicio, dataFim, params);
    }

    @GetMapping("/contas-a-pagar/tabela/total")
    @PreAuthorize("@acessoSeguranca.podeAcessar('contasAPagar')")
    public ResponseEntity<TotalRegistrosDTO> totalContasAPagar(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return total(DashboardExportDefinition.CONTAS_A_PAGAR, dataInicio, dataFim, params);
    }

    @GetMapping("/faturas/exportacao")
    @PreAuthorize("@acessoSeguranca.podeAcessar('faturas')")
    public ResponseEntity<byte[]> exportarFaturasProcessos(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return exportar(DashboardExportDefinition.FATURAS_PROCESSOS, dataInicio, dataFim, params);
    }

    @GetMapping("/faturas/exportacao-financeira")
    @PreAuthorize("@acessoSeguranca.podeAcessar('faturas')")
    public ResponseEntity<byte[]> exportarFaturasFinanceiro(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return exportar(DashboardExportDefinition.FATURAS_FINANCEIRO, dataInicio, dataFim, params);
    }

    @GetMapping("/faturas/tabela/total")
    @PreAuthorize("@acessoSeguranca.podeAcessar('faturas')")
    public ResponseEntity<TotalRegistrosDTO> totalFaturas(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return total(DashboardExportDefinition.FATURAS_PROCESSOS, dataInicio, dataFim, params);
    }

    @GetMapping("/faturas-por-cliente/exportacao")
    @PreAuthorize("@acessoSeguranca.podeAcessar('faturasPorCliente')")
    public ResponseEntity<byte[]> exportarFaturasPorCliente(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return exportar(DashboardExportDefinition.FATURAS_POR_CLIENTE, dataInicio, dataFim, params);
    }

    @GetMapping("/faturas-por-cliente/tabela/total")
    @PreAuthorize("@acessoSeguranca.podeAcessar('faturasPorCliente')")
    public ResponseEntity<TotalRegistrosDTO> totalFaturasPorCliente(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return total(DashboardExportDefinition.FATURAS_POR_CLIENTE, dataInicio, dataFim, params);
    }

    @GetMapping("/etl-saude/exportacao")
    @PreAuthorize("@acessoSeguranca.podeAcessar('etlSaude')")
    public ResponseEntity<byte[]> exportarEtlSaude(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return exportar(DashboardExportDefinition.ETL_SAUDE, dataInicio, dataFim, params);
    }

    @GetMapping("/etl-saude/tabela/total")
    @PreAuthorize("@acessoSeguranca.podeAcessar('etlSaude')")
    public ResponseEntity<TotalRegistrosDTO> totalEtlSaude(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return total(DashboardExportDefinition.ETL_SAUDE, dataInicio, dataFim, params);
    }

    @GetMapping("/indicadores-gestao-a-vista/performance-entrega/exportacao")
    @PreAuthorize("@acessoSeguranca.podeAcessar('indicadoresGestaoAVista')")
    public ResponseEntity<byte[]> exportarPerformanceEntrega(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        FiltroConsultaDTO filtro = filtro(dataInicio, dataFim, params);
        return dashboardExportService.exportarBeans(
                "indicadores-performance-entrega",
                filtro,
                performanceEntregaService.buscarExportacao(filtro)
        );
    }

    @GetMapping("/indicadores-gestao-a-vista/utilizacao-coletores/exportacao")
    @PreAuthorize("@acessoSeguranca.podeAcessar('indicadoresGestaoAVista')")
    public ResponseEntity<byte[]> exportarUtilizacaoColetores(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        FiltroConsultaDTO filtro = filtro(dataInicio, dataFim, params);
        return dashboardExportService.exportarBeans(
                "indicadores-utilizacao-coletores",
                filtro,
                utilizacaoColetoresService.buscarExportacao(filtro)
        );
    }

    @GetMapping("/indicadores-gestao-a-vista/cubagem-mercadorias/exportacao")
    @PreAuthorize("@acessoSeguranca.podeAcessar('indicadoresGestaoAVista')")
    public ResponseEntity<byte[]> exportarCubagemMercadorias(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        FiltroConsultaDTO filtro = filtro(dataInicio, dataFim, params);
        return dashboardExportService.exportarBeans(
                "indicadores-cubagem-mercadorias",
                filtro,
                cubagemMercadoriasService.buscarExportacao(filtro)
        );
    }

    @GetMapping("/indicadores-gestao-a-vista/indenizacao-mercadorias/exportacao")
    @PreAuthorize("@acessoSeguranca.podeAcessar('indicadoresGestaoAVista')")
    public ResponseEntity<byte[]> exportarIndenizacaoMercadorias(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        FiltroConsultaDTO filtro = filtro(dataInicio, dataFim, params);
        return dashboardExportService.exportarBeans(
                "indicadores-indenizacao-mercadorias",
                filtro,
                indenizacaoMercadoriasService.buscarExportacao(filtro)
        );
    }

    @GetMapping("/indicadores-gestao-a-vista/horarios-corte/exportacao")
    @PreAuthorize("@acessoSeguranca.podeAcessar('indicadoresGestaoAVista')")
    public ResponseEntity<byte[]> exportarHorariosCorte(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        FiltroConsultaDTO filtro = filtro(dataInicio, dataFim, params);
        return dashboardExportService.exportarBeans(
                "indicadores-horarios-corte",
                filtro,
                horariosCorteService.buscarHorariosCorteExportacao(filtro)
        );
    }

    private ResponseEntity<byte[]> exportar(
            DashboardExportDefinition definition,
            LocalDate dataInicio,
            LocalDate dataFim,
            MultiValueMap<String, String> params
    ) {
        return dashboardExportService.exportar(definition, filtro(dataInicio, dataFim, params));
    }

    private ResponseEntity<TotalRegistrosDTO> total(
            DashboardExportDefinition definition,
            LocalDate dataInicio,
            LocalDate dataFim,
            MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(new TotalRegistrosDTO(
                dashboardExportService.total(definition, filtro(dataInicio, dataFim, params))
        ));
    }

    private FiltroConsultaDTO filtro(LocalDate dataInicio, LocalDate dataFim, MultiValueMap<String, String> params) {
        return FiltroRequestMapper.from(dataInicio, dataFim, params);
    }
}
