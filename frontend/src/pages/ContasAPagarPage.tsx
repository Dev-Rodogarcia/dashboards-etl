import type { EChartsOption } from 'echarts';
import ChartWrapper from '../components/charts/ChartWrapper';
import { useEchartsTheme } from '../components/charts/useEchartsTheme';
import ContasAPagarKpiGrid from '../components/domain/contasAPagar/ContasAPagarKpiGrid';
import AsyncMultiSelect from '../components/shared/AsyncMultiSelect';
import AnalyticalDataTable, { type ColunaTabelaAnalitica } from '../components/shared/AnalyticalDataTable';
import DateRangePicker from '../components/shared/DateRangePicker';
import ExportButton from '../components/shared/ExportButton';
import FiliaisParceirosFilter from '../components/shared/FiliaisParceirosFilter';
import FilterBar, { type ActiveFilter } from '../components/shared/FilterBar';
import StatusBadge from '../components/shared/StatusBadge';
import MensagemErro from '../components/ui/MensagemErro';
import { exportarContasAPagarCsv } from '../api/endpoints/contasAPagarServico';
import { getApiErrorMessage, getTipoErro } from '../utils/apiError';
import { useFiltro } from '../contexts/FiltroContext';
import { usePageHeader } from '../contexts/PageHeaderContext';
import { useFiliais, usePlanoContas } from '../hooks/queries/useDimensoes';
import { useContasAPagarGraficos, useContasAPagarOverview, useContasAPagarSerie, useContasAPagarTabelaPaginada } from '../hooks/queries/useContasAPagar';
import { useAnalyticalTableFilters } from '../hooks/useAnalyticalTableFilters';
import { useTabelaPaginadaState } from '../hooks/useTabelaPaginadaState';
import type { ContaPagarResumoRow, ContasAPagarFiltro } from '../types/contasAPagar';
import { buildBaseBarOption, buildBaseDonutOption, getEchartsThemeTokens } from '../utils/echartsBuilders';
import { formatarMoeda } from '../utils/formatadores';
import { combinarStatusOptions } from '../utils/tableStatusOptions';

