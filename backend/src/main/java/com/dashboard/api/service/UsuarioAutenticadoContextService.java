package com.dashboard.api.service;

import com.dashboard.api.model.acesso.UsuarioEntity;
import com.dashboard.api.repository.acesso.UsuarioRepository;
import com.dashboard.api.service.acesso.PermissaoResolverService;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioAutenticadoContextService {

    private static final int MAX_AUDIT_LENGTH = 100;

    private final UsuarioRepository usuarioRepository;
    private final PermissaoResolverService permissaoResolver;

    public UsuarioAutenticadoContextService(
            UsuarioRepository usuarioRepository,
            PermissaoResolverService permissaoResolver
    ) {
        this.usuarioRepository = usuarioRepository;
        this.permissaoResolver = permissaoResolver;
    }

    @Transactional(readOnly = true)
    public String getNomeComPapel() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticationName = authentication != null ? authentication.getName() : null;
        return getNomeComPapel(authenticationName);
    }

    @Transactional(readOnly = true)
    public String getNomeComPapel(String authenticationName) {
        String fallback = normalizar(authenticationName);
        if ("sistema".equals(fallback)) {
            return fallback;
        }

        return usuarioRepository.findByEmailIgnoreCase(fallback)
                .filter(UsuarioEntity::isAtivo)
                .map(usuario -> {
                    Long usuarioId = Objects.requireNonNull(usuario.getId(), "usuario.id e obrigatorio.");
                    String papel = permissaoResolver.papel(usuarioId);
                    return normalizar(usuario.getNome() + " | " + papel);
                })
                .orElse(fallback);
    }

    private String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return "sistema";
        }

        String normalizado = valor.trim();
        return normalizado.length() > MAX_AUDIT_LENGTH
                ? normalizado.substring(0, MAX_AUDIT_LENGTH)
                : normalizado;
    }
}
