# Implementação: Tema Claro/Escuro e Ícone

Este documento descreve o padrão adotado neste projeto para tema claro/escuro e ícone, de forma agnóstica à linguagem ou framework. Qualquer stack pode seguir estes passos para reproduzir o mesmo comportamento.

---

## 1. Estrutura geral da solução

A solução é composta por quatro partes independentes que trabalham juntas:

1. **Variáveis de cor centralizadas** — um único lugar define todas as cores dos dois temas
2. **Classe CSS no elemento raiz** — a troca de tema é feita adicionando/removendo uma classe no elemento mais externo do documento
3. **Persistência da preferência** — o tema escolhido é salvo no armazenamento local do navegador
4. **Botão de alternância na interface** — um botão visível no cabeçalho controla a troca, com ícone que muda conforme o estado

---

## 2. Variáveis de cor centralizadas

### Princípio

Todos os tokens de cor da interface (fundo, card, texto, borda, cor primária) são definidos como variáveis no escopo raiz (`:root`). Uma segunda declaração, vinculada à classe de tema escuro, sobrescreve essas mesmas variáveis com os valores correspondentes ao modo escuro.

Nenhum componente deve usar valores de cor fixos. Todos os componentes consomem apenas as variáveis.

### Paleta utilizada neste projeto

| Variável             | Tema claro       | Tema escuro      | Propósito                      |
|----------------------|------------------|------------------|-------------------------------|
| `--color-primary`    | `#21478A`        | `#3B82F6`        | Cor de destaque/ação           |
| `--color-bg`         | `#F3F7FB`        | `#0B1120`        | Fundo geral da página          |
| `--color-card`       | `#FFFFFF`        | `#111827`        | Fundo de cartões/painéis       |
| `--color-border`     | `#D9E4F0`        | `#1F2937`        | Bordas e divisores             |
| `--color-text`       | `#1e293b`        | `#E5E7EB`        | Texto principal                |
| `--color-text-muted` | `#64748b`        | `#9CA3AF`        | Texto secundário/suavizado     |
| `--color-text-subtle`| `#64748b`        | `#F3F4F6`        | Texto de menor hierarquia      |

### Passo a passo

1. Declare todas as variáveis de cor no escopo global/raiz com os valores do **tema claro**
2. Crie um segundo bloco de declaração escopo ao seletor `.dark` (ou qualquer classe de tema escuro escolhida) e redefina as mesmas variáveis com os valores do **tema escuro**
3. Em todos os componentes, use apenas as variáveis — nunca valores fixos

---

## 3. Alternância por classe no elemento raiz

### Princípio

A troca de tema não altera estilos diretamente em componentes. Ela apenas adiciona ou remove uma classe (`dark`) no elemento raiz do documento (equivalente à tag `<html>`). O CSS, por cascata, aplica os valores corretos automaticamente.

### Passo a passo

1. Ao carregar a página, leia o valor salvo no armazenamento local (chave `theme`)
2. Se o valor for `dark`, adicione a classe `dark` ao elemento raiz; se for `light` ou ausente, não adicione nada
3. O **tema padrão** (sem preferência salva) é **claro**
4. A detecção automática de preferência do sistema operacional (`prefers-color-scheme`) está **desativada** — o controle é exclusivamente manual pelo usuário

---

## 4. Persistência da preferência

### Princípio

A escolha do usuário deve sobreviver ao fechamento e reabertura do navegador.

### Passo a passo

1. Ao inicializar a aplicação, leia o `localStorage` na chave `theme`
2. Ao alternar o tema, escreva o novo valor (`"light"` ou `"dark"`) no `localStorage` sob a mesma chave
3. Não use cookies nem variáveis de sessão — o `localStorage` é suficiente e não expira

---

## 5. Botão de alternância

### Princípio

Um botão no cabeçalho (canto superior direito) permite ao usuário trocar o tema. O ícone do botão reflete o estado atual: exibe **sol** quando o tema ativo é claro, e **lua** quando o tema ativo é escuro.

### Passo a passo

1. Coloque o botão no canto superior direito da barra de navegação/cabeçalho
2. Use dois ícones SVG inline (sol e lua) — exiba apenas o correspondente ao tema atual
3. A ação do botão inverte o tema atual:
   - se `dark` → muda para `light`
   - se `light` → muda para `dark`
