package com.dashboard.api.controller;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.PaginaDTO;
import com.dashboard.api.dto.indicadoresgestao.CubagemMercadoriasOverviewDTO;
import com.dashboard.api.dto.indicadoresgestao.CubagemMercadoriasRowDTO;
import com.dashboard.api.dto.indicadoresgestao.CubagemMercadoriasSeriePointDTO;
import com.dashboard.api.dto.indicadoresgestao.HorarioCorteRowDTO;
import com.dashboard.api.dto.indicadoresgestao.HorariosCorteOverviewDTO;
import com.dashboard.api.dto.indicadoresgestao.HorariosCorteSeriePointDTO;
import com.dashboard.api.dto.indicadoresgestao.IndenizacaoMercadoriasOverviewDTO;
import com.dashboard.api.dto.indicadoresgestao.IndenizacaoMercadoriasRowDTO;
import com.dashboard.api.dto.indicadoresgestao.IndenizacaoMercadoriasSeriePointDTO;
import com.dashboard.api.dto.indicadoresgestao.PerformanceEntregaOverviewDTO;
import com.dashboard.api.dto.indicadoresgestao.PerformanceEntregaRowDTO;
import com.dashboard.api.dto.indicadoresgestao.PerformanceEntregaSeriePointDTO;
import com.dashboard.api.dto.indicadoresgestao.UtilizacaoColetoresOverviewDTO;
import com.dashboard.api.dto.indicadoresgestao.UtilizacaoColetoresRankingDTO;
import com.dashboard.api.dto.indicadoresgestao.UtilizacaoColetoresRowDTO;
import com.dashboard.api.dto.indicadoresgestao.UtilizacaoColetoresSeriePointDTO;
import com.dashboard.api.service.CubagemMercadoriasIndicadorService;
import com.dashboard.api.service.IndenizacaoMercadoriasIndicadorService;
import com.dashboard.api.service.IndicadoresGestaoAVistaService;
import com.dashboard.api.service.PerformanceEntregaIndicadorService;
import com.dashboard.api.service.UtilizacaoColetoresIndicadorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/painel/indicadores-gestao-a-vista")
@PreAuthorize("@acessoSeguranca.podeAcessar('indicadoresGestaoAVista')")
public class IndicadoresGestaoAVistaController {

    private static final Logger log = LoggerFactory.getLogger(IndicadoresGestaoAVistaController.class);

    private final IndicadoresGestaoAVistaService horariosCorteService;
    private final PerformanceEntregaIndicadorService performanceEntregaService;
    private final UtilizacaoColetoresIndicadorService utilizacaoColetoresService;
    private final CubagemMercadoriasIndicadorService cubagemMercadoriasService;
    private final IndenizacaoMercadoriasIndicadorService indenizacaoMercadoriasService;

    public IndicadoresGestaoAVistaController(
            IndicadoresGestaoAVistaService horariosCorteService,
            PerformanceEntregaIndicadorService performanceEntregaService,
            UtilizacaoColetoresIndicadorService utilizacaoColetoresService,
            CubagemMercadoriasIndicadorService cubagemMercadoriasService,
            IndenizacaoMercadoriasIndicadorService indenizacaoMercadoriasService
    ) {
        this.horariosCorteService = horariosCorteService;
        this.performanceEntregaService = performanceEntregaService;
        this.utilizacaoColetoresService = utilizacaoColetoresService;
        this.cubagemMercadoriasService = cubagemMercadoriasService;
        this.indenizacaoMercadoriasService = indenizacaoMercadoriasService;
    }

