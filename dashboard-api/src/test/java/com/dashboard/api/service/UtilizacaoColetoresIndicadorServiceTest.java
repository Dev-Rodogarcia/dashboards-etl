package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.indicadoresgestao.UtilizacaoColetoresOverviewDTO;
import com.dashboard.api.model.VisaoInventarioEntity;
import com.dashboard.api.model.VisaoManifestosEntity;
import com.dashboard.api.model.VisaoManifestosId;
import com.dashboard.api.repository.VisaoInventarioRepository;
import com.dashboard.api.repository.VisaoManifestosRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UtilizacaoColetoresIndicadorServiceTest {

    @Mock
    private VisaoInventarioRepository inventarioRepository;

    @Mock
    private VisaoManifestosRepository manifestosRepository;

    private UtilizacaoColetoresIndicadorService service;

    @BeforeEach
    void setUp() {
        service = new UtilizacaoColetoresIndicadorService(
                new ValidadorPeriodoService(),
                inventarioRepository,
                manifestosRepository,
                escopoSemRestricao(),
                PeriodoOffsetDateTimeHelper.padrao()
        );
    }

    @Test
    void buscarOverviewDeveCalcularOrdensEManifestosOficiais() {
        when(inventarioRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                inventario("inv-1", 1001L, "SPO", "Loading"),
                inventario("inv-2", 1002L, "SPO", "Picking"),
                inventario("inv-3", 1002L, "SPO", "Picking")
        ));
        when(manifestosRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                manifesto(10L, "SPO", "REC"),
                manifesto(11L, "SPO", "SPO, REC")
        ));

        UtilizacaoColetoresOverviewDTO overview = service.buscarOverview(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of("filiais", List.of("SPO")))
        );

        assertThat(overview.ordensConferencia()).isEqualTo(2);
        assertThat(overview.manifestosEmitidos()).isEqualTo(2);
        assertThat(overview.manifestosDescarregamento()).isEqualTo(1);
        assertThat(overview.totalManifestos()).isEqualTo(3);
        assertThat(overview.pctUtilizacao()).isEqualTo(66.7);
    }

    @Test
    void buscarOverviewDeveRespeitarFiltroDeFiliais() {
        when(inventarioRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                inventario("inv-1", 1001L, "SPO", "Loading"),
                inventario("inv-2", 2001L, "REC", "Loading")
        ));
        when(manifestosRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                manifesto(10L, "SPO", "SPO"),
                manifesto(20L, "REC", "REC")
        ));

        UtilizacaoColetoresOverviewDTO overview = service.buscarOverview(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of("filiais", List.of("SPO")))
        );

        assertThat(overview.ordensConferencia()).isEqualTo(1);
        assertThat(overview.manifestosEmitidos()).isEqualTo(1);
        assertThat(overview.manifestosDescarregamento()).isEqualTo(1);
        assertThat(overview.totalManifestos()).isEqualTo(2);
    }

    private static VisaoInventarioEntity inventario(String id, Long numeroOrdem, String filial, String tipo) {
        VisaoInventarioEntity entity = TestReflectionUtils.novaInstancia(VisaoInventarioEntity.class);
        TestReflectionUtils.setField(entity, "identificadorUnico", id);
        TestReflectionUtils.setField(entity, "numeroOrdem", numeroOrdem);
        TestReflectionUtils.setField(entity, "filialEmissoraFrete", filial);
        TestReflectionUtils.setField(entity, "tipo", tipo);
        TestReflectionUtils.setField(entity, "dataHoraInicio", OffsetDateTime.parse("2026-04-02T08:00:00-03:00"));
        TestReflectionUtils.setField(entity, "dataExtracao", LocalDateTime.of(2026, 4, 3, 9, 0));
        return entity;
    }

    private static VisaoManifestosEntity manifesto(Long numero, String filialEmissora, String localDescarregamento) {
        VisaoManifestosEntity entity = TestReflectionUtils.novaInstancia(VisaoManifestosEntity.class);
        TestReflectionUtils.setField(entity, "id", manifestoId(numero));
        TestReflectionUtils.setField(entity, "filialEmissora", filialEmissora);
        TestReflectionUtils.setField(entity, "localDescarregamento", localDescarregamento);
        TestReflectionUtils.setField(entity, "dataCriacao", OffsetDateTime.parse("2026-04-02T07:00:00-03:00"));
        TestReflectionUtils.setField(entity, "valorFrete", BigDecimal.TEN);
        TestReflectionUtils.setField(entity, "dataExtracao", LocalDateTime.of(2026, 4, 3, 9, 0));
        return entity;
    }

    private static VisaoManifestosId manifestoId(Long numero) {
        try {
            Constructor<VisaoManifestosId> constructor = VisaoManifestosId.class.getDeclaredConstructor(Long.class, String.class);
            constructor.setAccessible(true);
            return constructor.newInstance(numero, "uid-" + numero);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Nao foi possivel instanciar VisaoManifestosId", ex);
        }
    }

    private static EscopoFilialService escopoSemRestricao() {
        return new EscopoFilialService(null, null) {
            @Override
            public EscopoFilial escopoAtual() {
                return EscopoFilial.comAcessoTotal();
            }
        };
    }
}
