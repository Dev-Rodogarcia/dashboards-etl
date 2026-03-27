import type { EChartsOption } from 'echarts';
import ChartWrapper from '../components/charts/ChartWrapper';
import CotacoesKpiGrid from '../components/domain/cotacoes/CotacoesKpiGrid';
import AsyncMultiSelect from '../components/shared/AsyncMultiSelect';
import DataTable, { type ColunaTabela } from '../components/shared/DataTable';
import DateRangePicker from '../components/shared/DateRangePicker';
import ExportButton from '../components/shared/ExportButton';
import FilterBar, { type ActiveFilter } from '../components/shared/FilterBar';
import LastUpdated from '../components/shared/LastUpdated';
import StatusBadge from '../components/shared/StatusBadge';
import MensagemErro from '../components/ui/MensagemErro';
import { getApiErrorMessage, getTipoErro } from '../utils/apiError';
import { useFiltro } from '../contexts/FiltroContext';
import { useClientes, useFiliais } from '../hooks/queries/useDimensoes';
import { useCotacoesGraficos, useCotacoesOverview, useCotacoesSerie, useCotacoesTabela } from '../hooks/queries/useCotacoes';
import type { CotacaoResumoRow, CotacoesFiltro } from '../types/cotacoes';
import { CORES } from '../utils/chartColors';
import { formatarMoeda } from '../utils/formatadores';

const ORDEM_FUNIL = ['Pendente', 'Convertida', 'Reprovada', 'Sem status'] as const;

function normalizarEtapa(etapa: string | null | undefined) {
  return etapa?.trim().toLowerCase() ?? '';
}

function corEtapaFunil(etapa: string) {
  switch (normalizarEtapa(etapa)) {
    case 'pendente':
      return CORES.aviso;
    case 'convertida':
      return CORES.sucesso;
    case 'reprovada':
      return CORES.perigo;
    case 'sem status':
      return CORES.cinza;
    default:
      return CORES.cinza;
  }
}

