package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.fretes.FretesGoalSummaryDTO;
import com.dashboard.api.dto.fretes.FretesOverviewDTO;
import com.dashboard.api.dto.fretes.FretesTrendPointDTO;
import com.dashboard.api.model.VisaoFretesEntity;
import com.dashboard.api.repository.VisaoFretesRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FretesServiceTest {

    @Mock
    private VisaoFretesRepository repository;

    private FakeFretesGoalService fretesGoalService;
    private FretesService service;

    @BeforeEach
    void setUp() {
        fretesGoalService = new FakeFretesGoalService();
        service = new FretesService(
                new ValidadorPeriodoService(),
                repository,
                escopoSemRestricao(),
                PeriodoOffsetDateTimeHelper.padrao(),
                fretesGoalService
        );
    }

    @Test
    void buscarOverviewIncluiCalculosDiariosDeMetaETendencia() {
        LocalDate dataInicio = LocalDate.of(2026, 5, 1);
        LocalDate dataFim = LocalDate.of(2026, 5, 19);
        when(repository.findAll(ArgumentMatchers.<Specification<VisaoFretesEntity>>any())).thenReturn(List.of(
                frete(1L, "SPO", "2400000.00"),
                frete(2L, "REC", "2430280.00")
        ));
        fretesGoalService.summary = new FretesGoalSummaryDTO(
                "2026-05-01",
                "2026-05-19",
                new BigDecimal("8400000.00"),
                new BigDecimal("4830280.00"),
                57.50,
                List.of()
        );

        FretesOverviewDTO overview = service.buscarOverview(new FiltroConsultaDTO(dataInicio, dataFim, Map.of()));

        assertThat(overview.receitaBruta()).isEqualByComparingTo("4830280.00");
        assertThat(overview.faturamentoDiario().totalDiasUteisMes()).isEqualTo(21);
        assertThat(overview.faturamentoDiario().diasUteisDecorridos()).isEqualTo(13);
        assertThat(overview.faturamentoDiario().diasUteisRestantes()).isEqualTo(8);
        assertThat(overview.faturamentoDiario().metaDiariaBase()).isEqualByComparingTo("400000.00");
        assertThat(overview.faturamentoDiario().faturamentoDiarioReal()).isEqualByComparingTo("371560.00");
        assertThat(overview.faturamentoDiario().metaDiariaDinamica()).isEqualByComparingTo("446215.00");
        assertThat(overview.faturamentoDiario().faturamentoFaltante()).isEqualByComparingTo("3569720.00");
        assertThat(overview.faturamentoDiario().tendenciaFaturamento()).isEqualByComparingTo("7802760.00");
        assertThat(overview.faturamentoDiario().tendenciaPercentual()).isEqualByComparingTo("-0.071100");
    }

    @Test
    void buscarOverviewMantemTotalOperacionalEExcluiCortesiasEBloqueiosDoFaturamento() {
        LocalDate dataInicio = LocalDate.of(2026, 5, 1);
        LocalDate dataFim = LocalDate.of(2026, 5, 19);
        when(repository.findAll(ArgumentMatchers.<Specification<VisaoFretesEntity>>any())).thenReturn(List.of(
                frete(1L, "SPO", "100.00"),
                frete(2L, "SPO", "999.00", null, true, null),
                frete(3L, "SPO", "999.00", null, false, "Bloqueio (Anulação e Isolamento)")
        ));

        FretesOverviewDTO overview = service.buscarOverview(new FiltroConsultaDTO(dataInicio, dataFim, Map.of()));

        assertThat(overview.totalFretes()).isEqualTo(3);
        assertThat(overview.receitaBruta()).isEqualByComparingTo("100.00");
        assertThat(overview.valorFrete()).isEqualByComparingTo("100.00");
        assertThat(overview.ticketMedio()).isEqualByComparingTo("100.00");
        assertThat(fretesGoalService.realizadosRecebidos).singleElement().satisfies(realizado -> {
            assertThat(realizado.branchId()).isEqualTo("SPO");
            assertThat(realizado.realizadoFaturamento()).isEqualByComparingTo("100.00");
        });
    }

    @Test
    void buscarSerieTemporalUsaEmissaoCteComFallbackDataFreteEApenasFaturamentoElegivel() {
        when(repository.findAll(ArgumentMatchers.<Specification<VisaoFretesEntity>>any())).thenReturn(List.of(
                frete(1L, "SPO", "100.00",
                        OffsetDateTime.of(2026, 3, 31, 10, 0, 0, 0, ZoneOffset.ofHours(-3)),
                        OffsetDateTime.of(2026, 4, 2, 11, 0, 0, 0, ZoneOffset.ofHours(-3)),
                        false,
                        null),
                frete(2L, "REC", "50.00",
                        OffsetDateTime.of(2026, 4, 3, 10, 0, 0, 0, ZoneOffset.ofHours(-3)),
                        null,
                        false,
                        null),
                frete(3L, "SPO", "999.00",
                        OffsetDateTime.of(2026, 4, 2, 10, 0, 0, 0, ZoneOffset.ofHours(-3)),
                        null,
                        true,
                        null),
                frete(4L, "SPO", "999.00",
                        OffsetDateTime.of(2026, 4, 3, 10, 0, 0, 0, ZoneOffset.ofHours(-3)),
                        null,
                        false,
                        "BLOQUEIO - ISOLAMENTO")
        ));

        List<FretesTrendPointDTO> serie = service.buscarSerieTemporal(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of())
        );

        assertThat(serie).extracting(FretesTrendPointDTO::date).containsExactly("2026-04-02", "2026-04-03");
        assertThat(serie.get(0).receitaBruta()).isEqualByComparingTo("100.00");
        assertThat(serie.get(0).valorFrete()).isEqualByComparingTo("100.00");
        assertThat(serie.get(0).fretes()).isEqualTo(1);
        assertThat(serie.get(1).receitaBruta()).isEqualByComparingTo("50.00");
        assertThat(serie.get(1).valorFrete()).isEqualByComparingTo("50.00");
        assertThat(serie.get(1).fretes()).isEqualTo(1);
    }

    private static VisaoFretesEntity frete(Long id, String filial, String valorTotal) {
        return frete(id, filial, valorTotal, null, false, null);
    }

    private static VisaoFretesEntity frete(
            Long id,
            String filial,
            String valorTotal,
            OffsetDateTime cteEmissao,
            boolean cortesia,
            String classificacao
    ) {
        return frete(
                id,
                filial,
                valorTotal,
                OffsetDateTime.of(2026, 5, 19, 10, 0, 0, 0, ZoneOffset.ofHours(-3)),
                cteEmissao,
                cortesia,
                classificacao
        );
    }

    private static VisaoFretesEntity frete(
            Long id,
            String filial,
            String valorTotal,
            OffsetDateTime dataFrete,
            OffsetDateTime cteEmissao,
            boolean cortesia,
            String classificacao
    ) {
        VisaoFretesEntity entity = Objects.requireNonNull(novaInstancia(VisaoFretesEntity.class));
        ReflectionTestUtils.setField(entity, "id", id);
        ReflectionTestUtils.setField(entity, "dataFrete", dataFrete);
        ReflectionTestUtils.setField(entity, "cteEmissao", cteEmissao);
        ReflectionTestUtils.setField(entity, "dataReferenciaFaturamento", cteEmissao != null ? cteEmissao : dataFrete);
        ReflectionTestUtils.setField(entity, "valorTotal", new BigDecimal(valorTotal));
        ReflectionTestUtils.setField(entity, "subtotal", new BigDecimal(valorTotal));
        ReflectionTestUtils.setField(entity, "pesoTaxado", BigDecimal.ZERO);
        ReflectionTestUtils.setField(entity, "volumes", 1);
        ReflectionTestUtils.setField(entity, "filialNome", filial);
        ReflectionTestUtils.setField(entity, "classificacaoNome", classificacao != null ? classificacao : "LTL");
        ReflectionTestUtils.setField(entity, "cortesiaFlag", cortesia);
        ReflectionTestUtils.setField(entity, "elegivelFaturamento", !cortesia && !bloqueioFaturamento(classificacao));
        ReflectionTestUtils.setField(entity, "status", "Aberto");
        ReflectionTestUtils.setField(entity, "dataExtracao", LocalDateTime.of(2026, 5, 19, 12, 0));
        return entity;
    }

    private static boolean bloqueioFaturamento(String classificacao) {
        if (classificacao == null) {
            return false;
        }
        String normalizada = java.text.Normalizer.normalize(classificacao, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(java.util.Locale.ROOT);
        return normalizada.contains("bloqueio")
                && (normalizada.contains("anulacao") || normalizada.contains("isolamento"));
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

    private static EscopoFilialService escopoSemRestricao() {
        return new EscopoFilialService(null, null) {
            @Override
            public EscopoFilial escopoAtual() {
                return EscopoFilial.comAcessoTotal();
            }
        };
    }

    private static final class FakeFretesGoalService extends FretesGoalService {
        private FretesGoalSummaryDTO summary;
        private List<FretesBranchRealizado> realizadosRecebidos = List.of();

        FakeFretesGoalService() {
            super(null, null);
        }

        @Override
        public FretesGoalSummaryDTO buscarResumo(
                LocalDate dataInicio,
                LocalDate dataFim,
                Collection<FretesBranchRealizado> realizados,
                Collection<String> filiaisSelecionadas
        ) {
            realizadosRecebidos = new ArrayList<>(realizados);
            if (summary != null) {
                return summary;
            }

            BigDecimal realizadoTotal = realizados.stream()
                    .map(FretesBranchRealizado::realizadoFaturamento)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            return new FretesGoalSummaryDTO(
                    dataInicio.toString(),
                    dataFim.toString(),
                    BigDecimal.ZERO,
                    realizadoTotal,
                    0.0,
                    List.of()
            );
        }
    }
}