    @GetMapping("/performance-entrega/overview")
    public ResponseEntity<PerformanceEntregaOverviewDTO> performanceOverview(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(performanceEntregaService.buscarOverview(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/performance-entrega/serie")
    public ResponseEntity<List<PerformanceEntregaSeriePointDTO>> performanceSerie(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(performanceEntregaService.buscarSerie(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/performance-entrega/tabela")
    public ResponseEntity<List<PerformanceEntregaRowDTO>> performanceTabela(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "100") int limite,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(performanceEntregaService.buscarTabela(FiltroRequestMapper.from(dataInicio, dataFim, params), limite));
    }

    @GetMapping("/performance-entrega/diagnostico")
    public ResponseEntity<List<PerformanceEntregaRowDTO>> performanceDiagnostico(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(performanceEntregaService.buscarExportacao(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/performance-entrega/tabela/paginada")
    public ResponseEntity<PaginaDTO<PerformanceEntregaRowDTO>> performanceTabelaPaginada(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int tamanhoPagina,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(performanceEntregaService.buscarTabelaPaginada(FiltroRequestMapper.from(dataInicio, dataFim, params), pagina, tamanhoPagina));
    }

    @GetMapping("/utilizacao-coletores/overview")
    public ResponseEntity<UtilizacaoColetoresOverviewDTO> utilizacaoColetoresOverview(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(utilizacaoColetoresService.buscarOverview(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/utilizacao-coletores/serie")
    public ResponseEntity<List<UtilizacaoColetoresSeriePointDTO>> utilizacaoColetoresSerie(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(utilizacaoColetoresService.buscarSerie(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/utilizacao-coletores/ranking")
    public ResponseEntity<List<UtilizacaoColetoresRankingDTO>> utilizacaoColetoresRanking(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(utilizacaoColetoresService.buscarRanking(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/utilizacao-coletores/tabela")
    public ResponseEntity<List<UtilizacaoColetoresRowDTO>> utilizacaoColetoresTabela(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "100") int limite,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(utilizacaoColetoresService.buscarTabela(FiltroRequestMapper.from(dataInicio, dataFim, params), limite));
    }

    @GetMapping("/utilizacao-coletores/diagnostico")
    public ResponseEntity<List<UtilizacaoColetoresRowDTO>> utilizacaoColetoresDiagnostico(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(utilizacaoColetoresService.buscarExportacao(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/utilizacao-coletores/tabela/paginada")
    public ResponseEntity<PaginaDTO<UtilizacaoColetoresRowDTO>> utilizacaoColetoresTabelaPaginada(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int tamanhoPagina,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(utilizacaoColetoresService.buscarTabelaPaginada(FiltroRequestMapper.from(dataInicio, dataFim, params), pagina, tamanhoPagina));
    }

    @GetMapping("/cubagem-mercadorias/overview")
    public ResponseEntity<CubagemMercadoriasOverviewDTO> cubagemOverview(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(cubagemMercadoriasService.buscarOverview(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/cubagem-mercadorias/serie")
    public ResponseEntity<List<CubagemMercadoriasSeriePointDTO>> cubagemSerie(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(cubagemMercadoriasService.buscarSerie(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/cubagem-mercadorias/tabela")
    public ResponseEntity<List<CubagemMercadoriasRowDTO>> cubagemTabela(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "100") int limite,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(cubagemMercadoriasService.buscarTabela(FiltroRequestMapper.from(dataInicio, dataFim, params), limite));
    }

    @GetMapping("/cubagem-mercadorias/diagnostico")
    public ResponseEntity<List<CubagemMercadoriasRowDTO>> cubagemDiagnostico(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(cubagemMercadoriasService.buscarExportacao(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/cubagem-mercadorias/tabela/paginada")
    public ResponseEntity<PaginaDTO<CubagemMercadoriasRowDTO>> cubagemTabelaPaginada(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int tamanhoPagina,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(cubagemMercadoriasService.buscarTabelaPaginada(FiltroRequestMapper.from(dataInicio, dataFim, params), pagina, tamanhoPagina));
    }

    @GetMapping("/indenizacao-mercadorias/overview")
    public ResponseEntity<IndenizacaoMercadoriasOverviewDTO> indenizacaoOverview(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(indenizacaoMercadoriasService.buscarOverview(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/indenizacao-mercadorias/serie")
    public ResponseEntity<List<IndenizacaoMercadoriasSeriePointDTO>> indenizacaoSerie(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(indenizacaoMercadoriasService.buscarSerie(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/indenizacao-mercadorias/tabela")
    public ResponseEntity<List<IndenizacaoMercadoriasRowDTO>> indenizacaoTabela(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "100") int limite,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(indenizacaoMercadoriasService.buscarTabela(FiltroRequestMapper.from(dataInicio, dataFim, params), limite));
    }

    @GetMapping("/indenizacao-mercadorias/diagnostico")
    public ResponseEntity<List<IndenizacaoMercadoriasRowDTO>> indenizacaoDiagnostico(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(indenizacaoMercadoriasService.buscarExportacao(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/indenizacao-mercadorias/tabela/paginada")
    public ResponseEntity<PaginaDTO<IndenizacaoMercadoriasRowDTO>> indenizacaoTabelaPaginada(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int tamanhoPagina,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(indenizacaoMercadoriasService.buscarTabelaPaginada(FiltroRequestMapper.from(dataInicio, dataFim, params), pagina, tamanhoPagina));
    }

    @GetMapping("/horarios-corte/overview")
    public ResponseEntity<HorariosCorteOverviewDTO> overview(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        FiltroConsultaDTO filtro = FiltroRequestMapper.from(dataInicio, dataFim, params);
        log.info("GET /api/painel/indicadores-gestao-a-vista/horarios-corte/overview - periodo: {} a {}", dataInicio, dataFim);
        return ResponseEntity.ok(horariosCorteService.buscarHorariosCorteOverview(filtro));
    }

    @GetMapping("/horarios-corte/serie")
    public ResponseEntity<List<HorariosCorteSeriePointDTO>> serie(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(horariosCorteService.buscarHorariosCorteSerie(FiltroRequestMapper.from(dataInicio, dataFim, params)));
    }

    @GetMapping("/horarios-corte/tabela")
    public ResponseEntity<List<HorarioCorteRowDTO>> tabela(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "100") int limite,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(horariosCorteService.buscarHorariosCorteTabela(FiltroRequestMapper.from(dataInicio, dataFim, params), limite));
    }

    @GetMapping("/horarios-corte/tabela/paginada")
    public ResponseEntity<PaginaDTO<HorarioCorteRowDTO>> tabelaPaginada(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int tamanhoPagina,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(horariosCorteService.buscarHorariosCorteTabelaPaginada(FiltroRequestMapper.from(dataInicio, dataFim, params), pagina, tamanhoPagina));
    }
}
