# Skills para Claude e Codex

Este guia explica como usar skills com Claude e Codex neste monorepo.

## Regra simples

- Use `npx ai-agent-skills ...` para instalar, listar, atualizar ou remover skills.
- Use o chat normal do Claude ou do Codex para pedir tarefas e passar contexto.
- Nao e necessario rodar `npx` toda vez que for fazer uma solicitacao.

## Onde instalar

Para este projeto, o recomendado e instalar no escopo do repositorio:

```powershell
npx ai-agent-skills install frontend-design -p
```

Com `-p`, as skills vao para `.agents/skills/` e podem ser usadas tanto pelo Claude quanto pelo Codex dentro deste repositorio.

Se quiser instalar globalmente para os dois agentes fora do projeto:

```powershell
npx ai-agent-skills install frontend-design --agents claude,codex
```

## Fluxo recomendado

1. Instale a skill uma vez.
2. Abra Claude ou Codex normalmente.
3. Faca o pedido no chat com contexto claro.
4. Se precisar de outra habilidade, instale mais uma skill.

## Exemplo de instalacao inicial

```powershell
npx ai-agent-skills install frontend-design -p
npx ai-agent-skills install backend-development -p
npx ai-agent-skills install database-design -p
npx ai-agent-skills install webapp-testing -p
npx ai-agent-skills install code-documentation -p
```

## Exemplo de pedido no chat

```text
Use a skill frontend-design para melhorar a tela de filtros do dashboard-ui.

Contexto:
- projeto React + Vite + Tailwind
- manter o padrao visual atual
- nao trocar bibliotecas
- validar desktop e mobile
```

Outro exemplo:

```text
Use database-design para revisar as queries do modulo de fretes.

Contexto:
- backend Spring Boot com SQL Server
- priorizar performance e legibilidade
- nao quebrar contratos existentes
- dizer como validar o resultado
```

## Como passar contexto bem

Sempre que possivel, informe:

- o objetivo da tarefa;
- o modulo ou pasta envolvida;
- restricoes do trabalho;
- como validar se ficou certo.

Exemplo de checklist rapido:

- objetivo: o que precisa mudar;
- local: arquivo, pasta, endpoint ou tela;
- restricoes: sem novas libs, sem mudar contrato, sem refatorar o resto;
- validacao: teste, build, tela, consulta SQL, log ou checklist.

## Quando voltar ao `npx`

Use `npx ai-agent-skills ...` novamente quando quiser:

- instalar uma skill nova;
- atualizar skills;
- listar skills disponiveis;
- remover uma skill;
- verificar problemas de instalacao.

## Resumo

- `npx` habilita ou gerencia a habilidade.
- o chat executa o trabalho.
- para este repositorio, prefira `.agents/skills/` com `-p`.
