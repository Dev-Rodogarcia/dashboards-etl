package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.manifestos.ManifestosCustosEvolucaoDTO;
import com.dashboard.api.dto.manifestos.ManifestosPerformanceDTO;
import com.dashboard.api.dto.manifestos.ManifestosPerformanceDTO.GaugeMetricDTO;
import com.dashboard.api.dto.manifestos.ManifestosPerformanceDTO.KpisManifestosDTO;
import com.dashboard.api.repository.ManifestosPerformanceSqlRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ManifestosPerformanceServiceTest {

    @Test
    void preservaFiltroOriginalParaFatosENormalizaFiliaisApenasParaOrcamento() {
        ManifestosPerformanceDTO performance = performance();
        ManifestosCustosEvolucaoDTO custosEvolucao = custosEvolucao();
        CapturingPerformanceRepository repository = new CapturingPerformanceRepository(performance);
        CapturingCostGoalService costGoalService = new CapturingCostGoalService(custosEvolucao);
        ManifestosPerformanceService service = new ManifestosPerformanceService(repository, costGoalService);
        FiltroConsultaDTO filtro = new FiltroConsultaDTO(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 19),
                Map.of(
                        "filiais", List.of("AGU - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA"),
                        "status", List.of("ENCERRADO")
                )
        );

        ManifestosPerformanceDTO resultado = service.buscarPerformance(filtro, "dia", 2026, 5);

        assertThat(repository.filtroRecebido).isSameAs(filtro);
        assertThat(repository.nivelRecebido).isEqualTo("dia");
        assertThat(repository.anoRecebido).isEqualTo(2026);
        assertThat(repository.mesRecebido).isEqualTo(5);
        assertThat(costGoalService.filtroFatoRecebido).isSameAs(filtro);
        assertThat(costGoalService.filtroFatoRecebido.valores("filiais"))
                .containsExactly("AGU - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA");
        assertThat(costGoalService.filtroOrcamentoRecebido).isNotSameAs(filtro);
        assertThat(costGoalService.filtroOrcamentoRecebido.valores("filiais")).containsExactly("AGU");
        assertThat(costGoalService.filtroOrcamentoRecebido.valores("status")).containsExactly("ENCERRADO");
        assertThat(costGoalService.custoRealRecebido).isEqualByComparingTo("500000.00");
        assertThat(resultado.totalDiasUteis()).isEqualTo(13);
        assertThat(resultado.custosEvolucao()).isSameAs(custosEvolucao);
    }

    private static final class CapturingPerformanceRepository extends ManifestosPerformanceSqlRepository {

        private final ManifestosPerformanceDTO response;
        private FiltroConsultaDTO filtroRecebido;
        private String nivelRecebido;
        private Integer anoRecebido;
        private Integer mesRecebido;

        private CapturingPerformanceRepository(ManifestosPerformanceDTO response) {
            super(null, null, null, null);
            this.response = response;
        }

        @Override
        public ManifestosPerformanceDTO buscarPerformance(
                FiltroConsultaDTO filtro,
                String nivel,
                Integer ano,
                Integer mes
        ) {
            this.filtroRecebido = filtro;
            this.nivelRecebido = nivel;
            this.anoRecebido = ano;
            this.mesRecebido = mes;
            return response;
        }

        @Override
        public Integer contarDiasUteisCalendario(LocalDate dataInicio, LocalDate dataFim) {
            return 13;
        }
    }

    private static final class CapturingCostGoalService extends ManifestosCostGoalService {

        private final ManifestosCustosEvolucaoDTO response;
        private FiltroConsultaDTO filtroFatoRecebido;
        private FiltroConsultaDTO filtroOrcamentoRecebido;
        private BigDecimal custoRealRecebido;

        private CapturingCostGoalService(ManifestosCustosEvolucaoDTO response) {
            super(null, null, null, Clock.systemUTC());
            this.response = response;
        }

        @Override
        public ManifestosCustosEvolucaoDTO calcular(
                FiltroConsultaDTO filtro,
                FiltroConsultaDTO filtroOrcamento,
                BigDecimal custoRealPeriodo
        ) {
            this.filtroFatoRecebido = filtro;
            this.filtroOrcamentoRecebido = filtroOrcamento;
            this.custoRealRecebido = custoRealPeriodo;
            return response;
        }
    }

    private static ManifestosPerformanceDTO performance() {
        return new ManifestosPerformanceDTO(
                "2026-05-19T12:00:00",
                0,
                new KpisManifestosDTO(
                        10,
                        1,
                        2,
                        7,
                        new BigDecimal("1000.00"),
                        new BigDecimal("500000.00"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                gauge(),
                gauge(),
                gauge(),
                List.of(),
                List.of(),
                List.of(),
                null
        );
    }

    private static GaugeMetricDTO gauge() {
        return new GaugeMetricDTO(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private static ManifestosCustosEvolucaoDTO custosEvolucao() {
        return new ManifestosCustosEvolucaoDTO(
                true,
                true,
                null,
                20,
                11,
                9,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                List.of()
        );
    }
}
