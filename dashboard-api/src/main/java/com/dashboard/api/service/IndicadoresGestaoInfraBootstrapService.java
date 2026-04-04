package com.dashboard.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Order(30)
public class IndicadoresGestaoInfraBootstrapService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IndicadoresGestaoInfraBootstrapService.class);

    private final JdbcTemplate jdbcTemplate;
    private final boolean failOnSchemaMismatch;

    public IndicadoresGestaoInfraBootstrapService(
            JdbcTemplate jdbcTemplate,
            @Value("${dashboard.indicadores.fail-on-schema-mismatch:false}") boolean failOnSchemaMismatch
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.failOnSchemaMismatch = failOnSchemaMismatch;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            auditarCompatibilidadeViews();
        } catch (DataAccessException ex) {
            log.error("Falha ao auditar infraestrutura de Indicadores de Gestão à Vista: {}", ex.getMessage(), ex);
            if (failOnSchemaMismatch) {
                throw ex;
            }
        }
    }

    private void auditarCompatibilidadeViews() {
        List<String> inconsistencias = new ArrayList<>();
        inconsistencias.addAll(auditarObjeto("vw_fretes_powerbi", List.of(
                "ID", "Nº Minuta", "Filial Emissora", "Responsável pela Região de Destino", "Previsão de Entrega",
                "Data de Finalização", "Performance Status", "Peso Cubado", "Total M3", "Data de extracao"
        )));
        inconsistencias.addAll(auditarObjeto("vw_localizacao_cargas_powerbi", List.of(
                "N° Minuta", "Filial Emissora", "Responsável pela Região de Destino", "Data de extracao"
        )));
        inconsistencias.addAll(auditarObjeto("vw_manifestos_powerbi", List.of(
                "Número", "Filial Emissora", "Local de Descarregamento", "Data criação", "Data de extracao"
        )));
        inconsistencias.addAll(auditarObjeto("vw_inventario_powerbi", List.of(
                "Identificador Único", "N° Ordem", "Nº Minuta", "Filial Emissora do Frete", "Tipo", "Status",
                "Data/Hora início", "Data de extracao"
        )));
        inconsistencias.addAll(auditarObjeto("vw_sinistros_powerbi", List.of(
                "Identificador Único", "Nº do Sinistro", "Data abertura", "Minuta", "Resultado final",
                "Pessoa/Nome fantasia", "Data de extracao"
        )));
        inconsistencias.addAll(auditarObjeto("horarios_corte", List.of(
                "data_operacao", "linha_ou_operacao_original", "linha_ou_operacao_chave", "filial_canonica",
                "sm_gerada", "corte", "nome_arquivo"
        )));
        inconsistencias.addAll(auditarObjeto("vw_horarios_corte_powerbi", List.of(
                "Data", "Filial", "Saiu no Horário", "Atraso Minutos", "Data de extracao"
        )));

        if (!inconsistencias.isEmpty() && failOnSchemaMismatch) {
            throw new IllegalStateException("Auditoria Indicadores de Gestão encontrou incompatibilidades: " + inconsistencias);
        }
    }

    private List<String> auditarObjeto(String nomeObjeto, List<String> colunasEsperadas) {
        Integer existe = jdbcTemplate.queryForObject("""
                SELECT
                    (SELECT COUNT(1) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?)
                  + (SELECT COUNT(1) FROM INFORMATION_SCHEMA.VIEWS WHERE TABLE_NAME = ?)
                """, Integer.class, nomeObjeto, nomeObjeto);

        if (existe == null || existe == 0) {
            log.warn("Auditoria Indicadores de Gestão: objeto {} não encontrado.", nomeObjeto);
            return List.of("Objeto ausente: " + nomeObjeto);
        }

        List<String> colunasEncontradas = jdbcTemplate.queryForList("""
                SELECT COLUMN_NAME
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = ?
                """, String.class, nomeObjeto);

        List<String> faltantes = colunasEsperadas.stream()
                .filter(coluna -> !colunasEncontradas.contains(coluna))
                .toList();

        if (faltantes.isEmpty()) {
            log.info("Auditoria Indicadores de Gestão: {} compatível com colunas esperadas.", nomeObjeto);
            return List.of();
        }

        log.warn("Auditoria Indicadores de Gestão: {} com colunas faltantes: {}", nomeObjeto, faltantes);
        return faltantes.stream()
                .map(coluna -> nomeObjeto + "." + coluna)
                .toList();
    }
}
