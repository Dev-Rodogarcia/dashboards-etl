package com.dashboard.api.service;

import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
public class CsvExportWriter {

    private static final String COLUNA_INTERNA = "__rn";
    private static final char SEPARADOR = ';';
    private static final String QUEBRA_LINHA = "\r\n";
    private static final char UTF8_BOM = '\ufeff';

    public void escreverResultSet(OutputStream outputStream, ResultSet resultSet) throws SQLException, IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
        writer.write(UTF8_BOM);

        ResultSetMetaData metaData = resultSet.getMetaData();
        List<Integer> colunas = colunasExportaveis(metaData);
        escreverLinha(writer, headers(metaData, colunas));

        while (resultSet.next()) {
            List<Object> valores = new ArrayList<>(colunas.size());
            for (Integer coluna : colunas) {
                valores.add(resultSet.getObject(coluna));
            }
            escreverLinha(writer, valores);
        }

        writer.flush();
    }

    public void escreverBeans(OutputStream outputStream, List<?> rows) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
        writer.write(UTF8_BOM);

        if (rows.isEmpty()) {
            writer.flush();
            return;
        }

        Class<?> tipo = rows.get(0).getClass();
        if (!tipo.isRecord()) {
            throw new IllegalArgumentException("Exportacao CSV de objetos suporta records Java");
        }

        RecordComponent[] componentes = tipo.getRecordComponents();
        List<String> headers = new ArrayList<>(componentes.length);
        for (RecordComponent componente : componentes) {
            headers.add(componente.getName());
        }
        escreverLinha(writer, headers);

        try {
            for (Object row : rows) {
                List<Object> valores = new ArrayList<>(componentes.length);
                for (RecordComponent componente : componentes) {
                    valores.add(componente.getAccessor().invoke(row));
                }
                escreverLinha(writer, valores);
            }
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Nao foi possivel ler record para exportacao CSV", ex);
        }

        writer.flush();
    }

    private void escreverLinha(BufferedWriter writer, List<?> valores) throws IOException {
        for (int i = 0; i < valores.size(); i++) {
            if (i > 0) {
                writer.write(SEPARADOR);
            }
            writer.write(escapar(formatarValor(valores.get(i))));
        }
        writer.write(QUEBRA_LINHA);
    }

    private String formatarValor(Object valor) {
        if (valor == null) {
            return "";
        }
        if (valor instanceof BigDecimal bigDecimal) {
            return bigDecimal.toPlainString();
        }
        if (valor instanceof LocalDate data) {
            return data.format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        if (valor instanceof LocalDateTime dataHora) {
            return dataHora.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        if (valor instanceof OffsetDateTime dataHoraOffset) {
            return dataHoraOffset.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
        if (valor instanceof LocalTime hora) {
            return hora.format(DateTimeFormatter.ISO_LOCAL_TIME);
        }
        return String.valueOf(valor);
    }

    private String escapar(String valorOriginal) {
        String valor = protegerFormula(valorOriginal);
        boolean precisaAspas = valor.indexOf(SEPARADOR) >= 0
                || valor.indexOf('"') >= 0
                || valor.indexOf('\r') >= 0
                || valor.indexOf('\n') >= 0
                || comecaOuTerminaComEspaco(valor);

        if (!precisaAspas) {
            return valor;
        }

        return "\"" + valor.replace("\"", "\"\"") + "\"";
    }

    private String protegerFormula(String valor) {
        if (valor.isEmpty()) {
            return valor;
        }

        return switch (valor.charAt(0)) {
            case '=', '+', '-', '@' -> "'" + valor;
            default -> valor;
        };
    }

    private boolean comecaOuTerminaComEspaco(String valor) {
        return !valor.isEmpty()
                && (Character.isWhitespace(valor.charAt(0))
                || Character.isWhitespace(valor.charAt(valor.length() - 1)));
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
