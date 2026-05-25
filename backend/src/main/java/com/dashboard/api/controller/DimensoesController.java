package com.dashboard.api.controller;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.dimensoes.PagadorDimDTO;
import com.dashboard.api.dto.dimensoes.PlanoContasDimDTO;
import com.dashboard.api.dto.dimensoes.UsuarioDimDTO;
import com.dashboard.api.dto.dimensoes.VeiculoDimDTO;
import com.dashboard.api.service.DashboardExportService;
import com.dashboard.api.service.DimensoesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dimensoes")
public class DimensoesController {

    private static final Logger log = LoggerFactory.getLogger(DimensoesController.class);

    private final DimensoesService dimensoesService;
    private final DashboardExportService dashboardExportService;

    public DimensoesController(DimensoesService dimensoesService, DashboardExportService dashboardExportService) {
        this.dimensoesService = dimensoesService;
        this.dashboardExportService = dashboardExportService;
    }

    @GetMapping("/filiais")
    @PreAuthorize("@acessoSeguranca.podeAcessarDimensaoFiliais()")
    public List<String> filiais() {
        log.info("GET /api/dimensoes/filiais");
        return dimensoesService.listarFiliais();
    }

    @GetMapping("/clientes")
    @PreAuthorize("@acessoSeguranca.podeAcessarDimensaoClientes()")
    public List<String> clientes() {
        log.info("GET /api/dimensoes/clientes");
        return dimensoesService.listarClientes();
    }

    @GetMapping("/pagadores")
    @PreAuthorize("@acessoSeguranca.podeAcessar('performance')")
    public List<PagadorDimDTO> pagadores(
            @RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "50") int limite
    ) {
        log.info("GET /api/dimensoes/pagadores - busca: {}", busca);
        return dimensoesService.buscarPagadores(busca, limite);
    }

    @GetMapping("/faturas-por-cliente/clientes-cnpj")
    @PreAuthorize("@acessoSeguranca.podeAcessar('faturasPorCliente')")
    public List<String> faturasPorClienteClientesCnpj() {
        log.info("GET /api/dimensoes/faturas-por-cliente/clientes-cnpj");
        return dimensoesService.listarClientesCnpjFaturasPorCliente();
    }

    @GetMapping("/motoristas")
    @PreAuthorize("@acessoSeguranca.podeAcessarDimensaoMotoristas()")
    public List<String> motoristas() {
        log.info("GET /api/dimensoes/motoristas");
        return dimensoesService.listarMotoristas();
    }

    @GetMapping("/veiculos")
    @PreAuthorize("@acessoSeguranca.podeAcessarDimensaoVeiculos()")
    public List<VeiculoDimDTO> veiculos() {
        log.info("GET /api/dimensoes/veiculos");
        return dimensoesService.listarVeiculos();
    }

    @GetMapping("/planocontas")
    @PreAuthorize("@acessoSeguranca.podeAcessarDimensaoPlanoContas()")
    public List<PlanoContasDimDTO> planoContas() {
        log.info("GET /api/dimensoes/planocontas");
        return dimensoesService.listarPlanoContas();
    }

    @GetMapping("/usuarios")
    @PreAuthorize("@acessoSeguranca.podeAcessarDimensaoUsuarios()")
    public List<UsuarioDimDTO> usuarios() {
        log.info("GET /api/dimensoes/usuarios");
        return dimensoesService.listarUsuarios();
    }

    @GetMapping("/fretes/status")
    @PreAuthorize("@acessoSeguranca.podeAcessar('fretes')")
    public List<String> fretesStatus(
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim,
            @RequestParam MultiValueMap<String, String> params
    ) {
        log.info("GET /api/dimensoes/fretes/status - periodo: {} a {}", dataInicio, dataFim);
        FiltroConsultaDTO filtro = FiltroRequestMapper.from(dataInicio, dataFim, params);
        return dashboardExportService.listarStatusFretes(filtro);
    }
}
