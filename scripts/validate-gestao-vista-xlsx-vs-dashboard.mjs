import { execFileSync, spawnSync } from 'node:child_process';
import { createHmac } from 'node:crypto';
import { existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import os from 'node:os';
import path from 'node:path';

const ROOT_DIR = process.cwd();
const REPORTS_DIR = path.join(ROOT_DIR, 'reports');
const DEFAULT_XLSX = path.join(ROOT_DIR, 'docs', 'Análise - Divergências - Indicadores Projeto Gestão a Vista Operacional.xlsx');
const DEFAULT_API_BASE_URL = 'http://localhost:5011';

const METRIC_TOLERANCES = {
  count: 0,
  currency: 0,
  percentage: 0,
};

const JAVA_READER_SOURCE = String.raw`
import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.Styles;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class GestaoVistaXlsxReader {
    private static final Set<String> TIPOS_ORDEM = Set.of("picking", "retorno", "recebimento", "carregamento", "descarregamento");
    private static final Pattern CELL_REF_PATTERN = Pattern.compile("^([A-Z]+)(\\d+)$");

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("Uso: GestaoVistaXlsxReader <xlsx> <dataInicio> <dataFim>");
        }

        File xlsx = new File(args[0]);
        LocalDate dataInicio = LocalDate.parse(args[1]);
        LocalDate dataFim = LocalDate.parse(args[2]);
        Map<String, TableDef> tables = loadTables(xlsx.toPath());
        Stats stats = new Stats();

        try (OPCPackage pkg = OPCPackage.open(xlsx, PackageAccess.READ)) {
            XSSFReader reader = new XSSFReader(pkg);
            reader.setUseReadOnlySharedStringsTable(true);
            Styles styles = reader.getStylesTable();
            SharedStrings sharedStrings = reader.getSharedStringsTable();

            XSSFReader.SheetIterator sheets = reader.getSheetIterator();
            while (sheets.hasNext()) {
                try (InputStream stream = sheets.next()) {
                    String sheetName = sheets.getSheetName();
                    if ("Clientes_sem_Cub (Apoio cub)".equals(sheetName)) {
                        parseSheet(stream, styles, sharedStrings, table(tables, "Tabela9"),
                                List.of("CNPJ"),
                                stats::acceptClienteSemCubagem);
                    } else if ("HC (Apoio)".equals(sheetName)) {
                        parseSheet(stream, styles, sharedStrings, table(tables, "Horário_Corte"),
                                List.of("ORIGEM - SM", "DESTINO - SM", "Origem x Destino", "HORÁRIO CORTE"),
                                stats::acceptHorarioCorteApoio);
                    }
                }
            }

            sheets = reader.getSheetIterator();
            while (sheets.hasNext()) {
                try (InputStream stream = sheets.next()) {
                    String sheetName = sheets.getSheetName();
                    switch (sheetName) {
                        case "Performance" -> parseSheet(stream, styles, sharedStrings, table(tables, "performance"),
                                List.of("N° Minuta", "Status", "Previsão Entrega/Previsão de entrega", "Perf Status"),
                                row -> stats.acceptPerformance(row, dataInicio, dataFim));
                        case "Ordens" -> parseSheet(stream, styles, sharedStrings, table(tables, "ordens"),
                                List.of("N° Ordem", "Tipo", "Data/Hora início", "Data/Hora fim"),
                                row -> stats.acceptOrdem(row, dataInicio, dataFim));
                        case "Manifestos (Apoio Ordens)" -> parseSheet(stream, styles, sharedStrings, table(tables, "manifestos"),
                                List.of("Número", "Classificação", "Data criação", "Manifesto/Locais de descarregamento"),
                                row -> stats.acceptManifesto(row, dataInicio, dataFim));
                        case "Cubagem" -> parseSheet(stream, styles, sharedStrings, table(tables, "cubagem"),
                                List.of("N° Minuta", "Status", "Data do frete", "Pagador do frete/Documento", "Total M3", "Peso cubado", "Peso real", "CUBADO?"),
                                row -> stats.acceptCubagem(row, dataInicio, dataFim));
                        case "Sinistros" -> parseSheet(stream, styles, sharedStrings, table(tables, "sinistros"),
                                List.of("Nº do Sinistro", "Data abertura", "valor a pagar ao cliente"),
                                row -> stats.acceptSinistro(row, dataInicio, dataFim));
                        case "Faturamento (Apoio Sinistros)" -> parseSheet(stream, styles, sharedStrings, table(tables, "faturamento"),
                                List.of("N° Minuta", "Data do frete", "Total a receber"),
                                row -> stats.acceptFaturamento(row, dataInicio, dataFim));
                        case "Horário de Corte" -> parseSheet(stream, styles, sharedStrings, table(tables, "Viagens"),
                                List.of("Código", "Dt. Hr. Início Viagem", "Dt. Hr. Fim Viagem", "Origem", "Destino", "Dt. Horario de Corte"),
                                row -> stats.acceptHorarioCorte(row, dataInicio, dataFim));
                        default -> {
                        }
                    }
                }
            }
        }

        System.out.println(stats.toJson());
    }

    private static TableDef table(Map<String, TableDef> tables, String name) {
        TableDef table = tables.get(name);
        if (table == null) {
            throw new IllegalArgumentException("Tabela não encontrada no XLSX: " + name);
        }
        return table;
    }

    private static Map<String, TableDef> loadTables(Path xlsx) throws Exception {
        Map<String, TableDef> tables = new HashMap<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);

        try (ZipFile zip = new ZipFile(xlsx.toFile())) {
            for (ZipEntry entry : zip.stream().filter(e -> e.getName().startsWith("xl/tables/table") && e.getName().endsWith(".xml")).toList()) {
                try (InputStream stream = zip.getInputStream(entry)) {
                    Document document = factory.newDocumentBuilder().parse(stream);
                    Element root = document.getDocumentElement();
                    String name = attr(root, "name", attr(root, "displayName", entry.getName()));
                    String ref = root.getAttribute("ref");
                    Range range = Range.parse(ref);
                    List<String> columns = new ArrayList<>();
                    NodeList nodes = root.getElementsByTagName("tableColumn");
                    for (int i = 0; i < nodes.getLength(); i++) {
                        columns.add(((Element) nodes.item(i)).getAttribute("name"));
                    }
                    tables.put(name, new TableDef(name, range, columns));
                }
            }
        }
        return tables;
    }

    private static String attr(Element element, String name, String fallback) {
        String value = element.getAttribute(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static void parseSheet(
            InputStream stream,
            Styles styles,
            SharedStrings sharedStrings,
            TableDef table,
            List<String> wantedColumns,
            RowConsumer consumer
    ) throws Exception {
        SAXParserFactory saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        XMLReader parser = saxFactory.newSAXParser().getXMLReader();
        SheetHandler sheetHandler = new SheetHandler(styles, sharedStrings, table, wantedColumns, consumer);
        parser.setContentHandler(sheetHandler);
        parser.parse(new InputSource(stream));
    }

    private static int columnIndex(String cellReference) {
        Matcher matcher = CELL_REF_PATTERN.matcher(cellReference);
        if (!matcher.find()) {
            return -1;
        }

        int value = 0;
        for (char c : matcher.group(1).toCharArray()) {
            value = value * 26 + (c - 'A' + 1);
        }
        return value;
    }

    private static boolean inPeriod(LocalDate date, LocalDate start, LocalDate end) {
        return date != null && !date.isBefore(start) && !date.isAfter(end);
    }

    private static LocalDate parseDate(String value) {
        String text = clean(value);
        if (text.isBlank()) {
            return null;
        }

        text = text.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
        if (text.matches("-?\\d+(\\.\\d+)?")) {
            try {
                return org.apache.poi.ss.usermodel.DateUtil.getJavaDate(Double.parseDouble(text)).toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate();
            } catch (RuntimeException ignored) {
            }
        }

        String dateOnly = text.split(" ")[0];
        List<DateTimeFormatter> dateFormats = List.of(
                new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d/M/uuuu").toFormatter(Locale.forLanguageTag("pt-BR")).withResolverStyle(ResolverStyle.SMART),
                new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d/M/uu").toFormatter(Locale.forLanguageTag("pt-BR")).withResolverStyle(ResolverStyle.SMART),
                new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("uuuu-M-d").toFormatter(Locale.ROOT).withResolverStyle(ResolverStyle.SMART),
                new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d-MMM-uu").toFormatter(Locale.forLanguageTag("pt-BR")).withResolverStyle(ResolverStyle.SMART)
        );

        for (String candidate : List.of(text, dateOnly)) {
            for (DateTimeFormatter formatter : dateFormats) {
                try {
                    return LocalDate.parse(candidate, formatter);
                } catch (DateTimeParseException ignored) {
                }
            }
            try {
                return LocalDateTime.parse(candidate.replace(" ", "T")).toLocalDate();
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private static LocalDateTime parseDateTime(String value) {
        String text = clean(value);
        if (text.isBlank()) {
            return null;
        }

        text = text.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
        if (text.matches("-?\\d+(\\.\\d+)?")) {
            try {
                return DateUtil.getLocalDateTime(Double.parseDouble(text)).withNano(0);
            } catch (RuntimeException ignored) {
            }
        }

        List<DateTimeFormatter> formats = List.of(
                new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("uuuu-M-d H:m[:s]").toFormatter(Locale.ROOT).withResolverStyle(ResolverStyle.SMART),
                new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d/M/uuuu H:m[:s]").toFormatter(Locale.forLanguageTag("pt-BR")).withResolverStyle(ResolverStyle.SMART),
                new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d/M/uu H:m[:s]").toFormatter(Locale.forLanguageTag("pt-BR")).withResolverStyle(ResolverStyle.SMART)
        );

        for (DateTimeFormatter formatter : formats) {
            try {
                return LocalDateTime.parse(text.replace('T', ' '), formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        try {
            return LocalDateTime.parse(text.replace(" ", "T")).withNano(0);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static int parseTimeMinutes(String value) {
        String text = clean(value);
        if (text.isBlank()) {
            return -1;
        }
        if (text.matches("-?\\d+(\\.\\d+)?")) {
            BigDecimal decimal = new BigDecimal(text);
            return decimal.multiply(BigDecimal.valueOf(24 * 60)).setScale(0, RoundingMode.HALF_UP).intValue();
        }
        try {
            LocalTime time = LocalTime.parse(text.length() == 5 ? text + ":00" : text);
            return time.getHour() * 60 + time.getMinute();
        } catch (DateTimeParseException ignored) {
            return -1;
        }
    }

    private static BigDecimal parseDecimal(String value) {
        String text = clean(value);
        if (text.isBlank()) {
            return BigDecimal.ZERO;
        }

        text = text.replace("R$", "")
                .replace("\u00A0", "")
                .replace(" ", "")
                .replaceAll("[^0-9,.-]", "");
        int comma = text.lastIndexOf(',');
        int dot = text.lastIndexOf('.');
        if (comma >= 0 && dot >= 0) {
            text = comma > dot ? text.replace(".", "").replace(',', '.') : text.replace(",", "");
        } else if (comma >= 0) {
            text = text.replace(',', '.');
        }
        if (text.isBlank() || text.equals("-") || text.equals(".")) {
            return BigDecimal.ZERO;
        }

        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ignored) {
            return BigDecimal.ZERO;
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeText(String value) {
        String text = Normalizer.normalize(clean(value).toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return text.replaceAll("\\s+", " ").trim();
    }

    private static String normalizeRouteKey(String value) {
        return normalizeText(value).replace(" x ", "|").replaceAll("\\s+", " ");
    }

    private static String normalizeDoc(String value) {
        return clean(value).replaceAll("[^0-9A-Za-z]", "");
    }

    private static BigDecimal pct(int part, int total, int scale) {
        if (total == 0) {
            return null;
        }
        return BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), scale, RoundingMode.HALF_UP);
    }

    private static BigDecimal pct(BigDecimal part, BigDecimal total, int scale) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return part.multiply(BigDecimal.valueOf(100)).divide(total, scale, RoundingMode.HALF_UP);
    }

    private record Range(int startCol, int startRow, int endCol, int endRow) {
        private static Range parse(String ref) {
            String[] parts = ref.split(":");
            CellRef start = CellRef.parse(parts[0]);
            CellRef end = CellRef.parse(parts[1]);
            return new Range(start.col(), start.row(), end.col(), end.row());
        }
    }

    private record CellRef(int col, int row) {
        private static CellRef parse(String value) {
            Matcher matcher = CELL_REF_PATTERN.matcher(value);
            if (!matcher.find()) {
                throw new IllegalArgumentException("Referência de célula inválida: " + value);
            }
            int col = 0;
            for (char c : matcher.group(1).toCharArray()) {
                col = col * 26 + (c - 'A' + 1);
            }
            return new CellRef(col, Integer.parseInt(matcher.group(2)));
        }
    }

    private record TableDef(String name, Range range, List<String> columns) {
        private Map<Integer, String> wantedColumns(List<String> wanted) {
            Map<Integer, String> found = new HashMap<>();
            for (String name : wanted) {
                int relative = columns.indexOf(name) + 1;
                if (relative <= 0) {
                    throw new IllegalArgumentException("Coluna não encontrada na tabela " + this.name + ": " + name);
                }
                found.put(relative, name);
            }
            return found;
        }
    }

    private interface RowConsumer {
        void accept(Map<String, String> row);
    }

    private static final class SheetHandler extends DefaultHandler {
        private final Styles styles;
        private final SharedStrings sharedStrings;
        private final TableDef table;
        private final Map<Integer, String> wantedColumns;
        private final RowConsumer consumer;
        private Map<String, String> currentRow;
        private boolean activeRow;
        private boolean wantedCell;
        private boolean collectingValue;
        private String currentColumnName;
        private String currentCellType;
        private int currentCellStyle;
        private StringBuilder value;

        private SheetHandler(Styles styles, SharedStrings sharedStrings, TableDef table, List<String> wantedColumns, RowConsumer consumer) {
            this.styles = styles;
            this.sharedStrings = sharedStrings;
            this.table = table;
            this.wantedColumns = table.wantedColumns(wantedColumns);
            this.consumer = consumer;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            String name = xmlName(localName, qName);
            if ("row".equals(name)) {
                String rowRef = attributes.getValue("r");
                int row = rowRef == null || rowRef.isBlank() ? -1 : Integer.parseInt(rowRef);
                this.activeRow = row > table.range().startRow() && row <= table.range().endRow();
                this.currentRow = activeRow ? new HashMap<>() : null;
                return;
            }

            if ("c".equals(name)) {
                this.currentColumnName = null;
                this.currentCellType = attributes.getValue("t");
                this.currentCellStyle = parseInt(attributes.getValue("s"), -1);
                this.value = new StringBuilder();
                this.wantedCell = false;

                if (activeRow && currentRow != null) {
                    String cellReference = attributes.getValue("r");
                    int relativeCol = columnIndex(cellReference) - table.range().startCol() + 1;
                    this.currentColumnName = wantedColumns.get(relativeCol);
                    this.wantedCell = this.currentColumnName != null;
                }
                return;
            }

            if (wantedCell && ("v".equals(name) || "t".equals(name))) {
                this.collectingValue = true;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (wantedCell && collectingValue && value != null) {
                value.append(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            String name = xmlName(localName, qName);
            if (wantedCell && ("v".equals(name) || "t".equals(name))) {
                collectingValue = false;
                return;
            }

            if ("c".equals(name)) {
                if (wantedCell && currentRow != null && currentColumnName != null) {
                    currentRow.put(currentColumnName, resolveCellValue());
                }
                wantedCell = false;
                currentColumnName = null;
                currentCellType = null;
                currentCellStyle = -1;
                value = null;
                return;
            }

            if ("row".equals(name)) {
                if (activeRow && currentRow != null) {
                    consumer.accept(currentRow);
                }
                currentRow = null;
                activeRow = false;
            }
        }

        private String resolveCellValue() {
            String raw = value == null ? "" : value.toString();
            if ("s".equals(currentCellType) && !raw.isBlank()) {
                return sharedStrings.getItemAt(Integer.parseInt(raw)).getString();
            }
            if (currentCellStyle >= 0 && raw.matches("-?\\d+(\\.\\d+)?") && isDateStyle(currentCellStyle)) {
                return formatDateCell(raw, currentCellStyle);
            }
            return raw;
        }

        private String formatDateCell(String raw, int styleIndex) {
            LocalDateTime value = DateUtil.getLocalDateTime(Double.parseDouble(raw));
            String format = "";
            try {
                CellStyle style = styles.getStyleAt(styleIndex);
                format = Objects.toString(style.getDataFormatString(), "").toLowerCase(Locale.ROOT);
            } catch (RuntimeException ignored) {
            }

            boolean hasDate = format.contains("d") || format.contains("y") || format.contains("m/");
            boolean hasTime = format.contains("h") || format.contains("s");
            if (hasTime && !hasDate) {
                return value.toLocalTime().withNano(0).toString();
            }
            if (hasTime) {
                return value.withNano(0).toString().replace('T', ' ');
            }
            return value.toLocalDate().toString();
        }

        private boolean isDateStyle(int styleIndex) {
            try {
                CellStyle style = styles.getStyleAt(styleIndex);
                return DateUtil.isADateFormat(style.getDataFormat(), style.getDataFormatString());
            } catch (RuntimeException ignored) {
                return false;
            }
        }

        private static String xmlName(String localName, String qName) {
            return localName == null || localName.isBlank() ? qName : localName;
        }

        private static int parseInt(String value, int fallback) {
            if (value == null || value.isBlank()) {
                return fallback;
            }
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
    }

    private static final class Stats {
        private final Set<String> performanceMinutas = new HashSet<>();
        private final Map<String, Object> performanceDetalhes = new TreeMap<>();
        private int performanceTotal;
        private int performanceNoPrazo;
        private int performanceForaPrazo;
        private int performanceEmAberto;

        private final Set<String> ordens = new HashSet<>();
        private final Set<String> ordensIncompletas = new HashSet<>();
        private final Set<String> manifestosEmitidos = new HashSet<>();
        private final Set<String> manifestosDescarregamento = new HashSet<>();
        private final Map<String, Object> ordensDetalhes = new TreeMap<>();
        private final Map<String, Object> manifestosEmitidosDetalhes = new TreeMap<>();
        private final Map<String, Object> manifestosDescarregamentoDetalhes = new TreeMap<>();

        private final Set<String> clientesSemCubagem = new HashSet<>();
        private final Set<String> cubagemMinutas = new HashSet<>();
        private final Map<String, Object> cubagemDetalhes = new TreeMap<>();
        private int cubagemTotal;
        private int cubagemCubados;
        private int cubagemPesoReal;

        private final Set<String> sinistros = new HashSet<>();
        private final Map<String, Object> sinistrosDetalhes = new TreeMap<>();
        private int sinistrosTotal;
        private BigDecimal valorIndenizado = BigDecimal.ZERO;

        private int faturamentoLinhas;
        private final Set<String> faturamentoMinutas = new HashSet<>();
        private final Map<String, Object> faturamentoPorMinuta = new TreeMap<>();
        private BigDecimal faturamentoBase = BigDecimal.ZERO;

        private final Map<String, Integer> horariosCortePorRota = new HashMap<>();
        private final Set<String> horariosCodigos = new HashSet<>();
        private final Map<String, Object> horariosDetalhes = new TreeMap<>();
        private int horariosTotalProgramado;
        private int horariosSaidasNoHorario;
        private int horariosSaidasForaHorario;
        private int horariosSemCorte;

        private void acceptPerformance(Map<String, String> row, LocalDate start, LocalDate end) {
            LocalDate date = parseDate(row.get("Previsão Entrega/Previsão de entrega"));
            if (!inPeriod(date, start, end) || normalizeText(row.get("Status")).equals("cancelado")) {
                return;
            }
            String minuta = clean(row.get("N° Minuta"));
            if (minuta.isBlank() || !performanceMinutas.add(minuta)) {
                return;
            }
            performanceTotal++;
            String status = normalizeText(row.get("Perf Status"));
            String statusOriginal = clean(row.get("Perf Status"));
            if (status.equals("no prazo") || status.equals("dentro do prazo")) {
                performanceNoPrazo++;
            } else if (status.equals("fora do prazo")) {
                performanceForaPrazo++;
            } else {
                performanceEmAberto++;
            }
            performanceDetalhes.put(minuta, mapOf(
                    "numeroMinuta", minuta,
                    "previsaoEntrega", date.toString(),
                    "perfStatus", statusOriginal.isBlank() ? "EM ABERTO" : statusOriginal
            ));
        }

        private void acceptOrdem(Map<String, String> row, LocalDate start, LocalDate end) {
            LocalDate date = parseDate(row.get("Data/Hora início"));
            if (!inPeriod(date, start, end) || !TIPOS_ORDEM.contains(normalizeText(row.get("Tipo")))) {
                return;
            }
            String ordem = clean(row.get("N° Ordem"));
            if (ordem.isBlank()) {
                return;
            }
            if (ordens.add(ordem)) {
                ordensDetalhes.put(ordem, mapOf(
                        "numeroOrdem", ordem,
                        "tipo", clean(row.get("Tipo")),
                        "dataHoraInicio", clean(row.get("Data/Hora início"))
                ));
            }
            if (clean(row.get("Data/Hora fim")).isBlank()) {
                ordensIncompletas.add(ordem);
            }
        }

        private void acceptManifesto(Map<String, String> row, LocalDate start, LocalDate end) {
            LocalDate date = parseDate(row.get("Data criação"));
            if (!inPeriod(date, start, end)) {
                return;
            }
            String classificacao = normalizeText(row.get("Classificação"));
            if (classificacao.startsWith("carga fechada") || classificacao.startsWith("frete retorno") || classificacao.startsWith("viagem vazia")) {
                return;
            }
            String numero = clean(row.get("Número"));
            if (numero.isBlank()) {
                return;
            }

            if (manifestosEmitidos.add(numero)) {
                manifestosEmitidosDetalhes.put(numero, mapOf(
                        "numero", numero,
                        "dataCriacao", date.toString(),
                        "classificacao", clean(row.get("Classificação"))
                ));
            }
            String locais = clean(row.get("Manifesto/Locais de descarregamento"));
            for (String parte : locais.split("[,;\\r\\n]+")) {
                String local = normalizeText(parte);
                if (!local.isBlank() && !local.equals("null")) {
                    String chave = numero + "|" + local;
                    if (manifestosDescarregamento.add(chave)) {
                        manifestosDescarregamentoDetalhes.put(chave, mapOf(
                                "numero", numero,
                                "localDescarregamento", clean(parte),
                                "dataCriacao", date.toString()
                        ));
                    }
                }
            }
        }

        private void acceptClienteSemCubagem(Map<String, String> row) {
            String doc = normalizeDoc(row.get("CNPJ"));
            if (!doc.isBlank()) {
                clientesSemCubagem.add(doc);
            }
        }

        private void acceptCubagem(Map<String, String> row, LocalDate start, LocalDate end) {
            LocalDate date = parseDate(row.get("Data do frete"));
            if (!inPeriod(date, start, end) || normalizeText(row.get("Status")).equals("cancelado")) {
                return;
            }
            String pagadorDoc = normalizeDoc(row.get("Pagador do frete/Documento"));
            if (!pagadorDoc.isBlank() && clientesSemCubagem.contains(pagadorDoc)) {
                return;
            }
            String minuta = clean(row.get("N° Minuta"));
            if (minuta.isBlank() || !cubagemMinutas.add(minuta)) {
                return;
            }

            cubagemTotal++;
            String cubadoPlanilha = normalizeText(row.get("CUBADO?"));
            boolean cubado = cubadoPlanilha.equals("sim")
                    || cubadoPlanilha.equals("s")
                    || cubadoPlanilha.equals("yes")
                    || cubadoPlanilha.equals("cubado");
            if (cubadoPlanilha.isBlank()) {
                cubado = parseDecimal(row.get("Total M3")).compareTo(BigDecimal.ZERO) > 0
                        || parseDecimal(row.get("Peso cubado")).compareTo(BigDecimal.ZERO) > 0;
            }
            if (cubado) {
                cubagemCubados++;
            }
            if (parseDecimal(row.get("Peso real")).compareTo(BigDecimal.ZERO) > 0) {
                cubagemPesoReal++;
            }
            cubagemDetalhes.put(minuta, mapOf(
                    "numeroMinuta", minuta,
                    "dataFrete", date.toString(),
                    "pagadorDocumento", pagadorDoc,
                    "totalM3", parseDecimal(row.get("Total M3")),
                    "pesoCubado", parseDecimal(row.get("Peso cubado")),
                    "pesoReal", parseDecimal(row.get("Peso real")),
                    "cubado", cubado
            ));
        }

        private void acceptSinistro(Map<String, String> row, LocalDate start, LocalDate end) {
            LocalDate date = parseDate(row.get("Data abertura"));
            if (!inPeriod(date, start, end)) {
                return;
            }
            String numero = clean(row.get("Nº do Sinistro"));
            if (numero.isBlank() || !sinistros.add(numero)) {
                return;
            }
            sinistrosTotal++;
            BigDecimal valor = parseDecimal(row.get("valor a pagar ao cliente"));
            valorIndenizado = valorIndenizado.add(valor);
            sinistrosDetalhes.put(numero, mapOf(
                    "numeroSinistro", numero,
                    "dataAbertura", date.toString(),
                    "valorAPagarCliente", valor
            ));
        }

        private void acceptFaturamento(Map<String, String> row, LocalDate start, LocalDate end) {
            LocalDate date = parseDate(row.get("Data do frete"));
            if (!inPeriod(date, start, end)) {
                return;
            }
            String minuta = clean(row.get("N° Minuta"));
            if (!minuta.isBlank()) {
                faturamentoMinutas.add(minuta);
                faturamentoPorMinuta.merge(minuta, parseDecimal(row.get("Total a receber")), (atual, valor) -> ((BigDecimal) atual).add((BigDecimal) valor));
            }
            faturamentoLinhas++;
            faturamentoBase = faturamentoBase.add(parseDecimal(row.get("Total a receber")));
        }

        private void acceptHorarioCorteApoio(Map<String, String> row) {
            String origem = clean(row.get("ORIGEM - SM"));
            String destino = clean(row.get("DESTINO - SM"));
            int minutos = parseTimeMinutes(row.get("HORÁRIO CORTE"));
            if (origem.isBlank() || destino.isBlank() || minutos < 0) {
                return;
            }

            String origemDestino = clean(row.get("Origem x Destino"));
            if (!origemDestino.isBlank()) {
                horariosCortePorRota.put(normalizeRouteKey(origemDestino), minutos);
            }
            horariosCortePorRota.put(normalizeRouteKey(origem + " x " + destino), minutos);
            horariosCortePorRota.put(normalizeRouteKey(origem + destino), minutos);
        }

        private void acceptHorarioCorte(Map<String, String> row, LocalDate start, LocalDate end) {
            LocalDate dataCorte = parseDate(row.get("Dt. Horario de Corte"));
            if (dataCorte == null) {
                dataCorte = parseDate(row.get("Dt. Hr. Fim Viagem"));
            }
            if (!inPeriod(dataCorte, start, end)) {
                return;
            }

            String codigo = clean(row.get("Código"));
            if (codigo.isBlank() || !horariosCodigos.add(codigo)) {
                return;
            }

            String origem = clean(row.get("Origem"));
            String destino = clean(row.get("Destino"));
            LocalDateTime inicio = parseDateTime(row.get("Dt. Hr. Início Viagem"));
            Integer minutos = horariosCortePorRota.get(normalizeRouteKey(origem + " x " + destino));
            if (minutos == null || inicio == null) {
                horariosSemCorte++;
                horariosDetalhes.put(codigo, mapOf(
                        "codigo", codigo,
                        "origem", origem,
                        "destino", destino,
                        "dataCorte", dataCorte.toString(),
                        "status", "SEM DADO"
                ));
                return;
            }

            LocalDateTime dataHoraCorte = dataCorte.atStartOfDay().plusMinutes(minutos);
            boolean dentro = !inicio.isAfter(dataHoraCorte);
            horariosTotalProgramado++;
            if (dentro) {
                horariosSaidasNoHorario++;
            } else {
                horariosSaidasForaHorario++;
            }
            horariosDetalhes.put(codigo, mapOf(
                    "codigo", codigo,
                    "origem", origem,
                    "destino", destino,
                    "dataHoraInicio", inicio.toString(),
                    "dataHoraCorte", dataHoraCorte.toString(),
                    "status", dentro ? "DENTRO" : "FORA"
            ));
        }

        private String toJson() {
            int coletoresTotal = manifestosEmitidos.size() + manifestosDescarregamento.size();
            int cubagemNaoCubados = Math.max(cubagemTotal - cubagemCubados, 0);
            BigDecimal valorIndenizadoAbs = valorIndenizado.abs().setScale(2, RoundingMode.HALF_UP);
            BigDecimal faturamento = faturamentoBase.setScale(2, RoundingMode.HALF_UP);

            Map<String, Object> root = new LinkedHashMap<>();
            root.put("performance", mapOf(
                    "totalEntregas", performanceTotal,
                    "entregasNoPrazo", performanceNoPrazo,
                    "entregasForaDoPrazo", performanceForaPrazo,
                    "entregasEmAberto", performanceEmAberto,
                    "pctNoPrazo", pct(performanceNoPrazo, performanceTotal, 4)
            ));
            root.put("coletores", mapOf(
                    "ordensConferencia", ordens.size(),
                    "manifestosEmitidos", manifestosEmitidos.size(),
                    "manifestosDescarregamento", manifestosDescarregamento.size(),
                    "totalManifestos", coletoresTotal,
                    "ordensIncompletas", ordensIncompletas.size(),
                    "pctUtilizacao", pct(ordens.size(), coletoresTotal, 4)
            ));
            root.put("cubagem", mapOf(
                    "totalFretes", cubagemTotal,
                    "fretesCubados", cubagemCubados,
                    "fretesNaoCubados", cubagemNaoCubados,
                    "fretesComPesoReal", cubagemPesoReal,
                    "pctCubagem", pct(cubagemCubados, cubagemTotal, 4),
                    "clientesExcluidos", clientesSemCubagem.size()
            ));
            root.put("indenizacao", mapOf(
                    "totalSinistros", sinistrosTotal,
                    "valorIndenizadoAbs", valorIndenizadoAbs,
                    "valorIndenizadoOriginal", valorIndenizado.setScale(2, RoundingMode.HALF_UP),
                    "faturamentoBase", faturamento,
                    "pctIndenizacao", pct(valorIndenizadoAbs, faturamento, 4),
                    "fretesFaturamento", faturamentoMinutas.size(),
                    "linhasFaturamento", faturamentoLinhas
            ));
            root.put("horariosCorte", mapOf(
                    "comparavel", true,
                    "totalProgramado", horariosTotalProgramado,
                    "saidasNoHorario", horariosSaidasNoHorario,
                    "saidasForaHorario", horariosSaidasForaHorario,
                    "semHorarioCorte", horariosSemCorte,
                    "pctNoHorario", pct(horariosSaidasNoHorario, horariosTotalProgramado, 4)
            ));
            root.put("details", mapOf(
                    "performanceMinutas", performanceDetalhes,
                    "coletoresOrdens", ordensDetalhes,
                    "coletoresManifestosEmitidos", manifestosEmitidosDetalhes,
                    "coletoresManifestosDescarregamento", manifestosDescarregamentoDetalhes,
                    "cubagemMinutas", cubagemDetalhes,
                    "indenizacaoSinistros", sinistrosDetalhes,
                    "indenizacaoFaturamentoPorMinuta", faturamentoPorMinuta,
                    "horariosCorteViagens", horariosDetalhes
            ));
            return json(root);
        }

        private static Map<String, Object> mapOf(Object... values) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (int i = 0; i < values.length; i += 2) {
                map.put((String) values[i], values[i + 1]);
            }
            return map;
        }
    }

    private static String json(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder out = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                out.append(json(entry.getKey().toString())).append(':').append(json(entry.getValue()));
            }
            return out.append('}').toString();
        }
        if (value instanceof Iterable<?> items) {
            StringBuilder out = new StringBuilder("[");
            boolean first = true;
            for (Object item : items) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                out.append(json(item));
            }
            return out.append(']').toString();
        }
        if (value instanceof Number number) {
            if (number instanceof BigDecimal decimal) {
                return decimal.stripTrailingZeros().toPlainString();
            }
            return number.toString();
        }
        if (value instanceof Boolean bool) {
            return bool.toString();
        }
        String text = value.toString();
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n") + "\"";
    }
}
`;

function parseArgs(argv) {
  const args = {};
  for (const raw of argv) {
    if (!raw.startsWith('--')) {
      continue;
    }
    const separator = raw.indexOf('=');
    if (separator < 0) {
      args[raw.slice(2)] = 'true';
    } else {
      args[raw.slice(2, separator)] = raw.slice(separator + 1);
    }
  }
  return args;
}

function parseDotEnv(filePath) {
  if (!existsSync(filePath)) {
    return {};
  }

  return readFileSync(filePath, 'utf8')
    .split(/\r?\n/u)
    .reduce((env, line) => {
      const trimmed = line.trim();
      if (!trimmed || trimmed.startsWith('#')) {
        return env;
      }
      const separator = trimmed.indexOf('=');
      if (separator < 0) {
        return env;
      }
      const key = trimmed.slice(0, separator).trim();
      let value = trimmed.slice(separator + 1).trim();
      if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
        value = value.slice(1, -1);
      }
      env[key] = value;
      return env;
    }, {});
}

