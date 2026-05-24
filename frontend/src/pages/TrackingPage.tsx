import { useEffect, useMemo, useState } from 'react';
import type { EChartsOption } from 'echarts';
import { ChevronLeft, ChevronRight, MapPin } from 'lucide-react';
import { useSearchParams } from 'react-router-dom';
import ChartWrapper from '../components/charts/ChartWrapper';
import TrackingKpiGrid from '../components/domain/tracking/TrackingKpiGrid';
import AsyncMultiSelect from '../components/shared/AsyncMultiSelect';
import AnalyticalDataTable, { type ColunaTabelaAnalitica } from '../components/shared/AnalyticalDataTable';
import DateRangePicker from '../components/shared/DateRangePicker';
import ExportButton from '../components/shared/ExportButton';
import FilterBar, { type ActiveFilter } from '../components/shared/FilterBar';
import StatusBadge from '../components/shared/StatusBadge';
import MensagemErro from '../components/ui/MensagemErro';
import { exportarTrackingCsv } from '../api/endpoints/trackingServico';
import { useAutenticacao } from '../contexts/AutenticacaoContext';
import { useFiltro } from '../contexts/FiltroContext';
import { usePageHeader } from '../contexts/PageHeaderContext';
import { useFiliais } from '../hooks/queries/useDimensoes';
import { useTrackingDashboard, useTrackingDetalhesPaginada } from '../hooks/queries/useTracking';
import { useAnalyticalTableFilters } from '../hooks/useAnalyticalTableFilters';
import { useTabelaPaginadaState } from '../hooks/useTabelaPaginadaState';
import type { TrackingFiltro, TrackingMatrizRegiao, TrackingRawRow } from '../types/tracking';
import { CORES, PALETA_SERIES } from '../utils/chartColors';
import { getApiErrorMessage, getTipoErro } from '../utils/apiError';
import { dataHojeLocal, dataNDiasAtrasLocal } from '../utils/dateUtils';
import { formatarMoeda, formatarPeso } from '../utils/formatadores';
import { combinarStatusOptions } from '../utils/tableStatusOptions';

const STATUS_RAPIDOS = ['Em armazém', 'Em transferência', 'Em entrega'] as const;
const STATUS_BASE = ['Pendente', 'Manifestado', 'Em armazém', 'Em transferência', 'Em entrega', 'Finalizado', 'Entregue', 'Cancelado'];
const REGIOES_POR_PAGINA = 9;
const STATUS_ROSCA_CORES: Record<string, string> = {
  pendente: CORES.aviso,
  manifestado: PALETA_SERIES[8],
  'em armazém': CORES.secundaria,
  'em transferência': CORES.info,
  'em entrega': CORES.primaria,
  finalizado: CORES.sucesso,
  entregue: PALETA_SERIES[7],
  cancelado: CORES.perigo,
  canceled: CORES.perigo,
  cancelled: CORES.perigo,
  'sem status': CORES.cinza,
};

function numeroCurto(valor: number): string {
  return valor.toLocaleString('pt-BR', { maximumFractionDigits: 0 });
}

function codigoRegiaoDestino(linha: TrackingMatrizRegiao): string {
  const bruto = linha.siglaRegiaoDestino?.trim() || 'SEM_MAP';
  const primeiraParte = bruto.split(/\s*-\s*/)[0]?.trim() || bruto;

  if (/^[A-Za-z0-9_/-]{2,8}$/.test(primeiraParte)) {
    return primeiraParte.toUpperCase();
  }

  return bruto.length <= 8 ? bruto.toUpperCase() : bruto.slice(0, 6).toUpperCase();
}

function removerPrefixoCodigoRegiao(texto: string, codigo: string): string {
  if (!texto.toUpperCase().startsWith(codigo.toUpperCase())) {
    return texto;
  }

  const restante = texto.slice(codigo.length).trimStart();
  return restante.startsWith('-') ? restante.slice(1).trimStart() : texto;
}

function descricaoRegiaoDestino(linha: TrackingMatrizRegiao, codigo: string): string {
  let descricao = linha.siglaRegiaoDestino?.trim() || '';

  for (let i = 0; i < 2; i += 1) {
    descricao = removerPrefixoCodigoRegiao(descricao, codigo);
  }

  if (descricao && descricao.toUpperCase() !== codigo.toUpperCase()) {
    return descricao;
  }

  return linha.responsavelRegiaoDestino?.trim() || 'Sem responsável';
}

