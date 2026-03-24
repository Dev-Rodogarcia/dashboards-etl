# Guia de Integracao SQL, DTOs e Views

## Objetivo

Este arquivo consolida o contrato tecnico que um novo projeto precisa conhecer para consumir os dados extraidos por este ETL no SQL Server.

O foco deste guia e:

- mostrar o caminho completo `API -> DTO -> Mapper -> Entity -> Repository -> Tabela -> View`;
- documentar quais objetos SQL devem ser consumidos;
- deixar claro onde cada dominio nasce, onde e persistido e qual view deve ser usada no projeto consumidor;
- registrar observacoes importantes sobre `metadata`, chaves, joins e dimensoes.

## Regra principal de consumo

Para um novo projeto consumidor, a regra recomendada e:

1. Consumir **views** `dbo.vw_*_powerbi` e `dbo.vw_dim_*`.
2. Ler **tabelas base** `dbo.*` somente quando for necessario acessar campo tecnico, auditoria, `metadata` bruto ou alguma coluna ainda nao exposta em view.
3. Modelar os DTOs do novo projeto em cima das **colunas das views**, e nao em cima dos DTOs originais das APIs.

## Fluxo de dados do extrator

```text
API GraphQL / API DataExport
    -> DTO de origem
    -> Mapper
    -> Entity de persistencia
    -> Repository
    -> Tabela SQL Server
    -> View Power BI / View Dimensional
    -> Projeto consumidor
```

## Observacoes criticas de contrato

- O contrato mais seguro para integracao externa e o conjunto de views definido em `src/main/java/br/com/extrator/suporte/formatacao/ConstantesViewsPowerBI.java`.
- Nem todo campo presente em `Entity` Java existe fisicamente na tabela. Para integracao SQL, confie primeiro no DDL em `database/tabelas/` e nas `views`.
- Quase todas as entidades de negocio preservam o payload completo da origem em `metadata`. Quando algum campo da API ainda nao estiver materializado em coluna dedicada, ele tende a estar presente ali.
- `coletas` e `manifestos` possuem relacao logica por `coletas.sequence_code = manifestos.pick_sequence_code`.
- `coletas.cancellation_user_id` e `coletas.destroy_user_id` se relacionam logicamente com `dim_usuarios.user_id`.
- `vw_faturas_por_cliente_powerbi` ja faz `LEFT JOIN` com `dbo.faturas_graphql` por `faturas_por_cliente.fit_ant_document = faturas_graphql.document`.
- As views dimensionais foram criadas para consumo analitico e para facilitar joins 1:N no Power BI e em outros consumidores SQL.

## Onde o esquema mora

- Tabelas: `database/tabelas/`
- Views principais: `database/views/`
- Views dimensionais: `database/views-dimensao/`
- Migrations complementares: `database/migrations/`
- Indices extras de performance: `database/indices/001_criar_indices_performance.sql`
- Validacoes de integridade: `database/validacao/`
- Documentacao antiga de DER: `docs/DER-COMPLETO-BANCO-DADOS.md`

## Mapa fim a fim por entidade

