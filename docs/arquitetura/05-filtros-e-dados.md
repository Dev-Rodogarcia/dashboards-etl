# Filtros, Periodos e Semantica de Dados

## Por que este documento existe

Historicamente, as maiores regressoes do BI vieram de uma mistura incorreta entre:

- `LocalDate` vs `OffsetDateTime`
- `between` vs janela `>=` e `<`
- UTC tecnico vs dia civil local
- SQL de validacao diferente da API

Este documento fixa a semantica atual.

## Contrato publico de filtro

Toda rota de dashboard recebe:

- `dataInicio=YYYY-MM-DD`
- `dataFim=YYYY-MM-DD`
- filtros adicionais via query string com prefixo `f.`

Exemplo:

```text
/api/painel/fretes?dataInicio=2026-02-24&dataFim=2026-03-26&f.filiais=SP&f.filiais=RJ
```

No backend, isso vira `FiltroConsultaDTO` via `FiltroRequestMapper`.

## Limite de periodo

`ValidadorPeriodoService` aplica:

- datas obrigatorias;
- inicio nao pode ser maior que fim;
- periodo maximo de `365 dias`.

Erro esperado:

- `400 Bad Request`
- mensagem descritiva para a UI

## Tipos de campo de data no projeto

### Grupo A: filtros por `LocalDate`

A comparacao e feita por dia puro, sem timezone.

Modulos:

- Coletas -> `solicitacao`
- Contas a Pagar -> `emissao`
- Faturas financeiro -> `emissao`

Padrao recomendado:

- `between(dataInicio, dataFim)`

### Grupo B: filtros por `DATETIMEOFFSET`

O filtro representa dia civil local, mas a coluna representa instante.

Modulos:

- Fretes -> `dataFrete`
- Tracking -> `dataFrete`
- Manifestos -> `dataCriacao`
- Cotacoes -> `dataCotacao`
- Faturas operacional -> `dataEmissaoCte`
- Faturas por Cliente -> `dataEmissaoCte`

Padrao obrigatorio:

- timezone canonico: `America/Sao_Paulo`
- inicio: `dataInicio 00:00 local`
- fim exclusivo: `dataFim + 1 dia 00:00 local`
- comparacao: `>= inicioInclusivo` e `< fimExclusivo`

## Helper oficial para `DATETIMEOFFSET`

Arquivo:

- `dashboard-api/src/main/java/com/dashboard/api/service/PeriodoOffsetDateTimeHelper.java`

Responsabilidade:

- centralizar a traducao de `LocalDate` para `OffsetDateTime`;
- eliminar duplicacao e evitar regressao para UTC puro.

## Regra de ouro para SQL e validacao BI

Se a API usa dia local, o SQL de validacao tambem precisa usar dia local.

Consequencia pratica:

- `scripts/dashboard-validation/entities.mjs` deve espelhar a mesma janela da API;
- nao aceitar um validador com semantica diferente do backend;
- divergencia entre SQL e API pode ser bug no sistema ou bug no proprio harness de validacao.

## Presets do frontend

O frontend oferece atalhos:

- `7d`
- `15d`
- `30d`
- `60d`
- `90d`
- `180d`

Eles usam utilitarios em `src/utils/dateUtils.ts` e escrevem as datas finais na URL.

Importante:

- o frontend envia datas locais;
- o backend decide a interpretacao final conforme o tipo da coluna;
- nao usar `toISOString()` para montar filtros de data de UI.

## Erros classicos e como evitar

### Erro 1: usar UTC em `DATETIMEOFFSET`

Sintoma:

- dashboards de 30d e 60d pegam linhas do dia anterior local;
- divergencia pequena e persistente entre SQL e API.

Correcao:

- usar `PeriodoOffsetDateTimeHelper`;
- comparar com `>=` e `<`.

### Erro 2: usar `between` em fim do dia

Sintoma:

- risco de perder linhas com fracao de segundo ou offset inesperado.

Correcao:

- trocar por janela fechada-aberta.

### Erro 3: SQL do validador nao acompanha a API

Sintoma:

- BI aparece divergente mesmo com endpoint correto.

Correcao:

- alinhar o SQL de `entities.mjs` com a semantica atual do backend.

## Checklist para adicionar um novo filtro de data

1. identificar o tipo real da coluna na view;
2. decidir se o dado representa dia ou instante;
3. usar o helper oficial se houver `OffsetDateTime`;
4. cobrir com teste de service;
5. atualizar o validador BI;
6. documentar o campo no catalogo de dashboards.