function mergedEnv() {
  return {
    ...parseDotEnv(path.join(ROOT_DIR, 'backend', '.env')),
    ...parseDotEnv(path.join(ROOT_DIR, '.env')),
    ...process.env,
  };
}

function resolveCommand(name) {
  return name;
}

function runMaven(args) {
  const command = process.platform === 'win32' ? 'cmd.exe' : resolveCommand('mvn');
  const commandArgs = process.platform === 'win32'
    ? ['/d', '/s', '/c', 'mvn', ...args]
    : args;
  const result = spawnSync(command, commandArgs, {
    cwd: ROOT_DIR,
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'pipe'],
  });
  if (result.status !== 0) {
    throw new Error(`Falha ao montar classpath Maven.\nSTDOUT:\n${result.stdout}\nSTDERR:\n${result.stderr}`);
  }
}

function resolveJavaCommand(name) {
  const executable = process.platform === 'win32' ? `${name}.exe` : name;
  if (process.env.JAVA_HOME) {
    const candidate = path.join(process.env.JAVA_HOME, 'bin', executable);
    if (existsSync(candidate)) {
      return candidate;
    }
  }
  return name;
}

function buildClasspath(tempDir) {
  const classpathFile = path.join(tempDir, 'classpath.txt');
  runMaven([
    '-q',
    '-f',
    path.join(ROOT_DIR, 'backend', 'pom.xml'),
    'dependency:build-classpath',
    `-Dmdep.outputFile=${classpathFile}`,
    '-DincludeScope=runtime',
  ]);
  return readFileSync(classpathFile, 'utf8').trim();
}

