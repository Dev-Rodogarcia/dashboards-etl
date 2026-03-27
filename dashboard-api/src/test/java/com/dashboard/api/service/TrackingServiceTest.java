package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.tracking.TrackingChartsDTO;
import com.dashboard.api.dto.tracking.TrackingOverviewDTO;
import com.dashboard.api.model.VisaoLocalizacaoCargasEntity;
import com.dashboard.api.repository.VisaoLocalizacaoCargasRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingServiceTest {

    @Mock
    private VisaoLocalizacaoCargasRepository repository;

    private TrackingService service;

    @BeforeEach
    void setUp() {
        service = new TrackingService(new ValidadorPeriodoService(), repository);
    }

    @Test
    void buscarOverviewDeveIgnorarStatusNuloNaPrevisaoVencida() {
        when(repository.findByDataFreteGreaterThanEqualAndDataFreteLessThan(any(), any())).thenReturn(List.of(
                carga(1L, "Em entrega", -2),
                carga(2L, null, -2),
                carga(3L, "Finalizado", -2),
                carga(4L, "Manifestado", 2)
        ));

        TrackingOverviewDTO overview = service.buscarOverview(filtroPadrao());

        assertThat(overview.totalCargas()).isEqualTo(4);
        assertThat(overview.emTransito()).isEqualTo(2);
        assertThat(overview.previsaoVencida()).isEqualTo(1);
        assertThat(overview.valorFreteEmCarteira()).isEqualByComparingTo("400.00");
        assertThat(overview.pesoTaxadoTotal()).isEqualByComparingTo("40.00");
        assertThat(overview.pctFinalizado()).isEqualTo(25.0);
    }

    @Test
    void buscarGraficosDeveAgruparPrevisaoVencidaSemFilial() {
        when(repository.findByDataFreteGreaterThanEqualAndDataFreteLessThan(any(), any())).thenReturn(List.of(
                carga(1L, "Em entrega", -2, null),
                carga(2L, "Manifestado", -3, "   "),
                carga(3L, "Em entrega", -1, "Filial SP"),
                carga(4L, "Finalizado", -5, null)
        ));

        TrackingChartsDTO graficos = service.buscarGraficos(filtroPadrao());

        assertThat(graficos.previsaoVencidaPorFilialAtual()).extracting(
                dto -> dto.filialAtual(),
                dto -> dto.vencidas(),
                dto -> dto.total()
        ).containsExactly(
                org.assertj.core.groups.Tuple.tuple("Sem filial", 2, 2),
                org.assertj.core.groups.Tuple.tuple("Filial SP", 1, 1)
        );
    }

    @Test
    void buscarOverviewDeveConsultarPeriodoNoFusoDeSaoPaulo() {
        when(repository.findByDataFreteGreaterThanEqualAndDataFreteLessThan(any(), any())).thenReturn(List.of());

        service.buscarOverview(filtroPadrao());

        ArgumentCaptor<OffsetDateTime> inicio = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> fim = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(repository).findByDataFreteGreaterThanEqualAndDataFreteLessThan(inicio.capture(), fim.capture());

        assertThat(inicio.getValue())
                .isEqualTo(OffsetDateTime.of(2026, 2, 21, 0, 0, 0, 0, ZoneOffset.ofHours(-3)));
        assertThat(fim.getValue())
                .isEqualTo(OffsetDateTime.of(2026, 3, 24, 0, 0, 0, 0, ZoneOffset.ofHours(-3)));
    }

    private static FiltroConsultaDTO filtroPadrao() {
        return new FiltroConsultaDTO(LocalDate.of(2026, 2, 21), LocalDate.of(2026, 3, 23), Map.of());
    }

    private static VisaoLocalizacaoCargasEntity carga(Long numeroMinuta, String statusCarga, int diasPrevisao) {
        return carga(numeroMinuta, statusCarga, diasPrevisao, null);
    }

    private static VisaoLocalizacaoCargasEntity carga(Long numeroMinuta, String statusCarga, int diasPrevisao, String filialAtual) {
        VisaoLocalizacaoCargasEntity entity = Objects.requireNonNull(novaInstancia(VisaoLocalizacaoCargasEntity.class));
        ReflectionTestUtils.setField(entity, "sequenceNumber", numeroMinuta);
        ReflectionTestUtils.setField(entity, "statusCarga", statusCarga);
        ReflectionTestUtils.setField(entity, "filialAtual", filialAtual);
        ReflectionTestUtils.setField(entity, "previsaoEntrega", OffsetDateTime.now().plusDays(diasPrevisao));
        ReflectionTestUtils.setField(entity, "dataFrete", OffsetDateTime.of(2026, 3, 20, 10, 0, 0, 0, ZoneOffset.UTC));
        ReflectionTestUtils.setField(entity, "valorFrete", new BigDecimal("100.00"));
        ReflectionTestUtils.setField(entity, "pesoTaxado", "10");
        ReflectionTestUtils.setField(entity, "regiaoDestino", "Sudeste");
        ReflectionTestUtils.setField(entity, "dataExtracao", LocalDateTime.of(2026, 3, 23, 12, 0));
        return entity;
    }

    private static <T> T novaInstancia(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Nao foi possivel instanciar " + type.getSimpleName(), ex);
        }
    }
}
