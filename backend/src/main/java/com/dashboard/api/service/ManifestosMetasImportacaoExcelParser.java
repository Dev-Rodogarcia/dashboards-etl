package com.dashboard.api.service;

import com.dashboard.api.util.UploadFileTypeValidator;
import com.dashboard.api.util.UploadFileTypeValidator.FileType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ManifestosMetasImportacaoExcelParser {

    private static final List<String> CABECALHO_ESPERADO = List.of(
            "Mês/Ano",
            "Filial",
            "Tipo de Contrato",
            "Classificação",
            "Valor da Meta"
    );
    private static final long MAX_FILE_SIZE_BYTES = 2L * 1024L * 1024L;
    private static final int MAX_LINHAS_DADOS = 1000;
    private static final Pattern MES_ANO = Pattern.compile("^(\\d{1,2})[/-](\\d{4})$");
    private static final Pattern ANO_MES = Pattern.compile("^(\\d{4})[/-](\\d{1,2})$");
    private static final Pattern DATA_COMPLETA_BR = Pattern.compile("^(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})$");

    public List<String> cabecalhoEsperado() {
        return CABECALHO_ESPERADO;
    }

    public PlanilhaImportada parse(MultipartFile arquivo) {
        validarArquivo(arquivo);

        String nome = Objects.toString(arquivo.getOriginalFilename(), "manifestos-metas-importacao.xlsx");
        try {
            String nomeNormalizado = nome.toLowerCase(Locale.ROOT);
            if (nomeNormalizado.endsWith(".csv")) {
                return parseCsv(arquivo);
            }
            return parseExcel(arquivo);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (RuntimeException | IOException ex) {
            throw new IllegalArgumentException("Não foi possível ler a planilha de metas enviada.");
        }
    }

    private PlanilhaImportada parseExcel(MultipartFile arquivo) throws IOException {
        try (InputStream inputStream = arquivo.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new IllegalArgumentException("A planilha está vazia.");
            }

            validarCabecalho(linhaExcel(sheet.getRow(0)));

            List<LinhaPlanilha> linhas = new ArrayList<>();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                List<String> valores = linhaExcel(row);
                if (linhaVazia(valores)) {
                    continue;
                }
                validarLimite(linhas);
                linhas.add(parseLinha(rowIndex + 1, valores));
            }

            return new PlanilhaImportada(
                    Objects.toString(arquivo.getOriginalFilename(), "manifestos-metas-importacao.xlsx"),
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
            List<String> header = splitCsvLine(removerBom(headerLine), delimiter);
            validarCabecalho(header);

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
                linhas.add(parseLinha(lineNumber, valores));
            }

            return new PlanilhaImportada(
                    Objects.toString(arquivo.getOriginalFilename(), "manifestos-metas-importacao.csv"),
                    linhas
            );
        }
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Envie um arquivo .xlsx, .xls ou .csv para importar as metas.");
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

    private void validarCabecalho(List<String> colunas) {
        if (colunas == null || colunas.size() < CABECALHO_ESPERADO.size()) {
            throw new IllegalArgumentException("Cabeçalho inválido. Use o modelo oficial de importação de metas.");
        }

        List<String> normalizado = new ArrayList<>();
        for (int index = 0; index < CABECALHO_ESPERADO.size(); index++) {
            normalizado.add(colunas.get(index).trim());
        }

        if (!CABECALHO_ESPERADO.equals(normalizado)) {
            throw new IllegalArgumentException("Cabeçalho inválido. Use o modelo oficial de importação de metas.");
        }
    }

    private void validarLimite(List<LinhaPlanilha> linhas) {
        if (linhas.size() >= MAX_LINHAS_DADOS) {
            throw new IllegalArgumentException("A planilha excede o limite de 1000 metas.");
        }
    }

    private LinhaPlanilha parseLinha(int numeroLinha, List<String> valores) {
        List<String> mensagens = new ArrayList<>();
        Competencia competencia = parseCompetencia(valor(valores, 0));
        if (competencia.mensagem() != null) {
            mensagens.add(competencia.mensagem());
        }

        String branchId = normalizarBranchId(valor(valores, 1), mensagens);
        ContractType contrato = normalizarContrato(valor(valores, 2), mensagens);
        String classificationKey = normalizarClassificationKey(valor(valores, 3), mensagens);
        BigDecimal costGoal = parseMeta(valor(valores, 4), mensagens);

        return new LinhaPlanilha(
                numeroLinha,
                competencia.ano(),
                competencia.mes(),
                branchId,
                contrato.label(),
                contrato.key(),
                classificationKey,
                costGoal,
                List.copyOf(mensagens)
        );
    }

    private Competencia parseCompetencia(String valor) {
        if (valor == null || valor.isBlank()) {
            return new Competencia(null, null, "Competência Mês/Ano é obrigatória.");
        }

        String normalizado = valor.trim();
        Matcher mesAno = MES_ANO.matcher(normalizado);
        if (mesAno.matches()) {
            return validarCompetencia(Integer.parseInt(mesAno.group(2)), Integer.parseInt(mesAno.group(1)));
        }

        Matcher anoMes = ANO_MES.matcher(normalizado);
        if (anoMes.matches()) {
            return validarCompetencia(Integer.parseInt(anoMes.group(1)), Integer.parseInt(anoMes.group(2)));
        }

        Matcher dataBr = DATA_COMPLETA_BR.matcher(normalizado);
        if (dataBr.matches()) {
            return validarCompetencia(Integer.parseInt(dataBr.group(3)), Integer.parseInt(dataBr.group(2)));
        }

        try {
            LocalDate data = LocalDate.parse(normalizado);
            return validarCompetencia(data.getYear(), data.getMonthValue());
        } catch (RuntimeException ignored) {
            return new Competencia(null, null, "Competência inválida. Use formatos como 05/2026 ou 2026-05.");
        }
    }

    private Competencia validarCompetencia(int ano, int mes) {
        if (ano < 2000 || ano > 2100 || mes < 1 || mes > 12) {
            return new Competencia(ano, mes, "Competência inválida. Ano deve estar entre 2000 e 2100 e mês entre 1 e 12.");
        }
        return new Competencia(ano, mes, null);
    }

    private String normalizarBranchId(String valor, List<String> mensagens) {
        if (valor == null || valor.isBlank() || "GLOBAL".equalsIgnoreCase(valor.trim())) {
            return null;
        }
        String normalizado = valor.trim();
        if (normalizado.length() > 120) {
            mensagens.add("Filial excede 120 caracteres.");
        }
        return normalizado;
    }

    private ContractType normalizarContrato(String valor, List<String> mensagens) {
        String label = valor == null || valor.isBlank() ? "Geral" : valor.trim();
        String key = label.toLowerCase(Locale.ROOT);
        if (label.length() > 100) {
            mensagens.add("Tipo de contrato excede 100 caracteres.");
        }
        if (key.length() > 100) {
            mensagens.add("Chave do tipo de contrato excede 100 caracteres.");
        }
        return new ContractType(label, key.isBlank() ? "geral" : key);
    }

    private String normalizarClassificationKey(String valor, List<String> mensagens) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String normalizado = valor.trim().toLowerCase(Locale.ROOT);
        if ("geral".equals(normalizado) || "global".equals(normalizado)) {
            return null;
        }
        if (normalizado.length() > 120) {
            mensagens.add("Chave da classificação excede 120 caracteres.");
        }
        return normalizado;
    }

    private BigDecimal parseMeta(String valor, List<String> mensagens) {
        if (valor == null || valor.isBlank()) {
            mensagens.add("Valor da Meta é obrigatório.");
            return null;
        }

        String somenteNumero = valor.trim()
                .replace("R$", "")
                .replace(" ", "")
                .replaceAll("[^0-9,.-]", "");
        if (somenteNumero.isBlank()) {
            mensagens.add("Valor da Meta precisa ser numérico.");
            return null;
        }

        String normalizado = somenteNumero;
        if (normalizado.contains(",") && normalizado.contains(".")) {
            normalizado = normalizado.replace(".", "").replace(",", ".");
        } else if (normalizado.contains(",")) {
            normalizado = normalizado.replace(",", ".");
        }

        try {
            BigDecimal valorMeta = new BigDecimal(normalizado);
            if (valorMeta.compareTo(BigDecimal.ZERO) < 0) {
                mensagens.add("Valor da Meta não pode ser negativo.");
            }
            return valorMeta;
        } catch (NumberFormatException ex) {
            mensagens.add("Valor da Meta precisa ser numérico.");
            return null;
        }
    }

    private List<String> linhaExcel(Row row) {
        List<String> valores = new ArrayList<>();
        for (int index = 0; index < CABECALHO_ESPERADO.size(); index++) {
            valores.add(valorTexto(row == null ? null : row.getCell(index)));
        }
        return valores;
    }

    private String valorTexto(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            LocalDate data = cell.getLocalDateTimeCellValue().toLocalDate();
            return "%02d/%d".formatted(data.getMonthValue(), data.getYear());
        }

        DataFormatter formatter = new DataFormatter(Locale.forLanguageTag("pt-BR"));
        return formatter.formatCellValue(cell).trim();
    }

    private boolean linhaVazia(List<String> valores) {
        if (valores == null) {
            return true;
        }
        for (int index = 0; index < CABECALHO_ESPERADO.size(); index++) {
            if (!valor(valores, index).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String valor(List<String> valores, int index) {
        if (valores == null || index >= valores.size()) {
            return "";
        }
        return Objects.toString(valores.get(index), "").trim();
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
            Integer ano,
            Integer mes,
            String branchId,
            String contractType,
            String contractTypeKey,
            String classificationKey,
            BigDecimal costGoal,
            List<String> mensagens
    ) {
        public boolean valida() {
            return mensagens == null || mensagens.isEmpty();
        }

        public String chaveImportacao() {
            return "%s|%s|%s|%s|%s".formatted(
                    branchId == null ? "GLOBAL" : branchId.toLowerCase(Locale.ROOT),
                    ano,
                    mes,
                    contractTypeKey,
                    classificationKey == null ? "GERAL" : classificationKey
            );
        }
    }

    private record Competencia(Integer ano, Integer mes, String mensagem) {
    }

    private record ContractType(String label, String key) {
    }
}
