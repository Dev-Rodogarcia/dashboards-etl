package com.dashboard.api.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class ExcelExportWriter {

    private static final String COLUNA_INTERNA = "__rn";
    private static final String SHEET_NAME = "Exportacao";
    private static final int MAX_COLUMN_WIDTH = 80 * 256;

    public void escreverResultSet(OutputStream outputStream, ResultSet resultSet) throws SQLException, IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            CellStyle headerStyle = headerStyle(workbook);
            List<Integer> colunas = colunasExportaveis(resultSet.getMetaData());
            escreverLinha(sheet.createRow(0), headers(resultSet.getMetaData(), colunas), headerStyle);

            int rowIndex = 1;
            while (resultSet.next()) {
                List<Object> valores = new ArrayList<>(colunas.size());
                for (Integer coluna : colunas) {
                    valores.add(resultSet.getObject(coluna));
                }
                escreverLinha(sheet.createRow(rowIndex++), valores, null);
            }

            finalizarPlanilha(sheet, colunas.size());
            workbook.write(outputStream);
        }
    }

    public void escreverBeans(OutputStream outputStream, List<?> rows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            if (rows.isEmpty()) {
                workbook.write(outputStream);
                return;
            }

            Class<?> tipo = rows.get(0).getClass();
            if (!tipo.isRecord()) {
                throw new IllegalArgumentException("Exportacao Excel de objetos suporta records Java");
            }

            RecordComponent[] componentes = tipo.getRecordComponents();
            List<String> headers = new ArrayList<>(componentes.length);
            for (RecordComponent componente : componentes) {
                headers.add(componente.getName());
            }
            escreverLinha(sheet.createRow(0), headers, headerStyle(workbook));

            try {
                for (int i = 0; i < rows.size(); i++) {
                    List<Object> valores = new ArrayList<>(componentes.length);
                    for (RecordComponent componente : componentes) {
                        valores.add(componente.getAccessor().invoke(rows.get(i)));
                    }
                    escreverLinha(sheet.createRow(i + 1), valores, null);
                }
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Nao foi possivel ler record para exportacao Excel", ex);
            }

            finalizarPlanilha(sheet, componentes.length);
            workbook.write(outputStream);
        }
    }

    private void escreverLinha(Row row, List<?> valores, CellStyle style) {
        for (int i = 0; i < valores.size(); i++) {
            Cell cell = row.createCell(i);
            if (style != null) {
                cell.setCellStyle(style);
            }
            escreverValor(cell, valores.get(i));
        }
    }

    private void escreverValor(Cell cell, Object valor) {
        if (valor == null) {
            cell.setBlank();
            return;
        }
        if (valor instanceof BigDecimal bigDecimal) {
            cell.setCellValue(bigDecimal.doubleValue());
            return;
        }
        if (valor instanceof Number number) {
            cell.setCellValue(number.doubleValue());
            return;
        }
        if (valor instanceof Boolean bool) {
            cell.setCellValue(bool);
            return;
        }
        if (valor instanceof LocalDate data) {
            cell.setCellValue(data.format(DateTimeFormatter.ISO_LOCAL_DATE));
            return;
        }
        if (valor instanceof LocalDateTime dataHora) {
            cell.setCellValue(dataHora.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            return;
        }
        if (valor instanceof OffsetDateTime dataHoraOffset) {
            cell.setCellValue(dataHoraOffset.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            return;
        }
        if (valor instanceof LocalTime hora) {
            cell.setCellValue(hora.format(DateTimeFormatter.ISO_LOCAL_TIME));
            return;
        }
        cell.setCellValue(String.valueOf(valor));
    }

    private CellStyle headerStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private void finalizarPlanilha(Sheet sheet, int totalColunas) {
        sheet.createFreezePane(0, 1);
        for (int i = 0; i < totalColunas; i++) {
            sheet.autoSizeColumn(i);
            if (sheet.getColumnWidth(i) > MAX_COLUMN_WIDTH) {
                sheet.setColumnWidth(i, MAX_COLUMN_WIDTH);
            }
        }
    }

    private List<Integer> colunasExportaveis(ResultSetMetaData metaData) throws SQLException {
        List<Integer> colunas = new ArrayList<>();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String label = metaData.getColumnLabel(i);
            if (!COLUNA_INTERNA.equals(label.toLowerCase(Locale.ROOT))) {
                colunas.add(i);
            }
        }
        return colunas;
    }

    private List<String> headers(ResultSetMetaData metaData, List<Integer> colunas) throws SQLException {
        List<String> headers = new ArrayList<>(colunas.size());
        for (Integer coluna : colunas) {
            headers.add(metaData.getColumnLabel(coluna));
        }
        return headers;
    }
}
