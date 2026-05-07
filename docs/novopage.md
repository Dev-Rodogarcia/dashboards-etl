**Base Encontrada**
Li a estrutura do projeto antes de propor. Hoje o `/` redireciona para o primeiro dashboard permitido; não existe Home real. O menu e as permissões vêm de `DASHBOARD_NAV_ITEMS`, `PermissaoCatalogo` e `permissoesEfetivas`. Os setores atuais mapeiam acessos como: TI vê `ETL Saúde` e `Indicadores de Gestão à Vista`; Financeiro vê `Faturas`, `Contas a Pagar`, `Cotações`; Logística vê `Coletas`, `Manifestos`, `Fretes`, `Tracking`, `Cotações`, `Indicadores`.

**Diretriz Principal**
A Home deve ser a rota `/`, e o logo deve levar para ela. Ela não deve competir com os dashboards; deve funcionar como uma central de orientação: “o que eu posso acessar, o que importa hoje e para onde eu vou agora”.

**Componentes Essenciais**

1. Cabeçalho da HomeSaudação curta, nome do usuário, setor, papel administrativo se houver, quantidade de dashboards liberados e escopo de filiais.
2. Busca globalCampo “Buscar dashboards, tabelas ou indicadores” filtrando apenas itens permitidos. Exemplo: buscar “fatura” mostra `Faturas` e `Faturas por Cliente` somente se liberados.
3. Minhas Tabelas FavoritasCards fixos no topo com estrela, ícone, nome, descrição curta e botão “Abrir”. Se o usuário ainda não favoritou nada, mostrar “Sugestões para seu perfil”.
4. Atalhos LiberadosLista/grid com todos os dashboards permitidos: `Coletas`, `Manifestos`, `Fretes`, `Tracking`, `Faturas`, `Faturas por Cliente`, `Contas a Pagar`, `Cotações`, `Indicadores de Gestão à Vista`, `Executivo`, `ETL Saúde`.
5. Catálogo de Dashboards/TabelasTabela com busca, categoria, descrição, última atualização quando disponível, permissão necessária e ação. Categorias sugeridas: Operação, Financeiro, Comercial, Executivo, TI/ETL.
6. Comunicados InternosMural editável por administradores específicos, com prioridade, público-alvo e validade.
7. Indicadores Rápidos
   Poucos KPIs, não todos. Exibir só de dashboards permitidos e preferencialmente favoritos/recentes para evitar sobrecarregar a Home.

**Permissões e Exibição Dinâmica**
A Home deve usar `permissoesEfetivas` como fonte de verdade. Nada deve aparecer “bloqueado” para usuários comuns; se não tem acesso, não entra na Home. Admins podem ver uma visão ampliada, mas ainda com indicação clara de perfil.

Regra recomendada:

- Usuário comum: vê apenas dashboards permitidos.
- Admin acesso: vê Home normal + bloco administrativo pequeno.
- Admin plataforma/desenvolvedor: vê todos os dashboards + status/administração.
- Conteúdo do mural: filtrado por `todos`, `setores`, `permissoes`, ou `papeis`.

**Admin Editável**
Administradores específicos devem editar:

- Comunicados: título, texto, prioridade, publicação, expiração, público-alvo.
- Avisos críticos: aparecem em faixa no topo até expirar ou serem resolvidos.
- “O que há de novo”: novidades de dashboards, permissões, exportações e mudanças operacionais.
- Links úteis: documentos, templates, runbooks, arquivos de apoio.
- Status manual do sistema: aviso de manutenção, instabilidade conhecida, janela de atualização.

Sugestão de governança: criar permissão própria de conteúdo, tipo `homeComunicadosManage`, em vez de amarrar tudo somente ao `admin_acesso`.

**Alertas, Novidades e Críticos**

- Aviso crítico: faixa vermelha/âmbar acima de tudo, com validade e público-alvo.
- Comunicado comum: card no mural, ordenado por fixado e data.
- O que há de novo: feed compacto com etiquetas “Novo dashboard”, “Mudança de regra”, “Exportação”, “Permissão”.
- Status ETL: para quem tem acesso a `etlSaude`, mostrar resumo de saúde do ETL; para outros, mostrar apenas aviso geral se houver impacto.

**Versão A: Simples e Extremamente Funcional**
Wireframe:

```text
[TopNav com logo -> Home]

[Olá, Lucas] [Setor: TI] [Dashboards liberados: 3] [Filiais: 8]

[Buscar dashboards, tabelas ou indicadores...]

[Minhas Favoritas]
[ETL Saúde] [Indicadores Gestão à Vista] [Tracking]

[Meus Acessos]
Operação: Coletas, Manifestos, Fretes, Tracking
Financeiro: Faturas, Contas a Pagar
TI: ETL Saúde
Executivo: Executivo

[Catálogo de Dashboards/Tabelas]
Nome | Área | Descrição | Última atualização | Ação | Favorito

[Comunicados Internos]
Fixados + recentes
```

Melhor para implementar primeiro. Resolve o problema do logo, onboarding, permissões e favoritos com pouco risco.

**Versão B: Portal Corporativo Moderno**
Wireframe:

```text
[TopNav com logo -> Home]

[Faixa de aviso crítico, se existir]

[Resumo do usuário]
Nome, setor, papel, filiais efetivas, dashboards liberados

[Busca global / command palette]

Coluna principal:
[Favoritos e Recentes]
[Indicadores rápidos do meu perfil]
[Catálogo de Dashboards/Tabelas com categorias e filtros]

Coluna lateral:
[Comunicados]
[O que há de novo]
[Status do Sistema / ETL]
[Links úteis e documentos]
[Admin: publicar comunicado]
```

Essa versão dá sensação mais próxima de Power BI Service/Tableau/SAP: central pessoal, conteúdo corporativo, status operacional e catálogo navegável.

**Recomendação**
Começaria pela Versão A como MVP da Home e já deixaria a arquitetura preparada para evoluir para a Versão B. A ordem ideal de entrega é: rota Home no `/`, logo clicável, catálogo permissionado, favoritos, mural administrativo e depois indicadores rápidos/status.
