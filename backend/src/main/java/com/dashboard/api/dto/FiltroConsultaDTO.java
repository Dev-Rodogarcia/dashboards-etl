package com.dashboard.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record FiltroConsultaDTO(
        LocalDate dataInicio,
        LocalDate dataFim,
        Map<String, List<String>> filtros
) {

    public FiltroConsultaDTO {
        filtros = filtros == null ? Map.of() : filtros.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank())
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue() == null ? List.of() : entry.getValue().stream()
                                .filter(valor -> valor != null && !valor.isBlank())
                                .map(String::trim)
                                .toList()
                ));
    }

    public List<String> valores(String chave) {
        return filtros.getOrDefault(chave, List.of());
    }

    public boolean temFiltro(String chave) {
        return !valores(chave).isEmpty();
    }

    public boolean corresponde(String chave, String valor) {
        if (!temFiltro(chave)) {
            return true;
        }

        if (valor == null || valor.isBlank()) {
            return false;
        }

        String valorNormalizado = normalizar(valor);
        return valores(chave).stream()
                .map(FiltroConsultaDTO::normalizar)
                .anyMatch(valorNormalizado::equals);
    }

    public boolean correspondeAlgum(String chave, String... valores) {
        if (!temFiltro(chave)) {
            return true;
        }

        for (String valor : valores) {
            if (corresponde(chave, valor)) {
                return true;
            }
        }

        return false;
    }

    private static String normalizar(String valor) {
        return valor.trim().toLowerCase(Locale.ROOT);
    }
}
