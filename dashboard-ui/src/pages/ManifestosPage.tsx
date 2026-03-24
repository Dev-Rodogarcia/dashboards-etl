import type { EChartsOption } from 'echarts';
import ChartWrapper from '../components/charts/ChartWrapper';
import ManifestosKpiGrid from '../components/domain/manifestos/ManifestosKpiGrid';
import ManifestosTrend from '../components/domain/manifestos/ManifestosTrend';
import AsyncMultiSelect from '../components/shared/AsyncMultiSelect';
import DataTable, { type ColunaTabela } from '../components/shared/DataTable';
import DateRangePicker from '../components/shared/DateRangePicker';
import ExportButton from '../components/shared/ExportButton';
import FilterBar from '../components/shared/FilterBar';
import LastUpdated from '../components/shared/LastUpdated';
import StatusBadge from '../components/shared/StatusBadge';
import MensagemErro from '../components/ui/MensagemErro';
import { useFiltro } from '../contexts/FiltroContext';
import { useFiliais, useMotoristas, useVeiculos } from '../hooks/queries/useDimensoes';
import { useManifestosGraficos, useManifestosOverview, useManifestosSerie, useManifestosTabela } from '../hooks/queries/useManifestos';
import type { ManifestoResumoRow, ManifestosFiltro } from '../types/manifestos';
import { CORES } from '../utils/chartColors';
import { formatarMoeda, formatarNumero, formatarPeso } from '../utils/formatadores';