export default function ContasAPagarPage() {
  const { dataInicio, dataFim, filtros, setDataInicio, setDataFim, setDataRange, setFiltro, limparFiltros } = useFiltro();
  const { isDark } = useEchartsTheme();
  const filiais = useFiliais();
  const planoContas = usePlanoContas();

  const filtro: ContasAPagarFiltro = {
    dataInicio,
    dataFim,
    filiais: filtros.filiais,
    parceirosLogisticos: filtros.parceirosLogisticos,
    classificacoes: filtros.classificacoes,
    pago: filtros.pago,
    conciliado: filtros.conciliado,
  };

  const activeFilters: ActiveFilter[] = [
    { label: 'Filiais', count: filtros.filiais?.length ?? 0, onRemove: () => setFiltro('filiais', []) },
    { label: 'Parceiros Logísticos', count: filtros.parceirosLogisticos?.length ?? 0, onRemove: () => setFiltro('parceirosLogisticos', []) },
    { label: 'Plano Contas', count: filtros.classificacoes?.length ?? 0, onRemove: () => setFiltro('classificacoes', []) },
    { label: 'Pago', count: filtros.pago?.length ?? 0, onRemove: () => setFiltro('pago', []) },
  ];

  const overview = useContasAPagarOverview(filtro);
  const serie = useContasAPagarSerie(filtro);
  const graficos = useContasAPagarGraficos(filtro);
  const filtrosTabela = useAnalyticalTableFilters();
  const paginacaoTabela = useTabelaPaginadaState(JSON.stringify({ filtro, tabela: filtrosTabela.resetKey }));
  const tabela = useContasAPagarTabelaPaginada(filtro, paginacaoTabela.pagina, paginacaoTabela.tamanhoPagina, filtrosTabela.apiFilters);

  usePageHeader({
    title: 'Contas a Pagar',
    description: 'Fluxo mensal, fornecedores relevantes e conciliação financeira.',
    updatedAt: overview.data?.updatedAt ?? null,
  });

  const rankingFornecedor = graficos.data?.topFornecedores ?? [];
  const centroCusto = graficos.data?.centroCusto ?? [];
  const conciliacao = graficos.data?.conciliacao ?? [];
  const tokens = getEchartsThemeTokens(isDark);
  const statusTabelaOptions = combinarStatusOptions(
    ['Sim', 'Não'],
    (tabela.data?.conteudo ?? []).map((item) => item.statusPagamento),
    filtros.pago,
  );

  const serieOption: EChartsOption = buildBaseBarOption(isDark, {
    legend: { bottom: 0 },
    xAxis: { type: 'category', data: (serie.data ?? []).map((item) => item.month) },
    yAxis: { type: 'value' },
    series: [
      { name: 'Pago', type: 'bar', stack: 'contas', data: (serie.data ?? []).map((item) => item.pago), itemStyle: { color: tokens.palette[2] } },
      { name: 'Aberto', type: 'bar', stack: 'contas', data: (serie.data ?? []).map((item) => item.aberto), itemStyle: { color: tokens.palette[1] } },
    ],
  });

  const fornecedorOption: EChartsOption = buildBaseBarOption(isDark, {
    grid: { left: 10, containLabel: true },
    xAxis: { type: 'value' },
    yAxis: {
      type: 'category',
      data: rankingFornecedor.map((item) => item.fornecedor).reverse(),
      axisLabel: {
        formatter: (value: string) => value.length > 22 ? value.slice(0, 22) + '…' : value,
      },
    },
    tooltip: {
      trigger: 'axis',
      formatter: (params: unknown) => {
        const p = (params as { name: string; value: number }[])[0];
        return `${p.name}<br/>R$ ${p.value.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`;
      },
    },
    series: [{ type: 'bar', data: rankingFornecedor.map((item) => item.valor).reverse(), itemStyle: { color: tokens.palette[0] } }],
  });

  const centroOption: EChartsOption = buildBaseBarOption(isDark, {
    xAxis: { type: 'category', data: centroCusto.map((item) => item.centroCusto) },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: centroCusto.map((item) => item.valor), itemStyle: { color: tokens.palette[1] } }],
  });

  const conciliacaoOption: EChartsOption = buildBaseDonutOption(isDark, {
    series: [{ name: 'Conciliação', type: 'pie', data: conciliacao.map((item) => ({ name: item.status, value: item.valor })) }],
  });

  const colunas: ColunaTabelaAnalitica<ContaPagarResumoRow>[] = [
    { chave: 'lancamentoNumero', label: 'Lancamento', fixo: true, filtroTabela: 'codigo' },
    { chave: 'emissao', label: 'Emissao' },
    { chave: 'filial', label: 'Filial' },
    { chave: 'fornecedor', label: 'Fornecedor', largura: '220px', filtroTabela: 'razaoSocial' },
    { chave: 'classificacao', label: 'Classificacao' },
    { chave: 'centroCusto', label: 'Centro Custo' },
    { chave: 'valorAPagar', label: 'Valor a Pagar', formato: (valor) => formatarMoeda(Number(valor ?? 0)) },
    { chave: 'valorPago', label: 'Valor Pago', formato: (valor) => formatarMoeda(Number(valor ?? 0)) },
    { chave: 'statusPagamento', label: 'Pagamento', filtroTabela: 'status', formato: (valor) => <StatusBadge status={String(valor)} /> },
  ];

  return (
    <div className="w-full">
      <FilterBar onClear={limparFiltros} activeFilters={activeFilters} dataInicio={dataInicio} dataFim={dataFim}>
        <DateRangePicker dataInicio={dataInicio} dataFim={dataFim} onDataInicioChange={setDataInicio} onDataFimChange={setDataFim} onRangeChange={setDataRange} />
        <FiliaisParceirosFilter
          opcoes={filiais.data ?? []}
          filiaisSelecionadas={filtros.filiais ?? []}
          parceirosSelecionados={filtros.parceirosLogisticos ?? []}
          onFiliaisChange={(valores) => setFiltro('filiais', valores)}
          onParceirosChange={(valores) => setFiltro('parceirosLogisticos', valores)}
          isLoading={filiais.isLoading}
        />
        <AsyncMultiSelect label="Plano Contas" opcoes={(planoContas.data ?? []).map((item) => item.classificacao)} selecionados={filtros.classificacoes ?? []} onChange={(valores) => setFiltro('classificacoes', valores)} isLoading={planoContas.isLoading} />
        <AsyncMultiSelect label="Pago" opcoes={['PAGO', 'Sim', 'Nao']} selecionados={filtros.pago ?? []} onChange={(valores) => setFiltro('pago', valores)} />
      </FilterBar>

      {overview.isError && <MensagemErro mensagem={getApiErrorMessage(overview.error, 'Erro ao carregar indicadores de contas a pagar.')} tipo={getTipoErro(overview.error)} />}
      {overview.data && <ContasAPagarKpiGrid overview={overview.data} />}

      <div className="mb-6 grid grid-cols-1 gap-6 xl:grid-cols-2">
        <ChartWrapper titulo="Pagos x Abertos por Mes" chartKey="contasPagarSerie" option={serieOption} isLoading={serie.isLoading} isEmpty={(serie.data ?? []).length === 0} />
        <ChartWrapper titulo="Top Fornecedores" chartKey="contasPagarTopFornecedores" option={fornecedorOption} isLoading={graficos.isLoading} isEmpty={rankingFornecedor.length === 0} />
      </div>

      <div className="mb-6 grid grid-cols-1 gap-6 xl:grid-cols-2">
        <ChartWrapper titulo="Centro de Custo" chartKey="contasPagarCentroCusto" option={centroOption} isLoading={graficos.isLoading} isEmpty={centroCusto.length === 0} />
        <ChartWrapper titulo="Conciliação" chartKey="contasPagarConciliacao" option={conciliacaoOption} isLoading={graficos.isLoading} isEmpty={conciliacao.length === 0} />
      </div>

      <div className="mb-3 flex justify-end">
        <ExportButton nomeArquivo="contas-a-pagar" onExport={() => exportarContasAPagarCsv(filtro, filtrosTabela.apiFilters)} />
      </div>
      <AnalyticalDataTable
        titulo="Lançamentos Analiticos"
        dados={tabela.data?.conteudo ?? []}
        colunas={colunas}
        chaveLinha="lancamentoNumero"
        filtros={filtrosTabela.filters}
        hiddenActiveCount={filtrosTabela.hiddenActiveCount}
        hasAnyFilter={filtrosTabela.hasAnyFilter}
        onTextFilterChange={filtrosTabela.setTextFilter}
        onMultiFilterChange={filtrosTabela.setMultiFilter}
        onColumnFilterChange={filtrosTabela.setColumnFilter}
        onClearFilters={filtrosTabela.clearTableFilters}
        statusOptions={statusTabelaOptions}
        isLoading={tabela.isLoading}
        error={tabela.error}
        errorFallbackMessage="Erro ao carregar lançamentos analíticos."
        totalRegistros={tabela.data?.totalElementos}
        paginaAtual={paginacaoTabela.pagina}
        tamanhoPagina={paginacaoTabela.tamanhoPagina}
        onPaginaChange={paginacaoTabela.setPagina}
        onTamanhoPaginaChange={paginacaoTabela.setTamanhoPagina}
      />
    </div>
  );
}
