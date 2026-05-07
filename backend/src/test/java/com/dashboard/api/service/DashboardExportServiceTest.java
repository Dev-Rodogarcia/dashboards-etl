package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardExportServiceTest {

    private DashboardExportService service;

    @BeforeEach
    void setUp() {
        PeriodoOffsetDateTimeHelper periodoHelper = PeriodoOffsetDateTimeHelper.padrao();
        service = new DashboardExportService(
                null,
                new DashboardExportSqlBuilder(periodoHelper),
                new CsvExportWriter(),
                new ValidadorPeriodoService(),
                escopoComAcessoTotal()
        );
    }

    @Test
    void exportarDeveRetornarHeadersCsvComFilenameDoPeriodo() throws Exception {
        ResponseEntity<StreamingResponseBody> response = service.exportarBeans(
                "fretes",
                filtro(),
                List.of(new LinhaCsv("SPO", 10))
        );

        var contentType = Objects.requireNonNull(response.getHeaders().getContentType());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Objects.requireNonNull(response.getBody()).writeTo(outputStream);
        byte[] body = outputStream.toByteArray();
        String csv = new String(body, StandardCharsets.UTF_8);

        assertThat(contentType.toString()).isEqualTo("text/csv;charset=UTF-8");
        assertThat(response.getHeaders().getContentDisposition().isAttachment()).isTrue();
        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("fretes_2026-03-17_2026-04-16.csv");
        assertThat(response.getHeaders().getContentLength()).isEqualTo(body.length);
        assertThat(body).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        assertThat(csv).isEqualTo("\ufefffilial;total\r\nSPO;10\r\n");
    }

    private record LinhaCsv(String filial, int total) {
    }

    private static FiltroConsultaDTO filtro() {
        return new FiltroConsultaDTO(LocalDate.of(2026, 3, 17), LocalDate.of(2026, 4, 16), Map.of());
    }

    private static EscopoFilialService escopoComAcessoTotal() {
        return new EscopoFilialService(null, null) {
            @Override
            public EscopoFilial escopoAtual() {
                return EscopoFilial.comAcessoTotal();
            }
        };
    }
}
