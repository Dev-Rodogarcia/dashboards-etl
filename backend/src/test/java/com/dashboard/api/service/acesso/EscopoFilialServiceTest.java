package com.dashboard.api.service.acesso;

import com.dashboard.api.model.acesso.SetorEntity;
import com.dashboard.api.model.acesso.UsuarioEntity;
import com.dashboard.api.repository.acesso.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EscopoFilialServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    private final StubPermissaoResolverService permissaoResolver = new StubPermissaoResolverService();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void usuarioSemOverrideHerdaFiliaisDoSetor() {
        UsuarioEntity usuario = usuarioBase();
        usuario.setEscopoFiliaisTipo(EscopoFiliaisUsuarioPolicy.HERDAR_SETOR);

        EscopoFilialService.EscopoFilial escopo = resolver(usuario);

        assertFalse(escopo.acessoTotal());
        assertEquals(List.of("AGU", "CAS"), escopo.filiaisPermitidas());
    }

    @Test
    void usuarioComTodasRecebeAcessoTotal() {
        UsuarioEntity usuario = usuarioBase();
        usuario.setEscopoFiliaisTipo(EscopoFiliaisUsuarioPolicy.TODAS);

        EscopoFilialService.EscopoFilial escopo = resolver(usuario);

        assertTrue(escopo.acessoTotal());
        assertEquals(List.of(), escopo.filiaisPermitidas());
    }

    @Test
    void usuarioComSelecionadasUsaFiliaisDoUsuario() {
        UsuarioEntity usuario = usuarioBase();
        usuario.setEscopoFiliaisTipo(EscopoFiliaisUsuarioPolicy.SELECIONADAS);
        usuario.setFiliaisPermitidasUsuario(Set.of("CPQ", "AGU"));

        EscopoFilialService.EscopoFilial escopo = resolver(usuario);

        assertFalse(escopo.acessoTotal());
        assertEquals(List.of("AGU", "CPQ"), escopo.filiaisPermitidas());
    }

    @Test
    void adminPlataformaMantemAcessoTotalPorPapel() {
        UsuarioEntity usuario = usuarioBase();
        usuario.setEscopoFiliaisTipo(EscopoFiliaisUsuarioPolicy.SELECIONADAS);
        usuario.setFiliaisPermitidasUsuario(Set.of("AGU"));

        autenticar(usuario);
        permissaoResolver.adminPlataforma = true;

        EscopoFilialService service = new EscopoFilialService(usuarioRepository, permissaoResolver);
        EscopoFilialService.EscopoFilial escopo = service.escopoAtual();

        assertTrue(escopo.acessoTotal());
        assertEquals(List.of(), escopo.filiaisPermitidas());
    }

    private EscopoFilialService.EscopoFilial resolver(UsuarioEntity usuario) {
        autenticar(usuario);
        permissaoResolver.adminPlataforma = false;
        permissaoResolver.desenvolvedor = false;

        EscopoFilialService service = new EscopoFilialService(usuarioRepository, permissaoResolver);
        return service.escopoAtual();
    }

    private void autenticar(UsuarioEntity usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario.getEmail(), null, List.of())
        );
        when(usuarioRepository.findByEmailIgnoreCase(usuario.getEmail())).thenReturn(Optional.of(usuario));
    }

    private UsuarioEntity usuarioBase() {
        SetorEntity setor = new SetorEntity();
        setor.setId(1L);
        setor.setNome("Qualidade");
        setor.setFiliaisPermitidas(Set.of("CAS", "AGU"));

        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(10L);
        usuario.setEmail("qualidade@empresa.com");
        usuario.setAtivo(true);
        usuario.setSetor(setor);
        return usuario;
    }

    private static final class StubPermissaoResolverService extends PermissaoResolverService {
        private boolean adminPlataforma;
        private boolean desenvolvedor;

        private StubPermissaoResolverService() {
            super(
                    null,
                    null,
                    null,
                    null,
                    new UsuarioSupremo("supremo@empresa.com", "Senha@123456", "Supremo", "desenvolvedor", 1000, false)
            );
        }

        @Override
        public boolean ehAdminPlataforma(Long usuarioId) {
            return adminPlataforma;
        }

        @Override
        public boolean ehDesenvolvedor(Long usuarioId) {
            return desenvolvedor;
        }
    }
}
