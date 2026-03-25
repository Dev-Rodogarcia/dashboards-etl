package com.dashboard.api.service.acesso;

import com.dashboard.api.model.acesso.RefreshTokenSession;
import com.dashboard.api.model.acesso.UsuarioEntity;
import com.dashboard.api.repository.acesso.RefreshTokenSessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenSessionRepository refreshTokenRepository;
    private final long expiracaoDias;

    public RefreshTokenService(
            RefreshTokenSessionRepository refreshTokenRepository,
            @Value("${auth.refresh-token-expiracao-dias:30}") long expiracaoDias
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.expiracaoDias = expiracaoDias;
    }

    @Transactional
    public RefreshTokenEmitido emitir(UsuarioEntity usuario, String ipAddress, String userAgent) {
        String tokenPlano = UUID.randomUUID() + "." + UUID.randomUUID();

        RefreshTokenSession sessao = new RefreshTokenSession();
        sessao.setUsuario(usuario);
        sessao.setTokenHash(hash(tokenPlano));
        sessao.setExpiraEm(Instant.now().plusSeconds(expiracaoDias * 24 * 60 * 60));
        sessao.setCriadoIp(truncar(ipAddress, 45));
        sessao.setUserAgent(truncar(userAgent, 500));
        refreshTokenRepository.save(sessao);

        return new RefreshTokenEmitido(tokenPlano, sessao.getExpiraEm());
    }

    @Transactional
    public RefreshTokenRotacionado rotacionar(String tokenPlano, String ipAddress, String userAgent) {
        RefreshTokenSession atual = buscarSessaoValida(tokenPlano);
        UsuarioEntity usuario = atual.getUsuario();

        if (!usuario.isAtivo()) {
            revogarTodosDoUsuario(Objects.requireNonNull(usuario.getId(), "usuario.id é obrigatório."));
            throw new CredencialInvalidaException("Sessão expirada.");
        }

        RefreshTokenEmitido novo = emitir(usuario, ipAddress, userAgent);
        atual.setRevogadoEm(Instant.now());
        atual.setSubstituidoPorHash(hash(novo.tokenPlano()));
        refreshTokenRepository.save(atual);

        return new RefreshTokenRotacionado(usuario, novo.tokenPlano(), novo.expiraEm());
    }

    @Transactional
    public void revogar(String tokenPlano) {
        if (tokenPlano == null || tokenPlano.isBlank()) {
            return;
        }

        refreshTokenRepository.findByTokenHash(hash(tokenPlano)).ifPresent(sessao -> {
            if (sessao.getRevogadoEm() == null) {
                sessao.setRevogadoEm(Instant.now());
                refreshTokenRepository.save(sessao);
            }
        });
    }

    @Transactional
    public void revogarTodosDoUsuario(Long usuarioId) {
        List<RefreshTokenSession> sessoes = refreshTokenRepository.findAllByUsuarioIdAndRevogadoEmIsNull(usuarioId);
        Instant agora = Instant.now();

        for (RefreshTokenSession sessao : sessoes) {
            sessao.setRevogadoEm(agora);
        }

        if (!sessoes.isEmpty()) {
            refreshTokenRepository.saveAll(sessoes);
        }
    }

    @Transactional(readOnly = true)
    public Optional<UsuarioEntity> usuarioDaSessao(String tokenPlano) {
        try {
            return Optional.of(buscarSessaoValida(tokenPlano).getUsuario());
        } catch (CredencialInvalidaException ex) {
            return Optional.empty();
        }
    }

    private RefreshTokenSession buscarSessaoValida(String tokenPlano) {
        if (tokenPlano == null || tokenPlano.isBlank()) {
            throw new CredencialInvalidaException("Sessão expirada.");
        }

        RefreshTokenSession sessao = refreshTokenRepository.findByTokenHash(hash(tokenPlano))
                .orElseThrow(() -> new CredencialInvalidaException("Sessão expirada."));

        Instant agora = Instant.now();
        if (sessao.getRevogadoEm() != null) {
            if (sessao.getSubstituidoPorHash() != null) {
                revogarTodosDoUsuario(Objects.requireNonNull(sessao.getUsuario().getId(), "usuario.id é obrigatório."));
            }
            throw new CredencialInvalidaException("Sessão expirada.");
        }

        if (sessao.getExpiraEm().isBefore(agora)) {
            sessao.setRevogadoEm(agora);
            refreshTokenRepository.save(sessao);
            throw new CredencialInvalidaException("Sessão expirada.");
        }

        return sessao;
    }

    private String hash(String tokenPlano) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(tokenPlano.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível no ambiente.", ex);
        }
    }

    private String truncar(String valor, int limite) {
        if (valor == null) {
            return null;
        }
        return valor.length() > limite ? valor.substring(0, limite) : valor;
    }

    public record RefreshTokenEmitido(String tokenPlano, Instant expiraEm) {}

    public record RefreshTokenRotacionado(UsuarioEntity usuario, String tokenPlano, Instant expiraEm) {}
}