| Dominio | Fonte | DTO principal | Mapper | Entity | Repository | Extractor | Tabela SQL | View de consumo |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Coletas | GraphQL `pick` | `ColetaNodeDTO` | `ColetaMapper` | `ColetaEntity` | `ColetaRepository` | `ColetaExtractor` | `dbo.coletas` | `dbo.vw_coletas_powerbi` |
| Fretes | GraphQL `freight` | `FreteNodeDTO` | `FreteMapper` | `FreteEntity` | `FreteRepository` | `FreteExtractor` | `dbo.fretes` | `dbo.vw_fretes_powerbi` |
| Faturas GraphQL | GraphQL `creditCustomerBilling` | `CreditCustomerBillingNodeDTO` | `FaturaGraphQLEntityMapper` | `FaturaGraphQLEntity` | `FaturaGraphQLRepository` | `FaturaGraphQLExtractor` | `dbo.faturas_graphql` | `dbo.vw_faturas_graphql_powerbi` |
| Usuarios | GraphQL `individuals/users` | `IndividualNodeDTO` | `UsuarioSistemaMapper` | `UsuarioSistemaEntity` | `UsuarioSistemaRepository` | `UsuarioSistemaExtractor` | `dbo.dim_usuarios` | `dbo.vw_dim_usuarios` |
| Manifestos | DataExport template `6399` | `ManifestoDTO` | `ManifestoMapper` | `ManifestoEntity` | `ManifestoRepository` | `ManifestoExtractor` | `dbo.manifestos` | `dbo.vw_manifestos_powerbi` |
| Cotacoes | DataExport template `6906` | `CotacaoDTO` | `CotacaoMapper` | `CotacaoEntity` | `CotacaoRepository` | `CotacaoExtractor` | `dbo.cotacoes` | `dbo.vw_cotacoes_powerbi` |
| Localizacao de Cargas | DataExport template `8656` | `LocalizacaoCargaDTO` | `LocalizacaoCargaMapper` | `LocalizacaoCargaEntity` | `LocalizacaoCargaRepository` | `LocalizacaoCargaExtractor` | `dbo.localizacao_cargas` | `dbo.vw_localizacao_cargas_powerbi` |
| Contas a Pagar | DataExport template `8636` | `ContasAPagarDTO` | `ContasAPagarMapper` | `ContasAPagarDataExportEntity` | `ContasAPagarRepository` | `ContasAPagarExtractor` | `dbo.contas_a_pagar` | `dbo.vw_contas_a_pagar_powerbi` |
| Faturas por Cliente | DataExport template `4924` | `FaturaPorClienteDTO` | `FaturaPorClienteMapper` | `FaturaPorClienteEntity` | `FaturaPorClienteRepository` | `FaturaPorClienteExtractor` | `dbo.faturas_por_cliente` | `dbo.vw_faturas_por_cliente_powerbi` |

## Inventario do banco

### Tabelas de negocio

| Tabela | Colunas | PK / chave | Observacoes | Script |
| --- | ---: | --- | --- | --- |
| `dbo.coletas` | 36 | PK `id`, UQ `sequence_code` | origem GraphQL, flatten de coleta e endereco, possui `metadata` | `database/tabelas/001_criar_tabela_coletas.sql` |
| `dbo.fretes` | 99 | PK `id` | origem GraphQL, forte foco fiscal e documental, possui CT-e, NFS-e e `metadata` | `database/tabelas/002_criar_tabela_fretes.sql` |
| `dbo.manifestos` | 97 | PK `id`, chave de merge unica por `chave_merge_hash` | origem DataExport, alta densidade operacional/logistica | `database/tabelas/003_criar_tabela_manifestos.sql` |
| `dbo.cotacoes` | 38 | PK `sequence_code` | origem DataExport, cotacao comercial/logistica | `database/tabelas/004_criar_tabela_cotacoes.sql` |
| `dbo.localizacao_cargas` | 20 | PK `sequence_number` | origem DataExport, rastreio/andamento de carga | `database/tabelas/005_criar_tabela_localizacao_cargas.sql` |
| `dbo.contas_a_pagar` | 29 | PK `sequence_code` | origem DataExport, contas operacionais/financeiras | `database/tabelas/006_criar_tabela_contas_a_pagar.sql` |
| `dbo.faturas_por_cliente` | 34 | PK `unique_id` | origem DataExport, ponte entre CT-e/NFS-e e faturamento | `database/tabelas/007_criar_tabela_faturas_por_cliente.sql` |
| `dbo.faturas_graphql` | 29 | PK `id` | origem GraphQL, complemento de faturamento e dados bancarios | `database/tabelas/008_criar_tabela_faturas_graphql.sql` |
| `dbo.dim_usuarios` | 3 | PK `user_id` | dimensao de usuarios usada por coletas e exportacao | `database/tabelas/011_criar_tabela_dim_usuarios.sql` |

### Tabelas de suporte e observabilidade

| Tabela | Colunas | Papel | Script |
| --- | ---: | --- | --- |
| `dbo.log_extracoes` | 8 | historico sintetico das execucoes por entidade | `database/tabelas/009_criar_tabela_log_extracoes.sql` |
| `dbo.page_audit` | 19 | auditoria de pagina/processamento por execucao | `database/tabelas/010_criar_tabela_page_audit.sql` |
| `dbo.sys_execution_history` | 8 | historico agregado de execucao do pipeline | `database/tabelas/012_criar_tabela_sys_execution_history.sql` |
| `dbo.sys_auditoria_temp` | 3 | apoio temporario para auditorias/analises | `database/tabelas/013_criar_tabela_sys_auditoria_temp.sql` |
| `dbo.schema_migrations` | variavel via migration | controle de migrations aplicadas | `database/migrations/001_criar_tabela_schema_migrations.sql` |

