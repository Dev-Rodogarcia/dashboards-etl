package com.dashboard.api.controller;

import com.dashboard.api.dto.indicadoresgestao.ClienteExcecaoCubagemDTO;
import com.dashboard.api.service.CubagemClientesExcecaoService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/painel/gestao-vista/cubagem/clientes")
@PreAuthorize("@acessoSeguranca.podeAcessar('indicadoresGestaoAVista')")
public class CubagemClientesExcecaoController {

    private final CubagemClientesExcecaoService service;

    public CubagemClientesExcecaoController(CubagemClientesExcecaoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ClienteExcecaoCubagemDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @DeleteMapping("/{cnpj}")
    public ResponseEntity<Void> excluir(@PathVariable String cnpj) {
        service.excluir(cnpj);
        return ResponseEntity.noContent().build();
    }
}
