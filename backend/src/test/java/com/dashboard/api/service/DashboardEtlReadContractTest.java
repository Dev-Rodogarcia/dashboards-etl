package com.dashboard.api.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

class DashboardEtlReadContractTest {

    private static final Path MAIN_JAVA = Path.of("src", "main", "java", "com", "dashboard", "api");

    private static final Map<String, String> VIEW_ENTITIES = Map.ofEntries(
            entry("VisaoColetasEntity.java", "vw_coletas_powerbi"),
            entry("VisaoContasAPagarEntity.java", "vw_contas_a_pagar_powerbi"),
            entry("VisaoCotacoesEntity.java", "vw_cotacoes_powerbi"),
            entry("VisaoFaturasClienteEntity.java", "vw_faturas_por_cliente_powerbi"),
            entry("VisaoFretesEntity.java", "vw_fretes_powerbi"),
            entry("VisaoInventarioEntity.java", "vw_inventario_powerbi"),
            entry("VisaoLocalizacaoCargasEntity.java", "vw_localizacao_cargas_powerbi"),
            entry("VisaoManifestosEntity.java", "vw_manifestos_powerbi"),
            entry("VisaoMonitoramentoEntity.java", "vw_bi_monitoramento"),
            entry("VisaoSinistrosEntity.java", "vw_sinistros_powerbi")
    );

    private static final String RAW_ETL_TABLES = String.join("|",
            "coletas",
            "contas_a_pagar",
            "cotacoes",
            "faturas",
            "faturas_por_cliente",
            "fretes",
            "inventario",
            "localizacao_cargas",
            "manifestos",
            "sinistros"
    );

    private static final Pattern RAW_ETL_TABLE_SQL = Pattern.compile(
            "\\b(?:FROM|JOIN)\\s+(?:ETL_SISTEMA\\.)?dbo\\.\\[?(?:"
                    + RAW_ETL_TABLES
                    + ")\\]?(?![A-Za-z0-9_])",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern RAW_ETL_TABLE_DML = Pattern.compile(
            "\\b(?:UPDATE|DELETE\\s+FROM|INSERT\\s+INTO)\\s+(?:ETL_SISTEMA\\.)?dbo\\.\\[?(?:"
                    + RAW_ETL_TABLES
                    + ")\\]?(?![A-Za-z0-9_])",
            Pattern.CASE_INSENSITIVE
    );

    @Test
    void entidadesAnaliticasDoEtlDevemConsumirViewsOperacionais() throws IOException {
        for (Map.Entry<String, String> entidade : VIEW_ENTITIES.entrySet()) {
            Path path = MAIN_JAVA.resolve(Path.of("model", entidade.getKey()));
            String source = ler(path);

            assertThat(source)
                    .as(entidade.getKey() + " deve ser read-only")
                    .contains("@Immutable");
            assertThat(source)
                    .as(entidade.getKey() + " deve mapear a view operacional do ETL")
                    .contains("@Table(name = \"" + entidade.getValue() + "\")");
        }
    }

    @Test
    void codigoJavaNaoDeveConsultarTabelasCruasDoEtlSistema() throws IOException {
        List<String> violacoes = new ArrayList<>();

        try (var paths = Files.walk(MAIN_JAVA)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> coletarViolacoes(path, violacoes));
        }

        assertThat(violacoes)
                .as("O dashboard deve consumir dbo.vw_*_powerbi; o filtro excluido_na_origem fica no ETL.")
                .isEmpty();
    }

    private static void coletarViolacoes(Path path, List<String> violacoes) {
        try {
            String source = ler(path);
            coletarMatches(path, source, RAW_ETL_TABLE_SQL, violacoes);
            coletarMatches(path, source, RAW_ETL_TABLE_DML, violacoes);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao auditar " + path, ex);
        }
    }

    private static void coletarMatches(Path path, String source, Pattern pattern, List<String> violacoes) {
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            violacoes.add(MAIN_JAVA.relativize(path) + ": " + matcher.group());
        }
    }

    private static String ler(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
