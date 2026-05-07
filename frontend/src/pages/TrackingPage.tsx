import type { EChartsOption } from 'echarts';
import ChartWrapper from '../components/charts/ChartWrapper';
import TrackingKpiGrid from '../components/domain/tracking/TrackingKpiGrid';
import AsyncMultiSelect from '../components/shared/AsyncMultiSelect';
import DataTable, { type ColunaTabela } from '../components/shared/DataTable';
import DateRangePicker from '../components/shared/DateRangePicker';
import ExportButton from '../components/shared/ExportButton';
import FilterBar, { type ActiveFilter } from '../components/shared/FilterBar';
import StatusBadge from '../components/shared/StatusBadge';
import MensagemErro from '../components/ui/MensagemErro';
import { exportarTrackingCsv } from '../api/endpoints/trackingServico';
import { getApiErrorMessage, getTipoErro } from '../utils/apiError';
import { useFiltro } from '../contexts/FiltroContext';
import { usePageHeader } from '../contexts/PageHeaderContext';
import { useFiliais } from '../hooks/queries/useDimensoes';
import { useTrackingGraficos, useTrackingOverview, useTrackingSerie, useTrackingTabelaPaginada } from '../hooks/queries/useTracking';
import { useTabelaPaginadaState } from '../hooks/useTabelaPaginadaState';
import type { TrackingFiltro, TrackingRawRow } from '../types/tracking';
import { CORES } from '../utils/chartColors';
import { formatarMoeda, formatarPeso } from '../utils/formatadores';

