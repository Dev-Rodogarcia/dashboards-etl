package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.cotacoes.CotacaoResumoDTO;
import com.dashboard.api.dto.cotacoes.CotacoesChartsDTO;
import com.dashboard.api.dto.cotacoes.CotacoesOverviewDTO;
import com.dashboard.api.dto.cotacoes.CotacoesResumoAgregadoDTO;
import com.dashboard.api.dto.cotacoes.CotacoesTrendPointDTO;
import com.dashboard.api.repository.CotacoesDashboardSqlRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CotacoesServiceTest {

    private FakeCotacoesDashboardSqlRepository dashboardSqlRepository;
    private CotacoesService service;

    @BeforeEach
    void setUp() {
        dashboardSqlRepository = new FakeCotacoesDashboardSqlRepository();
        service = new CotacoesService(
                new ValidadorPeriodoService(),
                dashboardSqlRepository
        );
    }

    @Test
    void buscarOverviewDeveDelegarParaRepositorioSqlSemConsultarJpa() {
        FiltroConsultaDTO filtro = filtroPadrao();
        CotacoesOverviewDTO esperado = new CotacoesOverviewDTO(
                "2026-03-23T09:00:00",
                2,
                BigDecimal.valueOf(300).setScale(2),
                BigDecimal.valueOf(100).setScale(2),
                BigDecimal.valueOf(150).setScale(2),
                BigDecimal.TEN.setScale(2),
                33.33,
                50.0,
                0.0,
                50.0,
                0.0,
                2.0
        );
        dashboardSqlRepository.overviewResponse = esperado;

        CotacoesOverviewDTO resultado = service.buscarOverview(filtro);

        assertThat(resultado).isSameAs(esperado);
        assertThat(dashboardSqlRepository.overviewFiltro).isSameAs(filtro);
    }

    @Test
    void buscarSerieDeveDelegarParaRepositorioSql() {
        FiltroConsultaDTO filtro = filtroPadrao();
        List<CotacoesTrendPointDTO> esperado = List.of(new CotacoesTrendPointDTO(
                "2026-03-20",
                2,
                1,
                0,
                BigDecimal.valueOf(300).setScale(2),
                BigDecimal.valueOf(100).setScale(2)
        ));
        dashboardSqlRepository.serieResponse = esperado;

        assertThat(service.buscarSerie(filtro)).isSameAs(esperado);
        assertThat(dashboardSqlRepository.serieFiltro).isSameAs(filtro);
    }

    @Test
    void buscarGraficosDevePropagarFalhaSqlSemFallbackJpa() {
        FiltroConsultaDTO filtro = filtroPadrao();
        dashboardSqlRepository.graficosException = new DataAccessResourceFailureException("falha no SQL de cotacoes");

        assertThatThrownBy(() -> service.buscarGraficos(filtro))
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasMessageContaining("falha no SQL de cotacoes");
        assertThat(dashboardSqlRepository.graficosFiltro).isSameAs(filtro);
    }

    @Test
    void buscarTabelaDeveDelegarParaRepositorioSqlComLimiteAplicado() {
        FiltroConsultaDTO filtro = filtroPadrao();
        List<CotacaoResumoDTO> esperado = List.of(new CotacaoResumoDTO(
                1L,
                "2026-03-20T10:00Z",
                "SPO",
                "Maria",
                "Cliente Pagador",
                "Cliente",
                "SPO-CAS",
                new BigDecimal("100.00"),
                "Convertida",
                null,
                "LTL",
                1,
                new BigDecimal("10.00"),
                new BigDecimal("10.00"),
                BigDecimal.ZERO.setScale(2),
                new BigDecimal("1000.00"),
                new BigDecimal("10.00"),
                "Tabela A",
                "Sao Paulo - SP",
                "Cascavel - PR",
                null,
                null
        ));
        dashboardSqlRepository.tabelaResponse = esperado;

        assertThat(service.buscarTabela(filtro, 999)).isSameAs(esperado);
        assertThat(dashboardSqlRepository.tabelaFiltro).isSameAs(filtro);
        assertThat(dashboardSqlRepository.tabelaLimite).isEqualTo(200);
    }

    @Test
    void buscarResumosDeveDelegarParaRepositorioSql() {
        FiltroConsultaDTO filtro = filtroPadrao();
        List<CotacoesResumoAgregadoDTO> esperadoUsuario = List.of(resumo("maria", "Maria"));
        List<CotacoesResumoAgregadoDTO> esperadoFilial = List.of(resumo("SPO", "SPO"));
        List<CotacoesResumoAgregadoDTO> esperadoCliente = List.of(resumo("123", "Cliente"));
        dashboardSqlRepository.resumoUsuarioResponse = esperadoUsuario;
        dashboardSqlRepository.resumoFilialResponse = esperadoFilial;
        dashboardSqlRepository.resumoClienteResponse = esperadoCliente;

        assertThat(service.buscarResumoPorUsuario(filtro)).isSameAs(esperadoUsuario);
        assertThat(service.buscarResumoPorFilial(filtro)).isSameAs(esperadoFilial);
        assertThat(service.buscarResumoPorCliente(filtro)).isSameAs(esperadoCliente);
        assertThat(dashboardSqlRepository.resumoUsuarioFiltro).isSameAs(filtro);
        assertThat(dashboardSqlRepository.resumoFilialFiltro).isSameAs(filtro);
        assertThat(dashboardSqlRepository.resumoClienteFiltro).isSameAs(filtro);
    }

    private static FiltroConsultaDTO filtroPadrao() {
        return new FiltroConsultaDTO(LocalDate.of(2026, 2, 21), LocalDate.of(2026, 3, 23), Map.of());
    }

    private static CotacoesResumoAgregadoDTO resumo(String id, String entidade) {
        return new CotacoesResumoAgregadoDTO(
                id,
                entidade,
                10,
                4,
                6,
                40.0,
                new BigDecimal("1000.00"),
                new BigDecimal("400.00"),
                new BigDecimal("12.00")
        );
    }

    private static class FakeCotacoesDashboardSqlRepository extends CotacoesDashboardSqlRepository {

        private FiltroConsultaDTO overviewFiltro;
        private CotacoesOverviewDTO overviewResponse;
        private FiltroConsultaDTO serieFiltro;
        private List<CotacoesTrendPointDTO> serieResponse = List.of();
        private FiltroConsultaDTO graficosFiltro;
        private RuntimeException graficosException;
        private FiltroConsultaDTO tabelaFiltro;
        private int tabelaLimite;
        private List<CotacaoResumoDTO> tabelaResponse = List.of();
        private FiltroConsultaDTO resumoUsuarioFiltro;
        private List<CotacoesResumoAgregadoDTO> resumoUsuarioResponse = List.of();
        private FiltroConsultaDTO resumoFilialFiltro;
        private List<CotacoesResumoAgregadoDTO> resumoFilialResponse = List.of();
        private FiltroConsultaDTO resumoClienteFiltro;
        private List<CotacoesResumoAgregadoDTO> resumoClienteResponse = List.of();

        FakeCotacoesDashboardSqlRepository() {
            super(null, null, null);
        }

        @Override
        public CotacoesOverviewDTO buscarOverview(FiltroConsultaDTO filtro) {
            overviewFiltro = filtro;
            return overviewResponse;
        }

        @Override
        public List<CotacoesTrendPointDTO> buscarSerie(FiltroConsultaDTO filtro) {
            serieFiltro = filtro;
            return serieResponse;
        }

        @Override
        public CotacoesChartsDTO buscarGraficos(FiltroConsultaDTO filtro) {
            graficosFiltro = filtro;
            if (graficosException != null) {
                throw graficosException;
            }
            return new CotacoesChartsDTO(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        }

        @Override
        public List<CotacaoResumoDTO> buscarTabela(FiltroConsultaDTO filtro, int limite) {
            tabelaFiltro = filtro;
            tabelaLimite = limite;
            return tabelaResponse;
        }

        @Override
        public List<CotacoesResumoAgregadoDTO> buscarResumoPorUsuario(FiltroConsultaDTO filtro) {
            resumoUsuarioFiltro = filtro;
            return resumoUsuarioResponse;
        }

        @Override
        public List<CotacoesResumoAgregadoDTO> buscarResumoPorFilial(FiltroConsultaDTO filtro) {
            resumoFilialFiltro = filtro;
            return resumoFilialResponse;
        }

        @Override
        public List<CotacoesResumoAgregadoDTO> buscarResumoPorCliente(FiltroConsultaDTO filtro) {
            resumoClienteFiltro = filtro;
            return resumoClienteResponse;
        }
    }
}
