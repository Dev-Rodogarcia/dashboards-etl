package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.indicadoresgestao.UtilizacaoColetoresOverviewDTO;
import com.dashboard.api.dto.indicadoresgestao.UtilizacaoColetoresRankingDTO;
import com.dashboard.api.dto.indicadoresgestao.UtilizacaoColetoresSeriePointDTO;
import com.dashboard.api.model.VisaoInventarioEntity;
import com.dashboard.api.model.VisaoManifestosEntity;
import com.dashboard.api.model.VisaoManifestosId;
import com.dashboard.api.repository.DimFilialRepository;
import com.dashboard.api.repository.VisaoInventarioRepository;
import com.dashboard.api.repository.VisaoManifestosRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.lang.reflect.Constructor;
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
    private VisaoManifestosRepository manifestosRepository;

    @Mock
    private VisaoInventarioRepository inventarioRepository;

    @Mock
    private DimFilialRepository dimFilialRepository;

    private UtilizacaoColetoresIndicadorService service;

    @BeforeEach
    void setUp() {
        service = new UtilizacaoColetoresIndicadorService(
                new ValidadorPeriodoService(),
                manifestosRepository,
                inventarioRepository,
                dimFilialRepository,
                escopoSemRestricao(),
                PeriodoOffsetDateTimeHelper.padrao()
        );
    }

    @Test
    void buscarOverviewDeveUsarOrdensDistintasSobreManifestosBipaveis() {
        when(manifestosRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                manifesto(10L, "SPO", "REC", "encerrado", "DISTRIBUIÇÃO"),
                manifesto(10L, "REC", "REC", "encerrado", "DISTRIBUIÇÃO"),
                manifesto(11L, "SPO", "SPO, REC", "pendente", "TRANSFERÊNCIA"),
                manifesto(13L, "SPO", "REC", "encerrado", "ACERTO DE MOTORISTA"),
                manifesto(12L, "SPO", "REC", "encerrado", "CARGA FECHADA (TRÁFEGO)")
        ));
        when(inventarioRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                ordem(100L, "SPO", "picking", OffsetDateTime.parse("2026-04-02T08:00:00-03:00"), OffsetDateTime.parse("2026-04-02T08:30:00-03:00")),
                ordem(100L, "SPO", "picking", OffsetDateTime.parse("2026-04-02T08:05:00-03:00"), OffsetDateTime.parse("2026-04-02T08:35:00-03:00")),
                ordem(101L, "REC", "descarregamento", OffsetDateTime.parse("2026-04-02T09:00:00-03:00"), null),
                ordem(103L, null, "retorno", OffsetDateTime.parse("2026-04-02T09:30:00-03:00"), OffsetDateTime.parse("2026-04-02T09:50:00-03:00")),
                ordem(102L, "SPO", "inventario", OffsetDateTime.parse("2026-04-02T10:00:00-03:00"), null)
        ));

        UtilizacaoColetoresOverviewDTO overview = service.buscarOverview(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of())
        );

        assertThat(overview.manifestosBipados()).isEqualTo(3);
        assertThat(overview.manifestosEmitidos()).isEqualTo(2);
        assertThat(overview.manifestosDescarregamento()).isEqualTo(2);
        assertThat(overview.totalManifestos()).isEqualTo(4);
        assertThat(overview.manifestosIncompletos()).isEqualTo(1);
        assertThat(overview.pctUtilizacao()).isEqualTo(75.0);
    }

    @Test
    void buscarOverviewDeveAplicarFiltroDeFilialEmManifestosEOrdens() {
        when(manifestosRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                manifesto(20L, "SPO", "null, PARCEIRO X", "encerrado", "DISTRIBUIÇÃO"),
                manifesto(21L, "REC", "SPO", "encerrado", "DISTRIBUIÇÃO")
        ));
        when(inventarioRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                ordem(200L, "SPO", "recebimento", OffsetDateTime.parse("2026-04-02T08:00:00-03:00"), OffsetDateTime.parse("2026-04-02T08:30:00-03:00")),
                ordem(201L, "REC", "recebimento", OffsetDateTime.parse("2026-04-02T08:00:00-03:00"), OffsetDateTime.parse("2026-04-02T08:30:00-03:00"))
        ));

        UtilizacaoColetoresOverviewDTO overview = service.buscarOverview(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of("filiais", List.of("SPO")))
        );

        assertThat(overview.manifestosBipados()).isEqualTo(1);
        assertThat(overview.manifestosEmitidos()).isEqualTo(1);
        assertThat(overview.manifestosDescarregamento()).isEqualTo(1);
        assertThat(overview.totalManifestos()).isEqualTo(2);
        assertThat(overview.pctUtilizacao()).isEqualTo(50.0);
    }

    @Test
    void buscarOverviewDeveCanonicalizarAliasesDeFilialParaEvitarPercentualZero() {
        when(manifestosRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                manifesto(25L, "SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA", null, "encerrado", "DISTRIBUIÇÃO")
        ));
        when(inventarioRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                ordem(250L, "TR RODOGARCIA | SPO", "carregamento", OffsetDateTime.parse("2026-04-02T08:00:00-03:00"), OffsetDateTime.parse("2026-04-02T08:30:00-03:00"))
        ));

        UtilizacaoColetoresOverviewDTO overview = service.buscarOverview(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of("filiais", List.of("TR RODOGARCIA | SPO")))
        );

        assertThat(overview.manifestosBipados()).isEqualTo(1);
        assertThat(overview.totalManifestos()).isEqualTo(1);
        assertThat(overview.pctUtilizacao()).isEqualTo(100.0);
    }

    @Test
    void buscarSerieDeveUsarNomeCanonicoQuandoOrdemChegaComCodigoCurto() {
        when(manifestosRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                manifesto(26L, "SPO", null, "encerrado", "DISTRIBUIÇÃO")
        ));
        when(inventarioRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                ordem(260L, "SPO", "picking", OffsetDateTime.parse("2026-04-02T08:00:00-03:00"), OffsetDateTime.parse("2026-04-02T08:30:00-03:00"))
        ));

        List<UtilizacaoColetoresSeriePointDTO> serie = service.buscarSerie(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of())
        );

        assertThat(serie).extracting(UtilizacaoColetoresSeriePointDTO::filial)
                .containsExactly("SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA");
    }

    @Test
    void buscarSerieDeveAgruparPorDataFilialEClassificacaoGeral() {
        when(manifestosRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                manifesto(30L, "SPO", null, "encerrado", "DISTRIBUIÇÃO")
        ));
        when(inventarioRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                ordem(300L, "SPO", "carregamento", OffsetDateTime.parse("2026-04-03T08:00:00-03:00"), OffsetDateTime.parse("2026-04-03T08:30:00-03:00"))
        ));

        List<UtilizacaoColetoresSeriePointDTO> serie = service.buscarSerie(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of())
        );

        assertThat(serie)
                .extracting(UtilizacaoColetoresSeriePointDTO::date, UtilizacaoColetoresSeriePointDTO::filial, UtilizacaoColetoresSeriePointDTO::classificacao)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("2026-04-02", "SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA", "Geral"),
                        org.assertj.core.groups.Tuple.tuple("2026-04-03", "SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA", "Geral")
                );
    }

    @Test
    void buscarRankingDeveCalcularPercentualDecimalSemZerar() {
        when(manifestosRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                manifesto(40L, "SPO", null, "encerrado", "DISTRIBUIÇÃO"),
                manifesto(41L, "SPO", null, "encerrado", "DISTRIBUIÇÃO"),
                manifesto(42L, "SPO", "SPO", "encerrado", "DISTRIBUIÇÃO")
        ));
        when(inventarioRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                ordem(400L, "SPO", "picking", OffsetDateTime.parse("2026-04-02T08:00:00-03:00"), OffsetDateTime.parse("2026-04-02T08:30:00-03:00")),
                ordem(401L, "SPO", "recebimento", OffsetDateTime.parse("2026-04-02T09:00:00-03:00"), null)
        ));

        List<UtilizacaoColetoresRankingDTO> ranking = service.buscarRanking(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of("filiais", List.of("SPO")))
        );

        assertThat(ranking).singleElement().satisfies(item -> {
            assertThat(item.branchName()).isEqualTo("SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA");
            assertThat(item.ordensConferencia()).isEqualTo(2);
            assertThat(item.manifestosBipaveis()).isEqualTo(4);
            assertThat(item.descarregamentos()).isEqualTo(1);
            assertThat(item.utilization()).isEqualTo(50.0);
            assertThat(item.goal()).isEqualByComparingTo("90");
        });
    }

    @Test
    void buscarRankingDeveOcultarParceirosSemOrdensParaNaoZerarGrafico() {
        when(manifestosRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                manifesto(50L, "SPO", "RAO - CR TRANPORTES | PARCEIRO RIBEIRÃO PRETO", "encerrado", "DISTRIBUIÇÃO"),
                manifesto(51L, "REC", null, "encerrado", "DISTRIBUIÇÃO"),
                manifesto(52L, "SPO", null, "encerrado", "DISTRIBUIÇÃO")
        ));
        when(inventarioRepository.findAll(TestSpecificationMatchers.anySpecification())).thenReturn(List.of(
                ordem(500L, "SPO", "picking", OffsetDateTime.parse("2026-04-02T08:00:00-03:00"), OffsetDateTime.parse("2026-04-02T08:30:00-03:00"))
        ));

        List<UtilizacaoColetoresRankingDTO> ranking = service.buscarRanking(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of())
        );

        assertThat(ranking).extracting(UtilizacaoColetoresRankingDTO::branchName)
                .doesNotContain("RAO - CR TRANPORTES | PARCEIRO RIBEIRÃO PRETO")
                .contains(
                        "REC - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
                        "SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA"
                );
        assertThat(ranking)
                .filteredOn(item -> item.branchName().equals("REC - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA"))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.ordensConferencia()).isZero();
                    assertThat(item.manifestosBipaveis()).isEqualTo(1);
                    assertThat(item.utilization()).isZero();
                });
    }

    @Test
    void buscarOverviewDeveRetornarVazioQuandoBaseIndisponivel() {
        when(manifestosRepository.findAll(TestSpecificationMatchers.anySpecification()))
                .thenThrow(new DataAccessResourceFailureException("view indisponivel"));

        UtilizacaoColetoresOverviewDTO overview = service.buscarOverview(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of())
        );

        assertThat(overview.manifestosBipados()).isZero();
        assertThat(overview.totalManifestos()).isZero();
        assertThat(overview.pctUtilizacao()).isZero();
    }

    private static VisaoManifestosEntity manifesto(
            Long numero,
            String filialEmissora,
            String localDescarregamento,
            String status,
            String classificacao
    ) {
        VisaoManifestosEntity entity = TestReflectionUtils.novaInstancia(VisaoManifestosEntity.class);
        TestReflectionUtils.setField(entity, "id", manifestoId(numero, "uid-" + numero));
        TestReflectionUtils.setField(entity, "filialEmissora", filialEmissora);
        TestReflectionUtils.setField(entity, "filial", filialEmissora);
        TestReflectionUtils.setField(entity, "localDescarregamento", localDescarregamento);
        TestReflectionUtils.setField(entity, "status", status);
        TestReflectionUtils.setField(entity, "classificacao", classificacao);
        TestReflectionUtils.setField(entity, "dataCriacao", OffsetDateTime.parse("2026-04-02T07:00:00-03:00"));
        TestReflectionUtils.setField(entity, "dataExtracao", LocalDateTime.of(2026, 4, 3, 9, 0));
        return entity;
    }

    private static VisaoInventarioEntity ordem(
            Long numeroOrdem,
            String filial,
            String tipo,
            OffsetDateTime dataHoraInicio,
            OffsetDateTime dataHoraFim
    ) {
        VisaoInventarioEntity entity = TestReflectionUtils.novaInstancia(VisaoInventarioEntity.class);
        TestReflectionUtils.setField(entity, "identificadorUnico", "ordem-" + numeroOrdem + "-" + dataHoraInicio);
        TestReflectionUtils.setField(entity, "numeroOrdem", numeroOrdem);
        TestReflectionUtils.setField(entity, "filial", filial);
        TestReflectionUtils.setField(entity, "filialOrdemConferencia", filial);
        TestReflectionUtils.setField(entity, "tipo", tipo);
        TestReflectionUtils.setField(entity, "dataHoraInicio", dataHoraInicio);
        TestReflectionUtils.setField(entity, "dataHoraFim", dataHoraFim);
        TestReflectionUtils.setField(entity, "dataExtracao", LocalDateTime.of(2026, 4, 3, 9, 0));
        return entity;
    }

    private static VisaoManifestosId manifestoId(Long numero, String identificadorUnico) {
        try {
            Constructor<VisaoManifestosId> constructor = VisaoManifestosId.class.getDeclaredConstructor(Long.class, String.class);
            constructor.setAccessible(true);
            return constructor.newInstance(numero, identificadorUnico);
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
