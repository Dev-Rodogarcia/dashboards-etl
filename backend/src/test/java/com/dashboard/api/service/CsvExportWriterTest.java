package com.dashboard.api.service;

import com.dashboard.api.util.CsvColumn;
import com.dashboard.api.util.CsvExportWriter;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CsvExportWriterTest {

    private final CsvExportWriter writer = new CsvExportWriter();

    @Test
    void escreverBeansDeveGerarCsvComBomEscapeSeparadorQuebraLinhaEAcentos() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        writer.escreverBeans(outputStream, List.of(
                new LinhaCsv(
                        "São Paulo",
                        "texto; com \"aspas\"",
                        "primeira\nsegunda",
                        null,
                        new BigDecimal("1234.50"),
                        LocalDate.of(2026, 5, 7)
                )
        ));

        String csv = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);

        assertThat(outputStream.toByteArray()).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        assertThat(csv).isEqualTo(
                "\ufeffcidade;descricao;observacao;vazio;valor;data\r\n"
                        + "São Paulo;\"texto; com \"\"aspas\"\"\";\"primeira\nsegunda\";;1234,50;07/05/2026\r\n"
        );
    }

    @Test
    void escreverBeansDeveProtegerCelulasQuePodemVirarFormula() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        writer.escreverBeans(outputStream, List.of(
                new FormulaCsv("=SOMA(1;1)", "+10", "-20", "@cmd")
        ));

        String csv = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);

        assertThat(csv).isEqualTo("\ufeffigual;mais;menos;arroba\r\n\"'=SOMA(1;1)\";'+10;'-20;'@cmd\r\n");
    }

    @Test
    void escreverBeansDeveUsarNomeDeColunaCsvQuandoAnotado() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        writer.escreverBeans(outputStream, List.of(new HeaderCsv("texto")));

        String csv = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);

        assertThat(csv).isEqualTo("\ufeffJustificativa\r\ntexto\r\n");
    }

    private record LinhaCsv(
            String cidade,
            String descricao,
            String observacao,
            String vazio,
            BigDecimal valor,
            LocalDate data
    ) {
    }

    private record FormulaCsv(String igual, String mais, String menos, String arroba) {
    }

    private record HeaderCsv(@CsvColumn("Justificativa") String justificativa) {
    }
}