### Migrations relevantes

- `database/migrations/002_corrigir_constraint_manifestos.sql`
- `database/migrations/003_corrigir_tipo_datetime_faturas_graphql.sql`
- `database/migrations/004_adicionar_request_hour_coletas.sql`

### Indices relevantes

Os DDLs de tabela ja criam alguns indices basicos. O pacote adicional de performance esta em:

- `database/indices/001_criar_indices_performance.sql`

Esse script cria indices extras para:

- `manifestos`
- `cotacoes`
- `contas_a_pagar`
- `coletas`
- `fretes`
- `localizacao_cargas`
- `log_extracoes`

## Views recomendadas para consumo

### `dbo.vw_coletas_powerbi`

- Base: `dbo.coletas`
- Joins internos: `dbo.manifestos`, `dbo.dim_usuarios`
- Chave recomendada para DTO consumidor: `ID`
- Chave de negocio adicional: `Coleta`
- Script: `database/views/013_criar_view_coletas_powerbi.sql`

```text
ID
Coleta
Solicitacao
Hora (Solicitacao)
Agendamento
Finalizacao
Status
Volumes
Peso Real
Peso Taxado
Valor NF
Numero Manifesto
Veiculo
Cliente
Cliente Doc
Local da Coleta
Numero
Complemento
Cidade
Bairro
UF
CEP
Região da Coleta
Filial ID
Filial
Usuario
Motivo Cancel.
Usuario Cancel. ID
Usuario Cancel. Nome
Motivo Exclusao
Usuario Exclusao ID
Usuario Exclusao Nome
Status Atualizado Em
Última Ocorrência
Ação da Ocorrência
Nº Tentativas
Metadata
Data de extracao
```

### `dbo.vw_fretes_powerbi`

- Base: `dbo.fretes`
- Chave recomendada para DTO consumidor: `ID`
- Script: `database/views/012_criar_view_fretes_powerbi.sql`

```text
Hora (Solicitacao)
ID
Chave CT-e
Nº CT-e
Série
CT-e Emissão
CT-e Tipo Emissão
CT-e ID
CT-e Criado em
Documento Oficial/Tipo
Documento Oficial/Chave
Documento Oficial/Número
Documento Oficial/Série
Documento Oficial/XML
Data frete
Criado em
Valor Total do Serviço
Valor NF
Kg NF
Valor Frete
Volumes
Kg Taxado
Kg Real
Kg Cubado
M3
Pagador
Pagador Doc
Pagador ID
Remetente
Remetente Doc
Remetente ID
Origem
UF Origem
Destinatario
Destinatario Doc
Destinatario ID
Destino
UF Destino
Filial
Filial Apelido
Filial CNPJ
Tabela de Preço
Classificação
Centro de Custo
Usuário
NF
Referência
Corp ID
Cidade Destino ID
Previsão de Entrega
Modal
Status
Tipo Frete
Service Type
Seguro Habilitado
GRIS
TDE
Frete Peso
Ad Valorem
Pedágio
ITR
Modal CT-e
Redispatch
SUFRAMA
Tipo Pagamento
Doc Anterior
Valor Produtos
TRT
ICMS CST
CFOP
Valor ICMS
Valor PIS
Valor COFINS
Base de Cálculo ICMS
Alíquota ICMS %
Alíquota PIS %
Alíquota COFINS %
Possui DIFAL
DIFAL Origem
DIFAL Destino
Série NFS-e
Nº NFS-e
NFS-e/ID Integração
NFS-e/Status
NFS-e/Emissão
NFS-e/Cancelamento/Motivo
NFS-e/PDF
NFS-e/Filial ID
NFS-e/Serviço/Descrição
NFS-e/XML
Seguro ID
Outras Tarifas
KM
Tipo Contábil Pagamento
Valor Segurado
Globalizado
SEC/CAT
Tipo Globalizado
Tipo Contábil Tabela
Tipo Contábil Seguro
Metadata
Data de extracao
```

### `dbo.vw_faturas_graphql_powerbi`

- Base: `dbo.faturas_graphql`
- Chave recomendada para DTO consumidor: `ID`
- Script: `database/views/014_criar_view_faturas_graphql_powerbi.sql`

