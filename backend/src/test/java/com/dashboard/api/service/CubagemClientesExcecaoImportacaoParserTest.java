package com.dashboard.api.service;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CubagemClientesExcecaoImportacaoParserTest {

    private final CubagemClientesExcecaoImportacaoParser parser = new CubagemClientesExcecaoImportacaoParser();

    @Test
    void deveAceitarCsvUtf8ComCnpjFormatadoERazaoSocial() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "clientes-sem-cubagem.csv",
                "text/csv",
                ("CNPJ;Razão Social;Nome Fantasia;Cidade/UF\n"
                        + "43.996.693/0001-27;Cliente Ágil S/A;Cliente Ágil;São Paulo/SP\n")
                        .getBytes(StandardCharsets.UTF_8)
        );

        CubagemClientesExcecaoImportacaoParser.PlanilhaImportada resultado = parser.parse(arquivo);
        CubagemClientesExcecaoImportacaoParser.LinhaPlanilha linha = resultado.linhas().get(0);

        assertThat(resultado.linhas()).hasSize(1);
        assertThat(linha.clienteCnpj()).isEqualTo("43996693000127");
        assertThat(linha.razaoSocial()).isEqualTo("Cliente Ágil S/A");
        assertThat(linha.nomeFantasia()).isEqualTo("Cliente Ágil");
        assertThat(linha.cidadeUf()).isEqualTo("São Paulo/SP");
        assertThat(linha.mensagens()).isEmpty();
    }

    @Test
    void deveMarcarErroQuandoCnpjLimpoNaoTemQuatorzeDigitos() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "clientes-sem-cubagem.csv",
                "text/csv",
                ("CNPJ;Razão Social\n"
                        + "123;Cliente Invalido\n").getBytes(StandardCharsets.UTF_8)
        );

        CubagemClientesExcecaoImportacaoParser.LinhaPlanilha linha = parser.parse(arquivo).linhas().get(0);

        assertThat(linha.clienteCnpj()).isEqualTo("123");
        assertThat(linha.mensagens()).contains("CNPJ deve conter exatamente 14 dígitos após a limpeza.");
    }

    @Test
    void deveRejeitarCsvSemCabecalhoObrigatorio() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "clientes-sem-cubagem.csv",
                "text/csv",
                "Documento;Cliente\n".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> parser.parse(arquivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cabeçalho inválido");
    }
}
