package com.dashboard.api.service;

import com.dashboard.api.builder.DashboardExportSqlBuilder;
import com.dashboard.api.dto.faturascliente.FaturasPorClienteMensalDTO;
import com.dashboard.api.dto.faturascliente.FaturasPorClienteOverviewDTO;
import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.repository.FaturasPorClienteSqlRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import com.dashboard.api.util.PeriodoOffsetDateTimeHelper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FaturasPorClienteSqlRepositoryTest {

    @Mock
    private NamedParameterJdbcOperations jdbcTemplate;

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void buscarOverviewDeveAgregarFatoNoSqlComPeriodoSargable() {
        when(jdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(new FaturasPorClienteOverviewDTO(
                        "2026-03-23T09:00:00",
                        BigDecimal.ZERO,
                        0,
                        0,
                        0,
                        0.0,
                        0
                ));

        FaturasPorClienteSqlRepository repository = new FaturasPorClienteSqlRepository(
                jdbcTemplate,
                new DashboardExportSqlBuilder(PeriodoOffsetDateTimeHelper.padrao()),
                escopoSemRestricao()
        );

        repository.buscarOverview(filtroPadrao(), LocalDate.of(2026, 3, 23));

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForObject(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));

        assertThat(sqlCaptor.getValue())
                .contains("[ETL_SISTEMA].dbo.fato_gestao_vista_faturas")
                .contains("data_emissao_cte >= :inicioOffset AND data_emissao_cte < :fimOffset")
                .contains("excluido_na_origem = 0")
                .contains("SUM(CASE WHEN status_processo = N'Faturado' THEN valor_operacional ELSE 0 END)")
                .contains("COUNT(DISTINCT cliente_chave) AS clientes_ativos")
                .doesNotContain("ROW_NUMBER() OVER")
                .doesNotContain("WHERE TRY_CONVERT")
                .doesNotContain("TRY_CONVERT(datetimeoffset, data_emissao_cte)");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void buscarMensalDeveAgruparPorMesReferenciaNoSql() {
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(new FaturasPorClienteMensalDTO("2026-03", BigDecimal.ZERO, 0)));

        FaturasPorClienteSqlRepository repository = new FaturasPorClienteSqlRepository(
                jdbcTemplate,
                new DashboardExportSqlBuilder(PeriodoOffsetDateTimeHelper.padrao()),
                escopoSemRestricao()
        );

        repository.buscarMensal(filtroPadrao());

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));

        assertThat(sqlCaptor.getValue())
                .contains("CONVERT(CHAR(7), data_referencia_mensal, 23) AS mes")
                .contains("SUM(valor_operacional)")
                .contains("GROUP BY CONVERT(CHAR(7), data_referencia_mensal, 23)");
    }

    private static FiltroConsultaDTO filtroPadrao() {
        return new FiltroConsultaDTO(LocalDate.of(2026, 2, 21), LocalDate.of(2026, 3, 23), Map.of());
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
