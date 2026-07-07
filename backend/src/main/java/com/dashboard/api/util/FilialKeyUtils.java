package com.dashboard.api.util;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class FilialKeyUtils {

    private static final Pattern BRANCH_CODE_PATTERN = Pattern.compile("[A-Z0-9]{3}");
    private static final String GLOBAL_BRANCH_ID = "GLOBAL";
    private static final String SEM_MAP = "SEM_MAP";

    private FilialKeyUtils() {
    }

    public static List<String> normalizarCodigosParaFiltro(Collection<String> valores) {
        return codigos(valores).stream()
                .map(valor -> valor.toLowerCase(Locale.ROOT))
                .toList();
    }

    public static List<String> normalizarCodigosParaOrcamento(Collection<String> valores) {
        return codigos(valores);
    }

    public static boolean possuiTexto(Collection<String> valores) {
        return valores != null && valores.stream().anyMatch(valor -> valor != null && !valor.isBlank());
    }

    public static String extrairCodigoOperacional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        String normalizado = valor.trim().toUpperCase(Locale.ROOT);
        if (normalizado.isBlank() || GLOBAL_BRANCH_ID.equals(normalizado) || isParceiroLogistico(normalizado)) {
            return null;
        }

        String[] segmentos = normalizado.split("\\s*[-–—|]\\s*");
        String candidato = segmentos.length > 0 ? segmentos[0].trim() : normalizado;
        if (SEM_MAP.equals(candidato) && segmentos.length > 1) {
            candidato = segmentos[1].trim();
        }

        if (candidato.length() == 3 && BRANCH_CODE_PATTERN.matcher(candidato).matches()) {
            return candidato;
        }
        return null;
    }

    private static List<String> codigos(Collection<String> valores) {
        if (valores == null) {
            return List.of();
        }
        return valores.stream()
                .map(FilialKeyUtils::extrairCodigoOperacional)
                .filter(codigo -> codigo != null && !codigo.isBlank())
                .distinct()
                .toList();
    }

    private static boolean isParceiroLogistico(String valorNormalizado) {
        return valorNormalizado.toLowerCase(Locale.ROOT).contains("| parceiro");
    }
}
