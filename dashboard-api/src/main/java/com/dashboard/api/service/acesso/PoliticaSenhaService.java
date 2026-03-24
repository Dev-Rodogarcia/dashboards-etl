package com.dashboard.api.service.acesso;

import org.springframework.stereotype.Service;

@Service
public class PoliticaSenhaService {

    private static final int TAMANHO_MINIMO = 12;

    public void validar(String senha) {
        if (senha == null || senha.isBlank()) {
            throw new IllegalArgumentException("A senha é obrigatória.");
        }
        if (senha.length() < TAMANHO_MINIMO) {
            throw new IllegalArgumentException("A senha deve ter pelo menos " + TAMANHO_MINIMO + " caracteres.");
        }
        if (!senha.chars().anyMatch(Character::isUpperCase)) {
            throw new IllegalArgumentException("A senha deve conter ao menos uma letra maiúscula.");
        }
        if (!senha.chars().anyMatch(Character::isLowerCase)) {
            throw new IllegalArgumentException("A senha deve conter ao menos uma letra minúscula.");
        }
        if (!senha.chars().anyMatch(Character::isDigit)) {
            throw new IllegalArgumentException("A senha deve conter ao menos um número.");
        }
        if (senha.chars().noneMatch(this::isSimbolo)) {
            throw new IllegalArgumentException("A senha deve conter ao menos um caractere especial.");
        }
    }

    private boolean isSimbolo(int codePoint) {
        return !Character.isLetterOrDigit(codePoint) && !Character.isWhitespace(codePoint);
    }
}
