import { useCallback, useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import type { EChartsOption } from 'echarts';
import { Eye } from 'lucide-react';
import ChartWrapper from '../components/charts/ChartWrapper';
import { useEchartsTheme } from '../components/charts/useEchartsTheme';
import QuarentenaErrosPanel from '../components/domain/integracoes/QuarentenaErrosPanel';
import AnalyticalDataTable, {
  type ColunaTabelaAnalitica,
  type SortDirection,
} from '../components/shared/AnalyticalDataTable';
import CanhotoImagemModal from '../components/domain/integracoes/CanhotoImagemModal';
import KpiCard from '../components/shared/KpiCard';
import TooltipKpi from '../components/shared/TooltipKpi';
import DateRangePicker from '../components/shared/DateRangePicker';
import FilterBar from '../components/shared/FilterBar';
import StatusBadge from '../components/shared/StatusBadge';
import MensagemErro from '../components/ui/MensagemErro';
import {
  buscarIntegracoesAuditoria,
  buscarIntegracoesEvolucaoDiaria,
  type IntegracoesEscopo,
  type IntegracaoEvolucaoDiaria,
  type IntegracaoMetricaConsolidada,
  type IntegracaoPendencia,
} from '../api/endpoints/integracoesServico';
import { useFiltro } from '../contexts/FiltroContext';
import { usePageHeader } from '../contexts/PageHeaderContext';
import { useAnalyticalTableFilters } from '../hooks/useAnalyticalTableFilters';
import { useTabelaPaginadaState } from '../hooks/useTabelaPaginadaState';
import { getApiErrorMessage, getTipoErro } from '../utils/apiError';
import { buildBaseBarOption, buildBaseLineOption, getEchartsThemeTokens } from '../utils/echartsBuilders';
import { formatarDataHora, formatarNumero, formatarPorcentagem } from '../utils/formatadores';
import { OPERATIONAL_QUERY_POLLING_OPTIONS } from '../utils/pollingUtils';
import { combinarStatusOptions } from '../utils/tableStatusOptions';

const QUERY_KEY = ['integracoes'];
const STATUS_PADRAO = ['SUCESSO', 'ERRO_DESTINO', 'PENDENTE_FOTO'];
const EMPTY_METRICAS: IntegracaoMetricaConsolidada[] = [];
const EMPTY_PENDENCIAS: IntegracaoPendencia[] = [];
const EMPTY_EVOLUCAO_DIARIA: IntegracaoEvolucaoDiaria[] = [];
type IntegracoesAba = IntegracoesEscopo | 'QUARENTENA';
const ABAS_INTEGRACOES: { valor: IntegracoesAba; label: string }[] = [
  { valor: 'PENDENCIAS', label: 'Pendências' },
  { valor: 'SUCESSO', label: 'Integrados com Sucesso' },
  { valor: 'QUARENTENA', label: 'Quarentena' },
];

interface IntegracoesTableSort {
  field: keyof IntegracaoPendencia & string;
  direction: SortDirection;
}

function formatarPercentual(valor: number | null | undefined) {
  return typeof valor === 'number' && Number.isFinite(valor) ? formatarPorcentagem(valor, 2) : '-';
}

function tomPercentual(valor: number | null | undefined) {
  if (typeof valor !== 'number' || !Number.isFinite(valor)) return 'neutral';
  if (valor >= 95) return 'positive';
  if (valor >= 80) return 'warning';
  return 'negative';
}

function formatarInteiro(valor: unknown) {
  const numero = Number(valor);
  return Number.isFinite(numero) ? formatarNumero(numero) : '-';
}

function formatarData(valor: unknown) {
  return typeof valor === 'string' && valor.trim() ? formatarDataHora(valor) : '-';
}

function renderStatus(valor: unknown) {
  return valor ? <StatusBadge status={String(valor)} /> : '-';
}

function numeroSeguro(valor: unknown) {
  const numero = Number(valor);
  return Number.isFinite(numero) ? numero : 0;
}

function calcularVolumeTotal(metricas: IntegracaoMetricaConsolidada[]) {
  return metricas.reduce((total, item) => total + numeroSeguro(item.totalRegistros), 0);
}

function calcularTaxaPonderada(
  metricas: IntegracaoMetricaConsolidada[],
  campo: 'percentualXmlSucesso' | 'percentualCanhotoSucesso',
) {
  const volumeTotal = calcularVolumeTotal(metricas);
  if (volumeTotal <= 0) {
    return 0;
  }

  const sucessoPonderado = metricas.reduce(
    (total, item) => total + numeroSeguro(item.totalRegistros) * numeroSeguro(item[campo]),
    0,
  );
  return sucessoPonderado / volumeTotal;
}

function calcularTotalErros(evolucaoDiaria: IntegracaoEvolucaoDiaria[]) {
  return evolucaoDiaria.reduce((total, item) => total + numeroSeguro(item.erros), 0);
}

function formatarDataEixo(valor: string) {
  const [data] = valor.split('T');
  const partes = data.split('-');
  if (partes.length === 3) {
    return `${partes[2]}/${partes[1]}`;
  }

  return valor;
}

function calcularSucessosPorPercentual(totalRegistros: number, percentual: number) {
  return Math.round(totalRegistros * Math.max(0, Math.min(percentual, 100)) / 100);
}

function buildSazonalidadeOption(dados: IntegracaoEvolucaoDiaria[], isDark: boolean): EChartsOption {
  const tokens = getEchartsThemeTokens(isDark);
  const datas = dados.map((item) => formatarDataEixo(item.data));

  return buildBaseLineOption(isDark, {
    color: [tokens.palette[2], tokens.palette[3]],
    legend: {
      top: 0,
      right: 8,
      bottom: undefined,
    },
    grid: {
      top: 54,
      right: 24,
      bottom: 28,
      left: 48,
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: datas,
    },
    yAxis: {
      type: 'value',
      name: 'Registros',
    },
    series: [
      {
        name: 'Sucessos',
        type: 'line',
        data: dados.map((item) => numeroSeguro(item.sucessos)),
        areaStyle: {},
        itemStyle: { color: tokens.palette[2] },
        lineStyle: { color: tokens.palette[2] },
      },
      {
        name: 'Erros',
        type: 'line',
        data: dados.map((item) => numeroSeguro(item.erros)),
        areaStyle: {},
        itemStyle: { color: tokens.palette[3] },
        lineStyle: { color: tokens.palette[3], type: 'dashed' },
      },
    ],
  });
}

function buildSaudePorDestinoOption(metricas: IntegracaoMetricaConsolidada[], isDark: boolean): EChartsOption {
  const tokens = getEchartsThemeTokens(isDark);
  const dados = metricas
    .map((item) => {
      const totalRegistros = numeroSeguro(item.totalRegistros);
      const sucessos = calcularSucessosPorPercentual(totalRegistros, numeroSeguro(item.percentualXmlSucesso));

      return {
        destino: item.sistemaDestino,
        sucessos,
        erros: Math.max(totalRegistros - sucessos, 0),
      };
    })
    .sort((a, b) => (b.sucessos + b.erros) - (a.sucessos + a.erros));

  return buildBaseBarOption(isDark, {
    color: [tokens.palette[2], tokens.palette[3]],
    legend: {
      top: 0,
      right: 8,
      bottom: undefined,
    },
    grid: {
      top: 54,
      right: 24,
      bottom: 24,
      left: 76,
      containLabel: true,
    },
    xAxis: {
      type: 'value',
      name: 'Registros',
    },
    yAxis: {
      type: 'category',
      inverse: true,
      data: dados.map((item) => item.destino),
    },
    series: [
      {
        name: 'Sucessos',
        type: 'bar',
        stack: 'integracoes',
        data: dados.map((item) => item.sucessos),
        itemStyle: { color: tokens.palette[2] },
      },
      {
        name: 'Erros',
        type: 'bar',
        stack: 'integracoes',
        data: dados.map((item) => item.erros),
        itemStyle: { color: tokens.palette[3] },
      },
    ],
  });
}

function temIndicadorImagem(item: IntegracaoPendencia) {
  return Boolean(
    item.canhotoReferencia?.trim()
      || item.possuiImagem
      || item.possuiImagemCanhoto
      || item.possuiImagemPayload
      || item.imagemDisponivel,
  );
}

function podeVisualizarCanhoto(item: IntegracaoPendencia) {
  return temIndicadorImagem(item) && Boolean(item.canhotoReferencia?.trim());
}

function criarColunas(
  onVerCanhoto: (item: IntegracaoPendencia) => void,
): ColunaTabelaAnalitica<IntegracaoPendencia>[] {
  return [
    { chave: 'sistemaDestino', label: 'Sistema Destino', fixo: true, largura: '160px' },
    { chave: 'numeroNf', label: 'NF', largura: '120px', formato: formatarInteiro, filtroTabela: 'codigo' },
    { chave: 'serieNf', label: 'Série', largura: '100px' },
    {
      chave: 'chaveNfe',
      label: 'Chave NF-e',
      largura: '360px',
      formato: (valor) => (
        <span className="block max-w-[360px] truncate" title={typeof valor === 'string' ? valor : undefined}>
          {typeof valor === 'string' && valor.trim() ? valor : '-'}
        </span>
      ),
    },
    { chave: 'statusDados', label: 'Status XML', largura: '160px', filtroTabela: 'status', formato: renderStatus },
    { chave: 'statusCanhoto', label: 'Status Comprovante', largura: '190px', filtroTabela: 'status', formato: renderStatus },
    { chave: 'dataProcessamento', label: 'Data de Processamento', largura: '210px', formato: formatarData },
    {
      chave: 'id',
      label: 'Canhoto',
      largura: '112px',
      ordenavel: false,
      filtravel: false,
      formato: (_valor, row) => {
        const habilitado = podeVisualizarCanhoto(row);

        return (
          <button
            type="button"
            onClick={() => {
              if (habilitado) {
                onVerCanhoto(row);
              }
            }}
            disabled={!habilitado}
            className="inline-flex h-8 w-8 items-center justify-center rounded-lg border transition-colors disabled:cursor-not-allowed disabled:opacity-45 hover:border-[var(--color-primary)] focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)]"
            style={{
              backgroundColor: 'var(--color-bg)',
              borderColor: 'var(--color-border)',
              color: 'var(--color-text)',
            }}
            aria-label={habilitado ? 'Ver canhoto' : 'Canhoto indisponivel'}
            title={habilitado ? 'Ver canhoto' : 'Canhoto indisponivel'}
          >
            <Eye size={15} aria-hidden="true" />
          </button>
        );
      },
    },
  ];
}

function IntegracoesEscopoTabs({
  abaSelecionada,
  onChange,
}: {
  abaSelecionada: IntegracoesAba;
  onChange: (aba: IntegracoesAba) => void;
}) {
  return (
    <div
      role="tablist"
      aria-label="Escopo da auditoria de integrações"
      className="flex min-w-0 max-w-full items-center gap-1 overflow-x-auto rounded-lg border p-0.5"
      style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)' }}
    >
      {ABAS_INTEGRACOES.map((aba) => {
        const ativa = abaSelecionada === aba.valor;
        return (
          <button
            key={aba.valor}
            type="button"
            role="tab"
            data-state={ativa ? 'active' : 'inactive'}
            aria-selected={ativa}
            onClick={() => onChange(aba.valor)}
            className="inline-flex h-8 shrink-0 items-center justify-center whitespace-nowrap rounded-md px-3 text-xs font-semibold transition-colors hover:bg-[var(--color-card)] data-[state=active]:shadow-sm focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)]"
            style={{
              backgroundColor: ativa ? 'var(--color-card)' : 'transparent',
              color: ativa ? 'var(--color-text)' : 'var(--color-text-muted)',
            }}
          >
            {aba.label}
          </button>
        );
      })}
    </div>
  );
}

