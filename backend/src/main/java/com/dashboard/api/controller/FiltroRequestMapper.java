package com.dashboard.api.controller;

import com.dashboard.api.dto.FiltroConsultaDTO;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.util.MultiValueMap;

final class FiltroRequestMapper {

    private static final String PREFIXO = "f.";
    private static final String FILTRO_FILIAIS = "filiais";
    private static final String FILTRO_PARCEIROS_LOGISTICOS = "parceirosLogisticos";

    private FiltroRequestMapper() {
    }

    static FiltroConsultaDTO from(LocalDate dataInicio, LocalDate dataFim, MultiValueMap<String, String> params) {
        Map<String, List<String>> filtros = params.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(PREFIXO))
                .collect(Collectors.toMap(
                        entry -> entry.getKey().substring(PREFIXO.length()),
                        entry -> normalizarValoresFiltro(entry.getKey().substring(PREFIXO.length()), entry.getValue()),
                        (atual, ignorado) -> atual,
                        LinkedHashMap::new
                ));
        consolidarFiliaisParceiros(filtros);
        filtros.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        return new FiltroConsultaDTO(dataInicio, dataFim, filtros);
    }

    private static void consolidarFiliaisParceiros(Map<String, List<String>> filtros) {
        List<String> filiais = filtros.getOrDefault(FILTRO_FILIAIS, List.of());
        List<String> parceirosLogisticos = filtros.getOrDefault(FILTRO_PARCEIROS_LOGISTICOS, List.of());
        if (filiais.isEmpty() && parceirosLogisticos.isEmpty()) {
            return;
        }

        List<String> consolidadas = new ArrayList<>(filiais);
        for (String parceiroLogistico : parceirosLogisticos) {
            if (!consolidadas.contains(parceiroLogistico)) {
                consolidadas.add(parceiroLogistico);
            }
        }

        filtros.put(FILTRO_FILIAIS, consolidadas);
        filtros.remove(FILTRO_PARCEIROS_LOGISTICOS);
    }

    private static List<String> normalizarValoresFiltro(String chave, List<String> valores) {
        if (!FILTRO_FILIAIS.equals(chave) && !FILTRO_PARCEIROS_LOGISTICOS.equals(chave)) {
            return valores;
        }
        return valores.stream()
                .filter(valor -> valor != null && !valor.isBlank())
                .map(String::trim)
                .filter(valor -> !"GLOBAL".equalsIgnoreCase(valor))
                .distinct()
                .toList();
    }
}
