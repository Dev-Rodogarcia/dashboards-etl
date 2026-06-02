package com.dashboard.api.service;

import com.dashboard.api.util.JanelaOffsetDateTime;
import com.dashboard.api.util.PeriodoOffsetDateTimeHelper;
import com.dashboard.api.util.JanelaOffsetDateTime;
import java.time.LocalDate;
import com.dashboard.api.util.JanelaOffsetDateTime;
import java.time.OffsetDateTime;
import com.dashboard.api.util.JanelaOffsetDateTime;
import java.time.ZoneId;
import com.dashboard.api.util.JanelaOffsetDateTime;
import java.time.ZoneOffset;
import com.dashboard.api.util.JanelaOffsetDateTime;
import org.junit.jupiter.api.Test;
import com.dashboard.api.util.JanelaOffsetDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class PeriodoOffsetDateTimeHelperTest {

    private final PeriodoOffsetDateTimeHelper helper =
            new PeriodoOffsetDateTimeHelper(ZoneId.of("America/Sao_Paulo"));

    @Test
    void criarJanelaDeveUsarMeiaNoiteNoFusoConfigurado() {
        JanelaOffsetDateTime janela = helper.criarJanela(
                LocalDate.of(2026, 2, 24),
                LocalDate.of(2026, 3, 26)
        );

        assertThat(janela.inicioInclusivo())
                .isEqualTo(OffsetDateTime.of(2026, 2, 24, 0, 0, 0, 0, ZoneOffset.ofHours(-3)));
        assertThat(janela.fimExclusivo())
                .isEqualTo(OffsetDateTime.of(2026, 3, 27, 0, 0, 0, 0, ZoneOffset.ofHours(-3)));
    }

    @Test
    void criarJanelaDeveSerInclusivaNoInicioEExclusivaNoFim() {
        JanelaOffsetDateTime janela = helper.criarJanela(
                LocalDate.of(2026, 2, 24),
                LocalDate.of(2026, 3, 26)
        );

        OffsetDateTime noInicio = OffsetDateTime.of(2026, 2, 24, 0, 0, 0, 0, ZoneOffset.ofHours(-3));
        OffsetDateTime noFimExclusivo = OffsetDateTime.of(2026, 3, 27, 0, 0, 0, 0, ZoneOffset.ofHours(-3));

        assertThat(noInicio.isBefore(janela.inicioInclusivo())).isFalse();
        assertThat(noInicio.isBefore(janela.fimExclusivo())).isTrue();
        assertThat(noFimExclusivo.isBefore(janela.fimExclusivo())).isFalse();
    }

    @Test
    void regressaoManifestosDeveExcluirRegistroDaNoiteAnteriorNoFusoLocal() {
        JanelaOffsetDateTime janela = helper.criarJanela(
                LocalDate.of(2026, 2, 24),
                LocalDate.of(2026, 3, 26)
        );
        OffsetDateTime registro = OffsetDateTime.of(2026, 2, 23, 21, 3, 19, 973_000_000, ZoneOffset.ofHours(-3));

        assertThat(registro).isBefore(janela.inicioInclusivo());
    }
}
