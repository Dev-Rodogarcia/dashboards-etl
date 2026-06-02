package com.dashboard.api.service;

import com.dashboard.api.builder.DashboardExportSqlBuilder;
import com.dashboard.api.definition.DashboardExportDefinition;
import com.dashboard.api.dto.dimensoes.DimensaoOpcaoDTO;
import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.service.acesso.EscopoFilialService;
import com.dashboard.api.util.CsvExportWriter;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Service
public class DashboardExportService {

    private static final MediaType CSV = MediaType.parseMediaType("text/csv;charset=UTF-8");

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final DashboardExportSqlBuilder sqlBuilder;
    private final CsvExportWriter csvExportWriter;
    private final ValidadorPeriodoService validadorPeriodoService;
    private final EscopoFilialService escopoFilialService;

    public DashboardExportService(
            NamedParameterJdbcTemplate jdbcTemplate,
            DashboardExportSqlBuilder sqlBuilder,
            CsvExportWriter csvExportWriter,
            ValidadorPeriodoService validadorPeriodoService,
            EscopoFilialService escopoFilialService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlBuilder = sqlBuilder;
        this.csvExportWriter = csvExportWriter;
        this.validadorPeriodoService = validadorPeriodoService;
        this.escopoFilialService = escopoFilialService;
    }

    public ResponseEntity<StreamingResponseBody> exportar(DashboardExportDefinition definition, FiltroConsultaDTO filtro) {
        validadorPeriodoService.validar(filtro.dataInicio(), filtro.dataFim());
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        DashboardExportSqlBuilder.ExportSql query = sqlBuilder.buildSelect(definition, filtro, escopo, Set.of());
        return respostaCsv(nomeArquivo(definition.nomeArquivo(), filtro), gerarArquivoCsv(outputStream -> gerarCsv(query, outputStream)));
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

    public List<DimensaoOpcaoDTO> listarResponsaveisFretes(FiltroConsultaDTO filtro) {
        validadorPeriodoService.validar(filtro.dataInicio(), filtro.dataFim());
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        DashboardExportSqlBuilder.ExportSql query = sqlBuilder.buildDistinctOptions(
                DashboardExportDefinition.FRETES,
                "NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Responsável Região Destino Key]))), '')",
                "COALESCE(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Responsável pela Região de Destino]))), ''), NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial Emissora]))), ''), NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial]))), ''), N'Responsável não informado')",
                filtro,
                escopo,
                Set.of("responsaveis")
        );
        String sql = Objects.requireNonNull(query.sql(), "sql");
        MapSqlParameterSource params = Objects.requireNonNull(query.params(), "params");
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new DimensaoOpcaoDTO(
                rs.getString("value"),
                rs.getString("label")
        ));
    }

    public List<DimensaoOpcaoDTO> listarUsuariosCotacoes(FiltroConsultaDTO filtro) {
        validadorPeriodoService.validar(filtro.dataInicio(), filtro.dataFim());
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        DashboardExportSqlBuilder.ExportSql query = sqlBuilder.buildDistinctOptions(
                DashboardExportDefinition.COTACOES,
                "NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Usuario Key]))), '')",
                "COALESCE(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Usuário]))), ''), NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Solicitante]))), ''), NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Usuario Key]))), ''))",
                filtro,
                escopo,
                Set.of("usuarios")
        );
        String sql = Objects.requireNonNull(query.sql(), "sql");
        MapSqlParameterSource params = Objects.requireNonNull(query.params(), "params");
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new DimensaoOpcaoDTO(
                rs.getString("value"),
                rs.getString("label")
        ));
    }

    public ResponseEntity<StreamingResponseBody> exportarBeans(
            String nomeArquivoBase,
            FiltroConsultaDTO filtro,
            List<?> rows
    ) {
        validadorPeriodoService.validar(filtro.dataInicio(), filtro.dataFim());
        return respostaCsv(nomeArquivo(nomeArquivoBase, filtro), gerarArquivoCsv(outputStream -> csvExportWriter.escreverBeans(outputStream, rows)));
    }

    private void gerarCsv(DashboardExportSqlBuilder.ExportSql query, OutputStream outputStream) throws IOException {
        try {
            String sql = Objects.requireNonNull(query.sql(), "sql");
            MapSqlParameterSource params = Objects.requireNonNull(query.params(), "params");
            jdbcTemplate.query(sql, params, resultSet -> {
                try {
                    csvExportWriter.escreverResultSet(outputStream, resultSet);
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
                return null;
            });
        } catch (UncheckedIOException ex) {
            throw ex.getCause();
        }
    }

    private Path gerarArquivoCsv(StreamingResponseBody writer) {
        try {
            Path arquivo = Files.createTempFile("dashboard-export-", ".csv");
            try (OutputStream outputStream = Files.newOutputStream(
                    arquivo,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            )) {
                writer.writeTo(outputStream);
            } catch (Exception ex) {
                Files.deleteIfExists(arquivo);
                throw ex;
            }
            return arquivo;
        } catch (IOException ex) {
            throw new UncheckedIOException("Nao foi possivel gerar arquivo CSV temporario", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Nao foi possivel gerar arquivo CSV", ex);
        }
    }

    private ResponseEntity<StreamingResponseBody> respostaCsv(String nomeArquivo, Path arquivo) {
        long tamanho;
        try {
            tamanho = Files.size(arquivo);
        } catch (IOException ex) {
            throw new UncheckedIOException("Nao foi possivel medir arquivo CSV temporario", ex);
        }

        StreamingResponseBody body = outputStream -> {
            try (InputStream inputStream = Files.newInputStream(arquivo, StandardOpenOption.READ)) {
                inputStream.transferTo(outputStream);
            } finally {
                Files.deleteIfExists(arquivo);
            }
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(CSV);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(nomeArquivo, StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(tamanho)
                .body(body);
    }

    private String nomeArquivo(String base, FiltroConsultaDTO filtro) {
        return "%s_%s_%s.csv".formatted(base, filtro.dataInicio(), filtro.dataFim());
    }
}
