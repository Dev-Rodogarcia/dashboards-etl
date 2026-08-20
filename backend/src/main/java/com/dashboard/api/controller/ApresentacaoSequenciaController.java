package com.dashboard.api.controller;

import com.dashboard.api.dto.apresentacao.ApresentacaoSequenciaDTO;
import com.dashboard.api.dto.apresentacao.ApresentacaoSequenciaRequestDTO;
import com.dashboard.api.service.ApresentacaoSequenciaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/apresentacoes")
public class ApresentacaoSequenciaController {
    private final ApresentacaoSequenciaService service;
    public ApresentacaoSequenciaController(ApresentacaoSequenciaService service) { this.service = service; }
    @GetMapping public List<ApresentacaoSequenciaDTO> listar(Authentication authentication) { return service.listar(authentication.getName()); }
    @PostMapping public ResponseEntity<ApresentacaoSequenciaDTO> criar(@Valid @RequestBody ApresentacaoSequenciaRequestDTO request, Authentication authentication) { return ResponseEntity.ok(service.criar(request, authentication.getName())); }
    @PutMapping("/{id}") public ApresentacaoSequenciaDTO atualizar(@PathVariable Long id, @Valid @RequestBody ApresentacaoSequenciaRequestDTO request, Authentication authentication) { return service.atualizar(id, request, authentication.getName()); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> inativar(@PathVariable Long id, Authentication authentication) { service.inativar(id, authentication.getName()); return ResponseEntity.noContent().build(); }
}
