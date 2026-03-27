# Arquitetura Frontend

## Papel do frontend

O frontend organiza a experiencia do usuario, aplica protecao de rotas, controla estado remoto e sincroniza filtros na URL. Ele nao deve reimplementar regras de negocio do backend.

## Estrutura principal

```text
dashboard-ui/src/
|-- api/
|   |-- clienteAxios.ts
|   `-- endpoints/
|-- components/
|-- config/
|-- contexts/
|-- hooks/
|   `-- queries/
|-- pages/
|-- types/
`-- utils/
```

## Ponto de entrada

`src/App.tsx` monta:

- `QueryClientProvider`
- `BrowserRouter`
- `AutenticacaoProvider`
- `FiltroProvider`
- `RotaProtegida`
- `LayoutPainel`

Essa ordem importa. Sem ela, a UI perde sessao, cache ou sincronismo de filtros.

## Sessao e autenticacao

`AutenticacaoContext.tsx` faz o bootstrap da sessao com `obterSessao()` e `buscarSessaoAtual()`.

Responsabilidades:

- restaurar o token salvo;
- validar a sessao com `/api/auth/me`;
- expor `login`, `logout` e `alterarSenha`;
- apagar a sessao local quando refresh ou `me` falharem.

## Cliente HTTP e refresh silencioso

`src/api/clienteAxios.ts` concentra:

- `baseURL` via `VITE_API_BASE_URL`;
- envio do header `Authorization`;
- `withCredentials: true` para refresh token em cookie;
- tentativa automatica de refresh ao receber `401`;
- redirecionamento para `/acesso-negado` ao receber `403`.

Regra de manutencao:

- nao criar clientes Axios paralelos;
- qualquer nova integracao HTTP deve passar por `clienteAxios`.

## Filtros globais

`FiltroContext.tsx` usa `useSearchParams` como fonte de verdade.

Contrato:

- `dataInicio` e `dataFim` vivem na URL;
- filtros adicionais usam `f.<chave>`;
- limpar filtros deve manter o dashboard compartilhavel;
- presets do seletor de datas usam utilitarios em `src/utils/dateUtils.ts`.

## React Query

Cada dominio possui hooks dedicados em `src/hooks/queries`.

Beneficios:

- cache por chave;
- loading e erro independentes por bloco;
- invalidacao localizada;
- isolamento entre overview, serie, graficos e tabela.

Regra de projeto:

- o `queryKey` deve conter modulo, tipo de dado e filtro;
- um novo dashboard deve seguir o mesmo padrao de granularidade.

## Tratamento de erro

Arquivos centrais:

- `src/utils/apiError.ts`
- `src/components/ui/MensagemErro.tsx`

Contrato atual:

- ler `mensagem` do nosso backend;
- fazer fallback para `message` do Spring Boot;
- classificar visualmente em `periodo`, `timeout`, `indisponivel` e `erro`.

Regra de manutencao:

- nao hardcodar strings de erro dentro das paginas;
- sempre usar `getApiErrorMessage()` e `getTipoErro()`.

## Roteamento e permissoes

`RotaProtegida` usa o contrato de acesso definido em `src/utils/accessControl.ts`.

Conceitos:

- `admin_plataforma`
- `admin_acesso`
- `usuario_comum`

As paginas protegidas vivem em `src/pages/` e devem ser acessadas apenas quando o `permission key` correspondente estiver ativo.

## Como criar uma nova pagina de dashboard

1. Criar tipos em `src/types`.
2. Criar endpoints em `src/api/endpoints/<modulo>Servico.ts`.
3. Criar hooks em `src/hooks/queries/use<Modulo>.ts`.
4. Criar pagina em `src/pages/<Modulo>Page.tsx`.
5. Reutilizar `FiltroContext`, `DateRangePicker`, `MensagemErro` e componentes de tabela/grafico existentes.
6. Registrar a rota em `App.tsx`.
7. Adicionar item de navegacao em `src/utils/accessControl.ts`.
8. Cobrir utilitarios puros com teste quando houver logica nova.

## Antipadroes que devem ser evitados

- tratar erro diretamente com `error.message` sem parse do payload;
- guardar filtros fora da URL;
- usar fetch cru fora de `clienteAxios`;
- misturar transformacao visual com regra de negocio no mesmo componente;
- acoplar um grafico a outro via estado compartilhado desnecessario.
