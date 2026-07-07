package com.dashboard.api.service;

import com.dashboard.api.util.UploadFileTypeValidator;
import com.dashboard.api.util.UploadFileTypeValidator.FileType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
public class CubagemClientesExcecaoImportacaoParser {

    private static final long MAX_FILE_SIZE_BYTES = 2L * 1024L * 1024L;
    private static final int MAX_LINHAS_DADOS = 5000;

    public PlanilhaImportada parse(MultipartFile arquivo) {
        validarArquivo(arquivo);

        String nome = Objects.toString(arquivo.getOriginalFilename(), "clientes-sem-cubagem.xlsx");
        try {
            if (nome.toLowerCase(Locale.ROOT).endsWith(".csv")) {
                return parseCsv(arquivo);
            }
            return parseExcel(arquivo);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (RuntimeException | IOException ex) {
            throw new IllegalArgumentException("Não foi possível ler a planilha de clientes sem cubagem.");
        }
    }

    private PlanilhaImportada parseExcel(MultipartFile arquivo) throws IOException {
        try (InputStream inputStream = arquivo.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new IllegalArgumentException("A planilha está vazia.");
            }

            HeaderMap header = mapearCabecalho(linhaExcel(sheet.getRow(0), sheet.getRow(0) == null ? 0 : sheet.getRow(0).getLastCellNum()));
            List<LinhaPlanilha> linhas = new ArrayList<>();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                List<String> valores = linhaExcel(row, header.totalColunas());
                if (linhaVazia(valores)) {
                    continue;
                }
                validarLimite(linhas);
                linhas.add(parseLinha(rowIndex + 1, valores, header));
            }

            return new PlanilhaImportada(
                    Objects.toString(arquivo.getOriginalFilename(), "clientes-sem-cubagem.xlsx"),
                    linhas
            );
        }
    }

    private PlanilhaImportada parseCsv(MultipartFile arquivo) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(arquivo.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("O arquivo CSV está vazio.");
            }

            char delimiter = detectarDelimitador(headerLine);
            HeaderMap header = mapearCabecalho(splitCsvLine(removerBom(headerLine), delimiter));
            List<LinhaPlanilha> linhas = new ArrayList<>();
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                List<String> valores = splitCsvLine(line, delimiter);
                if (linhaVazia(valores)) {
                    continue;
                }
                validarLimite(linhas);
                linhas.add(parseLinha(lineNumber, valores, header));
            }

            return new PlanilhaImportada(
                    Objects.toString(arquivo.getOriginalFilename(), "clientes-sem-cubagem.csv"),
                    linhas
            );
        }
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Envie um arquivo .xlsx, .xls ou .csv para importar clientes sem cubagem.");
        }

        if (arquivo.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Arquivo muito grande. Envie uma planilha de até 2 MB.");
        }

        String nome = arquivo.getOriginalFilename();
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do arquivo é obrigatório.");
        }

        String nomeNormalizado = nome.toLowerCase(Locale.ROOT);
        if (!nomeNormalizado.endsWith(".xlsx") && !nomeNormalizado.endsWith(".xls") && !nomeNormalizado.endsWith(".csv")) {
            throw new IllegalArgumentException("Formato inválido. Envie somente arquivos .xlsx, .xls ou .csv.");
        }

        if (nomeNormalizado.endsWith(".csv")) {
            UploadFileTypeValidator.validarAssinatura(
                    arquivo,
                    UploadFileTypeValidator.tipos(FileType.CSV),
                    "Assinatura do arquivo inválida. Envie somente arquivos .csv reais."
            );
            return;
        }

        UploadFileTypeValidator.validarAssinatura(
                arquivo,
                UploadFileTypeValidator.tipos(FileType.XLSX, FileType.XLS),
                "Assinatura do arquivo inválida. Envie somente planilhas .xlsx ou .xls reais."
        );
    }

    private HeaderMap mapearCabecalho(List<String> colunas) {
        Map<String, Integer> indices = new LinkedHashMap<>();
        for (int index = 0; index < colunas.size(); index++) {
            String chave = normalizarCabecalho(colunas.get(index));
            if (!chave.isBlank()) {
                indices.putIfAbsent(chave, index);
            }
        }

        Integer cnpj = primeiroIndice(indices, "cnpj", "cliente cnpj", "cliente cnpj cpf", "cpf cnpj");
        Integer razaoSocial = primeiroIndice(indices, "razao social", "cliente", "nome razao social");
        if (cnpj == null || razaoSocial == null) {
            throw new IllegalArgumentException("Cabeçalho inválido. Use as colunas CNPJ e Razão Social.");
        }

        return new HeaderMap(
                cnpj,
                razaoSocial,
                primeiroIndice(indices, "nome fantasia", "fantasia"),
                primeiroIndice(indices, "cidade uf", "cidade/uf", "cidade"),
                colunas.size()
        );
    }

    private LinhaPlanilha parseLinha(int numeroLinha, List<String> valores, HeaderMap header) {
        List<String> mensagens = new ArrayList<>();
        String cnpjOriginal = valor(valores, header.cnpjIndex());
        String clienteCnpj = normalizarCnpj(cnpjOriginal);
        if (clienteCnpj.isBlank()) {
            mensagens.add("CNPJ é obrigatório.");
        } else if (clienteCnpj.length() != 14) {
            mensagens.add("CNPJ deve conter exatamente 14 dígitos após a limpeza.");
        }

        String razaoSocial = limitarTexto(valor(valores, header.razaoSocialIndex()), 255, "Razão Social", mensagens);
        if (razaoSocial == null || razaoSocial.isBlank()) {
            mensagens.add("Razão Social é obrigatória.");
        }
        String nomeFantasia = header.nomeFantasiaIndex() == null
                ? null
                : limitarTexto(valor(valores, header.nomeFantasiaIndex()), 255, "Nome Fantasia", mensagens);
        String cidadeUf = header.cidadeUfIndex() == null
                ? null
                : limitarTexto(valor(valores, header.cidadeUfIndex()), 150, "Cidade/UF", mensagens);

        return new LinhaPlanilha(
                numeroLinha,
                clienteCnpj,
                razaoSocial,
                nomeFantasia,
                cidadeUf,
                List.copyOf(mensagens)
        );
    }

    private void validarLimite(List<LinhaPlanilha> linhas) {
        if (linhas.size() >= MAX_LINHAS_DADOS) {
            throw new IllegalArgumentException("A planilha excede o limite de 5000 clientes.");
        }
    }

    private List<String> linhaExcel(Row row, int colunas) {
        int limite = Math.max(1, colunas);
        List<String> valores = new ArrayList<>();
        for (int index = 0; index < limite; index++) {
            valores.add(valorTexto(row == null ? null : row.getCell(index)));
        }
        return valores;
    }

    private String valorTexto(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue())
                    .setScale(0, RoundingMode.HALF_UP)
                    .toPlainString();
        }

        DataFormatter formatter = new DataFormatter(Locale.forLanguageTag("pt-BR"));
        return formatter.formatCellValue(cell).trim();
    }

    private boolean linhaVazia(List<String> valores) {
        if (valores == null) {
            return true;
        }
        return valores.stream().allMatch(valor -> valor == null || valor.isBlank());
    }

    private String valor(List<String> valores, Integer index) {
        if (valores == null || index == null || index < 0 || index >= valores.size()) {
            return "";
        }
        return Objects.toString(valores.get(index), "").trim();
    }

    private String limitarTexto(String valor, int limite, String campo, List<String> mensagens) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String normalizado = valor.trim();
        if (normalizado.length() > limite) {
            mensagens.add("%s excede %d caracteres.".formatted(campo, limite));
            return normalizado.substring(0, limite);
        }
        return normalizado;
    }

    private String normalizarCnpj(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replaceAll("[^0-9]", "");
    }

    private Integer primeiroIndice(Map<String, Integer> indices, String... nomes) {
        for (String nome : nomes) {
            Integer index = indices.get(normalizarCabecalho(nome));
            if (index != null) {
                return index;
            }
        }
        return null;
    }

    private String normalizarCabecalho(String valor) {
        if (valor == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[_\\-]+", " ")
                .replaceAll("\\s+", " ");
    }

    private char detectarDelimitador(String headerLine) {
        long semicolon = headerLine.chars().filter(ch -> ch == ';').count();
        long comma = headerLine.chars().filter(ch -> ch == ',').count();
        return semicolon >= comma ? ';' : ',';
    }

    private String removerBom(String texto) {
        return texto != null && texto.startsWith("\uFEFF") ? texto.substring(1) : texto;
    }

    private List<String> splitCsvLine(String line, char delimiter) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int index = 0; index < line.length(); index++) {
            char ch = line.charAt(index);
            if (ch == '"') {
                if (inQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == delimiter && !inQuotes) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    public record PlanilhaImportada(
            String nomeArquivo,
            List<LinhaPlanilha> linhas
    ) {
    }

    public record LinhaPlanilha(
            int linha,
            String clienteCnpj,
            String razaoSocial,
            String nomeFantasia,
            String cidadeUf,
            List<String> mensagens
    ) {
        public String chaveImportacao() {
            return clienteCnpj == null || clienteCnpj.isBlank() ? "linha:" + linha : clienteCnpj;
        }
    }

    private record HeaderMap(
            int cnpjIndex,
            int razaoSocialIndex,
            Integer nomeFantasiaIndex,
            Integer cidadeUfIndex,
            int totalColunas
    ) {
    }
}
