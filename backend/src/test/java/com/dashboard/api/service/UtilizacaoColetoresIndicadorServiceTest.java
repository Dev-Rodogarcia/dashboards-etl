package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.dto.indicadoresgestao.UtilizacaoColetoresOverviewDTO;
import com.dashboard.api.dto.indicadoresgestao.UtilizacaoColetoresRankingDTO;
import com.dashboard.api.dto.indicadoresgestao.UtilizacaoColetoresRowDTO;
import com.dashboard.api.dto.PaginaDTO;
import com.dashboard.api.repository.IndicadoresGestaoAVistaSqlRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UtilizacaoColetoresIndicadorServiceTest {

    private FakeIndicadoresGestaoAVistaSqlRepository sqlRepository;
    private UtilizacaoColetoresIndicadorService service;

    @BeforeEach
    void setUp() {
        sqlRepository = new FakeIndicadoresGestaoAVistaSqlRepository();
        service = new UtilizacaoColetoresIndicadorService(
                new ValidadorPeriodoService(),
                sqlRepository,
                escopoSemRestricao(),
                null
        );
    }

    @Test
    void buscarOverviewDeveMapearContagensAgregadasNoSql() {
        FiltroConsultaDTO filtro = filtroPadrao();
        sqlRepository.coletoresResumo = new IndicadoresGestaoAVistaSqlRepository.UtilizacaoColetoresResumo(
                "2026-04-03T09:00:00",
                3,
                2,
                2,
                1
        );

        UtilizacaoColetoresOverviewDTO overview = service.buscarOverview(filtro);

        assertThat(overview.manifestosBipados()).isEqualTo(3);
        assertThat(overview.manifestosEmitidos()).isEqualTo(2);
        assertThat(overview.manifestosDescarregamento()).isEqualTo(2);
        assertThat(overview.totalManifestos()).isEqualTo(4);
        assertThat(overview.manifestosIncompletos()).isEqualTo(1);
        assertThat(overview.pctUtilizacao()).isEqualTo(75.0);
    }

    @Test
    void buscarRankingDeveOcultarParceiroSemOrdensEAceitarFilialOperacionalZerada() {
        FiltroConsultaDTO filtro = filtroPadrao();
        sqlRepository.coletoresRanking = List.of(
                new IndicadoresGestaoAVistaSqlRepository.UtilizacaoColetoresRankingBase(
                        "SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
                        0,
                        1,
                        0,
                        0
                ),
                new IndicadoresGestaoAVistaSqlRepository.UtilizacaoColetoresRankingBase(
                        "PARCEIRO X",
                        0,
                        1,
                        0,
                        0
                ),
                new IndicadoresGestaoAVistaSqlRepository.UtilizacaoColetoresRankingBase(
                        "PARCEIRO COM LEITOR",
                        1,
                        0,
                        1,
                        0
                )
        );

        List<UtilizacaoColetoresRankingDTO> ranking = service.buscarRanking(filtro);

        assertThat(ranking).extracting(UtilizacaoColetoresRankingDTO::branchName)
                .contains("SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA", "PARCEIRO COM LEITOR")
                .doesNotContain("PARCEIRO X");
    }

    @Test
    void buscarTabelaPaginadaDeveUsarRowsAgregadasDoSql() {
        FiltroConsultaDTO filtro = filtroPadrao();
        UtilizacaoColetoresRowDTO row = new UtilizacaoColetoresRowDTO(
                "2026-04-02|SPO|geral",
                "2026-04-02",
                "SPO - RODOGARCIA TRANSPORTES RODOVIARIOS LTDA",
                "Geral",
                1,
                1,
                1,
                2,
                0,
                50.0
        );
        sqlRepository.coletoresTotal = 12L;
        sqlRepository.coletoresLinhas = List.of(row);

        PaginaDTO<UtilizacaoColetoresRowDTO> pagina = service.buscarTabelaPaginada(filtro, 2, 10);

        assertThat(pagina.conteudo()).containsExactly(row);
        assertThat(pagina.totalElementos()).isEqualTo(12);
        assertThat(pagina.totalPaginas()).isEqualTo(2);
        assertThat(sqlRepository.coletoresOffset).isEqualTo(10);
        assertThat(sqlRepository.coletoresLimite).isEqualTo(10);
    }

    @Test
    void buscarOverviewDevePropagarFalhaQuandoBaseIndisponivel() {
        FiltroConsultaDTO filtro = filtroPadrao();
        sqlRepository.coletoresResumoException = new DataAccessResourceFailureException("view indisponivel");

        assertThatThrownBy(() -> service.buscarOverview(filtro))
                .isInstanceOf(DataAccessResourceFailureException.class);
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