function readXlsxMetrics({ xlsxPath, dataInicio, dataFim }) {
  const tempDir = mkdtempSync(path.join(os.tmpdir(), 'gestao-vista-xlsx-'));
  try {
    const sourcePath = path.join(tempDir, 'GestaoVistaXlsxReader.java');
    writeFileSync(sourcePath, JAVA_READER_SOURCE, 'utf8');
    const dependencyClasspath = buildClasspath(tempDir);

    execFileSync(resolveJavaCommand('javac'), [
      '-encoding',
      'UTF-8',
      '-cp',
      dependencyClasspath,
      '-d',
      tempDir,
      sourcePath,
    ], {
      cwd: ROOT_DIR,
      stdio: ['ignore', 'pipe', 'pipe'],
    });

    const classpath = [tempDir, dependencyClasspath].filter(Boolean).join(path.delimiter);
    const result = spawnSync(resolveJavaCommand('java'), [
      '-Djdk.xml.maxGeneralEntitySizeLimit=0',
      '-Djdk.xml.totalEntitySizeLimit=0',
      '-Djdk.xml.entityExpansionLimit=0',
      '-cp',
      classpath,
      'GestaoVistaXlsxReader',
      xlsxPath,
      dataInicio,
      dataFim,
    ], {
      cwd: ROOT_DIR,
      encoding: 'utf8',
      maxBuffer: 1024 * 1024 * 20,
    });

    if (result.status !== 0) {
      throw new Error(`Falha ao ler XLSX.\nSTDOUT:\n${result.stdout}\nSTDERR:\n${result.stderr}`);
    }

    const jsonStart = result.stdout.indexOf('{');
    const jsonEnd = result.stdout.lastIndexOf('}');
    if (jsonStart < 0 || jsonEnd < jsonStart) {
      throw new Error(`Leitor XLSX não retornou JSON válido: ${result.stdout}`);
    }
    return JSON.parse(result.stdout.slice(jsonStart, jsonEnd + 1));
  } finally {
    rmSync(tempDir, { recursive: true, force: true });
  }
}

