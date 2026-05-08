package com.dashboard.api.service.acesso;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(5)
public class HomeComunicadosSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(HomeComunicadosSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public HomeComunicadosSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!schemaExiste("acesso")) {
                log.warn("Schema 'acesso' não encontrado. Bootstrap de comunicados da Home não executado.");
                return;
            }

            if (!tabelaExiste("acesso.home_comunicados")) {
                jdbcTemplate.execute("""
                    CREATE TABLE acesso.home_comunicados (
                        id             BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                        titulo         NVARCHAR(140)        NOT NULL,
                        corpo          NVARCHAR(700)        NOT NULL,
                        tag            VARCHAR(20)          NOT NULL,
                        publico_alvo   NVARCHAR(140)        NOT NULL
                            CONSTRAINT DF_home_comunicados_publico_alvo DEFAULT N'Todos',
                        publicado_em   DATETIME2(0)         NOT NULL
                            CONSTRAINT DF_home_comunicados_publicado_em DEFAULT SYSUTCDATETIME(),
                        ativo          BIT                  NOT NULL
                            CONSTRAINT DF_home_comunicados_ativo DEFAULT 1,
                        criado_por     NVARCHAR(120)        NULL,
                        criado_em      DATETIME2(0)         NOT NULL
                            CONSTRAINT DF_home_comunicados_criado_em DEFAULT SYSUTCDATETIME(),
                        atualizado_por NVARCHAR(120)        NULL,
                        atualizado_em  DATETIME2(0)         NULL
                    )
                    """);
                log.info("Tabela 'acesso.home_comunicados' criada automaticamente.");
            }

            if (!constraintExiste("acesso.home_comunicados", "CK_home_comunicados_tag")) {
                jdbcTemplate.execute("""
                    ALTER TABLE acesso.home_comunicados
                    WITH CHECK ADD CONSTRAINT CK_home_comunicados_tag
                    CHECK (tag IN ('NOVO', 'ATENCAO', 'FIXADO'))
                    """);
                log.info("Constraint 'CK_home_comunicados_tag' criada automaticamente.");
            }

            if (!indiceExiste("acesso.home_comunicados", "IX_home_comunicados_ativo_publicado")) {
                jdbcTemplate.execute("""
                    CREATE INDEX IX_home_comunicados_ativo_publicado
                    ON acesso.home_comunicados (ativo, publicado_em DESC, id DESC)
                    """);
                log.info("Índice 'IX_home_comunicados_ativo_publicado' criado automaticamente.");
            }

            if (tabelaVazia("acesso.home_comunicados")) {
                jdbcTemplate.update("""
                    INSERT INTO acesso.home_comunicados (titulo, corpo, tag, publico_alvo, criado_por)
                    VALUES
                        (?, ?, 'NOVO', ?, 'sistema'),
                        (?, ?, 'FIXADO', ?, 'sistema'),
                        (?, ?, 'ATENCAO', ?, 'sistema')
                    """,
                    "Indicadores de Gestão à Vista disponíveis",
                    "Performance de entrega, coletores, cubagem, indenização e horários de corte centralizados no painel operacional.",
                    "Operação, TI e Diretoria",
                    "Acesso por setor segue permissões efetivas",
                    "A Home mostra somente atalhos liberados para o usuário autenticado, respeitando setor, papel e exceções individuais.",
                    "Todos",
                    "Monitoramento do ETL em destaque",
                    "Acompanhe execuções, volume processado e erros no painel ETL Saúde quando a permissão estiver liberada.",
                    "TI e administradores"
                );
                log.info("Comunicados iniciais da Home criados automaticamente.");
            }
        } catch (DataAccessException ex) {
            log.warn("Bootstrap de comunicados da Home não conseguiu aplicar DDL automaticamente. Execute a migration V012 com um usuário de banco com permissão de ALTER/CREATE. Motivo: {}", ex.getMessage());
        }
    }

    private boolean schemaExiste(String nomeSchema) {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sys.schemas WHERE name = ?",
                Integer.class,
                nomeSchema
        );
        return total != null && total > 0;
    }

    private boolean tabelaExiste(String nomeCompletoTabela) {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) WHERE OBJECT_ID(?, 'U') IS NOT NULL",
                Integer.class,
                nomeCompletoTabela
        );
        return total != null && total > 0;
    }

    private boolean constraintExiste(String nomeCompletoTabela, String nomeConstraint) {
        Integer total = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(1)
                FROM sys.check_constraints
                WHERE name = ?
                  AND parent_object_id = OBJECT_ID(?, 'U')
                """,
                Integer.class,
                nomeConstraint,
                nomeCompletoTabela
        );
        return total != null && total > 0;
    }

    private boolean indiceExiste(String nomeCompletoTabela, String nomeIndice) {
        Integer total = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(1)
                FROM sys.indexes
                WHERE name = ?
                  AND object_id = OBJECT_ID(?, 'U')
                """,
                Integer.class,
                nomeIndice,
                nomeCompletoTabela
        );
        return total != null && total > 0;
    }

    private boolean tabelaVazia(String nomeCompletoTabela) {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM " + nomeCompletoTabela,
                Integer.class
        );
        return total == null || total == 0;
    }
}