```text
ID
Fatura/N° Documento
Emissão
Vencimento
Vencimento Original
Valor
Valor Pago
Valor a Pagar
Valor Desconto
Valor Juros
Pago
Status
Tipo
Observações
Código Sequencial
Mês Competência
Ano Competência
Data Criação
Data Atualização
Filial/ID
Filial/Nome
Filial/CNPJ
NFS-e/Número
Banco/Carteira
Banco/Instrução Boleto
Banco/Nome
Método Pagamento
Metadata
Data de extracao
```

### `dbo.vw_faturas_por_cliente_powerbi`

- Base: `dbo.faturas_por_cliente`
- Join interno: `dbo.faturas_graphql`
- Chave recomendada para DTO consumidor: `ID Único`
- Script: `database/views/011_criar_view_faturas_por_cliente_powerbi.sql`

```text
Hora (Solicitacao)
ID Único
Filial
Estado
CT-e/Número
Número do Documento
CT-e/Chave
CT-e/Data de emissão
Frete/Valor dos CT-es
Terceiros/Valor CT-es
CT-e/Status
CT-e/Resultado
Tipo
Classificação
Pagador do frete/Nome
Pagador do frete/Documento
Remetente/Nome
Remetente/Documento
Destinatário/Nome
Destinatário/Documento
Vendedor/Nome
NFS-e/Número
NFS-e/Série
fit_nse_number
N° NFS-e
Carteira/Descrição
Instrução Customizada
Status do Processo
Fatura/N° Documento
Fatura/Emissão
Fatura/Valor
Fatura/Valor Total
Fatura/Número
Fatura/Emissão Fatura
Parcelas/Vencimento
Fatura/Baixa
Fatura/Data Vencimento Original
Notas Fiscais
Pedidos/Cliente
Metadata
Data da Última Atualização
```

### `dbo.vw_cotacoes_powerbi`

- Base: `dbo.cotacoes`
- Chave recomendada para DTO consumidor: `N° Cotação`
- Script: `database/views/015_criar_view_cotacoes_powerbi.sql`

```text
N° Cotação
Data Cotação
Hora (Solicitacao)
Data de extracao
Filial
Unidade
Unidade_Origem
Solicitante
Usuário
Setor_Usuario
Empresa
Cliente Pagador
CNPJ/CPF Cliente
Pagador/Nome fantasia
Cliente Grupo
Cliente
Cidade Origem
UF Origem
CEP Origem
Origem
Cidade Destino
UF Destino
CEP Destino
Destino
Trecho
Volume
Peso real
Peso taxado
Valor NF
Valor frete
Min. Frete/KG
Tabela
Tipo de operação
Metadata
Status Conversão
Status_Sistema
Status_Sistema_CTe
Status_Sistema_NFSe
Refino_CTe
Motivo Perda
Observações para o frete
CT-e/Data de emissão
Nfse/Data de emissão
Remetente/CNPJ
Remetente/Nome fantasia
Destinatário/CNPJ
Destinatário/Nome fantasia
Descontos/Subtotal parcelas
Trechos/ITR
Trechos/TDE
Trechos/Coleta
Trechos/Entrega
Trechos/Outros valores
```

### `dbo.vw_contas_a_pagar_powerbi`

- Base: `dbo.contas_a_pagar`
- Chave recomendada para DTO consumidor: `Lançamento a Pagar/N°`
- Script: `database/views/016_criar_view_contas_a_pagar_powerbi.sql`

```text
Hora (Solicitacao)
Lançamento a Pagar/N°
N° Documento
Emissão
Tipo
Valor
Juros
Desconto
Valor a pagar
Pago
Valor pago
Fornecedor/Nome
Filial
Conta Contábil/Classificação
Conta Contábil/Descrição
Conta Contábil/Valor
Centro de custo/Nome
Centro de custo/Valor
Área de Lançamento
Mês de Competência
Ano de Competência
Data criação
Observações
Descrição da despesa
Baixa/Data liquidação
Data transação
Usuário/Nome
Status Pagamento
Conciliado
Metadata
Data de extracao
```

### `dbo.vw_localizacao_cargas_powerbi`

- Base: `dbo.localizacao_cargas`
- Chave recomendada para DTO consumidor: `N° Minuta`
- Script: `database/views/017_criar_view_localizacao_cargas_powerbi.sql`

```text
Hora (Solicitacao)
N° Minuta
Tipo
Data do frete
Volumes
Peso Taxado
Valor NF
Valor Frete
Tipo Serviço
Filial Emissora
Previsão Entrega/Previsão de entrega
Região Destino
Filial Destino
Classificação
Status Carga
Filial Atual
Região Origem
Filial Origem
Localização Atual
Metadata
Data de extracao
```

