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
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Duration REPLAY_ROTACAO_TOLERADO = Duration.ofSeconds(30);

    private final RefreshTokenSessionRepository refreshTokenRepository;
    private final Duration sessaoDuracao;

    public RefreshTokenService(
            RefreshTokenSessionRepository refreshTokenRepository,
            @Value("${auth.session-expiracao-horas:24}") long sessaoExpiracaoHoras
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        if (sessaoExpiracaoHoras <= 0) {
            throw new IllegalArgumentException("auth.session-expiracao-horas deve ser maior que zero.");
        }
        this.sessaoDuracao = Duration.ofHours(sessaoExpiracaoHoras);
    }

    @Transactional
    public RefreshTokenEmitido emitir(UsuarioEntity usuario, String ipAddress, String userAgent) {
        return emitir(usuario, ipAddress, userAgent, Instant.now().plus(sessaoDuracao));
    }

    private RefreshTokenEmitido emitir(UsuarioEntity usuario, String ipAddress, String userAgent, Instant expiraEm) {
        String tokenPlano = UUID.randomUUID() + "." + UUID.randomUUID();

        RefreshTokenSession sessao = new RefreshTokenSession();
        sessao.setUsuario(usuario);
        sessao.setTokenHash(hash(tokenPlano));
        sessao.setExpiraEm(Objects.requireNonNull(expiraEm, "expiraEm é obrigatório."));
        sessao.setCriadoIp(truncar(ipAddress, 45));
        sessao.setUserAgent(truncar(userAgent, 500));
        refreshTokenRepository.save(sessao);

        return new RefreshTokenEmitido(tokenPlano, sessao.getExpiraEm());
    }

    @Transactional
    public RefreshTokenRotacionado rotacionar(String tokenPlano, String ipAddress, String userAgent) {
        RefreshTokenSession atual = buscarSessaoParaRotacao(tokenPlano, ipAddress, userAgent);
        UsuarioEntity usuario = atual.getUsuario();

        if (!usuario.isAtivo()) {
            revogarTodosDoUsuario(Objects.requireNonNull(usuario.getId(), "usuario.id é obrigatório."));
            throw new CredencialInvalidaException("Sessão expirada.");
        }

        RefreshTokenEmitido novo = emitir(usuario, ipAddress, userAgent, atual.getExpiraEm());
        if (atual.getRevogadoEm() == null) {
            atual.setRevogadoEm(Instant.now());
        }
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
            throw new CredencialInvalidaException("Sessão expirada.");
        }

        if (sessao.getExpiraEm().isBefore(agora)) {
            sessao.setRevogadoEm(agora);
            refreshTokenRepository.save(sessao);
            throw new CredencialInvalidaException("Sessão expirada.");
        }

        return sessao;
    }

    private RefreshTokenSession buscarSessaoParaRotacao(String tokenPlano, String ipAddress, String userAgent) {
        if (tokenPlano == null || tokenPlano.isBlank()) {
            throw new CredencialInvalidaException("Sessão expirada.");
        }

        RefreshTokenSession sessao = refreshTokenRepository.findByTokenHash(hash(tokenPlano))
                .orElseThrow(() -> new CredencialInvalidaException("Sessão expirada."));

        Instant agora = Instant.now();
        if (sessao.getRevogadoEm() != null) {
            if (replayDeRotacaoRecente(sessao, agora, ipAddress, userAgent)) {
                return sessao;
            }

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

    private boolean replayDeRotacaoRecente(RefreshTokenSession sessao, Instant agora, String ipAddress, String userAgent) {
        if (sessao.getSubstituidoPorHash() == null || sessao.getRevogadoEm() == null) {
            return false;
        }

        return !sessao.getRevogadoEm().plus(REPLAY_ROTACAO_TOLERADO).isBefore(agora)
                && mesmoContextoCliente(sessao.getCriadoIp(), ipAddress)
                && mesmoContextoCliente(sessao.getUserAgent(), truncar(userAgent, 500));
    }

    private boolean mesmoContextoCliente(String esperado, String atual) {
        return esperado == null || atual == null || esperado.equals(atual);
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
