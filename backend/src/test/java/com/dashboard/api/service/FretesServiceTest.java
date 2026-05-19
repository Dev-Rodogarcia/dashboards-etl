package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.fretes.FretesGoalSummaryDTO;
import com.dashboard.api.dto.fretes.FretesOverviewDTO;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

    private static VisaoFretesEntity frete(Long id, String filial, String valorTotal) {
        VisaoFretesEntity entity = Objects.requireNonNull(novaInstancia(VisaoFretesEntity.class));
        ReflectionTestUtils.setField(entity, "id", id);
        ReflectionTestUtils.setField(entity, "dataFrete", OffsetDateTime.of(2026, 5, 19, 10, 0, 0, 0, ZoneOffset.ofHours(-3)));
        ReflectionTestUtils.setField(entity, "valorTotal", new BigDecimal(valorTotal));
        ReflectionTestUtils.setField(entity, "subtotal", new BigDecimal(valorTotal));
        ReflectionTestUtils.setField(entity, "pesoTaxado", BigDecimal.ZERO);
        ReflectionTestUtils.setField(entity, "volumes", 1);
        ReflectionTestUtils.setField(entity, "filialNome", filial);
        ReflectionTestUtils.setField(entity, "status", "Aberto");
        ReflectionTestUtils.setField(entity, "dataExtracao", LocalDateTime.of(2026, 5, 19, 12, 0));
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

        FakeFretesGoalService() {
            super(null, null);
        }

        @Override
        public FretesGoalSummaryDTO buscarResumo(
                LocalDate dataInicio,
                LocalDate dataFim,
                java.util.Collection<FretesBranchRealizado> realizados,
                java.util.Collection<String> filiaisSelecionadas
        ) {
            return summary;
        }
    }
}
