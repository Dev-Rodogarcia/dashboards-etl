package com.dashboard.api.service.acesso;

import com.dashboard.api.model.acesso.StatusSenhaUsuario;
import com.dashboard.api.model.acesso.UsuarioEntity;
import java.util.Locale;
import java.util.Objects;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordHashService {

    public static final String ALGORITMO_ARGON2ID = "argon2id";
    public static final String ALGORITMO_BCRYPT = "bcrypt";
    public static final String ALGORITMO_MD5 = "md5";
    public static final String ALGORITMO_SHA1 = "sha1";
    public static final String ALGORITMO_SHA256 = "sha256";

    private final Argon2PasswordEncoder argon2PasswordEncoder;
    private final BCryptPasswordEncoder bcryptPasswordEncoder;

    public PasswordHashService(
            Argon2PasswordEncoder argon2PasswordEncoder,
            BCryptPasswordEncoder bcryptPasswordEncoder
    ) {
        this.argon2PasswordEncoder = argon2PasswordEncoder;
        this.bcryptPasswordEncoder = bcryptPasswordEncoder;
    }

    public PasswordHash gerarHashSeguro(String senhaPlana) {
        String senha = Objects.requireNonNull(senhaPlana, "senhaPlana é obrigatória.");
        return new PasswordHash(argon2PasswordEncoder.encode(senha), ALGORITMO_ARGON2ID);
    }

    public PasswordVerification verificarSenha(UsuarioEntity usuario, String senhaPlana) {
        Objects.requireNonNull(usuario, "usuario é obrigatório.");
        String senha = Objects.requireNonNull(senhaPlana, "senhaPlana é obrigatória.");

        String algoritmo = algoritmoNormalizado(usuario.getAlgoritmoHash());
        String senhaHash = usuario.getSenhaHash();

        if (senhaHash == null || senhaHash.isBlank()) {
            return PasswordVerification.resetObrigatorio(algoritmoEfetivo(algoritmo));
        }

        return switch (algoritmo) {
            case ALGORITMO_ARGON2ID -> argon2PasswordEncoder.matches(senha, senhaHash)
                    ? PasswordVerification.validaSemUpgrade(ALGORITMO_ARGON2ID)
                    : PasswordVerification.invalida(ALGORITMO_ARGON2ID);
            case ALGORITMO_BCRYPT -> bcryptPasswordEncoder.matches(senha, senhaHash)
                    ? PasswordVerification.validaComUpgrade(ALGORITMO_BCRYPT)
                    : PasswordVerification.invalida(ALGORITMO_BCRYPT);
            case ALGORITMO_MD5, ALGORITMO_SHA1, ALGORITMO_SHA256, "desconhecido" ->
                    PasswordVerification.resetObrigatorio(algoritmoEfetivo(algoritmo));
            default -> PasswordVerification.resetObrigatorio(algoritmoEfetivo(algoritmo));
        };
    }

    public StatusSenhaUsuario statusAdministrativo(UsuarioEntity usuario) {
        return statusAdministrativo(usuario.getAlgoritmoHash());
    }

    public StatusSenhaUsuario statusAdministrativo(String algoritmoHash) {
        String algoritmo = algoritmoNormalizado(algoritmoHash);
        return switch (algoritmo) {
            case ALGORITMO_ARGON2ID -> StatusSenhaUsuario.SEGURA;
            case ALGORITMO_BCRYPT -> StatusSenhaUsuario.MIGRAR_NO_LOGIN;
            default -> StatusSenhaUsuario.RESET_OBRIGATORIO;
        };
    }

    public String algoritmoExibicao(UsuarioEntity usuario) {
        return algoritmoExibicao(usuario.getAlgoritmoHash());
    }

    public String algoritmoExibicao(String algoritmoHash) {
        return algoritmoEfetivo(algoritmoNormalizado(algoritmoHash));
    }

    public String inferirAlgoritmoMigracaoLegada(String senhaHash) {
        if (senhaHash == null || senhaHash.isBlank()) {
            return "desconhecido";
        }
        if (senhaHash.startsWith("$2")) {
            return ALGORITMO_BCRYPT;
        }
        return "desconhecido";
    }

    private String algoritmoNormalizado(String algoritmoHash) {
        if (algoritmoHash == null || algoritmoHash.isBlank()) {
            return "desconhecido";
        }
        return algoritmoHash.trim().toLowerCase(Locale.ROOT);
    }

    private String algoritmoEfetivo(String algoritmoHash) {
        return algoritmoHash == null || algoritmoHash.isBlank() ? "desconhecido" : algoritmoHash;
    }

    public record PasswordHash(String valor, String algoritmo) {
    }

    public record PasswordVerification(
            boolean valida,
            boolean precisaUpgrade,
            boolean resetObrigatorio,
            String algoritmoAtual
    ) {
        public static PasswordVerification invalida(String algoritmoAtual) {
            return new PasswordVerification(false, false, false, algoritmoAtual);
        }

        public static PasswordVerification validaSemUpgrade(String algoritmoAtual) {
            return new PasswordVerification(true, false, false, algoritmoAtual);
        }

        public static PasswordVerification validaComUpgrade(String algoritmoAtual) {
            return new PasswordVerification(true, true, false, algoritmoAtual);
        }

        public static PasswordVerification resetObrigatorio(String algoritmoAtual) {
            return new PasswordVerification(false, false, true, algoritmoAtual);
        }
    }
}
