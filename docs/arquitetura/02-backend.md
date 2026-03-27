# Arquitetura Backend

## Stack e objetivo

O backend e uma API Spring Boot 3.2 que consolida dados do SQL Server para dashboards e modulos administrativos. O foco nao e CRUD generico; o foco e expor contratos de leitura e operacao com seguranca, filtros consistentes e mensagens de erro acionaveis.

## Estrutura de pacotes

```text
dashboard-api/src/main/java/com/dashboard/api/
|-- config/
|-- controller/
|-- dto/
|-- exception/
|-- model/
|-- repository/
|-- security/
`-- service/
    `-- acesso/
```

## Responsabilidades por camada

### `controller/`

Responsavel por:

- declarar a rota;
- validar autenticacao/autorizacao via `@PreAuthorize`;
- converter `request params` em `FiltroConsultaDTO`;
- devolver DTOs, nunca entidades JPA.

Padrao atual:

- dashboards usam `FiltroRequestMapper.from(dataInicio, dataFim, params)`;
- filtros extras entram como `f.<chave>`;
- cada controller de dashboard expone overview, serie, graficos e/ou tabela.

### `service/`

Responsavel por:

- validar periodo com `ValidadorPeriodoService`;
- aplicar agregacoes e calculos de negocio;
- decidir entre `Specification` e fast path legado do repository;
- consolidar linhas SQL em DTOs finais;
- garantir semantica correta de data para `LocalDate` e `DATETIMEOFFSET`.

Arquivos centrais:

- `ConsultaFiltroUtils.java`
- `ConsultaLimiteUtils.java`
- `ConsultaSpecificationUtils.java`
- `PeriodoOffsetDateTimeHelper.java`
- `ValidadorPeriodoService.java`

### `repository/`

Responsavel por:

- encapsular acesso a entidades/vistas SQL;
- expor `findAll(Specification)` para caminho flexivel;
- manter metodos especificos quando existir fast path legado de alta frequencia.

Regra importante:

- para `OffsetDateTime`, prefira metodos `GreaterThanEqualAndLessThan` ou `Specification` com `>=` e `<`;
- evite `between` quando a coluna representa instante e o filtro representa dia civil.

### `exception/`

`ManipuladorGlobalExcecoes` padroniza erros relevantes:

- `400` para validacao de entrada e periodo invalido;
- `403` para acesso negado;
- `408` para timeout de consulta;
- `503` para indisponibilidade de banco;
- `500` para falhas nao tratadas.

Essa padronizacao e parte do contrato com a UI.

## Fluxo padrao para um novo dashboard

1. Criar DTOs de overview, serie, graficos e tabela.
2. Criar controller em `/api/painel/<modulo>`.
3. Chamar `FiltroRequestMapper`.
4. Validar o periodo no service.
5. Definir o campo de data correto da view.
6. Se a view usa `DATETIMEOFFSET`, construir janela com `PeriodoOffsetDateTimeHelper`.
7. Aplicar filtros de escopo e filtros textuais com `ConsultaSpecificationUtils`.
8. Limitar tabelas com `ConsultaLimiteUtils`.
9. Cobrir com teste de service.
10. Incluir o modulo na validacao BI em `scripts/dashboard-validation/entities.mjs`.

## Semantica de consulta por tipo de data

### `LocalDate`

Use quando a view representa dia de negocio, nao instante.

Exemplos:

- `coletas.solicitacao`
- `contas_a_pagar.emissao`
- `faturas_graphql.emissao`

Padrao:

- `between(dataInicio, dataFim)`

### `OffsetDateTime`

Use quando a view representa instante real, com offset.

Exemplos:

- `fretes.dataFrete`
- `tracking.dataFrete`
- `manifestos.dataCriacao`
- `cotacoes.dataCotacao`
- `faturas_por_cliente.dataEmissaoCte`

Padrao:

- janela local em `America/Sao_Paulo`
- `inicioInclusivo = dataInicio 00:00 local`
- `fimExclusivo = dataFim + 1 dia 00:00 local`
- filtros `>= inicioInclusivo` e `< fimExclusivo`

## Decisoes recentes que precisam ser preservadas

- periodo maximo aumentado para `365 dias`;
- timeout JPA aumentado para `30000 ms`;
- timeout de consulta mapeado para `408`;
- helper central de `DATETIMEOFFSET` introduzido para evitar regressao por UTC;
- overview de `Faturas` nao pode zerar `clientesAtivos` quando houver base operacional sem titulos financeiros.

## Checklist de revisao backend

Antes de considerar uma mudanca pronta:

- o service valida periodo?
- o filtro de data usa o tipo correto?
- o endpoint devolve DTO e nao entidade?
- existe teste cobrindo a regra nova?
- se o modulo entra no BI, o script de validacao foi atualizado?
- a mensagem de erro do backend chega clara para a UI?