function base64UrlJson(value) {
  return Buffer.from(JSON.stringify(value), 'utf8').toString('base64url');
}

function buildJwt(email, secret, alg) {
  const now = Math.floor(Date.now() / 1000);
  const header = { alg, typ: 'JWT' };
  const payload = { sub: email, iat: now, exp: now + 15 * 60 };
  const signingInput = `${base64UrlJson(header)}.${base64UrlJson(payload)}`;
  const digest = alg === 'HS512' ? 'sha512' : 'sha256';
  const signature = createHmac(digest, Buffer.from(secret, 'utf8')).update(signingInput).digest('base64url');
  return `${signingInput}.${signature}`;
}

async function fetchJson(url, token) {
  const response = await fetch(url, {
    headers: {
      Accept: 'application/json',
      Authorization: `Bearer ${token}`,
    },
  });
  const text = await response.text();
  if (!response.ok) {
    const error = new Error(`HTTP ${response.status} em ${url}: ${text}`);
    error.status = response.status;
    error.body = text;
    throw error;
  }
  return text ? JSON.parse(text) : null;
}

async function resolveToken(apiBaseUrl, email, secret) {
  const testUrl = new URL('/api/painel/indicadores-gestao-a-vista/performance-entrega/overview', apiBaseUrl);
  testUrl.searchParams.set('dataInicio', '2026-03-01');
  testUrl.searchParams.set('dataFim', '2026-03-01');

  const attempts = ['HS512', 'HS256'];
  let lastError = null;
  for (const alg of attempts) {
    const token = buildJwt(email, secret, alg);
    try {
      await fetchJson(testUrl, token);
      return { token, alg };
    } catch (error) {
      lastError = error;
      if (![401, 403].includes(error.status)) {
        throw error;
      }
    }
  }
  throw lastError ?? new Error('Não foi possível gerar um JWT aceito pela API.');
}

