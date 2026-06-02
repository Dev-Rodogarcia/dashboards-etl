package com.dashboard.api.service.acesso;

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UsuarioImportacaoExcelParser {

    private static final List<String> CABECALHO_ESPERADO = List.of(
            "Nome do Usuário",
            "E-mail",
            "Setor"
    );
    private static final long MAX_FILE_SIZE_BYTES = 2L * 1024L * 1024L;
    private static final int MAX_LINHAS_DADOS = 1000;

    public PlanilhaImportada parse(MultipartFile arquivo) {
        validarArquivo(arquivo);

        try (InputStream inputStream = arquivo.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new IllegalArgumentException("A planilha está vazia.");
            }

            validarCabecalho(sheet.getRow(0));

            List<LinhaPlanilha> linhas = new ArrayList<>();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (linhaVazia(row)) {
                    continue;
                }
                if (linhas.size() >= MAX_LINHAS_DADOS) {
                    throw new IllegalArgumentException("A planilha excede o limite de 1000 linhas de usuários.");
                }

                linhas.add(new LinhaPlanilha(
                        rowIndex + 1,
                        valorTexto(row, 0),
                        valorTexto(row, 1),
                        valorTexto(row, 2)
                ));
            }

            return new PlanilhaImportada(
                    Objects.toString(arquivo.getOriginalFilename(), "usuarios-importacao.xlsx"),
                    linhas
            );
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Não foi possível ler a planilha enviada.");
        } catch (IOException ex) {
            throw new IllegalArgumentException("Não foi possível ler a planilha enviada.");
        }
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Envie um arquivo .xlsx ou .xls para importar os usuários.");
        }

        if (arquivo.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Arquivo muito grande. Envie uma planilha de até 2 MB.");
        }

        String nome = arquivo.getOriginalFilename();
        if (nome == null) {
            throw new IllegalArgumentException("O nome do arquivo é obrigatório.");
        }

        String nomeNormalizado = nome.toLowerCase(Locale.ROOT);
        if (!nomeNormalizado.endsWith(".xlsx") && !nomeNormalizado.endsWith(".xls")) {
            throw new IllegalArgumentException("Formato inválido. Envie somente arquivos .xlsx ou .xls.");
        }
    }

    private void validarCabecalho(Row row) {
        if (row == null) {
            throw new IllegalArgumentException("Cabeçalho da planilha não encontrado.");
        }

        List<String> colunas = new ArrayList<>();
        for (int index = 0; index < CABECALHO_ESPERADO.size(); index++) {
            colunas.add(valorTexto(row, index));
        }

        if (!CABECALHO_ESPERADO.equals(colunas)) {
            throw new IllegalArgumentException("Cabeçalho inválido. Use o modelo oficial de importação de usuários.");
        }
    }

    private boolean linhaVazia(Row row) {
        if (row == null) {
            return true;
        }

        for (int index = 0; index < CABECALHO_ESPERADO.size(); index++) {
            if (!valorTexto(row, index).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String valorTexto(Row row, int cellIndex) {
        if (row == null) {
            return "";
        }
        return valorTexto(row.getCell(cellIndex));
    }

    private String valorTexto(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return "";
        }

        DataFormatter formatter = new DataFormatter(Locale.forLanguageTag("pt-BR"));
        return formatter.formatCellValue(cell).trim();
    }

    public record PlanilhaImportada(
            String nomeArquivo,
            List<LinhaPlanilha> linhas
    ) {
    }

    public record LinhaPlanilha(
            int linha,
            String nome,
            String email,
            String setor
    ) {
    }
}
