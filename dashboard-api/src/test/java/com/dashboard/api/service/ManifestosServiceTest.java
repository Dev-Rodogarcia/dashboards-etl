package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.manifestos.ManifestoResumoDTO;
import com.dashboard.api.dto.manifestos.ManifestosOverviewDTO;
import com.dashboard.api.model.VisaoManifestosEntity;
import com.dashboard.api.model.VisaoManifestosId;
import com.dashboard.api.repository.VisaoManifestosRepository;
import jakarta.persistence.EmbeddedId;
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
class ManifestosServiceTest {

    @Mock
    private VisaoManifestosRepository repository;

    private ManifestosService service;

    @BeforeEach
    void setUp() {
        service = new ManifestosService(new ValidadorPeriodoService(), repository);
    }

    @Test
    void buscarOverviewDeveSomarCadaLinhaMesmoQuandoONumeroSeRepete() {
        when(repository.findByDataCriacaoGreaterThanEqualAndDataCriacaoLessThan(any(), any())).thenReturn(List.of(
                manifesto(62848L, "62848_MDFE_4380", "em trânsito", "100.00", "200.00", "1000.00", "100.00", "4.00"),
                manifesto(62848L, "62848_MDFE_4381", "encerrado", "50.00", "100.00", "500.00", "200.00", "2.00"),
                manifesto(70000L, "70000_MDFE_1", "encerrado", "25.00", "50.00", "250.00", "50.00", "1.00")
        ));

        ManifestosOverviewDTO overview = service.buscarOverview(filtroPadrao());

        assertThat(overview.totalManifestos()).isEqualTo(3);
        assertThat(overview.emTransito()).isEqualTo(1);
        assertThat(overview.encerrados()).isEqualTo(2);
        assertThat(overview.kmTotal()).isEqualByComparingTo("350.00");
        assertThat(overview.custoTotal()).isEqualByComparingTo("1750.00");
        assertThat(overview.custoPorKm()).isEqualByComparingTo("5.00");
        assertThat(overview.ocupacaoPesoMediaPct()).isEqualTo(200.0);
        assertThat(overview.ocupacaoCubagemMediaPct()).isEqualTo(116.67);
    }

    @Test
    void buscarTabelaDevePreservarIdentificadoresUnicosParaLinhasComMesmoNumero() {
        when(repository.findByDataCriacaoGreaterThanEqualAndDataCriacaoLessThan(any(), any())).thenReturn(List.of(
                manifesto(62848L, "62848_MDFE_4380", "em trânsito", "100.00", "200.00", "1000.00", "100.00", "4.00"),
                manifesto(62848L, "62848_MDFE_4381", "encerrado", "50.00", "100.00", "500.00", "200.00", "2.00")
        ));

        List<ManifestoResumoDTO> tabela = service.buscarTabela(filtroPadrao(), 10);

        assertThat(tabela).hasSize(2);
        assertThat(tabela).extracting(ManifestoResumoDTO::numero).containsExactly(62848L, 62848L);
        assertThat(tabela).extracting(ManifestoResumoDTO::identificadorUnico)
                .containsExactly("62848_MDFE_4380", "62848_MDFE_4381");
    }

    @Test
    void entidadeDeveUsarChaveCompostaComNumeroEIdentificadorUnico() {
        VisaoManifestosEntity entity = Objects.requireNonNull(novaInstancia(VisaoManifestosEntity.class));
        ReflectionTestUtils.setField(entity, "id", new VisaoManifestosId(62848L, "62848_MDFE_4380"));

        assertThat(idEhEmbedded()).isTrue();
        assertThat(entity.getId()).isEqualTo(new VisaoManifestosId(62848L, "62848_MDFE_4380"));
        assertThat(entity.getNumero()).isEqualTo(62848L);
        assertThat(entity.getIdentificadorUnico()).isEqualTo("62848_MDFE_4380");
    }

    @Test
    void buscarOverviewDeveConsultarPeriodoNoFusoDeSaoPaulo() {
        when(repository.findByDataCriacaoGreaterThanEqualAndDataCriacaoLessThan(any(), any())).thenReturn(List.of());

        service.buscarOverview(filtroPadrao());

        ArgumentCaptor<OffsetDateTime> inicio = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> fim = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(repository).findByDataCriacaoGreaterThanEqualAndDataCriacaoLessThan(inicio.capture(), fim.capture());

        assertThat(inicio.getValue())
                .isEqualTo(OffsetDateTime.of(2026, 2, 21, 0, 0, 0, 0, ZoneOffset.ofHours(-3)));
        assertThat(fim.getValue())
                .isEqualTo(OffsetDateTime.of(2026, 3, 24, 0, 0, 0, 0, ZoneOffset.ofHours(-3)));
    }

    private static FiltroConsultaDTO filtroPadrao() {
        return new FiltroConsultaDTO(LocalDate.of(2026, 2, 21), LocalDate.of(2026, 3, 23), Map.of());
    }

    private static VisaoManifestosEntity manifesto(
            Long numero,
            String identificadorUnico,
            String status,
            String capacidadeKg,
            String totalPesoTaxado,
            String custoTotal,
            String kmTotal,
            String totalM3
    ) {
        VisaoManifestosEntity entity = Objects.requireNonNull(novaInstancia(VisaoManifestosEntity.class));
        ReflectionTestUtils.setField(entity, "id", new VisaoManifestosId(numero, identificadorUnico));
        ReflectionTestUtils.setField(entity, "status", status);
        ReflectionTestUtils.setField(entity, "capacidadeKg", new BigDecimal(capacidadeKg));
        ReflectionTestUtils.setField(entity, "totalPesoTaxado", new BigDecimal(totalPesoTaxado));
        ReflectionTestUtils.setField(entity, "custoTotal", new BigDecimal(custoTotal));
        ReflectionTestUtils.setField(entity, "kmTotal", new BigDecimal(kmTotal));
        ReflectionTestUtils.setField(entity, "veiculoPesoCubado", new BigDecimal("2.00"));
        ReflectionTestUtils.setField(entity, "totalM3", new BigDecimal(totalM3));
        ReflectionTestUtils.setField(entity, "dataCriacao", OffsetDateTime.of(2026, 3, 23, 10, 0, 0, 0, ZoneOffset.UTC));
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

    private static boolean idEhEmbedded() {
        try {
            return VisaoManifestosEntity.class.getDeclaredField("id").isAnnotationPresent(EmbeddedId.class);
        } catch (NoSuchFieldException ex) {
            throw new IllegalStateException("Campo id nao encontrado em VisaoManifestosEntity", ex);
        }
    }
}
