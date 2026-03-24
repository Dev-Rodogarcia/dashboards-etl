package com.dashboard.api.controller;

import com.dashboard.api.dto.acesso.*;
import com.dashboard.api.model.acesso.AuditLog;
import com.dashboard.api.repository.acesso.AuditLogRepository;
import com.dashboard.api.repository.acesso.PapelRepository;
import com.dashboard.api.security.PermissaoCatalogo;
import com.dashboard.api.service.acesso.GestaoSetorService;
import com.dashboard.api.service.acesso.GestaoUsuarioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/acesso")
@PreAuthorize("@acessoSeguranca.ehAdmin()")
public class AdminAcessoController {

    private final GestaoSetorService gestaoSetorService;
    private final GestaoUsuarioService gestaoUsuarioService;
    private final PapelRepository papelRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminAcessoController(
            GestaoSetorService gestaoSetorService,
            GestaoUsuarioService gestaoUsuarioService,
            PapelRepository papelRepository,
            AuditLogRepository auditLogRepository
    ) {
        this.gestaoSetorService = gestaoSetorService;
        this.gestaoUsuarioService = gestaoUsuarioService;
        this.papelRepository = papelRepository;
        this.auditLogRepository = auditLogRepository;
    }

    // --- Catalogo de permissoes ---

    @GetMapping("/catalogo-permissoes")
    public List<PermissaoCatalogoItemDTO> catalogoPermissoes() {
        return PermissaoCatalogo.catalogo();
    }

    // --- Setores ---

    @GetMapping("/setores")
    public List<SetorDTO> setores() {
        return gestaoSetorService.listarSetores();
    }

    @PostMapping("/setores")
    public ResponseEntity<SetorDTO> criarSetor(@Valid @RequestBody SetorRequestDTO request) {
        return ResponseEntity.ok(gestaoSetorService.criarSetor(request));
    }

    @PutMapping("/setores/{setorId}")
    public ResponseEntity<SetorDTO> atualizarSetor(
            @PathVariable Long setorId,
            @Valid @RequestBody SetorRequestDTO request) {
        return ResponseEntity.ok(gestaoSetorService.atualizarSetor(setorId, request));
    }

    @DeleteMapping("/setores/{setorId}")
    public ResponseEntity<Void> excluirSetor(@PathVariable Long setorId) {
        gestaoSetorService.excluirSetor(setorId);
        return ResponseEntity.noContent().build();
    }

    // --- Usuarios ---

    @GetMapping("/usuarios")
    public List<UsuarioAcessoDTO> usuarios() {
        return gestaoUsuarioService.listarUsuarios();
    }

    @PostMapping("/usuarios")
    public ResponseEntity<UsuarioAcessoDTO> criarUsuario(@Valid @RequestBody UsuarioRequestDTO request) {
        return ResponseEntity.ok(gestaoUsuarioService.criarUsuario(request));
    }

    @PutMapping("/usuarios/{usuarioId}")
    public ResponseEntity<UsuarioAcessoDTO> atualizarUsuario(
            @PathVariable Long usuarioId,
            @Valid @RequestBody UsuarioRequestDTO request) {
        return ResponseEntity.ok(gestaoUsuarioService.atualizarUsuario(usuarioId, request));
    }

    @DeleteMapping("/usuarios/{usuarioId}")
    public ResponseEntity<Void> excluirUsuario(@PathVariable Long usuarioId) {
        gestaoUsuarioService.excluirUsuario(usuarioId);
        return ResponseEntity.noContent().build();
    }

    // --- Papeis ---

    @GetMapping("/papeis")
    public List<PapelDTO> papeis() {
        return papelRepository.findAll().stream()
                .map(p -> new PapelDTO(p.getId(), p.getNome(), p.getDescricao(), p.getNivel()))
                .toList();
    }

    @PutMapping("/usuarios/{usuarioId}/papeis")
    public ResponseEntity<Void> atribuirPapeis(
            @PathVariable Long usuarioId,
            @Valid @RequestBody AtribuirPapeisRequestDTO request) {
        gestaoUsuarioService.atribuirPapeis(usuarioId, request.papelIds());
        return ResponseEntity.ok().build();
    }

    // --- Overrides de permissao ---

    @GetMapping("/usuarios/{usuarioId}/overrides")
    public List<PermissaoOverrideDTO> buscarOverrides(@PathVariable Long usuarioId) {
        return gestaoUsuarioService.buscarOverrides(usuarioId).stream()
                .map(o -> new PermissaoOverrideDTO(o.permissaoChave(), o.tipo()))
                .toList();
    }

    @PutMapping("/usuarios/{usuarioId}/overrides")
    public ResponseEntity<Void> salvarOverrides(
            @PathVariable Long usuarioId,
            @Valid @RequestBody List<PermissaoOverrideDTO> overrides) {
        List<GestaoUsuarioService.OverrideDTO> dtos = overrides.stream()
                .map(o -> new GestaoUsuarioService.OverrideDTO(o.permissaoChave(), o.tipo()))
                .toList();
        gestaoUsuarioService.salvarOverrides(usuarioId, dtos);
        return ResponseEntity.ok().build();
    }

    // --- Audit logs ---

    @GetMapping("/audit-logs")
    @PreAuthorize("@acessoSeguranca.possuiPapel('admin_plataforma')")
    public ResponseEntity<?> auditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String acao,
            @RequestParam(required = false) Long usuarioId) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 200));
        Page<AuditLog> resultado;

        if (acao != null && usuarioId != null) {
            resultado = auditLogRepository.findByAcaoAndUsuarioIdOrderByTimestampUtcDesc(acao, usuarioId, pageable);
        } else if (acao != null) {
            resultado = auditLogRepository.findByAcaoOrderByTimestampUtcDesc(acao, pageable);
        } else if (usuarioId != null) {
            resultado = auditLogRepository.findByUsuarioIdOrderByTimestampUtcDesc(usuarioId, pageable);
        } else {
            resultado = auditLogRepository.findAllByOrderByTimestampUtcDesc(pageable);
        }

        var content = resultado.getContent().stream()
                .map(log -> new AuditLogDTO(
                        log.getId(),
                        log.getTimestampUtc(),
                        log.getUsuarioLogin(),
                        log.getAcao(),
                        log.getRecurso(),
                        log.getDetalhesJson(),
                        log.getIpAddress()
                ))
                .toList();

        return ResponseEntity.ok(new AuditLogPageDTO(content, resultado.getTotalPages(), resultado.getTotalElements()));
    }
}
