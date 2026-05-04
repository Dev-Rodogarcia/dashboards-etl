package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class DashboardExportService {

    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final DashboardExportSqlBuilder sqlBuilder;
    private final ExcelExportWriter excelExportWriter;
    private final ValidadorPeriodoService validadorPeriodoService;
    private final EscopoFilialService escopoFilialService;

    public DashboardExportService(
            NamedParameterJdbcTemplate jdbcTemplate,
            DashboardExportSqlBuilder sqlBuilder,
            ExcelExportWriter excelExportWriter,
            ValidadorPeriodoService validadorPeriodoService,
            EscopoFilialService escopoFilialService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlBuilder = sqlBuilder;
        this.excelExportWriter = excelExportWriter;
        this.validadorPeriodoService = validadorPeriodoService;
        this.escopoFilialService = escopoFilialService;
    }

    public ResponseEntity<byte[]> exportar(DashboardExportDefinition definition, FiltroConsultaDTO filtro) {
        validadorPeriodoService.validar(filtro.dataInicio(), filtro.dataFim());
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        DashboardExportSqlBuilder.ExportSql query = sqlBuilder.buildSelect(definition, filtro, escopo, Set.of());
        byte[] conteudo = gerarXlsx(query);
        return respostaExcel(nomeArquivo(definition.nomeArquivo(), filtro), conteudo);
    }

    public long total(DashboardExportDefinition definition, FiltroConsultaDTO filtro) {
        validadorPeriodoService.validar(filtro.dataInicio(), filtro.dataFim());
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        DashboardExportSqlBuilder.ExportSql query = sqlBuilder.buildCount(definition, filtro, escopo, Set.of());
        String sql = Objects.requireNonNull(query.sql(), "sql");
        MapSqlParameterSource params = Objects.requireNonNull(query.params(), "params");
        Long total = jdbcTemplate.queryForObject(sql, params, Long.class);
        return total == null ? 0 : total;
    }

    public List<String> listarStatusFretes(FiltroConsultaDTO filtro) {
        validadorPeriodoService.validar(filtro.dataInicio(), filtro.dataFim());
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        DashboardExportSqlBuilder.ExportSql query = sqlBuilder.buildDistinct(
                DashboardExportDefinition.FRETES,
                "[Status]",
                filtro,
                escopo,
                Set.of("status")
        );
        String sql = Objects.requireNonNull(query.sql(), "sql");
        MapSqlParameterSource params = Objects.requireNonNull(query.params(), "params");
        return jdbcTemplate.queryForList(sql, params, String.class);
    }

    public ResponseEntity<byte[]> exportarBeans(
            String nomeArquivoBase,
            FiltroConsultaDTO filtro,
            List<?> rows
    ) {
        validadorPeriodoService.validar(filtro.dataInicio(), filtro.dataFim());
        return respostaExcel(nomeArquivo(nomeArquivoBase, filtro), gerarXlsx(rows));
    }

    private byte[] gerarXlsx(DashboardExportSqlBuilder.ExportSql query) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            String sql = Objects.requireNonNull(query.sql(), "sql");
            MapSqlParameterSource params = Objects.requireNonNull(query.params(), "params");
            jdbcTemplate.query(sql, params, resultSet -> {
                try {
                    excelExportWriter.escreverResultSet(outputStream, resultSet);
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
                return null;
            });
            return outputStream.toByteArray();
        } catch (UncheckedIOException ex) {
            throw ex;
        }
    }

    private byte[] gerarXlsx(List<?> rows) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            excelExportWriter.escreverBeans(outputStream, rows);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private ResponseEntity<byte[]> respostaExcel(String nomeArquivo, byte[] conteudo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(XLSX);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(nomeArquivo, StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(conteudo.length)
                .body(conteudo);
    }

    private String nomeArquivo(String base, FiltroConsultaDTO filtro) {
        return "%s_%s_%s.xlsx".formatted(base, filtro.dataInicio(), filtro.dataFim());
    }
}
