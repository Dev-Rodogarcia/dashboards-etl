package com.dashboard.api.security.acesso;

import com.dashboard.api.model.acesso.UsuarioEntity;
import jakarta.annotation.PostConstruct;
import java.util.Locale;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UsuarioSupremo {

    private final String email;
    private final String senhaInicial;
    private final String nome;
    private final String papel;
    private final int nivel;
    private final boolean rotacionarSenha;

    public UsuarioSupremo(
            @Value("${acesso.usuario-supremo.email}") String email,
            @Value("${acesso.usuario-supremo.senha-inicial}") String senhaInicial,
            @Value("${acesso.usuario-supremo.nome}") String nome,
            @Value("${acesso.usuario-supremo.papel}") String papel,
            @Value("${acesso.usuario-supremo.nivel}") int nivel,
            @Value("${acesso.usuario-supremo.rotacionar-senha:false}") boolean rotacionarSenha
    ) {
        this.email = normalizarEmail(email);
        this.senhaInicial = senhaInicial;
        this.nome = nome == null ? "" : nome.trim();
        this.papel = papel == null ? "" : papel.trim().toLowerCase(Locale.ROOT);
        this.nivel = nivel;
        this.rotacionarSenha = rotacionarSenha;
    }

    @PostConstruct
    public void validarConfiguracao() {
        exigirPreenchido(email, "ACESSO_USUARIO_SUPREMO_EMAIL");
        exigirPreenchido(senhaInicial, "ACESSO_USUARIO_SUPREMO_SENHA_INICIAL");
        exigirPreenchido(nome, "ACESSO_USUARIO_SUPREMO_NOME");
        exigirPreenchido(papel, "ACESSO_USUARIO_SUPREMO_PAPEL");
        if (nivel <= 0) {
            throw new IllegalStateException("ACESSO_USUARIO_SUPREMO_NIVEL deve ser maior que zero.");
        }
    }

    public boolean ehEmailSupremo(String email) {
        return this.email.equals(normalizarEmail(email));
    }

    public boolean ehUsuarioSupremo(UsuarioEntity usuario) {
        return usuario != null && ehEmailSupremo(usuario.getEmail());
    }

    public static String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public String email() {
        return email;
    }

    public String senhaInicial() {
        return senhaInicial;
    }

    public String nome() {
        return nome;
    }

    public String papel() {
        return papel;
    }

    public int nivel() {
        return nivel;
    }

    public boolean rotacionarSenha() {
        return rotacionarSenha;
    }

    private void exigirPreenchido(String valor, String variavel) {
        if (Objects.requireNonNullElse(valor, "").isBlank()) {
            throw new IllegalStateException(variavel + " é obrigatória.");
        }
    }
}