### `dbo.vw_manifestos_powerbi`

- Base: `dbo.manifestos`
- Chave recomendada para DTO consumidor: `Identificador Único`
- Chaves auxiliares: `Número`, `Coleta/Número`
- Script: `database/views/018_criar_view_manifestos_powerbi.sql`

```text
Hora (Solicitacao)
Hora (Criação)
Número
Identificador Único
Status
Classificação
Filial
Data criação
Saída
Fechamento
Chegada
MDFe
MDF-es/Chave
MDFe/Status
Polo de distribuição
Veículo/Placa
Tipo Veículo
Proprietário/Nome
Motorista
Km saída
Km chegada
KM viagem
Km manual
Qtd NF
Volumes NF
Peso NF
Total peso taxado
Total M3
Valor NF
Fretes/Total
Coleta/Número
CIOT/Número
Tipo de contrato
Tipo de cálculo
Tipo de carga
Diária
Custo total
Valor frete
Combustível
Pedágio
Serviços motorista/Total
Despesa operacional
Dados do agregado/INSS
Dados do agregado/SEST/SENAT
Dados do agregado/IR
Saldo a pagar
Destinos únicos/Qtd
Gerar MDF-e
Solicitou Monitoramento
Solicitação Monitoramento
Leitura Móvel/Em
KM Total
Itens/Entrega
Itens/Transferência
Itens/Coleta
Itens/Despacho Rascunho
Itens/Consolidação
Itens/Coleta Reversa
Itens/Total
Itens/Finalizados
Calculado/Coleta
Calculado/Entrega
Calculado/Despacho
Calculado/Consolidação
Calculado/Coleta Reversa
Valor/Coletas
Valor/Entregas
Despachos
Consolidações
Coleta Reversa
Adiantamento
Custos Frota
Adicionais
Descontos
Desconto/Valor
Liberação de Custo de Agregado/Comentários
IKS ID
Programação/Número
Programação/Início
Programação/Término
Carreta 1/Placa
Carreta 1/Capacidade Peso
Carreta 2/Placa
Carreta 2/Capacidade Peso
Veículo/Capacidade Peso
Veículo/Peso Cubado
Capacidade Lotação Kg
Descarregamento/Destinatários
Entrega/Regiões
Programação/Cliente
Programação/Tipo Serviço
Usuário/Emissor
Usuário/Ajuste
Liberação/Comentários Operacionais
Comentários Fechamento
Metadata
Data de extracao
```

### `dbo.vw_bi_monitoramento`

- Base: `dbo.sys_execution_history`
- Uso: monitoramento operacional, nao dados de negocio
- Chave recomendada: `Id`
- Script: `database/views/019_criar_view_bi_monitoramento.sql`

```text
Id
Inicio
Fim
Duracao (s)
Data
Status
Total Registros
Categoria Erro
Mensagem Erro
```

## Views dimensionais

### `dbo.vw_dim_filiais`

- Dependencias: `vw_fretes_powerbi`, `vw_manifestos_powerbi`, `vw_contas_a_pagar_powerbi`, `vw_faturas_por_cliente_powerbi`
- Chave: `NomeFilial`
- Script: `database/views-dimensao/019_criar_view_dim_filiais.sql`

```text
NomeFilial
Hora (Solicitacao)
```

### `dbo.vw_dim_clientes`

- Dependencias: `dbo.fretes`, `dbo.coletas`, `dbo.faturas_por_cliente`
- Chave: `Nome`
- Script: `database/views-dimensao/020_criar_view_dim_clientes.sql`

```text
Nome
```

### `dbo.vw_dim_veiculos`

- Dependencia: `vw_manifestos_powerbi`
- Chave: `Placa`
- Script: `database/views-dimensao/021_criar_view_dim_veiculos.sql`

```text
Placa
TipoVeiculo
Proprietario
```

### `dbo.vw_dim_motoristas`

- Dependencia: `vw_manifestos_powerbi`
- Chave: `NomeMotorista`
- Script: `database/views-dimensao/022_criar_view_dim_motoristas.sql`

```text
NomeMotorista
```

### `dbo.vw_dim_planocontas`

- Dependencia: `vw_contas_a_pagar_powerbi`
- Chave: `Descricao`
- Script: `database/views-dimensao/023_criar_view_dim_planocontas.sql`

