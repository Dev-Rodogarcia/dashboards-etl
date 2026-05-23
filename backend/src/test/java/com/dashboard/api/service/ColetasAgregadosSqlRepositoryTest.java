package com.dashboard.api.service;

import com.dashboard.api.dto.FiltroConsultaDTO;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ColetasAgregadosSqlRepositoryTest {

    @Mock
    private NamedParameterJdbcOperations jdbcTemplate;

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void buscarRegioesOrigemDeveAgregarColetasEPesoTaxado() {
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        ColetasAgregadosSqlRepository repository = new ColetasAgregadosSqlRepository(
                jdbcTemplate,
                new DashboardExportSqlBuilder(PeriodoOffsetDateTimeHelper.padrao()),
                escopoSemRestricao()
        );

        repository.buscarRegioesOrigem(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of())
        );

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));

        assertThat(sqlCaptor.getValue())
                .contains("COUNT(DISTINCT [Coleta]) AS total_coletas")
                .contains("SUM(COALESCE([Peso Taxado], 0)) AS peso_taxado")
                .contains("GROUP BY COALESCE(NULLIF(LTRIM(RTRIM([Região da Coleta]))");
    }

    @Test
    void buscarAgingDeveUsarDataReferenciaParametrizadaEStatusPendentesParametrizados() {
        ColetasAgregadosSqlRepository repository = new ColetasAgregadosSqlRepository(
                jdbcTemplate,
                new DashboardExportSqlBuilder(PeriodoOffsetDateTimeHelper.padrao()),
                escopoSemRestricao()
        );

        repository.buscarAgingAbertas(
                new FiltroConsultaDTO(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), Map.of()),
                LocalDate.of(2026, 5, 23)
        );

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), paramsCaptor.capture(), any(RowCallbackHandler.class));

        assertThat(sqlCaptor.getValue())
                .contains("DATEDIFF(day, [Solicitacao], :dataReferencia)")
                .contains("IN (:statusPendentes)")
                .doesNotContain("GETDATE()")
                .doesNotContain("TRY_CONVERT(date, [Solicitacao])");
        assertThat(paramsCaptor.getValue().getValue("dataReferencia")).isEqualTo(LocalDate.of(2026, 5, 23));
        assertThat(paramsCaptor.getValue().getValue("statusPendentes")).isEqualTo(List.of("pendente", "em aberto"));
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