async function fetchDashboardMetrics(apiBaseUrl, token, dataInicio, dataFim) {
  const endpoints = {
    performance: '/api/painel/indicadores-gestao-a-vista/performance-entrega/overview',
    coletores: '/api/painel/indicadores-gestao-a-vista/utilizacao-coletores/overview',
    cubagem: '/api/painel/indicadores-gestao-a-vista/cubagem-mercadorias/overview',
    indenizacao: '/api/painel/indicadores-gestao-a-vista/indenizacao-mercadorias/overview',
    horariosCorte: '/api/painel/indicadores-gestao-a-vista/horarios-corte/overview',
  };
  const output = {};

  for (const [key, endpoint] of Object.entries(endpoints)) {
    const url = new URL(endpoint, apiBaseUrl);
    url.searchParams.set('dataInicio', dataInicio);
    url.searchParams.set('dataFim', dataFim);
    try {
      output[key] = { ok: true, data: await fetchJson(url, token) };
    } catch (error) {
      output[key] = {
        ok: false,
        status: error.status ?? null,
        error: error.message,
        body: error.body ?? null,
      };
    }
  }

  return output;
}

async function fetchPaginatedRows(apiBaseUrl, token, endpoint, dataInicio, dataFim) {
  const firstUrl = new URL(endpoint, apiBaseUrl);
  firstUrl.searchParams.set('dataInicio', dataInicio);
  firstUrl.searchParams.set('dataFim', dataFim);
  firstUrl.searchParams.set('pagina', '1');
  firstUrl.searchParams.set('tamanhoPagina', '100');

  const firstPage = await fetchJson(firstUrl, token);
  if (Array.isArray(firstPage)) {
    return {
      totalElementos: firstPage.length,
      totalPaginas: firstPage.length > 0 ? 1 : 0,
      rows: firstPage,
    };
  }

  const rows = [...(firstPage?.conteudo ?? [])];
  const totalPages = Number(firstPage?.totalPaginas ?? 0);

  for (let page = 2; page <= totalPages; page += 1) {
    const url = new URL(endpoint, apiBaseUrl);
    url.searchParams.set('dataInicio', dataInicio);
    url.searchParams.set('dataFim', dataFim);
    url.searchParams.set('pagina', String(page));
    url.searchParams.set('tamanhoPagina', '100');
    const result = await fetchJson(url, token);
    rows.push(...(result?.conteudo ?? []));
  }

  return {
    totalElementos: Number(firstPage?.totalElementos ?? rows.length),
    totalPaginas: totalPages,
    rows,
  };
}

async function fetchDashboardDetails(apiBaseUrl, token, dataInicio, dataFim) {
  const endpoints = {
    performance: '/api/painel/indicadores-gestao-a-vista/performance-entrega/diagnostico',
    coletores: '/api/painel/indicadores-gestao-a-vista/utilizacao-coletores/diagnostico',
    cubagem: '/api/painel/indicadores-gestao-a-vista/cubagem-mercadorias/diagnostico',
    indenizacao: '/api/painel/indicadores-gestao-a-vista/indenizacao-mercadorias/diagnostico',
    horariosCorte: '/api/painel/indicadores-gestao-a-vista/horarios-corte/tabela/paginada',
  };
  const output = {};

  for (const [key, endpoint] of Object.entries(endpoints)) {
    try {
      output[key] = {
        ok: true,
        ...(await fetchPaginatedRows(apiBaseUrl, token, endpoint, dataInicio, dataFim)),
      };
    } catch (error) {
      output[key] = {
        ok: false,
        status: error.status ?? null,
        error: error.message,
        body: error.body ?? null,
        rows: [],
      };
    }
  }

  return output;
}

function roundTo(value, decimals) {
  if (value === null || value === undefined || !Number.isFinite(Number(value))) {
    return null;
  }
  return Number(Number(value).toFixed(decimals));
}

function compareValues({ type, xlsxValue, dashboardValue, displayDecimals }) {
  const tolerance = METRIC_TOLERANCES[type] ?? 0;
  const left = xlsxValue === null || xlsxValue === undefined ? null : Number(xlsxValue);
  const right = dashboardValue === null || dashboardValue === undefined ? null : Number(dashboardValue);

  if (left === null || right === null || !Number.isFinite(left) || !Number.isFinite(right)) {
    return {
      status: left === right ? 'OK' : 'ERRO',
      diffAbs: null,
      diffPct: null,
      comparedXlsxValue: left,
      comparedDashboardValue: right,
    };
  }

  const comparedXlsxValue = type === 'percentage' ? roundTo(left, displayDecimals) : roundTo(left, type === 'currency' ? 2 : 0);
  const comparedDashboardValue = type === 'percentage' ? roundTo(right, displayDecimals) : roundTo(right, type === 'currency' ? 2 : 0);
  const diffAbs = roundTo(right - left, type === 'percentage' ? 4 : type === 'currency' ? 2 : 0);
  const diffPct = left === 0 ? (right === 0 ? 0 : null) : roundTo((diffAbs / Math.abs(left)) * 100, 4);
  const ok = Math.abs(comparedDashboardValue - comparedXlsxValue) <= tolerance;

  return {
    status: ok ? 'OK' : 'ERRO',
    diffAbs,
    diffPct,
    comparedXlsxValue,
    comparedDashboardValue,
  };
}

function metric({ indicator, key, label, type, xlsxValue, dashboardValue, displayDecimals = 0 }) {
  return {
    indicator,
    key,
    label,
    type,
    displayDecimals,
    xlsxValue,
    dashboardValue,
    ...compareValues({ type, xlsxValue, dashboardValue, displayDecimals }),
  };
}

function objectValues(value) {
  return value && typeof value === 'object' && !Array.isArray(value) ? Object.values(value) : [];
}

function objectKeys(value) {
  return value && typeof value === 'object' && !Array.isArray(value) ? Object.keys(value) : [];
}

function keyMapFromRows(rows, keySelector) {
  const out = new Map();
  for (const row of rows ?? []) {
    const key = keySelector(row);
    if (key !== null && key !== undefined && String(key).trim()) {
      out.set(String(key), row);
    }
  }
  return out;
}

function normalizeStatus(value) {
  const text = String(value ?? '').trim().toLowerCase();
  if (text === 'dentro do prazo') {
    return 'no prazo';
  }
  return text || 'em aberto';
}

function cents(value) {
  return Math.round(Number(value ?? 0) * 100);
}

