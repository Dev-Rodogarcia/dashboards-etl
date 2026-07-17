package com.dashboard.api.config;

import com.dashboard.api.client.esl.EslGraphqlProperties;
import com.dashboard.api.model.acesso.UsuarioEntity;
import com.dashboard.api.repository.acesso.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EslContextoOperacionalProviderTest {

    @AfterEach
    void limparContextoDeSeguranca() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveUsarNomeEEmailDoUsuarioResolvidoPeloJwtSemExigirVariaveisDeSolicitante() {
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setNome("Operador Dashboard");
        usuario.setEmail("operador@rodogarcia.com.br");
        usuario.setAtivo(true);
        when(usuarioRepository.findByEmailIgnoreCase("operador@rodogarcia.com.br"))
                .thenReturn(Optional.of(usuario));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("operador@rodogarcia.com.br", null)
        );
        EslContextoOperacionalProvider provider = new EslContextoOperacionalProvider(
                propriedadesSemSolicitanteConfigurado(), null, null, usuarioRepository
        );

        var contexto = provider.obter();

        assertThat(contexto.documentoCorporacao()).isEqualTo("60960000024300");
        assertThat(contexto.idCorporacao()).isNull();
        assertThat(contexto.nomeSolicitante()).isEqualTo("Operador Dashboard");
        assertThat(contexto.emailSolicitante()).isEqualTo("operador@rodogarcia.com.br");
        verify(usuarioRepository).findByEmailIgnoreCase("operador@rodogarcia.com.br");
    }

    private EslGraphqlProperties propriedadesSemSolicitanteConfigurado() {
        return new EslGraphqlProperties(
                "https://esl.example/graphql",
                "token",
                "60960000024300",
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
