package com.dashboard.api.controller;

import com.dashboard.api.dto.LoginRequestDTO;
import com.dashboard.api.dto.LoginResponseDTO;
import com.dashboard.api.dto.SessaoUsuarioDTO;
import com.dashboard.api.dto.SetorSessaoDTO;
import com.dashboard.api.exception.RespostaErroPadrao;
import com.dashboard.api.model.acesso.SetorEntity;
import com.dashboard.api.model.acesso.UsuarioEntity;
import com.dashboard.api.repository.acesso.AuditLogRepository;
import com.dashboard.api.repository.acesso.PermissaoRepository;
import com.dashboard.api.repository.acesso.RefreshTokenSessionRepository;
import com.dashboard.api.repository.acesso.SetorPermissaoTemplateRepository;
import com.dashboard.api.repository.acesso.UsuarioPapelVinculoRepository;
import com.dashboard.api.repository.acesso.UsuarioPermissaoOverrideRepository;
import com.dashboard.api.repository.acesso.UsuarioRepository;
import com.dashboard.api.security.GerenciadorTokenJwt;
import com.dashboard.api.security.IpClienteResolver;
import com.dashboard.api.security.RateLimitService;
import com.dashboard.api.service.acesso.AutenticacaoService;
import com.dashboard.api.service.acesso.AuditService;
import com.dashboard.api.service.acesso.CredencialInvalidaException;
import com.dashboard.api.service.acesso.PasswordHashService;
import com.dashboard.api.service.acesso.PermissaoResolverService;
import com.dashboard.api.service.acesso.PoliticaSenhaService;
import com.dashboard.api.service.acesso.RefreshTokenService;
import com.dashboard.api.service.acesso.UsuarioSupremo;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class AutenticacaoControllerTest {

    @Test
    void loginDeveRetornarMensagemExataDaFalhaDeCredencial() {
        AutenticacaoController controller = new AutenticacaoController(
                new StubAutenticacaoService(),
                new RateLimitService(10, 900, 120, 60, 12, 60),
                new RefreshTokenService(mock(RefreshTokenSessionRepository.class), 30),
                new IpClienteResolver(false),
                "dashboard_refresh_token",
                false,
                "Lax"
        );

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("127.0.0.1");

        ResponseEntity<?> response = controller.login(new LoginRequestDTO("admin@empresa.com", "senha"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        Object responseBody = Objects.requireNonNull(response.getBody());
        assertThat(responseBody).isInstanceOf(RespostaErroPadrao.class);
        RespostaErroPadrao body = (RespostaErroPadrao) responseBody;
        assertThat(body.mensagem()).isEqualTo("Conta temporariamente bloqueada. Tente novamente mais tarde.");
    }

    @Test
    void loginDefineCookiePersistenteComMaxAgeDaSessao() {
        AutenticacaoController controller = new AutenticacaoController(
                new StubAutenticacaoServiceSucesso(),
                new RateLimitService(10, 900, 120, 60, 12, 60),
                new RefreshTokenService(mock(RefreshTokenSessionRepository.class), 24),
                new IpClienteResolver(false),
                "dashboard_refresh_token",
                true,
                "Lax"
        );

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "JUnit");

        ResponseEntity<?> response = controller.login(new LoginRequestDTO("admin@empresa.com", "senha"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String setCookie = response.getHeaders().getFirst("Set-Cookie");
        assertThat(setCookie).contains("dashboard_refresh_token=");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("Secure");
        assertThat(setCookie).contains("Max-Age=");
        assertThat(setCookie).contains("Path=/api/auth");
        assertThat(setCookie).contains("SameSite=Lax");
        Object body = Objects.requireNonNull(response.getBody());
        assertThat(body).isInstanceOf(LoginResponseDTO.class);
        assertThat(((LoginResponseDTO) body).sessaoExpiraEm()).isNotNull();
    }

    @Test
    void loginPermiteCookieCrossSiteQuandoSameSiteNoneEstaSeguro() {
        AutenticacaoController controller = new AutenticacaoController(
                new StubAutenticacaoServiceSucesso(),
                new RateLimitService(10, 900, 120, 60, 12, 60),
                new RefreshTokenService(mock(RefreshTokenSessionRepository.class), 24),
                new IpClienteResolver(false),
                "dashboard_refresh_token",
                true,
                "None"
        );

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "JUnit");

        ResponseEntity<?> response = controller.login(new LoginRequestDTO("admin@empresa.com", "senha"), request);

        String setCookie = response.getHeaders().getFirst("Set-Cookie");
        assertThat(setCookie).contains("Secure");
        assertThat(setCookie).contains("SameSite=None");
    }

    @Test
    void configuracaoCrossSiteSemSecureFalhaNoStartup() {
        assertThatThrownBy(() -> new AutenticacaoController(
                new StubAutenticacaoServiceSucesso(),
                new RateLimitService(10, 900, 120, 60, 12, 60),
                new RefreshTokenService(mock(RefreshTokenSessionRepository.class), 24),
                new IpClienteResolver(false),
                "dashboard_refresh_token",
                false,
                "None"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("auth.refresh-cookie-secure deve ser true quando auth.refresh-cookie-same-site=None.");
    }

    private static final class StubAutenticacaoService extends AutenticacaoService {

        private StubAutenticacaoService() {
            super(
                    mock(UsuarioRepository.class),
                    new PasswordHashService(
                            Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8(),
                            new BCryptPasswordEncoder()
                    ),
                    new GerenciadorTokenJwt("12345678901234567890123456789012", 15),
                    new StubPermissaoResolverService(),
                    new AuditService(mock(AuditLogRepository.class), new IpClienteResolver(false)),
                    new PoliticaSenhaService(),
                    new RefreshTokenService(mock(RefreshTokenSessionRepository.class), 24)
            );
        }

        @Override
        public com.dashboard.api.dto.LoginResponseDTO autenticar(String email, String senha) {
            throw new CredencialInvalidaException("Conta temporariamente bloqueada. Tente novamente mais tarde.");
        }
    }

    private static final class StubAutenticacaoServiceSucesso extends AutenticacaoService {

        private final UsuarioEntity usuario = criarUsuario();

        private StubAutenticacaoServiceSucesso() {
            super(
                    mock(UsuarioRepository.class),
                    new PasswordHashService(
                            Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8(),
                            new BCryptPasswordEncoder()
                    ),
                    new GerenciadorTokenJwt("12345678901234567890123456789012", 15),
                    new StubPermissaoResolverService(),
                    new AuditService(mock(AuditLogRepository.class), new IpClienteResolver(false)),
                    new PoliticaSenhaService(),
                    new RefreshTokenService(mock(RefreshTokenSessionRepository.class), 24)
            );
        }

        @Override
        public LoginResponseDTO autenticar(String email, String senha) {
            return new LoginResponseDTO(
                    new SessaoUsuarioDTO(
                            "1",
                            "Admin",
                            email,
                            "admin_plataforma",
                            new SetorSessaoDTO("1", "Administração"),
                            Map.of("coletas", true),
                            java.util.List.of(),
                            false
                    ),
                    "jwt",
                    false,
                    null
            );
        }

        @Override
        public UsuarioEntity carregarUsuarioAtivoPorEmail(String email) {
            return usuario;
        }

        private static UsuarioEntity criarUsuario() {
            SetorEntity setor = new SetorEntity();
            setor.setId(1L);
            setor.setNome("Administração");

            UsuarioEntity usuario = new UsuarioEntity();
            usuario.setId(1L);
            usuario.setNome("Admin");
            usuario.setEmail("admin@empresa.com");
            usuario.setLogin("admin@empresa.com");
            usuario.setSetor(setor);
            usuario.setAtivo(true);
            return usuario;
        }
    }

    private static final class StubPermissaoResolverService extends PermissaoResolverService {

        private StubPermissaoResolverService() {
            super(
                    mock(PermissaoRepository.class),
                    mock(SetorPermissaoTemplateRepository.class),
                    mock(UsuarioPapelVinculoRepository.class),
                    mock(UsuarioPermissaoOverrideRepository.class),
                    new UsuarioSupremo("supremo@empresa.com", "Senha@123456", "Supremo", "desenvolvedor", 1000, false)
            );
        }

        @Override
        public Map<String, Boolean> permissoesEfetivas(com.dashboard.api.model.acesso.UsuarioEntity usuario) {
            return Map.of();
        }
    }
}