export default function TrackingPage() {
  const { dataInicio, dataFim, filtros, setDataInicio, setDataFim, setDataRange, setFiltro, limparFiltros } = useFiltro();
  const filiais = useFiliais();

  const filtro: TrackingFiltro = {
    dataInicio,
    dataFim,
    filialEmissora: filtros.filialEmissora,
    filialAtual: filtros.filialAtual,
    filialDestino: filtros.filialDestino,
    regiaoOrigem: filtros.regiaoOrigem,
    regiaoDestino: filtros.regiaoDestino,
    statusCarga: filtros.statusCarga,
  };

  const activeFilters: ActiveFilter[] = [
    { label: 'Filial Emissora', count: filtros.filialEmissora?.length ?? 0, onRemove: () => setFiltro('filialEmissora', []) },
    { label: 'Filial Atual', count: filtros.filialAtual?.length ?? 0, onRemove: () => setFiltro('filialAtual', []) },
    { label: 'Status', count: filtros.statusCarga?.length ?? 0, onRemove: () => setFiltro('statusCarga', []) },
  ];

  const overview = useTrackingOverview(filtro);
  const serie = useTrackingSerie(filtro);
  const graficos = useTrackingGraficos(filtro);
  const paginacaoTabela = useTabelaPaginadaState(JSON.stringify(filtro));
  const tabela = useTrackingTabelaPaginada(filtro, paginacaoTabela.pagina, paginacaoTabela.tamanhoPagina);

  usePageHeader({
    title: 'Localização de Cargas',
    description: 'Status da carga, previsões vencidas e carteira em trânsito.',
    updatedAt: overview.data?.updatedAt ?? null,
  });

  const statusData = graficos.data?.statusDistribuicao ?? [];
  const vencidasFilial = graficos.data?.previsaoVencidaPorFilialAtual ?? [];
  const valorRegiao = graficos.data?.valorPorRegiaoDestino ?? [];

  const serieOption: EChartsOption = {
    legend: { bottom: 0 },
    xAxis: { type: 'category', data: (serie.data ?? []).map((item) => item.date) },
    yAxis: { type: 'value' },
    series: [
      { name: 'Pendente', type: 'line', data: (serie.data ?? []).map((item) => item.pendente), itemStyle: { color: CORES.aviso } },
      { name: 'Em entrega', type: 'line', data: (serie.data ?? []).map((item) => item.emEntrega), itemStyle: { color: CORES.primaria } },
      { name: 'Em transferencia', type: 'line', data: (serie.data ?? []).map((item) => item.emTransferencia), itemStyle: { color: CORES.secundaria } },
      { name: 'Finalizado', type: 'line', data: (serie.data ?? []).map((item) => item.finalizado), itemStyle: { color: CORES.sucesso } },
    ],
  };

  const statusOption: EChartsOption = {
    series: [{ type: 'pie', radius: ['38%', '68%'], data: statusData.map((item) => ({ name: item.status, value: item.total })) }],
  };

  const vencidasOption: EChartsOption = {
    grid: { left: 120 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: vencidasFilial.map((item) => item.filialAtual).reverse() },
    series: [{ type: 'bar', data: vencidasFilial.map((item) => item.vencidas).reverse(), itemStyle: { color: CORES.perigo } }],
  };

  const valorRegiaoOption: EChartsOption = {
    xAxis: { type: 'category', data: valorRegiao.map((item) => item.regiaoDestino) },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: valorRegiao.map((item) => item.valorFrete), itemStyle: { color: CORES.secundaria } }],
  };

  const colunas: ColunaTabela<TrackingRawRow>[] = [
    { chave: 'numeroMinuta', label: 'Minuta', fixo: true },
    { chave: 'dataFrete', label: 'Data' },
    { chave: 'statusCarga', label: 'Status', formato: (valor) => <StatusBadge status={String(valor)} /> },
    { chave: 'filialEmissora', label: 'Filial Emissora' },
    { chave: 'filialAtual', label: 'Filial Atual' },
    { chave: 'regiaoDestino', label: 'Regiao Destino' },
    { chave: 'pesoTaxadoRaw', label: 'Peso', formato: (valor) => formatarPeso(Number(valor ?? 0)) },
    { chave: 'valorFrete', label: 'Valor Frete', formato: (valor) => formatarMoeda(Number(valor ?? 0)) },
    { chave: 'previsaoEntrega', label: 'Previsao' },
  ];

  return (
    <div className="w-full">
      <FilterBar onClear={limparFiltros} activeFilters={activeFilters} dataInicio={dataInicio} dataFim={dataFim}>
        <DateRangePicker dataInicio={dataInicio} dataFim={dataFim} onDataInicioChange={setDataInicio} onDataFimChange={setDataFim} onRangeChange={setDataRange} />
        <AsyncMultiSelect label="Filial Emissora" opcoes={filiais.data ?? []} selecionados={filtros.filialEmissora ?? []} onChange={(valores) => setFiltro('filialEmissora', valores)} isLoading={filiais.isLoading} />
        <AsyncMultiSelect label="Filial Atual" opcoes={filiais.data ?? []} selecionados={filtros.filialAtual ?? []} onChange={(valores) => setFiltro('filialAtual', valores)} isLoading={filiais.isLoading} />
        <AsyncMultiSelect label="Status" opcoes={['Pendente', 'Em entrega', 'Em transferência', 'Manifestado', 'Finalizado']} selecionados={filtros.statusCarga ?? []} onChange={(valores) => setFiltro('statusCarga', valores)} />
      </FilterBar>

      {overview.isError && <MensagemErro mensagem={getApiErrorMessage(overview.error, 'Erro ao carregar indicadores de localização de cargas.')} tipo={getTipoErro(overview.error)} />}
      {overview.data && <TrackingKpiGrid overview={overview.data} />}

      <div className="mb-6 grid grid-cols-1 gap-6 xl:grid-cols-2">
        <ChartWrapper titulo="Timeline de Status" option={serieOption} isLoading={serie.isLoading} isEmpty={(serie.data ?? []).length === 0} />
        <ChartWrapper titulo="Distribuicao de Status" option={statusOption} isLoading={graficos.isLoading} isEmpty={statusData.length === 0} />
      </div>

      <div className="mb-6 grid grid-cols-1 gap-6 xl:grid-cols-2">
        <ChartWrapper titulo="Previsao Vencida por Filial Atual" option={vencidasOption} isLoading={graficos.isLoading} isEmpty={vencidasFilial.length === 0} />
        <ChartWrapper titulo="Valor por Regiao de Destino" option={valorRegiaoOption} isLoading={graficos.isLoading} isEmpty={valorRegiao.length === 0} />
      </div>

      <div className="mb-3 flex justify-end">
        <ExportButton nomeArquivo="localizacao-cargas" onExport={() => exportarTrackingCsv(filtro)} />
      </div>
      <DataTable
        titulo="Localização de Cargas Analítica"
        dados={tabela.data?.conteudo ?? []}
        colunas={colunas}
        chaveLinha="numeroMinuta"
        isLoading={tabela.isLoading}
        totalRegistros={tabela.data?.totalElementos}
        paginaAtual={paginacaoTabela.pagina}
        tamanhoPagina={paginacaoTabela.tamanhoPagina}
        onPaginaChange={paginacaoTabela.setPagina}
        onTamanhoPaginaChange={paginacaoTabela.setTamanhoPagina}
      />
    </div>
  );
}
