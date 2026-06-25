import { useCallback, useMemo, useState } from 'react';
import { isAxiosError } from 'axios';
import { useQuery } from '@tanstack/react-query';
import { Eye } from 'lucide-react';
import AnalyticalDataTable, {
  type ColunaTabelaAnalitica,
  type SortDirection,
} from '../components/shared/AnalyticalDataTable';
import CanhotoImagemModal from '../components/domain/integracoes/CanhotoImagemModal';
import KpiCard from '../components/shared/KpiCard';
import StatusBadge from '../components/shared/StatusBadge';
import MensagemErro from '../components/ui/MensagemErro';
import {
  buscarIntegracoesAuditoria,
  buscarImagemCanhotoIntegracao,
  type IntegracaoMetricaConsolidada,
  type IntegracaoPendencia,
} from '../api/endpoints/integracoesServico';
import { usePageHeader } from '../contexts/PageHeaderContext';
import { useAnalyticalTableFilters } from '../hooks/useAnalyticalTableFilters';
import { useTabelaPaginadaState } from '../hooks/useTabelaPaginadaState';
import { getApiErrorMessage, getTipoErro } from '../utils/apiError';
import { formatarDataHora, formatarNumero, formatarPorcentagem } from '../utils/formatadores';
import { OPERATIONAL_QUERY_POLLING_OPTIONS } from '../utils/pollingUtils';
import { combinarStatusOptions } from '../utils/tableStatusOptions';

const QUERY_KEY = ['integracoes'];
const QUERY_KEY_IMAGEM_CANHOTO = [...QUERY_KEY, 'imagem-canhoto'];
const STATUS_PADRAO = ['SUCESSO', 'ERRO_DESTINO', 'PENDENTE_FOTO'];
const EMPTY_METRICAS: IntegracaoMetricaConsolidada[] = [];
const EMPTY_PENDENCIAS: IntegracaoPendencia[] = [];

interface IntegracoesTableSort {
  field: keyof IntegracaoPendencia & string;
  direction: SortDirection;
}

function buscarMetrica(metricas: IntegracaoMetricaConsolidada[], sistemaDestino: string) {
  return metricas.find((item) => item.sistemaDestino.toUpperCase() === sistemaDestino);
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

function temIndicadorImagem(item: IntegracaoPendencia) {
  return Boolean(
    item.possuiImagem
      || item.possuiImagemCanhoto
      || item.possuiImagemPayload
      || item.imagemDisponivel,
  );
}

function podeVisualizarCanhoto(item: IntegracaoPendencia) {
  return item.sistemaDestino?.trim().toUpperCase() === 'PPG' || temIndicadorImagem(item);
}

function isNotFoundError(error: unknown) {
  return isAxiosError(error) && error.response?.status === 404;
}

async function buscarImagemCanhotoSegura(id: number) {
  try {
    return await buscarImagemCanhotoIntegracao(id);
  } catch (error) {
    if (isNotFoundError(error)) {
      return null;
    }

    throw error;
  }
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

export default function IntegracoesPage() {
  const [pendenciaCanhoto, setPendenciaCanhoto] = useState<IntegracaoPendencia | null>(null);
  const [tableSort, setTableSort] = useState<IntegracoesTableSort | null>(null);
  const filtrosTabela = useAnalyticalTableFilters();
  const paginacaoTabela = useTabelaPaginadaState(filtrosTabela.resetKey);

  const integracoes = useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: [
      ...QUERY_KEY,
      paginacaoTabela.pagina,
      paginacaoTabela.tamanhoPagina,
      filtrosTabela.apiFilters,
      tableSort,
    ],
    queryFn: () => buscarIntegracoesAuditoria(
      paginacaoTabela.pagina,
      paginacaoTabela.tamanhoPagina,
      filtrosTabela.apiFilters,
      tableSort?.field,
      tableSort?.direction,
    ),
    placeholderData: (previousData) => previousData,
    staleTime: 60 * 1000,
    retry: 1,
  });

  const imagemCanhoto = useQuery({
    queryKey: [...QUERY_KEY_IMAGEM_CANHOTO, pendenciaCanhoto?.id],
    queryFn: () => (pendenciaCanhoto ? buscarImagemCanhotoSegura(pendenciaCanhoto.id) : Promise.resolve(null)),
    enabled: Boolean(pendenciaCanhoto),
    staleTime: 5 * 60 * 1000,
    retry: false,
    refetchOnWindowFocus: false,
    throwOnError: false,
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
  const vedacit = buscarMetrica(metricas, 'VEDACIT');
  const ppg = buscarMetrica(metricas, 'PPG');
  const pendencias = integracoes.data?.pendencias.itens ?? EMPTY_PENDENCIAS;

  const statusOptions = useMemo(
    () => combinarStatusOptions(
      STATUS_PADRAO,
      pendencias.map((item) => item.statusDados),
      pendencias.map((item) => item.statusCanhoto),
      filtrosTabela.filters.status,
    ),
    [filtrosTabela.filters.status, pendencias],
  );

  return (
    <div className="w-full">
      {integracoes.isError && (
        <MensagemErro
          mensagem={getApiErrorMessage(integracoes.error, 'Erro ao carregar auditoria de integrações.')}
          tipo={getTipoErro(integracoes.error)}
        />
      )}

      <div className="mb-4 grid grid-cols-1 gap-3 md:grid-cols-3">
        <KpiCard
          label="VEDACIT XML"
          valor={formatarPercentual(vedacit?.percentualXmlSucesso)}
          tone={tomPercentual(vedacit?.percentualXmlSucesso)}
          metaLabel="Registros"
          metaValue={formatarInteiro(vedacit?.totalRegistros)}
          progressPct={vedacit?.percentualXmlSucesso ?? null}
        />
        <KpiCard
          label="VEDACIT Comprovante"
          valor={formatarPercentual(vedacit?.percentualCanhotoSucesso)}
          tone={tomPercentual(vedacit?.percentualCanhotoSucesso)}
          metaLabel="Registros"
          metaValue={formatarInteiro(vedacit?.totalRegistros)}
          progressPct={vedacit?.percentualCanhotoSucesso ?? null}
        />
        <KpiCard
          label="PPG Comprovante"
          valor={formatarPercentual(ppg?.percentualCanhotoSucesso)}
          tone={tomPercentual(ppg?.percentualCanhotoSucesso)}
          metaLabel="Registros"
          metaValue={formatarInteiro(ppg?.totalRegistros)}
          progressPct={ppg?.percentualCanhotoSucesso ?? null}
        />
      </div>

      <AnalyticalDataTable
        titulo="Pendências operacionais"
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
        errorFallbackMessage="Erro ao carregar pendências operacionais."
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
        imagemSrc={imagemCanhoto.data ?? null}
        isLoading={imagemCanhoto.isLoading}
        error={imagemCanhoto.error}
        onClose={fecharCanhoto}
      />
    </div>
  );
}
