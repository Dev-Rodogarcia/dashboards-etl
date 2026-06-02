package com.dashboard.api.service;

import com.dashboard.api.util.PerformanceMetricasUtils;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PerformanceMetricasUtilsTest {

    @Test
    void calculaDiferencaComDatasLocalDateSemDesvioDeFuso() {
        assertEquals(0, PerformanceMetricasUtils.diferencaDias(
                LocalDate.of(2026, 5, 24),
                LocalDate.of(2026, 5, 24)
        ));
        assertEquals(1, PerformanceMetricasUtils.diferencaDias(
                LocalDate.of(2026, 5, 24),
                LocalDate.of(2026, 5, 25)
        ));
        assertEquals(-1, PerformanceMetricasUtils.diferencaDias(
                LocalDate.of(2026, 5, 24),
                LocalDate.of(2026, 5, 23)
        ));
    }

    @Test
    void classificaPerformanceBinariaSomenteComDiferencaCalculada() {
        assertEquals("NO PRAZO", PerformanceMetricasUtils.performanceStatus(0));
        assertEquals("NO PRAZO", PerformanceMetricasUtils.performanceStatus(-2));
        assertEquals("FORA DO PRAZO", PerformanceMetricasUtils.performanceStatus(1));
        assertNull(PerformanceMetricasUtils.performanceStatus(null));
    }

    @Test
    void categorizaBucketsDeDiasComTetoAposTresDias() {
        assertEquals("NO PRAZO", PerformanceMetricasUtils.performanceStatusDias(0));
        assertEquals("1 DIA DE ATRASO", PerformanceMetricasUtils.performanceStatusDias(1));
        assertEquals("2 DIAS DE ATRASO", PerformanceMetricasUtils.performanceStatusDias(2));
        assertEquals("3 DIAS DE ATRASO", PerformanceMetricasUtils.performanceStatusDias(3));
        assertEquals("ACIMA DE 3 DIAS DE ATRASO", PerformanceMetricasUtils.performanceStatusDias(4));
        assertEquals("1 DIA ANTES", PerformanceMetricasUtils.performanceStatusDias(-1));
        assertEquals("2 DIAS ANTES", PerformanceMetricasUtils.performanceStatusDias(-2));
        assertEquals("3 DIAS ANTES", PerformanceMetricasUtils.performanceStatusDias(-3));
        assertEquals("ACIMA DE 3 DIAS ANTES", PerformanceMetricasUtils.performanceStatusDias(-4));
    }

    @Test
    void protegePercentualContraDivisaoPorZero() {
        assertEquals(0.0, PerformanceMetricasUtils.percentual(5, 0));
        assertEquals(33.33, PerformanceMetricasUtils.percentual(1, 3));
    }
}
