package com.dashboard.api.controller;

import com.dashboard.api.dto.coletas.ColetaResumoDTO;
import com.dashboard.api.dto.contaspagar.ContaPagarResumoDTO;
import com.dashboard.api.dto.cotacoes.CotacaoResumoDTO;
import com.dashboard.api.dto.etl.EtlExecucaoResumoDTO;
import com.dashboard.api.dto.faturascliente.FaturaPorClienteResumoDTO;
import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.fretes.FreteResumoDTO;
import com.dashboard.api.dto.manifestos.ManifestoResumoDTO;
import com.dashboard.api.dto.PaginaDTO;
import com.dashboard.api.dto.tracking.TrackingResumoDTO;
import com.dashboard.api.service.DashboardTabelaPaginadaService;
import com.dashboard.api.service.TrackingService;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/painel")
public class PainelTabelaPaginadaController {

    private final DashboardTabelaPaginadaService tabelaPaginadaService;
    private final TrackingService trackingService;

    public PainelTabelaPaginadaController(DashboardTabelaPaginadaService tabelaPaginadaService, TrackingService trackingService) {
        this.tabelaPaginadaService = tabelaPaginadaService;
        this.trackingService = trackingService;
    }

    @GetMapping("/coletas/tabela/paginada")
    @PreAuthorize("@acessoSeguranca.podeAcessar('coletas')")
    public ResponseEntity<PaginaDTO<ColetaResumoDTO>> coletas(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int tamanhoPagina,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(tabelaPaginadaService.buscarColetas(filtro(dataInicio, dataFim, params), pagina, tamanhoPagina));
    }

    @GetMapping("/fretes/tabela/paginada")
    @PreAuthorize("@acessoSeguranca.podeAcessar('fretes')")
    public ResponseEntity<PaginaDTO<FreteResumoDTO>> fretes(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int tamanhoPagina,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(tabelaPaginadaService.buscarFretes(filtro(dataInicio, dataFim, params), pagina, tamanhoPagina));
    }

    @GetMapping("/tracking/tabela/paginada")
    @PreAuthorize("@acessoSeguranca.podeAcessar('tracking')")
    public ResponseEntity<PaginaDTO<TrackingResumoDTO>> tracking(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int tamanhoPagina,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(tabelaPaginadaService.buscarTracking(filtroTracking(dataInicio, dataFim, params), pagina, tamanhoPagina));
    }

    @GetMapping("/manifestos/tabela/paginada")
    @PreAuthorize("@acessoSeguranca.podeAcessar('manifestos')")
    public ResponseEntity<PaginaDTO<ManifestoResumoDTO>> manifestos(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int tamanhoPagina,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(tabelaPaginadaService.buscarManifestos(filtro(dataInicio, dataFim, params), pagina, tamanhoPagina));
    }

    @GetMapping("/cotacoes/tabela/paginada")
    @PreAuthorize("@acessoSeguranca.podeAcessar('cotacoes')")
    public ResponseEntity<PaginaDTO<CotacaoResumoDTO>> cotacoes(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        int pagina = pageable.getPageNumber() + 1;
        int tamanhoPagina = pageable.getPageSize();
        return ResponseEntity.ok(tabelaPaginadaService.buscarCotacoes(filtro(dataInicio, dataFim, params), pagina, tamanhoPagina));
    }

    @GetMapping("/contas-a-pagar/tabela/paginada")
    @PreAuthorize("@acessoSeguranca.podeAcessar('contasAPagar')")
    public ResponseEntity<PaginaDTO<ContaPagarResumoDTO>> contasAPagar(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int tamanhoPagina,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(tabelaPaginadaService.buscarContasAPagar(filtro(dataInicio, dataFim, params), pagina, tamanhoPagina));
    }

    @GetMapping("/faturas-por-cliente/tabela/paginada")
    @PreAuthorize("@acessoSeguranca.podeAcessar('faturasPorCliente')")
    public ResponseEntity<PaginaDTO<FaturaPorClienteResumoDTO>> faturasPorCliente(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int tamanhoPagina,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(tabelaPaginadaService.buscarFaturasPorCliente(filtro(dataInicio, dataFim, params), pagina, tamanhoPagina));
    }

    @GetMapping("/etl-saude/tabela/paginada")
    @PreAuthorize("@acessoSeguranca.podeAcessar('etlSaude')")
    public ResponseEntity<PaginaDTO<EtlExecucaoResumoDTO>> etlSaude(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int tamanhoPagina,
            @RequestParam MultiValueMap<String, String> params
    ) {
        return ResponseEntity.ok(tabelaPaginadaService.buscarEtlSaude(filtro(dataInicio, dataFim, params), pagina, tamanhoPagina));
    }

    private FiltroConsultaDTO filtro(LocalDate dataInicio, LocalDate dataFim, MultiValueMap<String, String> params) {
        return FiltroRequestMapper.from(dataInicio, dataFim, params);
    }

    private FiltroConsultaDTO filtroTracking(LocalDate dataInicio, LocalDate dataFim, MultiValueMap<String, String> params) {
        return trackingService.normalizarFiltroComFilialAtualObrigatoria(filtro(dataInicio, dataFim, params));
    }
}