export default function ManifestosPage() {
  const { dataInicio, dataFim, filtros, setDataInicio, setDataFim, setFiltro, limparFiltros } = useFiltro();
  const filiais = useFiliais();
  const motoristas = useMotoristas();
  const veiculos = useVeiculos();

  const filtro: ManifestosFiltro = {
    dataInicio,
    dataFim,
    filiais: filtros.filiais,
    status: filtros.status,
    motoristas: filtros.motoristas,
    veiculos: filtros.veiculos,
  };

  const overview = useManifestosOverview(filtro);
  const serie = useManifestosSerie(filtro);
  const graficos = useManifestosGraficos(filtro);
  const tabela = useManifestosTabela(filtro, 120);

  const custoPorFilial = graficos.data?.custoPorFilial ?? [];
  const rankingMotorista = graficos.data?.rankingMotorista ?? [];
  const composicao = graficos.data?.composicaoCusto ?? [];
  const ocupacaoScatter = graficos.data?.ocupacaoScatter ?? [];

  const custoOption: EChartsOption = {
    grid: { left: 120 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: custoPorFilial.map((item) => item.filial).reverse() },
    series: [{ type: 'bar', data: custoPorFilial.map((item) => item.custoTotal).reverse(), itemStyle: { color: CORES.secundaria } }],
  };

  const rankingOption: EChartsOption = {
    grid: { left: 140 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: rankingMotorista.map((item) => item.motorista).reverse() },
    series: [{ type: 'bar', data: rankingMotorista.map((item) => item.custoTotal).reverse(), itemStyle: { color: CORES.primaria } }],
  };

  const composicaoOption: EChartsOption = {
    tooltip: { trigger: 'item' },
    series: [{ type: 'pie', radius: ['38%', '68%'], data: composicao.map((item) => ({ name: item.categoria, value: item.valor })) }],
  };

  const ocupacaoOption: EChartsOption = {
    xAxis: { type: 'value', name: 'Peso taxado' },
    yAxis: { type: 'value', name: 'M3' },
    series: [{ type: 'scatter', data: ocupacaoScatter.map((item) => [item.pesoTaxado, item.totalM3, item.custoTotal]), itemStyle: { color: CORES.aviso } }],
    tooltip: {
      formatter: (params: unknown) => {
        const item = params as { value?: [number, number, number] };
        if (!item.value) return '';
        return `Peso: ${formatarPeso(item.value[0])}<br/>M3: ${formatarNumero(item.value[1], 2)}<br/>Custo: ${formatarMoeda(item.value[2])}`;
      },
    },
  };

  const colunas: ColunaTabela<ManifestoResumoRow>[] = [
    { chave: 'numero', label: 'Manifesto', fixo: true },
    { chave: 'status', label: 'Status', formato: (valor) => <StatusBadge status={String(valor)} /> },
    { chave: 'filial', label: 'Filial' },
    { chave: 'motorista', label: 'Motorista' },
    { chave: 'veiculoPlaca', label: 'Veiculo' },
    { chave: 'dataCriacao', label: 'Criacao' },
    { chave: 'totalPesoTaxado', label: 'Peso', formato: (valor) => formatarPeso(Number(valor ?? 0)) },
    { chave: 'totalM3', label: 'M3', formato: (valor) => formatarNumero(Number(valor ?? 0), 2) },
    { chave: 'custoTotal', label: 'Custo', formato: (valor) => formatarMoeda(Number(valor ?? 0)) },
    { chave: 'valorFrete', label: 'Valor Frete', formato: (valor) => formatarMoeda(Number(valor ?? 0)) },
    { chave: 'kmTotal', label: 'KM', formato: (valor) => formatarNumero(Number(valor ?? 0), 0) },
  ];

  return (
    <div className="w-full">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-[#21478A]">Manifestos</h1>
          <p className="text-sm text-gray-500">Custos, ocupacao de carga e performance por motorista.</p>
        </div>
        <LastUpdated dataExtracao={overview.data?.updatedAt ?? null} />
      </div>

      <FilterBar onClear={limparFiltros}>
        <DateRangePicker
          dataInicio={dataInicio}
          dataFim={dataFim}
          onDataInicioChange={setDataInicio}
          onDataFimChange={setDataFim}
        />
        <AsyncMultiSelect
          label="Filiais"
          opcoes={filiais.data ?? []}
          selecionados={filtros.filiais ?? []}
          onChange={(valores) => setFiltro('filiais', valores)}
          isLoading={filiais.isLoading}
        />
        <AsyncMultiSelect
          label="Motoristas"
          opcoes={motoristas.data ?? []}
          selecionados={filtros.motoristas ?? []}
          onChange={(valores) => setFiltro('motoristas', valores)}
          isLoading={motoristas.isLoading}
        />
        <AsyncMultiSelect
          label="Veiculos"
          opcoes={(veiculos.data ?? []).map((item) => item.placa)}
          selecionados={filtros.veiculos ?? []}
          onChange={(valores) => setFiltro('veiculos', valores)}
          isLoading={veiculos.isLoading}
        />
      </FilterBar>

      {overview.isError && <MensagemErro mensagem="Erro ao carregar indicadores de manifestos." />}
      {overview.data && <ManifestosKpiGrid overview={overview.data} />}

      <div className="mb-6 grid grid-cols-1 gap-6 xl:grid-cols-3">
        <ManifestosTrend dados={serie.data ?? []} isLoading={serie.isLoading} />
        <ChartWrapper titulo="Custo por Filial" option={custoOption} isLoading={graficos.isLoading} isEmpty={custoPorFilial.length === 0} />
        <ChartWrapper titulo="Composicao de Custos" option={composicaoOption} isLoading={graficos.isLoading} isEmpty={composicao.length === 0} />
      </div>

      <div className="mb-6 grid grid-cols-1 gap-6 xl:grid-cols-2">
        <ChartWrapper titulo="Ranking de Motoristas por Custo" option={rankingOption} isLoading={graficos.isLoading} isEmpty={rankingMotorista.length === 0} />
        <ChartWrapper titulo="Peso x Cubagem x Custo" option={ocupacaoOption} isLoading={graficos.isLoading} isEmpty={ocupacaoScatter.length === 0} altura={320} />
      </div>

      <div className="mb-3 flex justify-end">
        <ExportButton dados={(tabela.data ?? []) as unknown as Record<string, unknown>[]} nomeArquivo="manifestos" />
      </div>
      <DataTable titulo="Manifestos Analiticos" dados={tabela.data ?? []} colunas={colunas} chaveLinha="identificadorUnico" isLoading={tabela.isLoading} />
    </div>
  );
}
