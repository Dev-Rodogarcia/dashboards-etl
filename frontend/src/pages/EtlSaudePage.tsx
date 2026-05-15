import type { EChartsOption } from 'echarts';
import ChartWrapper from '../components/charts/ChartWrapper';
import EtlSaudeKpiGrid from '../components/domain/etlSaude/EtlSaudeKpiGrid';
import DataTable, { type ColunaTabela } from '../components/shared/DataTable';
import DateRangePicker from '../components/shared/DateRangePicker';
import ExportButton from '../components/shared/ExportButton';
import FilterBar from '../components/shared/FilterBar';
import StatusBadge from '../components/shared/StatusBadge';
import MensagemErro from '../components/ui/MensagemErro';
import { exportarEtlSaudeCsv } from '../api/endpoints/etlSaudeServico';
import { getApiErrorMessage, getTipoErro } from '../utils/apiError';
import { useFiltro } from '../contexts/FiltroContext';
import { usePageHeader } from '../contexts/PageHeaderContext';
import { useEtlSaudeGraficos, useEtlSaudeOverview, useEtlSaudeSerie, useEtlSaudeTabelaPaginada } from '../hooks/queries/useEtlSaude';
import { useTabelaPaginadaState } from '../hooks/useTabelaPaginadaState';
import type { EtlExecucaoRow } from '../types/etlSaude';

export default function EtlSaudePage() {
  const { dataInicio, dataFim, setDataInicio, setDataFim, setDataRange, limparFiltros } = useFiltro();
  const filtro = { dataInicio, dataFim };

  const overview = useEtlSaudeOverview(filtro);
  const serie = useEtlSaudeSerie(filtro);
  const graficos = useEtlSaudeGraficos(filtro);
  const paginacaoTabela = useTabelaPaginadaState(JSON.stringify(filtro));
  const tabela = useEtlSaudeTabelaPaginada(filtro, paginacaoTabela.pagina, paginacaoTabela.tamanhoPagina);

  usePageHeader({
    title: 'Saúde do ETL',
    description: 'Execuções, volume processado e distribuição de erros.',
    updatedAt: overview.data?.updatedAt ?? null,
  });

  const serieDados = serie.data ?? [];
  const categorias = (graficos.data?.categoriasErro ?? []).slice(0, 10).reverse();
  const erroSerie = serie.isError ? getApiErrorMessage(serie.error, 'Erro ao carregar série do ETL.') : null;
  const erroGraficos = graficos.isError ? getApiErrorMessage(graficos.error, 'Erro ao carregar categorias de erro.') : null;

  const execucoesOption: EChartsOption = {
    legend: { bottom: 0 },
    tooltip: { trigger: 'axis' },
    grid: { top: 34, right: 18, bottom: 46, left: 42 },
    xAxis: { type: 'category', data: serieDados.map((item) => item.date) },
    yAxis: { type: 'value' },
    series: [
      {
        name: 'Execuções',
        type: 'bar',
        barMaxWidth: 28,
        data: serieDados.map((item) => item.execucoes),
      },
      {
        name: 'Erros',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        data: serieDados.map((item) => item.erros),
      },
    ],
  };

  const duracaoVolumeOption: EChartsOption = {
    legend: { bottom: 0 },
    tooltip: { trigger: 'axis' },
    grid: { top: 42, right: 54, bottom: 46, left: 48 },
    xAxis: { type: 'category', data: serieDados.map((item) => item.date) },
    yAxis: [
      { type: 'value', name: 'Duração (s)' },
      { type: 'value', name: 'Volume' },
    ],
    series: [
      {
        name: 'Duração média',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        yAxisIndex: 0,
        data: serieDados.map((item) => item.duracaoMedia),
      },
      {
        name: 'Volume processado',
        type: 'bar',
        barMaxWidth: 28,
        yAxisIndex: 1,
        data: serieDados.map((item) => item.volumeProcessado),
      },
    ],
  };

  const categoriasOption: EChartsOption = {
    tooltip: { trigger: 'axis' },
    grid: { top: 18, right: 24, bottom: 24, left: 160 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: categorias.map((item) => item.categoria) },
    series: [
      {
        name: 'Erros',
        type: 'bar',
        barMaxWidth: 22,
        data: categorias.map((item) => item.total),
      },
    ],
  };

  const colunas: ColunaTabela<EtlExecucaoRow>[] = [
    { chave: 'id', label: 'Execucao', fixo: true },
    { chave: 'data', label: 'Data' },
    { chave: 'inicio', label: 'Inicio' },
    { chave: 'fim', label: 'Fim' },
    { chave: 'duracaoSegundos', label: 'Duracao (s)' },
    { chave: 'totalRegistros', label: 'Volume' },
    { chave: 'status', label: 'Status', formato: (valor) => <StatusBadge status={String(valor)} /> },
    { chave: 'categoriaErro', label: 'Categoria Erro' },
  ];

  return (
    <div className="w-full">
      <FilterBar onClear={limparFiltros} dataInicio={dataInicio} dataFim={dataFim}>
        <DateRangePicker dataInicio={dataInicio} dataFim={dataFim} onDataInicioChange={setDataInicio} onDataFimChange={setDataFim} onRangeChange={setDataRange} />
      </FilterBar>

      {overview.isError && <MensagemErro mensagem={getApiErrorMessage(overview.error, 'Erro ao carregar indicadores do ETL.')} tipo={getTipoErro(overview.error)} />}
      {overview.data && <EtlSaudeKpiGrid overview={overview.data} />}

      <div className="mb-6 grid grid-cols-1 gap-6 xl:grid-cols-2">
        <ChartWrapper
          titulo="Execuções x Erros por Dia"
          option={execucoesOption}
          isLoading={serie.isLoading}
          isEmpty={serieDados.length === 0}
          erro={erroSerie}
          altura={320}
        />
        <ChartWrapper
          titulo="Duração Média x Volume"
          option={duracaoVolumeOption}
          isLoading={serie.isLoading}
          isEmpty={serieDados.length === 0}
          erro={erroSerie}
          altura={320}
        />
        <ChartWrapper
          titulo="Categorias de Erro"
          option={categoriasOption}
          isLoading={graficos.isLoading}
          isEmpty={categorias.length === 0}
          erro={erroGraficos}
          altura={320}
          className="xl:col-span-2"
        />
      </div>

      <div className="mb-3 flex justify-end">
        <ExportButton nomeArquivo="etl-saude" onExport={() => exportarEtlSaudeCsv(filtro)} />
      </div>
      <DataTable
        titulo="Execucoes do ETL"
        dados={tabela.data?.conteudo ?? []}
        colunas={colunas}
        chaveLinha="id"
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
