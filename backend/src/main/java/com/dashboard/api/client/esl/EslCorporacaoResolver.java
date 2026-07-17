package com.dashboard.api.client.esl;

import com.dashboard.api.exception.EslGraphqlConfiguracaoException;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Resolve a filial analítica selecionada para o CNPJ da corporação ESL. A lista
 * remota é pequena e fica somente em memória por um período curto.
 */
@Component
public class EslCorporacaoResolver {

    private static final Duration TTL_CACHE = Duration.ofMinutes(30);
    private static final int TAMANHO_PAGINA = 100;
    private static final int MAXIMO_PAGINAS = 10;

    private final EslGraphqlClient graphqlClient;
    private volatile CacheCorporacoes cache;

    public EslCorporacaoResolver(EslGraphqlClient graphqlClient) {
        this.graphqlClient = graphqlClient;
    }

    public CorporacaoResolvida resolverCorporacao(String filial) {
        String filialNormalizada = obrigatorio(filial, "filial");
        List<CorporacaoEsl> correspondentes = listarCorporacoesAtivas().stream()
                .filter(corporacao -> corporacao.corresponde(filialNormalizada))
                .toList();

        if (correspondentes.isEmpty()) {
            throw new EslGraphqlConfiguracaoException(
                    "A filial selecionada não possui uma corporação ESL ativa configurada."
            );
        }
        if (correspondentes.size() > 1) {
            throw new EslGraphqlConfiguracaoException(
                    "A filial selecionada corresponde a mais de uma corporação ESL. Ajuste o cadastro da filial."
            );
        }
        CorporacaoEsl corporacao = correspondentes.get(0);
        return new CorporacaoResolvida(corporacao.documento(), corporacao.id());
    }

    public String resolverDocumentoCorporacao(String filial) {
        return resolverCorporacao(filial).documento();
    }

    private List<CorporacaoEsl> listarCorporacoesAtivas() {
        CacheCorporacoes atual = cache;
        if (atual != null && atual.valido()) {
            return atual.corporacoes();
        }

        synchronized (this) {
            atual = cache;
            if (atual != null && atual.valido()) {
                return atual.corporacoes();
            }

            List<CorporacaoEsl> corporacoes = consultarCorporacoesAtivas();
            cache = new CacheCorporacoes(Instant.now().plus(TTL_CACHE), List.copyOf(corporacoes));
            return cache.corporacoes();
        }
    }

    private List<CorporacaoEsl> consultarCorporacoesAtivas() {
        List<CorporacaoEsl> corporacoes = new ArrayList<>();
        String cursor = null;

        for (int pagina = 0; pagina < MAXIMO_PAGINAS; pagina++) {
            Map<String, Object> variaveis = new LinkedHashMap<>();
            variaveis.put("params", Map.of("corporation", Map.of("active", true)));
            variaveis.put("first", TAMANHO_PAGINA);
            if (StringUtils.hasText(cursor)) {
                variaveis.put("after", cursor);
            }

            JsonNode conexao = graphqlClient.executarQuery(
                    EslGraphqlOperations.COMPANY_LIST,
                    EslGraphqlOperations.QUERY_COMPANY_LIST,
                    variaveis
            );
            for (JsonNode edge : conexao.path("edges")) {
                JsonNode node = edge.path("node");
                JsonNode corporation = node.path("corporation");
                String documento = somenteDigitos(corporation.path("person").path("cnpj").asText(""));
                if (documento.length() != 14) {
                    documento = somenteDigitos(node.path("cnpj").asText(""));
                }
                if (documento.length() == 14) {
                    corporacoes.add(new CorporacaoEsl(
                            numeroLongo(corporation, "id"),
                            documento,
                            node.path("name").asText(""),
                            node.path("nickname").asText("")
                    ));
                }
            }

            JsonNode pageInfo = conexao.path("pageInfo");
            if (!pageInfo.path("hasNextPage").asBoolean(false)) {
                return corporacoes;
            }
            cursor = pageInfo.path("endCursor").asText("");
            if (!StringUtils.hasText(cursor)) {
                break;
            }
        }

        throw new EslGraphqlConfiguracaoException(
                "A lista de corporações ESL excedeu o limite operacional de páginas."
        );
    }

    private String obrigatorio(String valor, String campo) {
        if (!StringUtils.hasText(valor)) {
            throw new EslGraphqlConfiguracaoException(
                    "Selecione exatamente uma " + campo + " para realizar operações ESL."
            );
        }
        return valor.trim();
    }

    private String somenteDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }

    private Long numeroLongo(JsonNode node, String campo) {
        JsonNode valor = node.path(campo);
        if (valor.isNumber()) {
            return valor.longValue();
        }
        if (!valor.isTextual() || !StringUtils.hasText(valor.asText())) {
            return null;
        }
        try {
            return Long.parseLong(valor.asText().trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private record CacheCorporacoes(Instant expiraEm, List<CorporacaoEsl> corporacoes) {
        private boolean valido() {
            return Instant.now().isBefore(expiraEm);
        }
    }

    public record CorporacaoResolvida(String documento, Long id) {
    }

    private record CorporacaoEsl(Long id, String documento, String nome, String apelido) {
        private boolean corresponde(String filial) {
            String selecionada = normalizar(filial);
            return aliases().stream().anyMatch(alias -> alias.equals(selecionada)
                    || codigoFilial(alias).equals(selecionada));
        }

        private Set<String> aliases() {
            Set<String> aliases = new LinkedHashSet<>();
            adicionarAlias(aliases, nome);
            adicionarAlias(aliases, apelido);
            return aliases;
        }

        private void adicionarAlias(Set<String> aliases, String valor) {
            String normalizado = normalizar(valor);
            if (!normalizado.isBlank()) {
                aliases.add(normalizado);
            }
        }

        private String codigoFilial(String valor) {
            int separador = valor.indexOf('-');
            return separador > 0 ? valor.substring(0, separador).trim() : valor;
        }

        private String normalizar(String valor) {
            return valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
        }
    }
}
