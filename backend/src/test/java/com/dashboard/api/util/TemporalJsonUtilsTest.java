package com.dashboard.api.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class TemporalJsonUtilsTest {

    @Test
    void formatarUtcDeveTratarLocalDateTimeDoBancoComoHorarioDeSaoPaulo() {
        String resultado = TemporalJsonUtils.formatarIsoComOffset(LocalDateTime.of(2026, 6, 8, 18, 0));

        assertThat(resultado).isEqualTo("2026-06-08T18:00:00-03:00");
    }

    @Test
    void garantirUtcDeveAplicarOffsetDeSaoPauloQuandoTextoNaoTemOffset() {
        String resultado = TemporalJsonUtils.garantirIsoComOffset("2026-06-08T18:00:00");

        assertThat(resultado).isEqualTo("2026-06-08T18:00:00-03:00");
    }

    @Test
    void garantirUtcDeveAceitarFormatoSqlComEspacoEFractionalSeconds() {
        String resultado = TemporalJsonUtils.garantirIsoComOffset("2026-06-08 18:00:00.1234567");

        assertThat(resultado).isEqualTo("2026-06-08T18:00:00.1234567-03:00");
    }

    @Test
    void garantirUtcDeveConverterInstanteComOffsetParaFusoOperacional() {
        String resultado = TemporalJsonUtils.garantirIsoComOffset("2026-06-08T21:00:00Z");

        assertThat(resultado).isEqualTo("2026-06-08T18:00:00-03:00");
    }

    @Test
    void formatarUtcComoIsoComOffsetDeveConverterSnapshotUtcIngenuoParaSaoPaulo() {
        String resultado = TemporalJsonUtils.formatarUtcComoIsoComOffset(LocalDateTime.of(2026, 6, 8, 22, 2));

        assertThat(resultado).isEqualTo("2026-06-08T19:02:00-03:00");
    }
}
