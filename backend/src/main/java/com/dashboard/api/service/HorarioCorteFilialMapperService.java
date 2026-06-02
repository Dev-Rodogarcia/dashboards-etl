package com.dashboard.api.service;

import com.dashboard.api.repository.DimFilialRepository;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class HorarioCorteFilialMapperService {

    private static final Logger log = LoggerFactory.getLogger(HorarioCorteFilialMapperService.class);

    public static final String FILIAL_NAO_MAPEADA = "Não mapeada";

    private static final Pattern DELIMITADORES = Pattern.compile("[-/\\s]+");
    private static final String[] FILIAIS_RASTER_PADRAO = {
            "AGU - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
            "CAS - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
            "CPQ - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
            "CWB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
            "NHB - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
            "REC - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
            "RJR - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
            "SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA"
    };
    private final DimFilialRepository dimFilialRepository;

    public HorarioCorteFilialMapperService(DimFilialRepository dimFilialRepository) {
        this.dimFilialRepository = dimFilialRepository;
    }

    public FilialMappingContext criarContexto() {
        Map<String, String> lookup = criarLookupPadrao();
        try {
            dimFilialRepository.findDistinctNomes().stream()
                    .filter(valor -> valor != null && !valor.isBlank())
                    .map(String::trim)
                    .distinct()
                    .forEach(filial -> registrarAliases(lookup, filial));
        } catch (DataAccessException ex) {
            log.warn(
                    "Falha ao consultar vw_dim_filiais para mapear filial de Horario de Corte; usando aliases Raster locais. causa={}",
                    ex.getMostSpecificCause().getMessage()
            );
        }
        return new FilialMappingContext(lookup);
    }

    public FilialMappingContext criarContextoRasterPadrao() {
        return new FilialMappingContext(criarLookupPadrao());
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

    private Map<String, String> criarLookupPadrao() {
        Map<String, String> lookup = new LinkedHashMap<>();
        Arrays.stream(FILIAIS_RASTER_PADRAO).forEach(filial -> registrarAliases(lookup, filial));
        return lookup;
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
