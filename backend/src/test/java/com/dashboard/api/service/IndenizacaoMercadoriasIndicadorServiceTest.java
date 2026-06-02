package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.indicadoresgestao.IndenizacaoMercadoriasOverviewDTO;
import com.dashboard.api.dto.indicadoresgestao.IndenizacaoMercadoriasRowDTO;
import com.dashboard.api.dto.indicadoresgestao.IndenizacaoMercadoriasSeriePointDTO;
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

class IndenizacaoMercadoriasIndicadorServiceTest {

    private FakeIndicadoresGestaoAVistaSqlRepository sqlRepository;
    private IndenizacaoMercadoriasIndicadorService service;

    @BeforeEach
    void setUp() {
        sqlRepository = new FakeIndicadoresGestaoAVistaSqlRepository();
        service = new IndenizacaoMercadoriasIndicadorService(
                new ValidadorPeriodoService(),
                sqlRepository,
                escopoSemRestricao()
        );
    }

    @Test
    void buscarOverviewDeveMapearSinistrosEFaturamentoSumarizadosNoSql() {
        FiltroConsultaDTO filtro = filtroPadrao();
        sqlRepository.indenizacaoResumo = new IndicadoresGestaoAVistaSqlRepository.IndenizacaoResumo(
                "2026-04-04T10:00:00",
                4,
                new BigDecimal("400.00"),
                new BigDecimal("400.00"),
                new BigDecimal("22777.77")
        );

        IndenizacaoMercadoriasOverviewDTO overview = service.buscarOverview(filtro);

        assertThat(overview.totalSinistros()).isEqualTo(4);
        assertThat(overview.valorIndenizadoOriginal()).isEqualByComparingTo("400.00");
        assertThat(overview.valorIndenizadoAbs()).isEqualByComparingTo("400.00");
        assertThat(overview.faturamentoBase()).isEqualByComparingTo("22777.77");
        assertThat(overview.pctIndenizacao()).isEqualTo(1.756);
    }

    @Test
    void buscarSerieDeveRetornarAgrupamentoMensalDoRepositorioSql() {
        FiltroConsultaDTO filtro = filtroPadrao();
        IndenizacaoMercadoriasSeriePointDTO ponto = new IndenizacaoMercadoriasSeriePointDTO(
                "2026-04-01",
                "SPO",
                2,
                new BigDecimal("80.00"),
                new BigDecimal("80.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("12000.00"),
                1.6
        );
        sqlRepository.indenizacaoSerie = List.of(ponto);

        assertThat(service.buscarSerie(filtro)).containsExactly(ponto);
    }

    @Test
    void buscarTabelaPaginadaDeveUsarCountELinhasDoSql() {
        FiltroConsultaDTO filtro = filtroPadrao();
        IndenizacaoMercadoriasRowDTO row = new IndenizacaoMercadoriasRowDTO(
                701L,
                "2026-04-15T00:00:00",
                "RJR",
                9001L,
                new BigDecimal("125.55"),
                new BigDecimal("125.55"),
                "Avaria Parcial",
                "FECHADO",
                2.511
        );
        sqlRepository.indenizacaoTotal = 21L;
        sqlRepository.indenizacaoLinhas = List.of(row);

        PaginaDTO<IndenizacaoMercadoriasRowDTO> pagina = service.buscarTabelaPaginada(filtro, 3, 10);

        assertThat(pagina.conteudo()).containsExactly(row);
        assertThat(pagina.totalElementos()).isEqualTo(21);
        assertThat(pagina.totalPaginas()).isEqualTo(3);
        assertThat(sqlRepository.indenizacaoOffset).isEqualTo(20);
        assertThat(sqlRepository.indenizacaoLimite).isEqualTo(10);
    }

    private static FiltroConsultaDTO filtroPadrao() {
        return new FiltroConsultaDTO(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                Map.of("filiais", List.of("SPO"))
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