4. Após a mudança, salve a preferência (passo 4) e atualize a classe no elemento raiz (passo 3)
5. O botão deve ter estado de hover/foco visível e tamanho confortável para clique (mínimo ~28×28 px)

### Ícones SVG utilizados

Os ícones são desenhados inline (sem biblioteca externa) para manter controle total sobre tamanho e cor.

**Sol (tema claro ativo):**
- Círculo central
- 8 linhas radiais curtas ao redor

**Lua (tema escuro ativo):**
- Meia-lua formada por dois círculos sobrepostos (técnica de "recorte")

Ambos os ícones herdam a cor do texto via `currentColor` para se adaptar automaticamente ao tema.

---

## 6. Logo / imagem de marca com adaptação de tema

### Princípio

Quando uma imagem PNG ou similar é usada como logo no cabeçalho, ela pode não ter boa legibilidade no tema escuro (ex.: logo escura sobre fundo escuro).

### Solução adotada

Aplicar ao elemento da imagem os filtros CSS `brightness(0) invert(1)` **somente quando o tema escuro estiver ativo**. Isso transforma qualquer imagem colorida ou escura em branco puro, garantindo visibilidade sobre fundos escuros.

### Passo a passo

1. Inclua o logo no cabeçalho com tamanho fixo (altura ~28 px, largura automática)
2. Adicione uma regra CSS que, dentro do seletor `.dark`, aplique `filter: brightness(0) invert(1)` ao elemento do logo
3. Nenhuma imagem alternativa é necessária — o filtro CSS é suficiente para logos simples

---

## 7. Ícone da aplicação (favicon)

### Princípio

O ícone exibido na aba do navegador é um arquivo SVG, o que garante nitidez em qualquer resolução e tamanho de tela.

### Passo a passo

1. Crie o ícone em formato SVG (preferencialmente 48×48 px ou proporcional)
2. Referencie-o no cabeçalho do documento HTML:
   ```
   <link rel="icon" type="image/svg+xml" href="/favicon.svg" />
   ```
3. O SVG pode usar gradientes, formas e cores fixas — ele não precisa responder ao tema, pois é exibido fora da aplicação
4. Não é necessário fornecer versões PNG como fallback para navegadores modernos

---

## 8. Tipografia

### Princípio

A fonte da interface é definida uma única vez no escopo global e aplicada a todos os elementos via `font-family` no corpo do documento.

### Fonte utilizada neste projeto

**Manrope** — fonte sem serifa moderna, carregada via Google Fonts.

### Passo a passo

1. Importe a fonte no ponto de entrada do documento (head do HTML ou arquivo de estilos global)
2. Aplique-a como `font-family` padrão no seletor do corpo (`body`)
3. Defina um fallback genérico (`sans-serif`) para o caso de falha no carregamento

---

## 9. Resumo do fluxo completo

```
Usuário abre a aplicação
  └─ Lê localStorage["theme"]
       ├─ "dark"  → adiciona classe .dark no elemento raiz
       └─ outro/ausente → mantém sem classe (tema claro padrão)

Usuário clica no botão de tema
  └─ Inverte o tema atual
       ├─ Remove/adiciona classe .dark no elemento raiz
       └─ Salva novo valor em localStorage["theme"]

CSS (sempre ativo)
  └─ :root define variáveis de cor para tema claro
  └─ .dark redefine as mesmas variáveis para tema escuro
  └─ Todos os componentes leem as variáveis → atualizam automaticamente
```

---

## 10. O que este padrão NÃO faz (decisões explícitas)

| Funcionalidade                              | Decisão              | Motivo                                              |
|---------------------------------------------|----------------------|-----------------------------------------------------|
| Detecção automática de tema do sistema      | Desativada           | Controle total pelo usuário, comportamento previsível |
| Temas adicionais (ex.: alto contraste)      | Não implementado     | Não era requisito                                   |
| Sincronização entre abas do navegador       | Não implementado     | Não era requisito                                   |
| Animação de transição ao trocar tema        | Não implementado     | Preferência de simplicidade                         |
| Favicon diferente por tema                  | Não implementado     | SVG único é suficiente                              |
