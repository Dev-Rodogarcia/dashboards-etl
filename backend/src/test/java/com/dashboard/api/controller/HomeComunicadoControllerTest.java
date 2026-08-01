package com.dashboard.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dashboard.api.model.acesso.HomeComunicadoComentarioEntity;
import com.dashboard.api.model.acesso.HomeComunicadoEntity;
import com.dashboard.api.model.acesso.UsuarioEntity;
import com.dashboard.api.repository.acesso.HomeComunicadoComentarioRepository;
import com.dashboard.api.repository.acesso.HomeComunicadoCurtidaRepository;
import com.dashboard.api.repository.acesso.HomeComunicadoRepository;
import com.dashboard.api.repository.acesso.UsuarioRepository;
import com.dashboard.api.security.AcessoSeguranca;
import com.dashboard.api.security.PermissaoCatalogo;
import com.dashboard.api.service.HomeComunicadoService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class HomeComunicadoControllerTest {

    private static final long COMUNICADO_ID = 10L;
    private static final long COMENTARIO_ID = 20L;

    @Mock private HomeComunicadoRepository comunicadoRepository;
    @Mock private HomeComunicadoCurtidaRepository curtidaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private HomeComunicadoComentarioRepository comentarioRepository;

    private HomeComunicadoController controller;
    private HomeComunicadoComentarioEntity comentario;
    private UsuarioEntity solicitante;

    @BeforeEach
    void setUp() {
        HomeComunicadoService service = new HomeComunicadoService(
                comunicadoRepository,
                curtidaRepository,
                usuarioRepository,
                comentarioRepository
        );
        controller = new HomeComunicadoController(service, new AcessoSeguranca());

        HomeComunicadoEntity comunicado = new HomeComunicadoEntity();
        comunicado.setId(COMUNICADO_ID);
        comunicado.setAtivo(true);
        when(comunicadoRepository.findById(COMUNICADO_ID)).thenReturn(Optional.of(comunicado));

        solicitante = new UsuarioEntity();
        solicitante.setId(1L);

        comentario = new HomeComunicadoComentarioEntity();
        comentario.setComunicadoId(COMUNICADO_ID);
        comentario.setUsuarioId(2L);
        comentario.setAtivo(true);
        when(comentarioRepository.findById(COMENTARIO_ID)).thenReturn(Optional.of(comentario));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void gestorDeComunicadosNaoPodeExcluirComentarioDeOutroUsuario() {
        when(usuarioRepository.findByEmailIgnoreCaseAndAtivoTrue("gestor@empresa.com")).thenReturn(Optional.of(solicitante));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "gestor@empresa.com",
                null,
                List.of(() -> PermissaoCatalogo.authorityForKey("can_manage_communications"))
        ));

        assertThatThrownBy(() -> controller.excluirComentario(
                COMUNICADO_ID,
                COMENTARIO_ID,
                new UsernamePasswordAuthenticationToken("gestor@empresa.com", null)
        )).isInstanceOf(AccessDeniedException.class);

        verify(comentarioRepository, never()).save(comentario);
        assertThat(comentario.isAtivo()).isTrue();
    }

    @Test
    void administradorPodeExcluirComentarioDeOutroUsuario() {
        when(usuarioRepository.findByEmailIgnoreCaseAndAtivoTrue("admin@empresa.com")).thenReturn(Optional.of(solicitante));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin@empresa.com",
                null,
                List.of(() -> "ROLE_ADMIN")
        ));

        controller.excluirComentario(
                COMUNICADO_ID,
                COMENTARIO_ID,
                new UsernamePasswordAuthenticationToken("admin@empresa.com", null)
        );

        verify(comentarioRepository).save(comentario);
        assertThat(comentario.isAtivo()).isFalse();
    }
}
