package com.dashboard.api.service;

import com.dashboard.api.dto.indicadoresgestao.ViagemJustificativaDTO;
import com.dashboard.api.dto.indicadoresgestao.ViagemJustificativaRequestDTO;
import com.dashboard.api.model.ViagemJustificativa;
import com.dashboard.api.repository.ViagemJustificativaRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ViagemJustificativaService {

    private final ViagemJustificativaRepository repository;

    public ViagemJustificativaService(ViagemJustificativaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ViagemJustificativaDTO salvar(ViagemJustificativaRequestDTO request) {
        ViagemJustificativa entity = repository.findAnyByCodSolicitacao(request.codSolicitacao())
                .orElseGet(ViagemJustificativa::new);

        entity.setCodSolicitacao(request.codSolicitacao());
        entity.setJustificativa(request.justificativa().trim());
        entity.setCriadoEm(OffsetDateTime.now(ZoneOffset.UTC));
        entity.setCriadoPor(usuarioAtual());
        entity.setAtivo(true);

        return toDto(repository.save(entity));
    }

    @Transactional
    public void excluir(Long codSolicitacao) {
        repository.findByCodSolicitacao(codSolicitacao)
                .ifPresent(repository::delete);
    }

    private ViagemJustificativaDTO toDto(ViagemJustificativa entity) {
        return new ViagemJustificativaDTO(
                entity.getId(),
                entity.getCodSolicitacao(),
                entity.getJustificativa(),
                entity.getCriadoEm(),
                entity.getCriadoPor()
        );
    }

    private String usuarioAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getName() != null && !authentication.getName().isBlank()
                ? authentication.getName()
                : "sistema";
    }
}