function compareKeyMaps({ xlsxMap, dashboardMap, mismatch }) {
  const xlsxKeys = new Set(objectKeys(xlsxMap));
  const dashboardKeys = new Set(dashboardMap.keys());
  const missingInDashboard = [...xlsxKeys].filter(key => !dashboardKeys.has(key)).sort();
  const extraInDashboard = [...dashboardKeys].filter(key => !xlsxKeys.has(key)).sort();
  const commonKeys = [...xlsxKeys].filter(key => dashboardKeys.has(key)).sort();
  const divergentRows = [];

  if (typeof mismatch === 'function') {
    for (const key of commonKeys) {
      const diff = mismatch(xlsxMap[key], dashboardMap.get(key));
      if (diff) {
        divergentRows.push({ key, ...diff });
      }
    }
  }

  return {
    xlsxCount: xlsxKeys.size,
    dashboardCount: dashboardKeys.size,
    commonCount: commonKeys.length,
    missingInDashboardCount: missingInDashboard.length,
    extraInDashboardCount: extraInDashboard.length,
    divergentRowsCount: divergentRows.length,
    missingInDashboardSample: missingInDashboard.slice(0, 100),
    extraInDashboardSample: extraInDashboard.slice(0, 100),
    divergentRowsSample: divergentRows.slice(0, 100),
  };
}

function buildReconciliation(xlsx, dashboardDetails) {
  const details = xlsx.details ?? {};
  const performanceMap = keyMapFromRows(dashboardDetails.performance?.rows, row => row.numeroMinuta);
  const cubagemMap = keyMapFromRows(dashboardDetails.cubagem?.rows, row => row.numeroMinuta);
  const indenizacaoMap = keyMapFromRows(dashboardDetails.indenizacao?.rows, row => row.numeroSinistro);
  const coletoresMap = keyMapFromRows(dashboardDetails.coletores?.rows, row => row.chave);
  const horariosMap = keyMapFromRows(dashboardDetails.horariosCorte?.rows, row => row.id == null ? null : String(row.id));

  return {
    performance: dashboardDetails.performance?.ok
      ? compareKeyMaps({
          xlsxMap: details.performanceMinutas,
          dashboardMap: performanceMap,
          mismatch: (xlsxRow, dashboardRow) => {
            const xlsxStatus = normalizeStatus(xlsxRow.perfStatus);
            const dashboardStatus = normalizeStatus(dashboardRow.performanceStatus);
            return xlsxStatus === dashboardStatus ? null : {
              field: 'performanceStatus',
              xlsxValue: xlsxRow.perfStatus,
              dashboardValue: dashboardRow.performanceStatus,
            };
          },
        })
      : { error: dashboardDetails.performance?.error ?? 'Detalhe da API indisponível.' },
    cubagem: dashboardDetails.cubagem?.ok
      ? compareKeyMaps({
          xlsxMap: details.cubagemMinutas,
          dashboardMap: cubagemMap,
          mismatch: (xlsxRow, dashboardRow) => {
            if (Boolean(xlsxRow.cubado) === Boolean(dashboardRow.cubado)) {
              return null;
            }
            return {
              field: 'cubado',
              xlsxValue: xlsxRow.cubado,
              dashboardValue: dashboardRow.cubado,
            };
          },
        })
      : { error: dashboardDetails.cubagem?.error ?? 'Detalhe da API indisponível.' },
    indenizacaoSinistros: dashboardDetails.indenizacao?.ok
      ? compareKeyMaps({
          xlsxMap: details.indenizacaoSinistros,
          dashboardMap: indenizacaoMap,
          mismatch: (xlsxRow, dashboardRow) => {
            if (cents(xlsxRow.valorAPagarCliente) === cents(dashboardRow.resultadoFinalOriginal)) {
              return null;
            }
            return {
              field: 'valorAPagarCliente',
              xlsxValue: xlsxRow.valorAPagarCliente,
              dashboardValue: dashboardRow.resultadoFinalOriginal,
            };
          },
        })
      : { error: dashboardDetails.indenizacao?.error ?? 'Detalhe da API indisponível.' },
    horariosCorte: dashboardDetails.horariosCorte?.ok
      ? compareKeyMaps({
          xlsxMap: details.horariosCorteViagens,
          dashboardMap: horariosMap,
          mismatch: (xlsxRow, dashboardRow) => {
            const xlsxStatus = normalizeStatus(xlsxRow.status);
            const dashboardStatus = dashboardRow.saiuNoHorario === true ? 'DENTRO' : dashboardRow.saiuNoHorario === false ? 'FORA' : 'SEM DADO';
            return xlsxStatus === normalizeStatus(dashboardStatus) ? null : {
              field: 'saiuNoHorario',
              xlsxValue: xlsxRow.status,
              dashboardValue: dashboardStatus,
            };
          },
        })
      : { error: dashboardDetails.horariosCorte?.error ?? 'Detalhe da API indisponível.' },
    coletores: {
      xlsxOrdensCount: objectValues(details.coletoresOrdens).length,
      xlsxManifestosEmitidosCount: objectValues(details.coletoresManifestosEmitidos).length,
      xlsxManifestosDescarregamentoCount: objectValues(details.coletoresManifestosDescarregamento).length,
      dashboardGroupedRowsCount: coletoresMap.size,
      note: dashboardDetails.coletores?.ok
        ? 'A API expõe Coletores agregado por dia/filial; ordens e manifestos brutos ficam disponíveis somente no detalhe XLSX deste JSON.'
        : dashboardDetails.coletores?.error ?? 'Detalhe da API indisponível.',
    },
    indenizacaoFaturamento: {
      xlsxFretesFaturamentoCount: objectValues(details.indenizacaoFaturamentoPorMinuta).length,
      note: 'O dashboard não expõe tabela de fretes usada no denominador; a reconciliação exata do faturamento exige SQL/ETL sobre vw_fretes_powerbi ou endpoint diagnóstico interno.',
    },
  };
}

function dashboardValue(apiResult, selector) {
  if (!apiResult?.ok) {
    return null;
  }
  return selector(apiResult.data);
}

