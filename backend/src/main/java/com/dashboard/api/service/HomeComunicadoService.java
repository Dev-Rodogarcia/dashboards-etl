package com.dashboard.api.service;

import com.dashboard.api.dto.home.HomeComunicadoDTO;
import com.dashboard.api.dto.home.HomeComunicadoRequestDTO;
import com.dashboard.api.model.acesso.HomeComunicadoEntity;
import com.dashboard.api.repository.acesso.HomeComunicadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class HomeComunicadoService {

    private static final Set<String> TAGS_VALIDAS = Set.of("NOVO", "ATENCAO", "FIXADO");
    private final HomeComunicadoRepository repository;

    public HomeComunicadoService(HomeComunicadoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<HomeComunicadoDTO> listarAtivos() {
        return repository.listarAtivosOrdenados().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public HomeComunicadoDTO criar(HomeComunicadoRequestDTO request, String usuarioLogin) {
        HomeComunicadoEntity entity = new HomeComunicadoEntity();
        aplicarRequest(entity, request);
        entity.setAtivo(true);
        entity.setPublicadoEm(Instant.now());
        entity.setCriadoPor(usuarioLogin);
        entity.setAtualizadoPor(usuarioLogin);
        return toDto(repository.save(entity));
    }

    @Transactional
    public HomeComunicadoDTO atualizar(Long id, HomeComunicadoRequestDTO request, String usuarioLogin) {
        HomeComunicadoEntity entity = buscarAtivo(id);
        aplicarRequest(entity, request);
        entity.setAtualizadoPor(usuarioLogin);
        return toDto(repository.save(entity));
    }

    @Transactional
    public void arquivar(Long id, String usuarioLogin) {
        HomeComunicadoEntity entity = buscarAtivo(id);
        entity.setAtivo(false);
        entity.setAtualizadoPor(usuarioLogin);
        repository.save(entity);
    }

    private HomeComunicadoEntity buscarAtivo(Long id) {
        HomeComunicadoEntity entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comunicado nao encontrado."));

        if (!entity.isAtivo()) {
            throw new IllegalArgumentException("Comunicado nao encontrado.");
        }

        return entity;
    }

    private void aplicarRequest(HomeComunicadoEntity entity, HomeComunicadoRequestDTO request) {
        String tag = normalizarTag(request.tag());
        if (!TAGS_VALIDAS.contains(tag)) {
            throw new IllegalArgumentException("Tag de comunicado invalida.");
        }

        entity.setTitulo(request.titulo().trim());
        entity.setCorpo(request.corpo().trim());
        entity.setTag(tag);
        entity.setPublicoAlvo(request.publicoAlvo().trim());
    }

    private String normalizarTag(String tag) {
        if (tag == null) return "";
        String semAcento = Normalizer.normalize(tag, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.trim().toUpperCase(Locale.ROOT);
    }

    private HomeComunicadoDTO toDto(HomeComunicadoEntity entity) {
        return new HomeComunicadoDTO(
                String.valueOf(entity.getId()),
                entity.getTitulo(),
                entity.getCorpo(),
                entity.getTag(),
                entity.getPublicoAlvo(),
                entity.getPublicadoEm(),
                entity.getAtualizadoPor(),
                entity.getAtualizadoEm()
        );
    }
}
