package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.indicadoresgestao.CubagemMercadoriasOverviewDTO;
import com.dashboard.api.dto.indicadoresgestao.CubagemMercadoriasRowDTO;
import com.dashboard.api.dto.PaginaDTO;
import com.dashboard.api.repository.IndicadoresGestaoAVistaSqlRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CubagemMercadoriasIndicadorServiceTest {

    private FakeIndicadoresGestaoAVistaSqlRepository sqlRepository;
    private CubagemMercadoriasIndicadorService service;

    @BeforeEach
    void setUp() {
        sqlRepository = new FakeIndicadoresGestaoAVistaSqlRepository();
        service = new CubagemMercadoriasIndicadorService(
                new ValidadorPeriodoService(),
                sqlRepository,
                escopoSemRestricao()
        );
    }

    @Test
    void buscarOverviewDeveMapearResumoAgregadoEPercentualCubagem() {
        FiltroConsultaDTO filtro = filtroPadrao();
        sqlRepository.cubagemResumo = new IndicadoresGestaoAVistaSqlRepository.CubagemResumo(
                "2026-04-03T08:00:00",
                3,
                1,
                2
        );

        CubagemMercadoriasOverviewDTO overview = service.buscarOverview(filtro);

        assertThat(overview.totalFretes()).isEqualTo(3);
        assertThat(overview.fretesCubados()).isEqualTo(1);
        assertThat(overview.fretesComPesoReal()).isEqualTo(2);
        assertThat(overview.pctCubagem()).isEqualTo(33.3);
    }

    @Test
    void buscarTabelaPaginadaDeveUsarRowsDeduplicadasDoSql() {
        FiltroConsultaDTO filtro = filtroPadrao();
        CubagemMercadoriasRowDTO row = new CubagemMercadoriasRowDTO(
                20L,
                "2026-04-02T10:00:00-03:00",
                "CWB",
                "Cliente",
                "12345678000190",
                "Curitiba",
                BigDecimal.TEN,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                BigDecimal.ONE,
                true
        );
        sqlRepository.cubagemTotal = 11L;
        sqlRepository.cubagemLinhas = List.of(row);

        PaginaDTO<CubagemMercadoriasRowDTO> pagina = service.buscarTabelaPaginada(filtro, 2, 10);

        assertThat(pagina.conteudo()).containsExactly(row);
        assertThat(pagina.totalElementos()).isEqualTo(11);
        assertThat(pagina.totalPaginas()).isEqualTo(2);
        assertThat(sqlRepository.cubagemOffset).isEqualTo(10);
        assertThat(sqlRepository.cubagemLimite).isEqualTo(10);
    }

    private static FiltroConsultaDTO filtroPadrao() {
        return new FiltroConsultaDTO(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                Map.of()
        );
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
