package com.dashboard.api.service.acesso;

import com.dashboard.api.model.acesso.UsuarioEntity;
import com.dashboard.api.policy.EscopoFiliaisUsuarioPolicy;
import com.dashboard.api.repository.acesso.EscopoFiliaisUsuarioStore;
import com.dashboard.api.repository.acesso.UsuarioRepository;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EscopoFilialService {

    private final UsuarioRepository usuarioRepository;
    private final PermissaoResolverService permissaoResolver;
    private final EscopoFiliaisUsuarioStore escopoFiliaisUsuarioStore;

    @Autowired
    public EscopoFilialService(
            UsuarioRepository usuarioRepository,
            PermissaoResolverService permissaoResolver,
            EscopoFiliaisUsuarioStore escopoFiliaisUsuarioStore
    ) {
        this.usuarioRepository = usuarioRepository;
        this.permissaoResolver = permissaoResolver;
        this.escopoFiliaisUsuarioStore = escopoFiliaisUsuarioStore;
    }

    public EscopoFilialService(
            UsuarioRepository usuarioRepository,
            PermissaoResolverService permissaoResolver
    ) {
        this(usuarioRepository, permissaoResolver, null);
    }

    @Transactional(readOnly = true)
    public EscopoFilial escopoAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return EscopoFilial.semAcesso();
        }

        UsuarioEntity usuario = usuarioRepository.findByEmailIgnoreCase(authentication.getName()).orElse(null);
        if (usuario == null || !usuario.isAtivo()) {
            return EscopoFilial.semAcesso();
        }
        if (usuario.getSetor() == null) {
            return EscopoFilial.semAcesso();
        }

        if (permissaoResolver.ehAdminPlataforma(usuario.getId()) || permissaoResolver.ehDesenvolvedor(usuario.getId())) {
            return EscopoFilial.comAcessoTotal();
        }

        if (escopoFiliaisUsuarioStore != null) {
            escopoFiliaisUsuarioStore.carregarNoUsuario(usuario);
        }
        return EscopoFiliaisUsuarioPolicy.resolverSemPapelElevado(usuario);
    }

    public record EscopoFilial(boolean acessoTotal, List<String> filiaisPermitidas) {

        public static EscopoFilial comAcessoTotal() {
            return new EscopoFilial(true, List.of());
        }

        public static EscopoFilial semAcesso() {
            return new EscopoFilial(false, List.of());
        }

        public boolean permiteAlgumaFilial(String... filiais) {
            if (acessoTotal) {
                return true;
            }

            Set<String> permitidas = filiaisPermitidas.stream()
                    .map(EscopoFilial::normalizar)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            if (permitidas.isEmpty()) {
                return false;
            }

            return Arrays.stream(filiais)
                    .filter(valor -> valor != null && !valor.isBlank())
                    .map(EscopoFilial::normalizar)
                    .anyMatch(permitidas::contains);
        }

        public List<String> filiaisOrdenadas() {
            return filiaisPermitidas.stream()
                    .filter(valor -> valor != null && !valor.isBlank())
                    .map(String::trim)
                    .sorted(Comparator.comparing(valor -> valor.toLowerCase(Locale.ROOT)))
                    .toList();
        }

        private static String normalizar(String valor) {
            return valor.trim().toLowerCase(Locale.ROOT);
        }
    }
}