function rotuloCurtoRegiao(valor: string): string {
  const texto = valor.trim();
  const primeiraParte = texto.split(/\s*-\s*/)[0]?.trim() || texto;

  if (/^[A-Za-z0-9_/-]{2,8}$/.test(primeiraParte)) {
    return primeiraParte.toUpperCase();
  }

  return texto.length > 14 ? `${texto.slice(0, 12)}...` : texto;
}

function corStatusRosca(status: string, index: number): string {
  return STATUS_ROSCA_CORES[status.toLowerCase()] ?? PALETA_SERIES[index % PALETA_SERIES.length];
}

function codigoFilialAtual(valor: string): string {
  const texto = valor.trim();
  const prefixo = texto.split(/\s*[-–—]\s*/)[0]?.trim() || texto;
  return prefixo.slice(0, 3).toUpperCase();
}

function MatrizRegiaoDestino({
  linhas,
  statusSelecionados,
  onToggleStatus,
}: {
  linhas: TrackingMatrizRegiao[];
  statusSelecionados: string[];
  onToggleStatus: (status: string) => void;
}) {
  const [pagina, setPagina] = useState(0);
  const totalPaginas = Math.max(1, Math.ceil(linhas.length / REGIOES_POR_PAGINA));
  const paginaAtual = Math.min(pagina, totalPaginas - 1);
  const inicio = paginaAtual * REGIOES_POR_PAGINA;
  const linhasVisiveis = linhas.slice(inicio, inicio + REGIOES_POR_PAGINA);
  const linhasReserva = linhas.length > 0 ? Math.max(0, REGIOES_POR_PAGINA - linhasVisiveis.length) : 0;

  return (
    <section className="mb-6 flex h-full min-h-0 flex-col rounded-[20px] border p-4 shadow-sm" style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)' }}>
      <div className="mb-3 flex shrink-0 flex-col gap-3 border-b pb-3 sm:flex-row sm:items-center sm:justify-between" style={{ borderColor: 'var(--color-border)' }}>
        <div className="min-w-0">
          <h2 className="truncate text-sm font-bold" style={{ color: 'var(--color-text)' }}>
            Responsável pela Região de Destino
          </h2>
          <span className="text-[11px] font-medium" style={{ color: 'var(--color-text-subtle)' }}>
            {linhas.length} regiões
          </span>
        </div>
        <div
          className="flex max-w-full items-center gap-1 overflow-x-auto rounded-xl border p-1"
          style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)' }}
          aria-label="Filtro rápido de status"
        >
          {STATUS_RAPIDOS.map((status) => {
            const ativo = statusSelecionados.includes(status);
            return (
              <button
                key={status}
                type="button"
                onClick={() => onToggleStatus(status)}
                className="inline-flex h-7 shrink-0 items-center gap-1.5 rounded-lg border px-3 text-[11px] font-semibold transition"
                style={{
                  backgroundColor: ativo ? 'var(--color-card)' : 'transparent',
                  borderColor: ativo ? 'var(--color-primary)' : 'transparent',
                  color: ativo ? 'var(--color-primary)' : 'var(--color-text-muted)',
                }}
              >
                <span
                  className="h-1.5 w-1.5 rounded-full"
                  style={{ backgroundColor: ativo ? 'var(--color-primary)' : 'var(--color-border)' }}
                />
                {status === 'Em armazém' ? 'No galpão' : status}
              </button>
            );
          })}
        </div>
      </div>

      <div className="min-h-0 flex-1">
        <div className="hidden grid-cols-[5.75rem_minmax(5.8rem,1fr)_minmax(6.1rem,1fr)_minmax(6.1rem,1fr)_minmax(4.25rem,0.74fr)_minmax(4.75rem,0.74fr)] gap-2 px-3 pb-2 text-[10px] font-semibold uppercase md:grid" style={{ color: 'var(--color-text-subtle)' }}>
          <span>Região</span>
          <span className="text-right">Peso Taxado</span>
          <span className="text-right">Valor Frete</span>
          <span className="text-right">Valor NF</span>
          <span className="text-right">Volumes</span>
          <span className="text-right">Fora Prazo</span>
        </div>

        <div className="space-y-1">
          {linhasVisiveis.map((linha, index) => {
            const codigo = codigoRegiaoDestino(linha);
            const descricao = descricaoRegiaoDestino(linha, codigo);

            return (
              <div
                key={`${linha.siglaRegiaoDestino}-${linha.responsavelRegiaoDestino}`}
                className="rounded-lg border px-3 py-2.5"
                style={{
                  borderColor: 'var(--color-border)',
                  backgroundColor: index % 2 === 0 ? 'var(--color-bg)' : 'transparent',
                }}
              >
                <div className="grid grid-cols-2 gap-x-3 gap-y-2 md:grid-cols-[5.75rem_minmax(5.8rem,1fr)_minmax(6.1rem,1fr)_minmax(6.1rem,1fr)_minmax(4.25rem,0.74fr)_minmax(4.75rem,0.74fr)] md:items-center md:gap-2">
                  <div className="col-span-2 min-w-0 md:col-span-1" title={`${codigo} - ${descricao}`}>
                    <div className="truncate text-2xl font-extrabold leading-none md:text-[23px]" style={{ color: 'var(--color-text)' }}>
                      {codigo}
                    </div>
                    <div className="mt-0.5 truncate text-[11px] font-medium" style={{ color: 'var(--color-text-subtle)' }}>
                      {descricao}
                    </div>
                  </div>
                  <MetricCell label="Peso Taxado" value={formatarPeso(linha.pesoTaxado)} />
                  <MetricCell label="Valor Frete" value={formatarMoeda(linha.valorFrete)} />
                  <MetricCell label="Valor NF" value={formatarMoeda(linha.valorNota)} />
                  <MetricCell label="Volumes" value={numeroCurto(linha.volumes)} />
                  <MetricCell label="Fora do Prazo" value={numeroCurto(linha.foraDoPrazo)} danger={linha.foraDoPrazo > 0} />
                </div>
              </div>
            );
          })}
          {Array.from({ length: linhasReserva }, (_, index) => (
            <div
              key={`reserva-matriz-${index}`}
              aria-hidden="true"
              className="pointer-events-none invisible hidden rounded-lg border px-3 py-2.5 md:block"
              style={{ borderColor: 'var(--color-border)' }}
            >
              <div className="grid grid-cols-[5.75rem_minmax(5.8rem,1fr)_minmax(6.1rem,1fr)_minmax(6.1rem,1fr)_minmax(4.25rem,0.74fr)_minmax(4.75rem,0.74fr)] items-center gap-2">
                <div className="min-w-0">
                  <div className="text-[23px] font-extrabold leading-none">---</div>
                  <div className="mt-0.5 text-[11px] font-medium">Reserva</div>
                </div>
                <MetricCell label="Peso Taxado" value="0 kg" />
                <MetricCell label="Valor Frete" value="R$ 0,00" />
                <MetricCell label="Valor NF" value="R$ 0,00" />
                <MetricCell label="Volumes" value="0" />
                <MetricCell label="Fora do Prazo" value="0" />
              </div>
            </div>
          ))}
          {linhas.length === 0 && (
            <div className="rounded-lg border border-dashed px-4 py-8 text-center text-sm" style={{ borderColor: 'var(--color-border)', color: 'var(--color-text-muted)' }}>
              Nenhuma carga encontrada.
            </div>
          )}
        </div>
      </div>

      <div className="mt-3 flex shrink-0 items-center justify-between gap-3 border-t pt-3" style={{ borderColor: 'var(--color-border)' }}>
        <span className="text-[11px] font-medium" style={{ color: 'var(--color-text-subtle)' }}>
          {linhas.length === 0 ? '0 regiões' : `${inicio + 1}-${Math.min(inicio + REGIOES_POR_PAGINA, linhas.length)} de ${linhas.length}`}
        </span>
        <div className="flex items-center gap-1">
          <button
            type="button"
            aria-label="Página anterior"
            disabled={paginaAtual === 0}
            onClick={() => setPagina(Math.max(0, paginaAtual - 1))}
            className="flex h-7 w-7 items-center justify-center rounded-lg border transition disabled:cursor-not-allowed disabled:opacity-40"
            style={{ borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
          >
            <ChevronLeft size={14} />
          </button>
          <span className="min-w-10 text-center text-[11px] font-semibold" style={{ color: 'var(--color-text)' }}>
            {paginaAtual + 1}/{totalPaginas}
          </span>
          <button
            type="button"
            aria-label="Próxima página"
            disabled={paginaAtual >= totalPaginas - 1}
            onClick={() => setPagina(Math.min(totalPaginas - 1, paginaAtual + 1))}
            className="flex h-7 w-7 items-center justify-center rounded-lg border transition disabled:cursor-not-allowed disabled:opacity-40"
            style={{ borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
          >
            <ChevronRight size={14} />
          </button>
        </div>
      </div>
    </section>
  );
}

function MetricCell({ label, value, danger = false }: { label: string; value: string; danger?: boolean }) {
  return (
    <div className="min-w-0 text-right">
      <div className="truncate text-[17px] font-bold leading-tight tabular-nums md:text-lg" style={{ color: danger ? 'var(--color-negative-text)' : 'var(--color-text)' }}>
        {value}
      </div>
      <div className="mt-0.5 truncate text-[11px] font-medium" style={{ color: 'var(--color-text-subtle)' }}>
        {label}
      </div>
    </div>
  );
}

export default function TrackingPage() {
  const { usuario } = useAutenticacao();
  const [searchParams] = useSearchParams();
  const { dataInicio, dataFim, filtros, setDataInicio, setDataFim, setDataRange, setFiltro, limparFiltros } = useFiltro();
  const filiais = useFiliais();

  const filiaisUsuario = useMemo(() => usuario?.filiaisPermitidasEfetivas ?? [], [usuario?.filiaisPermitidasEfetivas]);
  const filiaisDisponiveis = useMemo(() => filiais.data ?? [], [filiais.data]);
  const filialAtualSelecionada = filtros.filialAtual?.[0] ?? '';
  const dashboardHabilitado = filialAtualSelecionada.trim().length > 0 && (filtros.filialAtual?.length ?? 0) === 1;

  useEffect(() => {
    if ((filtros.filialAtual?.length ?? 0) > 0) {
      return;
    }

    const filialPadrao = filiaisUsuario[0] ?? filiaisDisponiveis[0];
    if (filialPadrao) {
      setFiltro('filialAtual', [filialPadrao]);
    }
  }, [filiaisDisponiveis, filiaisUsuario, filtros.filialAtual?.length, setFiltro]);

  useEffect(() => {
    if (!searchParams.has('dataInicio') && !searchParams.has('dataFim')) {
      setDataRange(dataNDiasAtrasLocal(90), dataHojeLocal());
    }
  }, [searchParams, setDataRange]);

  const filtro: TrackingFiltro = {
    dataInicio,
    dataFim,
    filialAtual: filtros.filialAtual,
    statusCarga: filtros.statusCarga,
  };

  const activeFilters: ActiveFilter[] = [
    {
      label: 'Filial Atual',
      count: filtros.filialAtual?.length ?? 0,
      valueLabel: codigoFilialAtual(filialAtualSelecionada),
      onRemove: () => setFiltro('filialAtual', []),
    },
    { label: 'Status', count: filtros.statusCarga?.length ?? 0, onRemove: () => setFiltro('statusCarga', []) },
  ];

  const dashboard = useTrackingDashboard(filtro, dashboardHabilitado);
  const filtrosTabela = useAnalyticalTableFilters();
  const paginacaoTabela = useTabelaPaginadaState(JSON.stringify({ filtro, tabela: filtrosTabela.resetKey }));
  const tabela = useTrackingDetalhesPaginada(
    filtro,
    paginacaoTabela.pagina,
    paginacaoTabela.tamanhoPagina,
    filtrosTabela.apiFilters,
    dashboardHabilitado,
  );

  usePageHeader({
    title: 'Localização de Cargas',
    description: 'Status da carga, previsões vencidas e carteira em trânsito.',
    updatedAt: dashboard.data?.overview.updatedAt ?? null,
  });

  const statusData = useMemo(() => dashboard.data?.graficos.statusDistribuicao ?? [], [dashboard.data?.graficos.statusDistribuicao]);
  const valorRegiao = useMemo(() => dashboard.data?.graficos.valorPorRegiaoDestino ?? [], [dashboard.data?.graficos.valorPorRegiaoDestino]);
  const matriz = dashboard.data?.matrizRegiaoDestino ?? [];

  const statusTabelaOptions = combinarStatusOptions(
    STATUS_BASE,
    statusData.map((item) => item.status),
    (tabela.data?.conteudo ?? []).map((item) => item.statusCarga),
    filtros.statusCarga,
  );

  const valorChartData = useMemo(() => valorRegiao, [valorRegiao]);
  const statusTotal = useMemo(() => statusData.reduce((total, item) => total + item.total, 0), [statusData]);

  const statusOption: EChartsOption = {
    tooltip: { trigger: 'item', formatter: '{b}<br/>{c} cargas ({d}%)' },
    grid: { top: 0, right: 0, bottom: 0, left: 0 },
    xAxis: { show: false },
    yAxis: { show: false },
    title: {
      text: numeroCurto(statusTotal),
      subtext: 'Cargas',
      left: 'center',
      top: '36%',
      textStyle: {
        color: CORES.primaria,
        fontSize: 22,
        fontWeight: 800,
      },
      subtextStyle: {
        color: CORES.cinza,
        fontSize: 11,
        fontWeight: 600,
      },
    },
    legend: {
      type: 'scroll',
      bottom: 0,
      left: 'center',
      itemWidth: 8,
      itemHeight: 8,
      formatter: (name: string) => {
        const item = statusData.find((status) => status.status === name);
        return item ? `${name} (${numeroCurto(item.total)})` : name;
      },
    },
    series: [{
      name: 'Cargas',
      type: 'pie',
      radius: ['46%', '72%'],
      center: ['50%', '44%'],
      minAngle: 4,
      avoidLabelOverlap: true,
      data: statusData.map((item, index) => ({
        name: item.status,
        value: item.total,
        itemStyle: { color: corStatusRosca(item.status, index) },
      })),
      label: {
        show: true,
        formatter: '{b}\n{c} ({d}%)',
        fontSize: 11,
        fontWeight: 600,
      },
      labelLine: { show: true, length: 10, length2: 8 },
      emphasis: {
        label: {
          show: true,
          formatter: '{b}\n{c} cargas\n{d}%',
          fontSize: 12,
          fontWeight: 700,
        },
      },
    }],
  };

  const valorRegiaoOption: EChartsOption = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 66, right: 12, top: 10, bottom: 38 },
    xAxis: {
      type: 'category',
      data: valorChartData.map((item) => item.regiaoDestino),
      axisLabel: {
        rotate: 0,
        interval: 0,
        width: 52,
        overflow: 'truncate',
        margin: 10,
        formatter: (value: string) => rotuloCurtoRegiao(value),
      },
    },
    yAxis: { type: 'value' },
    series: [{
      name: 'Valor Frete',
      type: 'bar',
      barWidth: 18,
      barCategoryGap: '32%',
      data: valorChartData.map((item) => item.valorFrete),
      itemStyle: { color: CORES.secundaria },
    }],
  };

  const colunas: ColunaTabelaAnalitica<TrackingRawRow>[] = [
    { chave: 'numeroMinuta', label: 'Minuta', fixo: true, filtroTabela: 'codigo' },
    { chave: 'dataFrete', label: 'Data' },
    { chave: 'statusCarga', label: 'Status', filtroTabela: 'status', formato: (valor) => <StatusBadge status={String(valor)} /> },
    { chave: 'filialAtual', label: 'Filial Atual' },
    { chave: 'filialDestino', label: 'Filial Destino' },
    { chave: 'regiaoDestino', label: 'Região Destino', filtroTabela: 'destino' },
    { chave: 'pesoTaxadoRaw', label: 'Peso', formato: (valor) => formatarPeso(Number(valor ?? 0)) },
    { chave: 'valorFrete', label: 'Valor Frete', formato: (valor) => formatarMoeda(Number(valor ?? 0)) },
    { chave: 'previsaoEntrega', label: 'Previsão' },
  ];

  function alternarStatus(status: string) {
    const atuais = filtros.statusCarga ?? [];
    setFiltro('statusCarga', atuais.includes(status) ? atuais.filter((item) => item !== status) : [...atuais, status]);
  }

  return (
    <div className="w-full">
      <FilterBar onClear={limparFiltros} activeFilters={activeFilters} dataInicio={dataInicio} dataFim={dataFim}>
        <DateRangePicker dataInicio={dataInicio} dataFim={dataFim} onDataInicioChange={setDataInicio} onDataFimChange={setDataFim} onRangeChange={setDataRange} />
        <AsyncMultiSelect
          label="Filial Atual"
          opcoes={filiaisDisponiveis}
          selecionados={filtros.filialAtual ?? []}
          onChange={(valores) => setFiltro('filialAtual', valores.slice(-1))}
          placeholder="Selecione"
          isLoading={filiais.isLoading}
        />
        <AsyncMultiSelect
          label="Status"
          opcoes={STATUS_BASE}
          selecionados={filtros.statusCarga ?? []}
          onChange={(valores) => setFiltro('statusCarga', valores)}
        />
      </FilterBar>

      {!dashboardHabilitado && (
        <section className="mb-6 flex min-h-64 flex-col items-center justify-center rounded-lg border px-6 text-center" style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)' }}>
          <MapPin size={34} style={{ color: 'var(--color-primary)' }} />
          <h2 className="mt-3 text-lg font-bold" style={{ color: 'var(--color-text)' }}>Selecione a Filial Atual</h2>
        </section>
      )}

      {dashboardHabilitado && dashboard.isError && <MensagemErro mensagem={getApiErrorMessage(dashboard.error, 'Erro ao carregar localização de cargas.')} tipo={getTipoErro(dashboard.error)} />}
      {dashboardHabilitado && dashboard.data && <TrackingKpiGrid overview={dashboard.data.overview} />}

      {dashboardHabilitado && (
        <div className="grid grid-cols-1 gap-6 xl:grid-cols-[minmax(0,1.18fr)_minmax(360px,0.82fr)]">
          <MatrizRegiaoDestino linhas={matriz} statusSelecionados={filtros.statusCarga ?? []} onToggleStatus={alternarStatus} />
          <div className="grid grid-cols-1 gap-6">
            <ChartWrapper titulo="Distribuição de Status" option={statusOption} isLoading={dashboard.isLoading} isEmpty={statusData.length === 0} altura={290} />
            <ChartWrapper titulo="Valor por Região de Destino" option={valorRegiaoOption} isLoading={dashboard.isLoading} isEmpty={valorRegiao.length === 0} altura={290} />
          </div>
        </div>
      )}

      {dashboardHabilitado && (
        <>
          <div className="mb-3 flex justify-end">
            <ExportButton nomeArquivo="localizacao-cargas" onExport={() => exportarTrackingCsv(filtro, filtrosTabela.apiFilters)} />
          </div>
          <AnalyticalDataTable
            titulo="Detalhamento de Cargas"
            dados={tabela.data?.conteudo ?? []}
            colunas={colunas}
            chaveLinha="numeroMinuta"
            filtros={filtrosTabela.filters}
            hiddenActiveCount={filtrosTabela.hiddenActiveCount}
            hasAnyFilter={filtrosTabela.hasAnyFilter}
            onTextFilterChange={filtrosTabela.setTextFilter}
            onMultiFilterChange={filtrosTabela.setMultiFilter}
            onColumnFilterChange={filtrosTabela.setColumnFilter}
            onClearFilters={filtrosTabela.clearTableFilters}
            statusOptions={statusTabelaOptions}
            statusOptionsLoading={dashboard.isLoading}
            isLoading={tabela.isLoading}
            totalRegistros={tabela.data?.totalElementos}
            paginaAtual={paginacaoTabela.pagina}
            tamanhoPagina={paginacaoTabela.tamanhoPagina}
            onPaginaChange={paginacaoTabela.setPagina}
            onTamanhoPaginaChange={paginacaoTabela.setTamanhoPagina}
          />
        </>
      )}
    </div>
  );
}
