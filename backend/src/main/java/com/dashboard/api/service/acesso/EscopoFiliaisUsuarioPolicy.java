package com.dashboard.api.service.acesso;

import com.dashboard.api.model.acesso.UsuarioEntity;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

final class EscopoFiliaisUsuarioPolicy {

    static final String HERDAR_SETOR = "HERDAR_SETOR";
    static final String TODAS = "TODAS";
    static final String SELECIONADAS = "SELECIONADAS";

    private static final Set<String> TIPOS_VALIDOS = Set.of(HERDAR_SETOR, TODAS, SELECIONADAS);

    private EscopoFiliaisUsuarioPolicy() {
    }

    static String normalizarTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return HERDAR_SETOR;
        }

        String normalizado = tipo.trim().toUpperCase(Locale.ROOT);
        if (!TIPOS_VALIDOS.contains(normalizado)) {
            throw new IllegalArgumentException("Escopo de filiais inválido: " + tipo);
        }
        return normalizado;
    }

    static LinkedHashSet<String> normalizarFiliaisSelecionadas(String tipo, Collection<String> filiais) {
        if (!SELECIONADAS.equals(tipo)) {
            return new LinkedHashSet<>();
        }

        LinkedHashSet<String> normalizadas = normalizarFiliais(filiais);
        if (normalizadas.isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos uma filial permitida para o usuário.");
        }
        return normalizadas;
    }

    static EscopoFilialService.EscopoFilial resolverSemPapelElevado(UsuarioEntity usuario) {
        String tipo = normalizarTipo(usuario.getEscopoFiliaisTipo());
        if (TODAS.equals(tipo)) {
            return EscopoFilialService.EscopoFilial.comAcessoTotal();
        }
        if (SELECIONADAS.equals(tipo)) {
            return new EscopoFilialService.EscopoFilial(false, listarFiliaisUsuario(usuario));
        }
        return new EscopoFilialService.EscopoFilial(false, listarFiliaisSetor(usuario));
    }

    static List<String> listarFiliaisUsuario(UsuarioEntity usuario) {
        return usuario.getFiliaisPermitidasUsuario().stream()
                .filter(valor -> valor != null && !valor.isBlank())
                .map(String::trim)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    static List<String> listarFiliaisSetor(UsuarioEntity usuario) {
        if (usuario.getSetor() == null) {
            return List.of();
        }

        return usuario.getSetor().getFiliaisPermitidas().stream()
                .filter(valor -> valor != null && !valor.isBlank())
                .map(String::trim)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private static LinkedHashSet<String> normalizarFiliais(Collection<String> filiais) {
        if (filiais == null) {
            return new LinkedHashSet<>();
        }

        return filiais.stream()
                .filter(valor -> valor != null && !valor.isBlank())
                .map(String::trim)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