function buildMetricRows(xlsx, dashboard) {
  const performanceDash = dashboard.performance;
  const coletoresDash = dashboard.coletores;
  const cubagemDash = dashboard.cubagem;
  const indenizacaoDash = dashboard.indenizacao;
  const horariosDash = dashboard.horariosCorte;

  const performanceEmAbertoDash = dashboardValue(performanceDash, data =>
    Math.max((data.totalEntregas ?? 0) - (data.entregasNoPrazo ?? 0) - (data.entregasForaDoPrazo ?? 0), 0));
  const cubagemNaoCubadosDash = dashboardValue(cubagemDash, data =>
    Math.max((data.totalFretes ?? 0) - (data.fretesCubados ?? 0), 0));

  return [
    metric({ indicator: 'Performance de Entrega', key: 'performance.totalEntregas', label: 'Total de entregas', type: 'count', xlsxValue: xlsx.performance.totalEntregas, dashboardValue: dashboardValue(performanceDash, d => d.totalEntregas) }),
    metric({ indicator: 'Performance de Entrega', key: 'performance.entregasNoPrazo', label: 'Entregas no prazo', type: 'count', xlsxValue: xlsx.performance.entregasNoPrazo, dashboardValue: dashboardValue(performanceDash, d => d.entregasNoPrazo) }),
    metric({ indicator: 'Performance de Entrega', key: 'performance.entregasForaDoPrazo', label: 'Entregas fora do prazo', type: 'count', xlsxValue: xlsx.performance.entregasForaDoPrazo, dashboardValue: dashboardValue(performanceDash, d => d.entregasForaDoPrazo) }),
    metric({ indicator: 'Performance de Entrega', key: 'performance.entregasEmAberto', label: 'Entregas em aberto', type: 'count', xlsxValue: xlsx.performance.entregasEmAberto, dashboardValue: performanceEmAbertoDash }),
    metric({ indicator: 'Performance de Entrega', key: 'performance.pctNoPrazo', label: '% no prazo', type: 'percentage', displayDecimals: 1, xlsxValue: xlsx.performance.pctNoPrazo, dashboardValue: dashboardValue(performanceDash, d => d.pctNoPrazo) }),

    metric({ indicator: 'Utilização dos Coletores', key: 'coletores.ordensConferencia', label: 'Ordens de conferência', type: 'count', xlsxValue: xlsx.coletores.ordensConferencia, dashboardValue: dashboardValue(coletoresDash, d => d.manifestosBipados) }),
    metric({ indicator: 'Utilização dos Coletores', key: 'coletores.manifestosEmitidos', label: 'Manifestos emitidos', type: 'count', xlsxValue: xlsx.coletores.manifestosEmitidos, dashboardValue: dashboardValue(coletoresDash, d => d.manifestosEmitidos) }),
    metric({ indicator: 'Utilização dos Coletores', key: 'coletores.manifestosDescarregamento', label: 'Manifestos descarregamento', type: 'count', xlsxValue: xlsx.coletores.manifestosDescarregamento, dashboardValue: dashboardValue(coletoresDash, d => d.manifestosDescarregamento) }),
    metric({ indicator: 'Utilização dos Coletores', key: 'coletores.totalManifestos', label: 'Manifestos bipáveis', type: 'count', xlsxValue: xlsx.coletores.totalManifestos, dashboardValue: dashboardValue(coletoresDash, d => d.totalManifestos) }),
    metric({ indicator: 'Utilização dos Coletores', key: 'coletores.ordensIncompletas', label: 'Ordens incompletas', type: 'count', xlsxValue: xlsx.coletores.ordensIncompletas, dashboardValue: dashboardValue(coletoresDash, d => d.manifestosIncompletos) }),
    metric({ indicator: 'Utilização dos Coletores', key: 'coletores.pctUtilizacao', label: '% utilização', type: 'percentage', displayDecimals: 1, xlsxValue: xlsx.coletores.pctUtilizacao, dashboardValue: dashboardValue(coletoresDash, d => d.pctUtilizacao) }),

    metric({ indicator: 'Cubagem de Mercadorias', key: 'cubagem.totalFretes', label: 'Total de minutas', type: 'count', xlsxValue: xlsx.cubagem.totalFretes, dashboardValue: dashboardValue(cubagemDash, d => d.totalFretes) }),
    metric({ indicator: 'Cubagem de Mercadorias', key: 'cubagem.fretesCubados', label: 'Minutas cubadas', type: 'count', xlsxValue: xlsx.cubagem.fretesCubados, dashboardValue: dashboardValue(cubagemDash, d => d.fretesCubados) }),
    metric({ indicator: 'Cubagem de Mercadorias', key: 'cubagem.fretesNaoCubados', label: 'Minutas sem cubagem', type: 'count', xlsxValue: xlsx.cubagem.fretesNaoCubados, dashboardValue: cubagemNaoCubadosDash }),
    metric({ indicator: 'Cubagem de Mercadorias', key: 'cubagem.fretesComPesoReal', label: 'Minutas com peso real', type: 'count', xlsxValue: xlsx.cubagem.fretesComPesoReal, dashboardValue: dashboardValue(cubagemDash, d => d.fretesComPesoReal) }),
    metric({ indicator: 'Cubagem de Mercadorias', key: 'cubagem.pctCubagem', label: '% cubagem', type: 'percentage', displayDecimals: 1, xlsxValue: xlsx.cubagem.pctCubagem, dashboardValue: dashboardValue(cubagemDash, d => d.pctCubagem) }),

    metric({ indicator: 'Indenização de Mercadorias', key: 'indenizacao.totalSinistros', label: 'Total de sinistros', type: 'count', xlsxValue: xlsx.indenizacao.totalSinistros, dashboardValue: dashboardValue(indenizacaoDash, d => d.totalSinistros) }),
    metric({ indicator: 'Indenização de Mercadorias', key: 'indenizacao.valorIndenizadoAbs', label: 'Valor indenizado', type: 'currency', xlsxValue: xlsx.indenizacao.valorIndenizadoAbs, dashboardValue: dashboardValue(indenizacaoDash, d => d.valorIndenizadoAbs) }),
    metric({ indicator: 'Indenização de Mercadorias', key: 'indenizacao.faturamentoBase', label: 'Faturamento base', type: 'currency', xlsxValue: xlsx.indenizacao.faturamentoBase, dashboardValue: dashboardValue(indenizacaoDash, d => d.faturamentoBase) }),
    metric({ indicator: 'Indenização de Mercadorias', key: 'indenizacao.pctIndenizacao', label: '% indenização', type: 'percentage', displayDecimals: 2, xlsxValue: xlsx.indenizacao.pctIndenizacao, dashboardValue: dashboardValue(indenizacaoDash, d => d.pctIndenizacao) }),

    metric({ indicator: 'Horários de Corte', key: 'horariosCorte.totalProgramado', label: 'Total programado', type: 'count', xlsxValue: xlsx.horariosCorte.totalProgramado, dashboardValue: dashboardValue(horariosDash, d => d.totalProgramado) }),
    metric({ indicator: 'Horários de Corte', key: 'horariosCorte.saidasNoHorario', label: 'Saídas no horário', type: 'count', xlsxValue: xlsx.horariosCorte.saidasNoHorario, dashboardValue: dashboardValue(horariosDash, d => d.saidasNoHorario) }),
    metric({ indicator: 'Horários de Corte', key: 'horariosCorte.saidasForaHorario', label: 'Saídas fora do horário', type: 'count', xlsxValue: xlsx.horariosCorte.saidasForaHorario, dashboardValue: dashboardValue(horariosDash, d => Math.max((d.totalProgramado ?? 0) - (d.saidasNoHorario ?? 0), 0)) }),
    metric({ indicator: 'Horários de Corte', key: 'horariosCorte.pctNoHorario', label: '% no horário', type: 'percentage', displayDecimals: 1, xlsxValue: xlsx.horariosCorte.pctNoHorario, dashboardValue: dashboardValue(horariosDash, d => d.pctNoHorario) }),
  ];
}

function possibleCause(row, dashboard) {
  const apiKey = row.key.split('.')[0];
  if (!dashboard[apiKey]?.ok) {
    return `API indisponível ou erro no endpoint do indicador: ${dashboard[apiKey]?.error ?? 'erro não informado'}`;
  }

  if (row.indicator === 'Performance de Entrega') {
    return 'Revisar filtro por Previsão de Entrega, exclusão de cancelados, tratamento de EM ABERTO e deduplicação por N° Minuta; se a regra estiver igual, a causa provável é snapshot SQL/API diferente do XLSX.';
  }
  if (row.indicator === 'Utilização dos Coletores') {
    return 'Revisar numerador por ordens de conferência, tipos elegíveis, deduplicação de N° Ordem e denominador por manifestos emitidos/descarregamento; atenção à validação de filiais e locais de descarregamento.';
  }
  if (row.indicator === 'Cubagem de Mercadorias') {
    return 'Revisar aplicação da lista Clientes_sem_Cub por Pagador do frete/Documento, exclusão de cancelados, deduplicação por N° Minuta e regra oficial do XLSX para CUBADO? baseada em Total M3.';
  }
  if (row.indicator === 'Indenização de Mercadorias') {
    return 'Revisar filtro por Data abertura, campo valor a pagar ao cliente, deduplicação por Nº do Sinistro e denominador de faturamento por Data do frete/Total a receber.';
  }
  if (row.indicator === 'Horários de Corte') {
    return 'Revisar mapeamento Origem x Destino contra HC (Apoio), data de corte pelo fim da viagem, timezone/localtime e rotas sem horário calculável.';
  }
  return 'Dado ausente ou regra não mapeada.';
}

function formatNumber(value, decimals = 2) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-';
  }
  return new Intl.NumberFormat('pt-BR', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(Number(value));
}

function formatValue(row, value, useCompared = false) {
  if (row.status === 'NAO_COMPARAVEL') {
    return value === null || value === undefined ? 'DADO AUSENTE / NÃO COMPARÁVEL' : String(value);
  }
  if (value === null || value === undefined) {
    return '-';
  }
  if (row.type === 'currency') {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(Number(value));
  }
  if (row.type === 'percentage') {
    const decimals = useCompared ? row.displayDecimals : 4;
    return `${formatNumber(value, decimals)}%`;
  }
  return new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 0 }).format(Number(value));
}

function formatDiff(row) {
  if (row.status === 'NAO_COMPARAVEL') {
    return '-';
  }
  if (row.diffAbs === null || row.diffAbs === undefined) {
    return '-';
  }
  const sign = row.diffAbs > 0 ? '+' : '';
  if (row.type === 'currency') {
    return `${sign}${new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(row.diffAbs)}`;
  }
  if (row.type === 'percentage') {
    return `${sign}${formatNumber(row.diffAbs, 4)} p.p.`;
  }
  return `${sign}${new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 0 }).format(row.diffAbs)}`;
}

function formatDiffPct(row) {
  if (row.status === 'NAO_COMPARAVEL' || row.diffPct === null || row.diffPct === undefined) {
    return '-';
  }
  const sign = row.diffPct > 0 ? '+' : '';
  return `${sign}${formatNumber(row.diffPct, 4)}%`;
}

