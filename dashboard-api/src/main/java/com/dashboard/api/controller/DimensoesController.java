package com.dashboard.api.controller;

import com.dashboard.api.dto.dimensoes.PlanoContasDimDTO;
import com.dashboard.api.dto.dimensoes.UsuarioDimDTO;
import com.dashboard.api.dto.dimensoes.VeiculoDimDTO;
import com.dashboard.api.service.DimensoesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dimensoes")
public class DimensoesController {

    private static final Logger log = LoggerFactory.getLogger(DimensoesController.class);

    private final DimensoesService dimensoesService;

    public DimensoesController(DimensoesService dimensoesService) {
        this.dimensoesService = dimensoesService;
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
}
