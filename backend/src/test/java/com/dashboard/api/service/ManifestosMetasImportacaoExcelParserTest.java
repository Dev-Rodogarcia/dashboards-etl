package com.dashboard.api.service;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManifestosMetasImportacaoExcelParserTest {

    private final ManifestosMetasImportacaoExcelParser parser = new ManifestosMetasImportacaoExcelParser();

    @Test
    void deveAceitarCsvTextoComCabecalhoOficial() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "manifestos-metas.csv",
                "text/csv",
                ("Mês/Ano;Filial;Tipo de Contrato;Classificação;Valor da Meta\n"
                        + "05/2026;GLOBAL;Geral;;123,45\n").getBytes(StandardCharsets.UTF_8)
        );

        ManifestosMetasImportacaoExcelParser.PlanilhaImportada resultado = parser.parse(arquivo);

        assertThat(resultado.linhas()).hasSize(1);
        assertThat(resultado.linhas().get(0).ano()).isEqualTo(2026);
        assertThat(resultado.linhas().get(0).mes()).isEqualTo(5);
    }

    @Test
    void deveRejeitarXlsxComConteudoTextoAntesDeAbrirPlanilha() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "manifestos-metas.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "Mês/Ano;Filial;Tipo de Contrato;Classificação;Valor da Meta".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> parser.parse(arquivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Assinatura do arquivo inválida");
    }
}
