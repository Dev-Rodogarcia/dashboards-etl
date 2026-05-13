package com.dashboard.api.controller;

import com.dashboard.api.dto.FiltroConsultaDTO;
import org.springframework.util.MultiValueMap;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class FiltroRequestMapper {

    private static final String PREFIXO = "f.";

    private FiltroRequestMapper() {
    }

    static FiltroConsultaDTO from(LocalDate dataInicio, LocalDate dataFim, MultiValueMap<String, String> params) {
        Map<String, List<String>> filtros = params.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(PREFIXO))
                .collect(Collectors.toMap(
                        entry -> entry.getKey().substring(PREFIXO.length()),
                        entry -> normalizarValoresFiltro(entry.getKey().substring(PREFIXO.length()), entry.getValue())
                ));
        filtros.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        return new FiltroConsultaDTO(dataInicio, dataFim, filtros);
    }

    private static List<String> normalizarValoresFiltro(String chave, List<String> valores) {
        if (!"filiais".equals(chave)) {
            return valores;
        }
        return valores.stream()
                .filter(valor -> valor != null && !valor.isBlank())
                .filter(valor -> !"GLOBAL".equalsIgnoreCase(valor.trim()))
                .toList();
    }
}
