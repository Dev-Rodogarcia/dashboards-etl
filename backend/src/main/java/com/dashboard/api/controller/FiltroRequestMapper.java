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
                        Map.Entry::getValue
                ));

        return new FiltroConsultaDTO(dataInicio, dataFim, filtros);
    }
}