function renderMarkdown(context, xlsxMetrics, dashboardMetrics, rows, reconciliation) {
  const errors = rows.filter(row => row.status === 'ERRO');
  const comparableRows = rows.filter(row => row.status !== 'NAO_COMPARAVEL');
  const okRows = comparableRows.filter(row => row.status === 'OK');
  const apiFailures = Object.entries(dashboardMetrics).filter(([, value]) => !value.ok);

  const lines = [];
  lines.push('# Validação XLSX vs Dashboard - Gestão à Vista');
  lines.push('');
  lines.push(`- Período: ${context.dataInicio} a ${context.dataFim}`);
  lines.push(`- XLSX fonte da verdade: \`${context.xlsxPath}\``);
  lines.push(`- Dashboard/API: \`${context.apiBaseUrl}\``);
  lines.push(`- JWT técnico: gerado em memória com ${context.jwtAlg}; login não foi executado.`);
  lines.push(`- Execução: ${context.executedAt}`);
  lines.push('');
  lines.push('## Resumo');
  lines.push('');
  lines.push(`- Métricas comparáveis: ${comparableRows.length}`);
  lines.push(`- OK: ${okRows.length}`);
  lines.push(`- ERRO: ${errors.length}`);
  lines.push('- Horários de Corte: DADO AUSENTE / NÃO COMPARÁVEL no XLSX.');
  if (apiFailures.length > 0) {
    lines.push(`- Endpoints com erro: ${apiFailures.map(([key, value]) => `${key} (${value.status ?? 'sem status'})`).join(', ')}`);
  }
  lines.push('');
  lines.push('## Tabela Final');
  lines.push('');
  lines.push('| Indicador | Métrica | Valor XLSX | Valor Dashboard | Diferença | Diferença % | Status |');
  lines.push('| --- | --- | ---: | ---: | ---: | ---: | --- |');
  for (const row of rows) {
    const status = row.status === 'NAO_COMPARAVEL' ? 'N/A' : row.status;
    const xlsxValue = row.status === 'NAO_COMPARAVEL' ? 'DADO AUSENTE' : formatValue(row, row.xlsxValue);
    const dashboardValue = row.status === 'NAO_COMPARAVEL' ? 'NÃO COMPARADO' : formatValue(row, row.dashboardValue, row.type === 'percentage');
    lines.push(`| ${row.indicator} | ${row.label} | ${xlsxValue} | ${dashboardValue} | ${formatDiff(row)} | ${formatDiffPct(row)} | ${status} |`);
  }
  lines.push('');
  lines.push('## Indicadores com ERRO');
  lines.push('');
  if (errors.length === 0) {
    lines.push('- Nenhuma divergência encontrada nas métricas comparáveis.');
  } else {
    for (const row of errors) {
      lines.push(`- ${row.indicator} / ${row.label}: XLSX ${formatValue(row, row.xlsxValue)} vs Dashboard ${formatValue(row, row.dashboardValue, row.type === 'percentage')} (${formatDiff(row)}; ${formatDiffPct(row)}). Hipótese: ${possibleCause(row, dashboardMetrics)}`);
    }
  }
  lines.push('');
  lines.push('## Dados Ausentes ou Ambíguos');
  lines.push('');
  lines.push(`- Horários de Corte ignora ${xlsxMetrics.horariosCorte.semHorarioCorte ?? 0} viagem(ns) do XLSX sem horário de corte mapeável em HC (Apoio) ou sem início real.`);
  lines.push('- Percentuais usam o arredondamento visual do dashboard para Status, mas a diferença exibida acima mantém a diferença bruta em pontos percentuais.');
  lines.push('');
  lines.push('## Reconciliação Detalhada');
  lines.push('');
  lines.push('| Base | XLSX | Dashboard/API | Comum | Faltando no dashboard | Sobrando no dashboard | Divergente por campo |');
  lines.push('| --- | ---: | ---: | ---: | ---: | ---: | ---: |');
  for (const [label, detail] of [
    ['Performance por N° Minuta', reconciliation.performance],
    ['Cubagem por N° Minuta', reconciliation.cubagem],
    ['Indenização por Nº do Sinistro', reconciliation.indenizacaoSinistros],
    ['Horários por Código Raster', reconciliation.horariosCorte],
  ]) {
    if (detail?.error) {
      lines.push(`| ${label} | - | - | - | - | - | - |`);
    } else {
      lines.push(`| ${label} | ${detail.xlsxCount} | ${detail.dashboardCount} | ${detail.commonCount} | ${detail.missingInDashboardCount} | ${detail.extraInDashboardCount} | ${detail.divergentRowsCount} |`);
    }
  }
  lines.push('');
  lines.push('- Amostras de chaves faltantes/extras/divergentes ficam no JSON gerado, em `reconciliation`.');
  lines.push(`- Coletores: XLSX detalhado contém ${reconciliation.coletores.xlsxOrdensCount} ordens, ${reconciliation.coletores.xlsxManifestosEmitidosCount} manifestos emitidos e ${reconciliation.coletores.xlsxManifestosDescarregamentoCount} locais de descarregamento; a API expõe ${reconciliation.coletores.dashboardGroupedRowsCount} linhas agrupadas por dia/filial.`);
  lines.push(`- Indenização/faturamento: ${reconciliation.indenizacaoFaturamento.xlsxFretesFaturamentoCount} minutas de faturamento no XLSX; a API atual só expõe o total agregado do denominador.`);
  lines.push('');
  lines.push('## Plano de Correção');
  lines.push('');
  lines.push('- SQL/views: revisar as colunas de período oficiais por indicador, chaves de deduplicação e equivalência de campos do XLSX com as views `vw_*_powerbi`.');
  lines.push('- Backend: alinhar os services de Gestão à Vista às regras do XLSX, principalmente filtros de data, exclusões, agrupamentos, campos financeiros e nomenclatura de DTOs.');
  lines.push('- ETL/importação: garantir snapshot rastreável da origem ESL/Data Export e validar listas de apoio como `Clientes_sem_Cub` antes de publicar o dashboard.');
  lines.push('- Frontend: manter os cards como simples formatação da API e revisar labels quando o campo interno não representar mais o conceito exibido.');
  lines.push('- Validação diária: agendar este script para gerar `.md` e `.json`; falhar o job se qualquer métrica comparável ficar `ERRO`.');
  lines.push('');

  return lines.join('\n');
}

function artifactBaseName(dataInicio, dataFim) {
  return `validacao-gestao-vista-xlsx-dashboard-${dataInicio}_${dataFim}`;
}

function normalizePathForReport(filePath) {
  return path.relative(ROOT_DIR, filePath) || filePath;
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const env = mergedEnv();
  const dataInicio = args.dataInicio ?? '2026-03-01';
  const dataFim = args.dataFim ?? '2026-03-31';
  const xlsxPath = path.resolve(ROOT_DIR, args.xlsx ?? DEFAULT_XLSX);
  const apiBaseUrl = args.apiBaseUrl ?? DEFAULT_API_BASE_URL;
  const apiUserEmail = args.apiUserEmail
    ?? env.ACESSO_USUARIO_SUPREMO_EMAIL
    ?? env.API_USER_EMAIL
    ?? env.USER_EMAIL;
  const jwtSecret = args.jwtSecret ?? env.JWT_SECRET;
  const failOnError = args.failOnError !== 'false';

  if (!existsSync(xlsxPath)) {
    throw new Error(`XLSX não encontrado: ${xlsxPath}`);
  }
  if (!apiUserEmail) {
    throw new Error('Informe --apiUserEmail ou defina ACESSO_USUARIO_SUPREMO_EMAIL/API_USER_EMAIL no .env.');
  }
  if (!jwtSecret) {
    throw new Error('JWT_SECRET não encontrado no .env.');
  }

  const xlsxMetrics = readXlsxMetrics({ xlsxPath, dataInicio, dataFim });
  const { token, alg } = await resolveToken(apiBaseUrl, apiUserEmail, jwtSecret);
  const dashboardMetrics = await fetchDashboardMetrics(apiBaseUrl, token, dataInicio, dataFim);
  const dashboardDetails = await fetchDashboardDetails(apiBaseUrl, token, dataInicio, dataFim);
  const rows = buildMetricRows(xlsxMetrics, dashboardMetrics);
  const reconciliation = buildReconciliation(xlsxMetrics, dashboardDetails);

  const context = {
    dataInicio,
    dataFim,
    xlsxPath: normalizePathForReport(xlsxPath),
    apiBaseUrl,
    apiUserEmail,
    jwtAlg: alg,
    executedAt: new Intl.DateTimeFormat('pt-BR', {
      dateStyle: 'short',
      timeStyle: 'medium',
      hour12: false,
    }).format(new Date()),
  };

  const markdown = renderMarkdown(context, xlsxMetrics, dashboardMetrics, rows, reconciliation);
  mkdirSync(REPORTS_DIR, { recursive: true });
  const baseName = artifactBaseName(dataInicio, dataFim);
  const markdownPath = path.join(REPORTS_DIR, `${baseName}.md`);
  const jsonPath = path.join(REPORTS_DIR, `${baseName}.json`);
  const errors = rows.filter(row => row.status === 'ERRO');

  writeFileSync(markdownPath, markdown, 'utf8');
  writeFileSync(jsonPath, JSON.stringify({
    context,
    summary: {
      comparableMetrics: rows.filter(row => row.status !== 'NAO_COMPARAVEL').length,
      ok: rows.filter(row => row.status === 'OK').length,
      erro: errors.length,
      naoComparavel: rows.filter(row => row.status === 'NAO_COMPARAVEL').length,
    },
    xlsxMetrics,
    dashboardMetrics,
    dashboardDetails,
    reconciliation,
    rows,
  }, null, 2), 'utf8');

  console.log(JSON.stringify({
    markdownPath,
    jsonPath,
    comparableMetrics: rows.filter(row => row.status !== 'NAO_COMPARAVEL').length,
    ok: rows.filter(row => row.status === 'OK').length,
    erro: errors.length,
    naoComparavel: rows.filter(row => row.status === 'NAO_COMPARAVEL').length,
  }, null, 2));

  if (failOnError && errors.length > 0) {
    process.exitCode = 1;
  }
}

main().catch((error) => {
  console.error(error instanceof Error ? error.stack ?? error.message : String(error));
  process.exitCode = 1;
});