export default function CotacoesPage() {
  const { dataInicio, dataFim, filtros, setDataInicio, setDataFim, setDataRange, setFiltro, limparFiltros } = useFiltro();
  const filiais = useFiliais();
  const clientes = useClientes();

  const filtro: CotacoesFiltro = {
    dataInicio,
    dataFim,
    filiais: filtros.filiais,
    clientes: filtros.clientes,
    statusConversao: filtros.statusConversao,
  };

  const activeFilters: ActiveFilter[] = [
    { label: 'Filiais', count: filtros.filiais?.length ?? 0, onRemove: () => setFiltro('filiais', []) },
    { label: 'Clientes', count: filtros.clientes?.length ?? 0, onRemove: () => setFiltro('clientes', []) },
    { label: 'Status', count: filtros.statusConversao?.length ?? 0, onRemove: () => setFiltro('statusConversao', []) },
  ];

  const overview = useCotacoesOverview(filtro);
  const serie = useCotacoesSerie(filtro);
  const graficos = useCotacoesGraficos(filtro);
  const tabela = useCotacoesTabela(filtro, 120);

  const funil = graficos.data?.funil ?? [];
  const corredores = graficos.data?.corredoresMaisValiosos ?? [];
  const motivos = graficos.data?.motivosPerda ?? [];

  const funilOrdenado = [
    ...ORDEM_FUNIL.flatMap((etapa) => {
      const item = funil.find((entrada) => normalizarEtapa(entrada.etapa) === normalizarEtapa(etapa));
      return item && item.total > 0 ? [item] : [];
    }),
    ...funil.filter((item) => (
      item.total > 0 && !ORDEM_FUNIL.some((etapa) => normalizarEtapa(etapa) === normalizarEtapa(item.etapa))
    )),
  ];

  const totalFunil = funilOrdenado.reduce((acc, item) => acc + item.total, 0);
  const maxFunil = Math.max(...funilOrdenado.map((item) => item.total), 0);

  const percentualFunil = (valor: number) => {
    if (totalFunil === 0) {
      return '0.0%';
    }

    return `${((valor / totalFunil) * 100).toFixed(1)}%`;
  };

  const serieOption: EChartsOption = {
    legend: { bottom: 0 },
    xAxis: { type: 'category', data: (serie.data ?? []).map((item) => item.date) },
    yAxis: { type: 'value' },
    series: [
      { name: 'Cotações', type: 'line', data: (serie.data ?? []).map((item) => item.cotacoes), itemStyle: { color: CORES.primaria } },
      { name: 'Convertidas', type: 'line', data: (serie.data ?? []).map((item) => item.convertidas), itemStyle: { color: CORES.sucesso } },
      { name: 'Reprovadas', type: 'line', data: (serie.data ?? []).map((item) => item.reprovadas), itemStyle: { color: CORES.perigo } },
    ],
  };

  const funilOption: EChartsOption = {
    legend: { show: false },
    tooltip: {
      trigger: 'item',
      formatter: (params: unknown) => {
        const item = params as { name?: string; value?: number };
        const total = Number(item.value ?? 0);
        return `${item.name ?? 'Sem status'}<br/>Total: ${total}<br/>Participação: ${percentualFunil(total)}`;
      },
    },
    series: [{
      type: 'funnel',
      sort: 'none',
      left: '5%',
      top: 8,
      right: '18%',
      bottom: 35,
      min: 0,
      max: maxFunil,
      minSize: '8%',
      maxSize: '80%',
      gap: 10,
      itemStyle: {
        borderColor: '#f8fafc',
        borderWidth: 2,
        borderRadius: 12,
      },
      label: {
        show: true,
        position: 'outer',
        color: '#374151',
        fontSize: 12,
        lineHeight: 18,
        formatter: (params: unknown) => {
          const item = params as { name?: string; value?: number };
          const total = Number(item.value ?? 0);
          return `${item.name ?? 'Sem status'}\n${total} • ${percentualFunil(total)}`;
        },
      },
      labelLine: {
        show: true,
        length: 18,
        lineStyle: {
          color: '#cbd5e1',
          width: 1.5,
        },
      },
      emphasis: {
        label: {
          fontWeight: 'bold',
        },
      },
      data: funilOrdenado.map((item) => ({
        name: item.etapa,
        value: item.total,
        itemStyle: { color: corEtapaFunil(item.etapa) },
      })),
    }],
  };

  const corredoresOption: EChartsOption = {
    grid: { left: 10, containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: corredores.map((item) => item.trecho).reverse() },
    series: [{ type: 'bar', data: corredores.map((item) => item.valorFrete).reverse(), itemStyle: { color: CORES.secundaria } }],
  };

  const motivosOption: EChartsOption = {
    xAxis: { type: 'category', data: motivos.map((item) => item.motivo) },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: motivos.map((item) => item.total), itemStyle: { color: CORES.aviso } }],
  };

  const colunas: ColunaTabela<CotacaoResumoRow>[] = [
    { chave: 'numeroCotacao', label: 'Cotacao', fixo: true },
    { chave: 'dataCotacao', label: 'Data' },
    { chave: 'filial', label: 'Filial' },
    { chave: 'clientePagador', label: 'Pagador', largura: '220px' },
    { chave: 'trecho', label: 'Trecho', largura: '220px' },
    { chave: 'valorFrete', label: 'Valor Frete', formato: (valor) => formatarMoeda(Number(valor ?? 0)) },
    { chave: 'statusConversao', label: 'Status', formato: (valor) => <StatusBadge status={String(valor)} /> },
    { chave: 'motivoPerda', label: 'Motivo Perda', largura: '220px' },
  ];

  return (
    <div className="w-full">
      <div className="mb-5 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold leading-tight" style={{ color: 'var(--color-text)' }}>Cotações</h1>
          <p className="text-sm" style={{ color: 'var(--color-text-subtle)' }}>Funil comercial, corredores mais valiosos e motivos de perda.</p>
        </div>
        <LastUpdated dataExtracao={overview.data?.updatedAt ?? null} />
      </div>

      <FilterBar onClear={limparFiltros} activeFilters={activeFilters} dataInicio={dataInicio} dataFim={dataFim}>
        <DateRangePicker dataInicio={dataInicio} dataFim={dataFim} onDataInicioChange={setDataInicio} onDataFimChange={setDataFim} onRangeChange={setDataRange} />
        <AsyncMultiSelect label="Filiais" opcoes={filiais.data ?? []} selecionados={filtros.filiais ?? []} onChange={(valores) => setFiltro('filiais', valores)} isLoading={filiais.isLoading} />
        <AsyncMultiSelect label="Clientes" opcoes={clientes.data ?? []} selecionados={filtros.clientes ?? []} onChange={(valores) => setFiltro('clientes', valores)} isLoading={clientes.isLoading} />
        <AsyncMultiSelect label="Status" opcoes={['Convertida', 'Reprovada', 'Pendente']} selecionados={filtros.statusConversao ?? []} onChange={(valores) => setFiltro('statusConversao', valores)} />
      </FilterBar>

      {overview.isError && <MensagemErro mensagem={getApiErrorMessage(overview.error, 'Erro ao carregar indicadores de cotações.')} tipo={getTipoErro(overview.error)} />}
      {overview.data && <CotacoesKpiGrid overview={overview.data} />}

      <div className="mb-6 grid grid-cols-1 gap-6 xl:grid-cols-2">
        <ChartWrapper titulo="Serie Diaria de Cotações" option={serieOption} isLoading={serie.isLoading} isEmpty={(serie.data ?? []).length === 0} />
        <ChartWrapper titulo="Funil Comercial" option={funilOption} isLoading={graficos.isLoading} isEmpty={funilOrdenado.length === 0} altura={260} />
      </div>

      <div className="mb-6 grid grid-cols-1 gap-6 xl:grid-cols-2">
        <ChartWrapper titulo="Corredores Mais Valiosos" option={corredoresOption} isLoading={graficos.isLoading} isEmpty={corredores.length === 0} />
        <ChartWrapper
          titulo="Motivos de Perda"
          option={motivosOption}
          isLoading={graficos.isLoading}
          isEmpty={motivos.length === 0}
          emptyMessage="Sem perdas/reprovações no período selecionado."
        />
      </div>

      <div className="mb-3 flex justify-end">
        <ExportButton dados={(tabela.data ?? []) as unknown as Record<string, unknown>[]} nomeArquivo="cotacoes" />
      </div>
      <DataTable titulo="Cotacoes Analiticas" dados={tabela.data ?? []} colunas={colunas} chaveLinha="numeroCotacao" isLoading={tabela.isLoading} />
    </div>
  );
}