```text
Descricao
Classificacao
Hora (Solicitacao)
```

### `dbo.vw_dim_usuarios`

- Base: `dbo.dim_usuarios`
- Chave: `User ID`
- Script: `database/views-dimensao/024_criar_view_dim_usuarios.sql`

```text
User ID
Nome
Data Atualizacao
```

## DTOs de origem usados pelo extrator

### GraphQL

- `ColetaNodeDTO`
  - Arquivo: `src/main/java/br/com/extrator/dominio/graphql/coletas/ColetaNodeDTO.java`
  - Nested DTOs relacionados: `CustomerDTO`, `PickAddressDTO`, `CityDTO`, `StateDTO`, `UserDTO`
  - Campos-raiz relevantes: `id`, `sequenceCode`, `requestDate`, `requestHour`, `serviceDate`, `serviceStartHour`, `serviceEndHour`, `finishDate`, `status`, `statusUpdatedAt`, `invoicesValue`, `invoicesWeight`, `invoicesVolumes`, `taxedWeight`, `customer`, `pickAddress`, `user`, `corporation`, `cancellationReason`, `cancellationUserId`, `destroyReason`, `destroyUserId`
- `FreteNodeDTO`
  - Arquivo: `src/main/java/br/com/extrator/dominio/graphql/fretes/FreteNodeDTO.java`
  - Nested DTOs relacionados: `PayerDTO`, `SenderDTO`, `ReceiverDTO`, `CorporationDTO`, `CustomerPriceTableDTO`, `FreightClassificationDTO`, `CostCenterDTO`, `UserDTO`, `CityDTO`, `StateDTO`, `FiscalDetailDTO`, `FreightInvoiceDTO`, `FreteNodeDTO.CteDTO`
  - Campos-raiz relevantes: `id`, `serviceAt`, `createdAt`, `status`, `modal`, `type`, `total`, `invoicesValue`, `invoicesWeight`, `taxedWeight`, `realWeight`, `totalCubicVolume`, `payer`, `sender`, `receiver`, `corporation`, `cte`, `originCity`, `destinationCity`, `fiscalDetail`, `referenceNumber`, `nfseSeries`, `nfseNumber`, `insuranceEnabled`
- `CreditCustomerBillingNodeDTO`
  - Arquivo: `src/main/java/br/com/extrator/dominio/graphql/faturas/CreditCustomerBillingNodeDTO.java`
  - Nested classes no mesmo arquivo: `CustomerDTO`, `CorporationDTO`, `InstallmentDTO`, `AccountingCreditDTO`, `AccountingBankAccountDTO`
  - Campos-raiz relevantes: `id`, `document`, `issueDate`, `dueDate`, `value`, `paidValue`, `valueToPay`, `discountValue`, `interestValue`, `paid`, `type`, `comments`, `sequenceCode`, `competenceMonth`, `competenceYear`, `ticketAccountId`, `customer`, `corporation`, `installments`
- `IndividualNodeDTO`
  - Arquivo: `src/main/java/br/com/extrator/dominio/graphql/usuarios/IndividualNodeDTO.java`
  - Campos principais: `id`, `name`

### DataExport

- `ManifestoDTO`
  - Arquivo: `src/main/java/br/com/extrator/dominio/dataexport/manifestos/ManifestoDTO.java`
  - Papel: DTO mais volumoso do projeto, com mapeamento amplo de manifesto/operacao/veiculo/motorista/frete/itens
  - Campos relevantes: `sequence_code`, `created_at`, `departured_at`, `closed_at`, `finished_at`, `status`, `mft_mfs_number`, `mft_mfs_key`, `mdfe_status`, `mft_crn_psn_nickname`, `mft_vie_license_plate`, `mft_mdr_iil_name`, `total_cost`, `paying_total`, `mft_pfs_pck_sequence_code`, `operational_comments`, `closing_comments`
- `CotacaoDTO`
  - Arquivo: `src/main/java/br/com/extrator/dominio/dataexport/cotacao/CotacaoDTO.java`
  - Campos relevantes: `requested_at`, `sequence_code`, `customer_name`, `customer_doc`, `origin_city`, `origin_state`, `destination_city`, `destination_state`, `taxed_weight`, `invoices_value`, `total_value`, `price_table`, `operation_type`
