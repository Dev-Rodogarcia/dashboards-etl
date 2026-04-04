package com.dashboard.api.service;

import com.dashboard.api.repository.DimFilialRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class HorarioCorteFilialMapperService {

    public static final String FILIAL_NAO_MAPEADA = "Não mapeada";

    private static final Pattern DELIMITADORES = Pattern.compile("[-/\\s]+");
    private final DimFilialRepository dimFilialRepository;

    public HorarioCorteFilialMapperService(DimFilialRepository dimFilialRepository) {
        this.dimFilialRepository = dimFilialRepository;
    }

    public FilialMappingContext criarContexto() {
        Map<String, String> lookup = new LinkedHashMap<>();
        dimFilialRepository.findAll().stream()
                .map(f -> f.getNomeFilial())
                .filter(valor -> valor != null && !valor.isBlank())
                .map(String::trim)
                .distinct()
                .forEach(filial -> registrarAliases(lookup, filial));
        return new FilialMappingContext(lookup);
    }

    public String mapearFilialCanonica(String linhaOuOperacao) {
        return mapearFilialCanonica(linhaOuOperacao, criarContexto());
    }

    public String mapearFilialCanonica(String linhaOuOperacao, FilialMappingContext context) {
        if (linhaOuOperacao == null || linhaOuOperacao.isBlank()) {
            return FILIAL_NAO_MAPEADA;
        }

        Map<String, String> lookup = context.lookup();

        String linhaNormalizada = normalizar(linhaOuOperacao);
        if (lookup.containsKey(linhaNormalizada)) {
            return lookup.get(linhaNormalizada);
        }

        Optional<String> token = Arrays.stream(DELIMITADORES.split(linhaOuOperacao.trim()))
                .filter(valor -> valor != null && !valor.isBlank())
                .map(String::trim)
                .filter(valor -> !valor.isBlank())
                .map(this::normalizar)
                .filter(lookup::containsKey)
                .map(lookup::get)
                .findFirst();

        return token.orElse(FILIAL_NAO_MAPEADA);
    }

    private String normalizar(String valor) {
        String semAcento = Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return semAcento.trim().toUpperCase(Locale.ROOT);
    }

    private void registrarAliases(Map<String, String> lookup, String filial) {
        registrarAlias(lookup, filial, filial);

        String codigoPrefixo = extrairCodigoPrefixo(filial);
        if (codigoPrefixo != null) {
            registrarAlias(lookup, codigoPrefixo, filial);
        }

        String codigoSufixo = extrairCodigoSufixoPipe(filial);
        if (codigoSufixo != null) {
            registrarAlias(lookup, codigoSufixo, filial);
        }
    }

    private void registrarAlias(Map<String, String> lookup, String alias, String filial) {
        if (alias == null || alias.isBlank()) {
            return;
        }
        lookup.merge(normalizar(alias), filial, HorarioCorteFilialMapperService::preferirNomeCanonico);
    }

    private String extrairCodigoPrefixo(String filial) {
        int separador = filial.indexOf(" - ");
        if (separador <= 0) {
            return null;
        }
        return filial.substring(0, separador).trim();
    }

    private String extrairCodigoSufixoPipe(String filial) {
        int separador = filial.lastIndexOf('|');
        if (separador < 0 || separador >= filial.length() - 1) {
            return null;
        }
        return filial.substring(separador + 1).trim();
    }

    private static String preferirNomeCanonico(String atual, String candidato) {
        return pontuacaoCanonica(candidato) > pontuacaoCanonica(atual) ? candidato : atual;
    }

    private static int pontuacaoCanonica(String valor) {
        if (valor == null || valor.isBlank()) {
            return 0;
        }
        int score = valor.length();
        if (valor.contains(" - ")) {
            score += 100;
        }
        if (valor.matches("^[A-Z]{2,4}\\s-\\s.+$")) {
            score += 100;
        }
        return score;
    }

    public record FilialMappingContext(Map<String, String> lookup) {
    }
}