export default function IntegracoesPage() {
  const [pendenciaCanhoto, setPendenciaCanhoto] = useState<IntegracaoPendencia | null>(null);
  const [abaSelecionada, setAbaSelecionada] = useState<IntegracoesAba>('PENDENCIAS');
  const [tableSort, setTableSort] = useState<IntegracoesTableSort | null>(null);
  const { dataInicio, dataFim, setDataInicio, setDataFim, setDataRange } = useFiltro();
  const { isDark } = useEchartsTheme();
  const filtrosTabela = useAnalyticalTableFilters();
  const paginacaoTabela = useTabelaPaginadaState(`${filtrosTabela.resetKey}:${abaSelecionada}:${dataInicio}:${dataFim}`);
  const abaAuditoriaSelecionada = abaSelecionada !== 'QUARENTENA';

  const integracoes = useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: [
      ...QUERY_KEY,
      dataInicio,
      dataFim,
      paginacaoTabela.pagina,
      paginacaoTabela.tamanhoPagina,
      filtrosTabela.apiFilters,
      tableSort,
      abaSelecionada,
    ],
    queryFn: () => buscarIntegracoesAuditoria(
      paginacaoTabela.pagina,
      paginacaoTabela.tamanhoPagina,
      dataInicio,
      dataFim,
      filtrosTabela.apiFilters,
      tableSort?.field,
      tableSort?.direction,
      abaSelecionada === 'QUARENTENA' ? 'PENDENCIAS' : abaSelecionada,
    ),
    enabled: abaAuditoriaSelecionada,
    placeholderData: (previousData) => previousData,
    staleTime: 60 * 1000,
    retry: 1,
  });

  const evolucaoDiariaQuery = useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: [...QUERY_KEY, 'evolucao-diaria', dataInicio, dataFim],
    queryFn: () => buscarIntegracoesEvolucaoDiaria(dataInicio, dataFim),
    placeholderData: (previousData) => previousData,
    staleTime: 60 * 1000,
    retry: 1,
  });

  const abrirCanhoto = useCallback((item: IntegracaoPendencia) => {
    if (!podeVisualizarCanhoto(item)) {
      return;
    }

    setPendenciaCanhoto(item);
  }, []);

  const fecharCanhoto = useCallback(() => {
    setPendenciaCanhoto(null);
  }, []);

  const colunas = useMemo(() => criarColunas(abrirCanhoto), [abrirCanhoto]);

  usePageHeader({
    title: 'Integrações',
    description: 'Auditoria operacional de XML e comprovantes enviados para clientes.',
    updatedAt: integracoes.data?.geradoEm ?? null,
  });

  const metricas = integracoes.data?.metricasConsolidadas ?? EMPTY_METRICAS;
  const evolucaoDiaria = evolucaoDiariaQuery.data ?? EMPTY_EVOLUCAO_DIARIA;
  const volumeTotal = calcularVolumeTotal(metricas);
  const taxaSucessoGlobal = calcularTaxaPonderada(metricas, 'percentualXmlSucesso');
  const taxaSucessoCanhotos = calcularTaxaPonderada(metricas, 'percentualCanhotoSucesso');
  const totalPendencias = calcularTotalErros(evolucaoDiaria);
  const pendencias = integracoes.data?.pendencias.itens ?? EMPTY_PENDENCIAS;
  const tituloTabela = abaSelecionada === 'PENDENCIAS'
    ? 'Pendências operacionais'
    : 'Integrados com sucesso';
  const sazonalidadeOption = useMemo(
    () => buildSazonalidadeOption(evolucaoDiaria, isDark),
    [evolucaoDiaria, isDark],
  );
  const saudePorDestinoOption = useMemo(
    () => buildSaudePorDestinoOption(metricas, isDark),
    [isDark, metricas],
  );

  const statusOptions = useMemo(
    () => combinarStatusOptions(
      STATUS_PADRAO,
      pendencias.map((item) => item.statusDados),
      pendencias.map((item) => item.statusCanhoto),
      filtrosTabela.filters.status,
    ),
    [filtrosTabela.filters.status, pendencias],
  );

  const selecionarAba = useCallback((aba: IntegracoesAba) => {
    setAbaSelecionada(aba);
    setPendenciaCanhoto(null);
  }, []);

  return (
    <div className="w-full">
      <FilterBar
        dataInicio={dataInicio}
        dataFim={dataFim}
        actions={<IntegracoesEscopoTabs abaSelecionada={abaSelecionada} onChange={selecionarAba} />}
      >
        <DateRangePicker
          dataInicio={dataInicio}
          dataFim={dataFim}
          onDataInicioChange={setDataInicio}
          onDataFimChange={setDataFim}
          onRangeChange={setDataRange}
        />
      </FilterBar>

      {abaAuditoriaSelecionada && integracoes.isError && (
        <MensagemErro
          mensagem={getApiErrorMessage(integracoes.error, 'Erro ao carregar auditoria de integrações.')}
          tipo={getTipoErro(integracoes.error)}
        />
      )}

      {abaSelecionada === 'QUARENTENA' ? (
        <QuarentenaErrosPanel />
      ) : (
        <>
          <div className="mb-4 grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <TooltipKpi kpiName="integracoes.volumeOperacional">
              <KpiCard
                label="Volume Operacional"
                valor={formatarInteiro(volumeTotal)}
                metaLabel="Origem"
                metaValue="Satélite"
              />
            </TooltipKpi>
            <TooltipKpi kpiName="integracoes.taxaSucessoGlobal">
              <KpiCard
                label="Sucesso Global"
                valor={formatarPercentual(taxaSucessoGlobal)}
                tone={tomPercentual(taxaSucessoGlobal)}
                metaLabel="Base"
                metaValue="XML"
                progressPct={taxaSucessoGlobal}
              />
            </TooltipKpi>
            <TooltipKpi kpiName="integracoes.taxaSucessoCanhotos">
              <KpiCard
                label="Sucesso Canhotos"
                valor={formatarPercentual(taxaSucessoCanhotos)}
                tone={tomPercentual(taxaSucessoCanhotos)}
                metaLabel="Base"
                metaValue="Canhotos"
                progressPct={taxaSucessoCanhotos}
              />
            </TooltipKpi>
            <TooltipKpi kpiName="integracoes.pendenciasErros">
              <KpiCard
                label="Pendências"
                valor={formatarInteiro(totalPendencias)}
                tone={numeroSeguro(totalPendencias) > 0 ? 'warning' : 'positive'}
                metaLabel="Base"
                metaValue="Erros"
              />
            </TooltipKpi>
          </div>

          <div className="mb-4 mt-6 grid grid-cols-1 gap-4 lg:grid-cols-2">
            <div className="h-[24rem] w-full">
              <ChartWrapper
                titulo="Sazonalidade de Integrações"
                option={sazonalidadeOption}
                isLoading={evolucaoDiariaQuery.isLoading}
                isEmpty={evolucaoDiaria.length === 0}
                erro={evolucaoDiariaQuery.isError
                  ? getApiErrorMessage(evolucaoDiariaQuery.error, 'Erro ao carregar evolução diária.')
                  : null}
                altura={300}
                className="h-full w-full"
                chartKey="integracoesSazonalidade"
              />
            </div>

            <div className="h-[24rem] w-full">
              <ChartWrapper
                titulo="Saúde por Sistema Destino"
                option={saudePorDestinoOption}
                isLoading={integracoes.isLoading}
                isEmpty={volumeTotal <= 0}
                erro={integracoes.isError
                  ? getApiErrorMessage(integracoes.error, 'Erro ao carregar saúde por destino.')
                  : null}
                altura={300}
                className="h-full w-full"
                chartKey="integracoesSaudeDestino"
              />
            </div>
          </div>

          <AnalyticalDataTable
            titulo={tituloTabela}
            dados={pendencias}
            colunas={colunas}
            chaveLinha="id"
            filtros={filtrosTabela.filters}
            hiddenActiveCount={filtrosTabela.hiddenActiveCount}
            hasAnyFilter={filtrosTabela.hasAnyFilter}
            onTextFilterChange={filtrosTabela.setTextFilter}
            onMultiFilterChange={filtrosTabela.setMultiFilter}
            onColumnFilterChange={filtrosTabela.setColumnFilter}
            onClearFilters={filtrosTabela.clearTableFilters}
            statusOptions={statusOptions}
            isLoading={integracoes.isLoading}
            isFetching={integracoes.isFetching}
            error={integracoes.error}
            errorFallbackMessage={`Erro ao carregar ${tituloTabela.toLowerCase()}.`}
            totalRegistros={integracoes.data?.pendencias.paginacao.totalElementos}
            paginaAtual={paginacaoTabela.pagina}
            tamanhoPagina={paginacaoTabela.tamanhoPagina}
            onPaginaChange={paginacaoTabela.setPagina}
            onTamanhoPaginaChange={paginacaoTabela.setTamanhoPagina}
            sortField={tableSort?.field}
            sortDirection={tableSort?.direction}
            onSortChange={(field, direction) => setTableSort({ field, direction })}
          />

          <CanhotoImagemModal
            pendencia={pendenciaCanhoto}
            onClose={fecharCanhoto}
          />
        </>
      )}
    </div>
  );
}
