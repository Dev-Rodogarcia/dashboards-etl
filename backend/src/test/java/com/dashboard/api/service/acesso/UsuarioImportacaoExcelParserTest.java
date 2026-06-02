package com.dashboard.api.service.acesso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioImportacaoExcelParserTest {

    private final UsuarioImportacaoExcelParser parser = new UsuarioImportacaoExcelParser();

    @Test
    void deveRejeitarArquivoMaiorQueDoisMbAntesDeAbrirPlanilha() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "usuarios.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[(2 * 1024 * 1024) + 1]
        );

        assertThatThrownBy(() -> parser.parse(arquivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("até 2 MB");
    }

    @Test
    void deveRejeitarMaisDeMilLinhasDeDados() throws Exception {
        String[][] linhas = new String[1001][3];
        for (int index = 0; index < linhas.length; index++) {
            linhas[index] = new String[]{
                    "Usuário " + index,
                    "usuario" + index + "@empresa.com",
                    "Logística"
            };
        }

        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "usuarios.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookUsuarios(linhas)
        );

        assertThatThrownBy(() -> parser.parse(arquivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1000 linhas");
    }

    private static byte[] workbookUsuarios(String[][] linhas) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Usuários");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Nome do Usuário");
            header.createCell(1).setCellValue("E-mail");
            header.createCell(2).setCellValue("Setor");

            for (int index = 0; index < linhas.length; index++) {
                Row row = sheet.createRow(index + 1);
                row.createCell(0).setCellValue(linhas[index][0]);
                row.createCell(1).setCellValue(linhas[index][1]);
                row.createCell(2).setCellValue(linhas[index][2]);
            }

            workbook.write(output);
            return output.toByteArray();
        }
    }
}
