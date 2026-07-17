package com.dashboard.api.config;

import com.dashboard.api.client.esl.EslCorporacaoResolver;
import com.dashboard.api.client.esl.EslCorporacaoResolver.CorporacaoResolvida;
import com.dashboard.api.client.esl.EslGraphqlProperties;
import com.dashboard.api.dto.esl.EslContextoOperacionalDTO;
import com.dashboard.api.exception.EslGraphqlConfiguracaoException;
import com.dashboard.api.model.acesso.UsuarioEntity;
import com.dashboard.api.repository.acesso.UsuarioRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Resolve a corporação pela filial selecionada e o solicitante pela sessão
 * autenticada, sem expor dados operacionais ao frontend.
 */
@Component
public class EslContextoOperacionalProvider {

    private final EslGraphqlProperties properties;
    private final EslCorporacaoResolver corporacaoResolver;
    private final EscopoFilialService escopoFilialService;
    private final UsuarioRepository usuarioRepository;

    @Autowired
    public EslContextoOperacionalProvider(
            EslGraphqlProperties properties,
            EslCorporacaoResolver corporacaoResolver,
            EscopoFilialService escopoFilialService,
            UsuarioRepository usuarioRepository
    ) {
        this.properties = properties;
        this.corporacaoResolver = corporacaoResolver;
        this.escopoFilialService = escopoFilialService;
        this.usuarioRepository = usuarioRepository;
    }

    public EslContextoOperacionalProvider(EslGraphqlProperties properties) {
        this(properties, null, null, null);
    }

    public EslContextoOperacionalDTO obter() {
        return obter(null);
    }

    public EslContextoOperacionalDTO obter(String filial) {
        CorporacaoResolvida corporacao = StringUtils.hasText(filial)
                ? resolverCorporacaoDaFilial(filial)
                : new CorporacaoResolvida(obrigatorio(properties.corporationDocument(), "ESL_CORPORATION_DOCUMENT"), null);
        UsuarioEntity usuario = usuarioAutenticado();
        return new EslContextoOperacionalDTO(
                corporacao.documento(),
                corporacao.id(),
                usuario != null ? obrigatorio(usuario.getNome(), "nome do usuário autenticado")
                        : obrigatorio(properties.requesterName(), "ESL_REQUESTER_NAME"),
                usuario != null ? obrigatorio(usuario.getEmail(), "e-mail do usuário autenticado")
                        : obrigatorio(properties.requesterEmail(), "ESL_REQUESTER_EMAIL"),
                textoOpcional(properties.requesterPhone()),
                textoOpcional(properties.requesterDepartment())
        );
    }

    private CorporacaoResolvida resolverCorporacaoDaFilial(String filial) {
        if (escopoFilialService != null && !escopoFilialService.escopoAtual().permiteAlgumaFilial(filial)) {
            throw new AccessDeniedException("A filial selecionada não pertence ao escopo autorizado.");
        }
        if (corporacaoResolver == null) {
            return new CorporacaoResolvida(
                    obrigatorio(properties.corporationDocument(), "ESL_CORPORATION_DOCUMENT"),
                    null
            );
        }
        return corporacaoResolver.resolverCorporacao(filial);
    }

    private UsuarioEntity usuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication != null ? authentication.getPrincipal() : null;
        if (principal instanceof UsuarioEntity usuario && usuario.isAtivo()) {
            return usuario;
        }
        if (authentication == null || !StringUtils.hasText(authentication.getName()) || usuarioRepository == null) {
            return null;
        }
        return usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .filter(UsuarioEntity::isAtivo)
                .orElse(null);
    }

    private String obrigatorio(String valor, String variavel) {
        if (!StringUtils.hasText(valor)) {
            throw new EslGraphqlConfiguracaoException(
                    "Configure " + variavel + " para habilitar as operações ESL."
            );
        }
        return valor.trim();
    }

    private String textoOpcional(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }
}
