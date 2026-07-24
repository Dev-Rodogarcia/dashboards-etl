package com.dashboard.api.service;

import com.dashboard.api.dto.home.HomeSolicitacaoMelhoriaDTO;
import com.dashboard.api.dto.home.HomeSolicitacaoMelhoriaRequestDTO;
import com.dashboard.api.model.acesso.HomeSolicitacaoMelhoriaEntity;
import com.dashboard.api.model.acesso.UsuarioEntity;
import com.dashboard.api.repository.acesso.HomeSolicitacaoMelhoriaRepository;
import com.dashboard.api.repository.acesso.UsuarioRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomeSolicitacaoMelhoriaService {

    private static final Set<String> TIPOS_VALIDOS = Set.of("MELHORIA", "AUTOMACAO", "DASHBOARD", "CORRECAO", "OUTRO");
    private final HomeSolicitacaoMelhoriaRepository repository;
    private final UsuarioRepository usuarioRepository;

    public HomeSolicitacaoMelhoriaService(
            HomeSolicitacaoMelhoriaRepository repository,
            UsuarioRepository usuarioRepository
    ) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<HomeSolicitacaoMelhoriaDTO> listarAtivas() {
        return repository.listarAtivasOrdenadas().stream().map(this::toDto).toList();
    }

    @Transactional
    public HomeSolicitacaoMelhoriaDTO criar(HomeSolicitacaoMelhoriaRequestDTO request, String usuarioLogin) {
        String login = normalizarLogin(usuarioLogin);
        UsuarioEntity usuario = usuarioRepository.findByEmailIgnoreCase(login)
                .filter(UsuarioEntity::isAtivo)
                .orElseThrow(() -> new IllegalArgumentException("Usuário autenticado não encontrado."));

        HomeSolicitacaoMelhoriaEntity entity = new HomeSolicitacaoMelhoriaEntity();
        aplicarRequest(entity, request);
        entity.setStatus("ABERTA");
        entity.setAtivo(true);
        entity.setSolicitanteNome(usuario.getNome());
        entity.setSolicitanteEmail(usuario.getEmail());
        entity.setAtualizadoPor(login);
        return toDto(repository.save(entity));
    }

    @Transactional
    public HomeSolicitacaoMelhoriaDTO concluir(Long id, String usuarioLogin) {
        HomeSolicitacaoMelhoriaEntity entity = buscarAtiva(id);
        entity.setStatus("CONCLUIDA");
        entity.setConcluidoEm(Instant.now());
        entity.setAtualizadoPor(normalizarLogin(usuarioLogin));
        return toDto(repository.save(entity));
    }

    @Transactional
    public void arquivar(Long id, String usuarioLogin) {
        HomeSolicitacaoMelhoriaEntity entity = buscarAtiva(id);
        entity.setAtivo(false);
        entity.setAtualizadoPor(normalizarLogin(usuarioLogin));
        repository.save(entity);
    }

    private HomeSolicitacaoMelhoriaEntity buscarAtiva(Long id) {
        HomeSolicitacaoMelhoriaEntity entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitação não encontrada."));
        if (!entity.isAtivo()) {
            throw new IllegalArgumentException("Solicitação não encontrada.");
        }
        return entity;
    }

    private void aplicarRequest(HomeSolicitacaoMelhoriaEntity entity, HomeSolicitacaoMelhoriaRequestDTO request) {
        String tipo = request.tipo().trim().toUpperCase(Locale.ROOT);
        if (!TIPOS_VALIDOS.contains(tipo)) {
            throw new IllegalArgumentException("Tipo de solicitação inválido.");
        }

        entity.setTipo(tipo);
        entity.setTitulo(request.titulo().trim());
        entity.setDescricao(request.descricao().trim());
        entity.setResultadoEsperado(limparOpcional(request.resultadoEsperado()));
    }

    private String normalizarLogin(String usuarioLogin) {
        if (usuarioLogin == null || usuarioLogin.isBlank()) {
            throw new IllegalArgumentException("Usuário autenticado não encontrado.");
        }
        return usuarioLogin.trim().toLowerCase(Locale.ROOT);
    }

    private String limparOpcional(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return valor.trim();
    }

    private HomeSolicitacaoMelhoriaDTO toDto(HomeSolicitacaoMelhoriaEntity entity) {
        return new HomeSolicitacaoMelhoriaDTO(
                String.valueOf(entity.getId()),
                entity.getTipo(),
                entity.getTitulo(),
                entity.getDescricao(),
                entity.getResultadoEsperado(),
                entity.getStatus(),
                entity.getSolicitanteNome(),
                entity.getSolicitanteEmail(),
                entity.getCriadoEm(),
                entity.getConcluidoEm(),
                entity.getAtualizadoPor()
        );
    }
}
