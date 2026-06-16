package com.dashboard.api.service.acesso;

import com.dashboard.api.exception.CredencialInvalidaException;
import com.dashboard.api.model.acesso.RefreshTokenSession;
import com.dashboard.api.model.acesso.UsuarioEntity;
import com.dashboard.api.repository.acesso.RefreshTokenSessionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.springframework.lang.NonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenSessionRepository refreshTokenRepository;

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(refreshTokenRepository, 24);
    }

    @Test
    void rotacionarPreservaExpiracaoAbsolutaDaSessaoOriginal() {
        String tokenAntigo = "refresh-antigo";
        Instant expiraEmOriginal = Instant.now().plusSeconds(6 * 60 * 60);
        UsuarioEntity usuario = usuarioAtivo();
        RefreshTokenSession sessaoAntiga = sessao(
                usuario,
                hash(tokenAntigo),
                expiraEmOriginal,
                null,
                null
        );

        when(refreshTokenRepository.findByTokenHash(hash(tokenAntigo))).thenReturn(Optional.of(sessaoAntiga));

        RefreshTokenService.RefreshTokenRotacionado resultado = service.rotacionar(tokenAntigo, "127.0.0.1", "JUnit");

        assertThat(resultado.expiraEm()).isEqualTo(expiraEmOriginal);
        assertThat(sessaoAntiga.getRevogadoEm()).isNotNull();
        assertThat(sessaoAntiga.getSubstituidoPorHash()).isEqualTo(hash(resultado.tokenPlano()));
    }

    @Test
    void rotacionarAceitaReplayRecenteDeTokenSubstituidoSemRevogarSessoes() {
        String tokenAntigo = "refresh-antigo";
        Instant revogadoEm = Instant.now().minusSeconds(5);
        UsuarioEntity usuario = usuarioAtivo();
        RefreshTokenSession sessaoAntiga = sessao(
                usuario,
                hash(tokenAntigo),
                Instant.now().plusSeconds(3600),
                revogadoEm,
                hash("refresh-substituto-anterior")
        );
        sessaoAntiga.setCriadoIp("127.0.0.1");
        sessaoAntiga.setUserAgent("JUnit");

        when(refreshTokenRepository.findByTokenHash(hash(tokenAntigo))).thenReturn(Optional.of(sessaoAntiga));

        RefreshTokenService.RefreshTokenRotacionado resultado = service.rotacionar(tokenAntigo, "127.0.0.1", "JUnit");

        assertThat(resultado.usuario()).isSameAs(usuario);
        assertThat(resultado.tokenPlano()).isNotBlank().isNotEqualTo(tokenAntigo);
        assertThat(resultado.expiraEm()).isEqualTo(sessaoAntiga.getExpiraEm());
        assertThat(sessaoAntiga.getRevogadoEm()).isEqualTo(revogadoEm);
        assertThat(sessaoAntiga.getSubstituidoPorHash()).isEqualTo(hash(resultado.tokenPlano()));
        verify(refreshTokenRepository, never()).findAllByUsuarioIdAndRevogadoEmIsNull(usuario.getId());
    }

    @Test
    void rotacionarRejeitaReplayRecenteQuandoContextoClienteDiverge() {
        String tokenAntigo = "refresh-antigo";
        UsuarioEntity usuario = usuarioAtivo();
        RefreshTokenSession sessaoAntiga = sessao(
                usuario,
                hash(tokenAntigo),
                Instant.now().plusSeconds(3600),
                Instant.now().minusSeconds(5),
                hash("refresh-substituto-anterior")
        );
        sessaoAntiga.setCriadoIp("10.0.0.1");
        sessaoAntiga.setUserAgent("JUnit");

        when(refreshTokenRepository.findByTokenHash(hash(tokenAntigo))).thenReturn(Optional.of(sessaoAntiga));
        when(refreshTokenRepository.findAllByUsuarioIdAndRevogadoEmIsNull(usuario.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> service.rotacionar(tokenAntigo, "127.0.0.1", "JUnit"))
                .isInstanceOf(CredencialInvalidaException.class)
                .hasMessage("Sessão expirada.");

        verify(refreshTokenRepository).findAllByUsuarioIdAndRevogadoEmIsNull(usuario.getId());
    }

    @Test
    void rotacionarRevogaSessoesQuandoReplayDeTokenSubstituidoPassaDaTolerancia() {
        String tokenAntigo = "refresh-antigo";
        UsuarioEntity usuario = usuarioAtivo();
        RefreshTokenSession sessaoAntiga = sessao(
                usuario,
                hash(tokenAntigo),
                Instant.now().plusSeconds(3600),
                Instant.now().minusSeconds(35),
                hash("refresh-substituto-anterior")
        );

        when(refreshTokenRepository.findByTokenHash(hash(tokenAntigo))).thenReturn(Optional.of(sessaoAntiga));
        when(refreshTokenRepository.findAllByUsuarioIdAndRevogadoEmIsNull(usuario.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> service.rotacionar(tokenAntigo, "127.0.0.1", "JUnit"))
                .isInstanceOf(CredencialInvalidaException.class)
                .hasMessage("Sessão expirada.");

        verify(refreshTokenRepository).findAllByUsuarioIdAndRevogadoEmIsNull(usuario.getId());
        verify(refreshTokenRepository, never()).save(anyNonNull(RefreshTokenSession.class));
    }

    private static UsuarioEntity usuarioAtivo() {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(10L);
        usuario.setEmail("maria@empresa.com");
        usuario.setLogin("maria@empresa.com");
        usuario.setAtivo(true);
        return usuario;
    }

    private static RefreshTokenSession sessao(
            UsuarioEntity usuario,
            String tokenHash,
            Instant expiraEm,
            Instant revogadoEm,
            String substituidoPorHash
    ) {
        RefreshTokenSession sessao = new RefreshTokenSession();
        sessao.setUsuario(usuario);
        sessao.setTokenHash(tokenHash);
        sessao.setExpiraEm(expiraEm);
        sessao.setRevogadoEm(revogadoEm);
        sessao.setSubstituidoPorHash(substituidoPorHash);
        return sessao;
    }

    private static String hash(String tokenPlano) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(tokenPlano.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @NonNull
    private static <T> T anyNonNull(Class<T> type) {
        return org.mockito.ArgumentMatchers.any(type);
    }
}