- `LocalizacaoCargaDTO`
  - Arquivo: `src/main/java/br/com/extrator/dominio/dataexport/localizacaocarga/LocalizacaoCargaDTO.java`
  - Campos relevantes: `corporation_sequence_number`, `type`, `service_at`, `invoices_volumes`, `taxed_weight`, `invoices_value`, `total`, `service_type`, `fit_crn_psn_nickname`, `fit_dpn_delivery_prediction_at`, `fit_dyn_name`, `fit_dyn_drt_nickname`, `fit_fln_status`
- `ContasAPagarDTO`
  - Arquivo: `src/main/java/br/com/extrator/dominio/dataexport/contasapagar/ContasAPagarDTO.java`
  - Campos relevantes: `ant_ils_sequence_code`, `document`, `issue_date`, `type`, `value`, `interest_value`, `discount_value`, `value_to_pay`, `paid`, `paid_value`, `competence_month`, `competence_year`, `created_at`, `ant_rir_name`, `ant_crn_psn_nickname`, `ant_ils_comments`
- `FaturaPorClienteDTO`
  - Arquivo: `src/main/java/br/com/extrator/dominio/dataexport/faturaporcliente/FaturaPorClienteDTO.java`
  - Campos relevantes: `fit_nse_number`, `fit_fhe_cte_number`, `fit_fhe_cte_issued_at`, `fit_fhe_cte_key`, `fit_ant_document`, `fit_ant_issue_date`, `fit_ant_value`, `fit_ant_ils_due_date`, `fit_ant_ils_original_due_date`, `third_party_ctes_value`, `type`, `fit_crn_psn_nickname`, `fit_pyr_name`, `fit_rpt_name`, `fit_sdr_name`, `invoices_mapping`

## Recomendacao para DTOs do novo projeto consumidor

- Crie um DTO por view principal:
  - `ColetaSqlViewDto`
  - `FreteSqlViewDto`
  - `FaturaGraphqlSqlViewDto`
  - `FaturaPorClienteSqlViewDto`
  - `CotacaoSqlViewDto`
  - `ContaAPagarSqlViewDto`
  - `LocalizacaoCargaSqlViewDto`
  - `ManifestoSqlViewDto`
  - DTOs auxiliares para `vw_dim_*`
- Se o projeto nao quiser carregar nomes de colunas com espaco, acento e barra, faça alias na query SQL e modele o DTO em `camelCase`.
- Preserve `Metadata` como `String`, `JsonNode` ou tipo equivalente para manter acesso ao payload bruto.
- Para joins analiticos, use as dimensoes em vez de normalizar os nomes no codigo.

## Relacionamentos e joins uteis

- `vw_coletas_powerbi.[Coleta] = vw_manifestos_powerbi.[Coleta/Número]`
- `vw_coletas_powerbi.[Usuario Cancel. ID] = vw_dim_usuarios.[User ID]`
- `vw_coletas_powerbi.[Usuario Exclusao ID] = vw_dim_usuarios.[User ID]`
- `vw_manifestos_powerbi.[Veículo/Placa] = vw_dim_veiculos.[Placa]`
- `vw_manifestos_powerbi.[Motorista] = vw_dim_motoristas.[NomeMotorista]` com normalizacao se necessario
- `vw_contas_a_pagar_powerbi.[Conta Contábil/Descrição] = vw_dim_planocontas.[Descricao]`
- `vw_faturas_por_cliente_powerbi.[Filial] = vw_dim_filiais.[NomeFilial]`

## Exemplo de consulta para um consumidor SQL

```sql
SELECT
    c.[ID]                  AS id,
    c.[Coleta]              AS coleta,
    c.[Solicitacao]         AS dataSolicitacao,
    c.[Hora (Solicitacao)]  AS horaSolicitacao,
    c.[Status]              AS status,
    c.[Cliente]             AS cliente,
    c.[Filial]              AS filial,
    c.[Numero Manifesto]    AS numeroManifesto,
    c.[Metadata]            AS metadata
FROM dbo.vw_coletas_powerbi c
WHERE c.[Solicitacao] >= '2026-01-01';
```

## Resumo final

- O novo projeto deve consumir preferencialmente as `views`.
- Os DTOs do novo projeto devem representar as `views`, nao os DTOs das APIs.
- O banco esta dividido em tabelas de negocio, tabelas de suporte, views principais e views dimensionais.
- Quando houver duvida entre Java e SQL, o contrato externo mais importante e: `DDL + views + migrations`.
