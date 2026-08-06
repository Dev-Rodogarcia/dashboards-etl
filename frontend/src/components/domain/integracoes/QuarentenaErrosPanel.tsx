import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { AlertCircle, Clock } from 'lucide-react';
import DataTable, { type ColunaTabela } from '../../shared/DataTable';
import ExportButton from '../../shared/ExportButton';
import MensagemErro from '../../ui/MensagemErro';
import {
  buscarErrosQuarentena,
  exportarErrosQuarentenaCsv,
  type QuarentenaErroManual,
} from '../../../api/endpoints/quarentenaServico';
import { getApiErrorMessage, getTipoErro } from '../../../utils/apiError';
import { formatarDataHora, formatarNumero } from '../../../utils/formatadores';
import { OPERATIONAL_QUERY_POLLING_OPTIONS } from '../../../utils/pollingUtils';

const QUERY_KEY = ['quarentena-erros'];
const EMPTY_ERROS: QuarentenaErroManual[] = [];

function textoSeguro(valor: unknown, fallback = '-') {
  return typeof valor === 'string' && valor.trim() ? valor : fallback;
}

function formatarNumeroNf(valor: unknown) {
  if (typeof valor === 'number' && Number.isFinite(valor)) {
    return formatarNumero(valor);
  }

  return textoSeguro(valor);
}

function formatarTentativas(valor: unknown) {
  const numero = Number(valor);
  return Number.isFinite(numero) ? formatarNumero(numero) : '-';
}

function formatarDataUltimaTentativa(valor: unknown) {
  return typeof valor === 'string' && valor.trim() ? formatarDataHora(valor) : '-';
}

function renderDestino(valor: unknown) {
  const destino = textoSeguro(valor);
  const paletaPorDestino: Record<string, { backgroundColor: string; borderColor: string; color: string }> = {
    VEDACIT: {
      backgroundColor: 'rgba(249, 115, 22, 0.12)',
      borderColor: 'rgba(249, 115, 22, 0.30)',
      color: '#c2410c',
    },
    PPG: {
      backgroundColor: 'rgba(33, 71, 138, 0.10)',
      borderColor: 'rgba(33, 71, 138, 0.24)',
      color: 'var(--color-primary)',
    },
    SELIA: {
      backgroundColor: 'rgba(13, 148, 136, 0.12)',
      borderColor: 'rgba(13, 148, 136, 0.30)',
      color: '#0f766e',
    },
  };
  const estilo = paletaPorDestino[destino.toUpperCase()] ?? paletaPorDestino.PPG;

  return (
    <span
      className="inline-flex rounded-full border px-2.5 py-1 text-xs font-bold"
      style={estilo}
    >
      {destino}
    </span>
  );
}

function renderChaveNfe(valor: unknown) {
  const chave = textoSeguro(valor);

  return (
    <span
      className="block max-w-[360px] truncate font-mono text-xs"
      title={chave !== '-' ? chave : undefined}
      style={{ color: 'var(--color-text)' }}
    >
      {chave}
    </span>
  );
}

function ErroLimpoCell({ erro }: { erro: string }) {
  const [expanded, setExpanded] = useState(false);
  const canExpand = erro.length > 88;

  return (
    <div
      className="max-w-[640px] rounded-lg border px-3 py-2"
      title={erro}
      style={{
        backgroundColor: 'rgba(249, 115, 22, 0.12)',
        borderColor: 'rgba(249, 115, 22, 0.34)',
        color: '#c2410c',
      }}
    >
      <div className={expanded ? 'block' : 'flex min-w-0 items-center gap-2'}>
        <span
          className={`block min-w-0 cursor-help text-sm font-semibold leading-relaxed ${expanded ? 'max-h-32 overflow-y-auto pr-1' : 'flex-1'}`}
          style={expanded
            ? { whiteSpace: 'normal' }
            : {
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
              }}
        >
          {erro}
        </span>
        {canExpand && (
          <button
            type="button"
            onClick={() => setExpanded((atual) => !atual)}
            className={`${expanded ? 'mt-1' : 'shrink-0'} text-[11px] font-bold underline-offset-2 transition-opacity hover:underline hover:opacity-80 focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)]`}
            style={{ color: '#9a3412' }}
            aria-expanded={expanded}
          >
            {expanded ? 'Recolher' : 'Ler mais'}
          </button>
        )}
      </div>
    </div>
  );
}

function renderErroLimpo(valor: unknown) {
  return <ErroLimpoCell erro={textoSeguro(valor, 'Motivo indisponivel')} />;
}

function criarColunas(): ColunaTabela<QuarentenaErroManual>[] {
  return [
    { chave: 'destino', label: 'Destino', largura: '130px', formato: renderDestino, ordenavel: false },
    { chave: 'numeroNf', label: 'NF', largura: '110px', formato: formatarNumeroNf, ordenavel: false },
    {
      chave: 'erroLimpo',
      label: 'Ação necessária',
      largura: '680px',
      formato: renderErroLimpo,
      ordenavel: false,
      tooltip: 'Motivo sanitizado retornado pelo robô após a repescagem lenta.',
    },
    { chave: 'chaveNfe', label: 'Chave NF-e', largura: '360px', formato: renderChaveNfe, ordenavel: false },
    { chave: 'tentativas', label: 'Tentativas', largura: '120px', alinhamento: 'center', formato: formatarTentativas, ordenavel: false },
    {
      chave: 'dataUltimaTentativa',
      label: 'Última tentativa',
      largura: '190px',
      formato: formatarDataUltimaTentativa,
      ordenavel: false,
    },
  ];
}

function QuarentenaEmptyState() {
  return (
    <section
      className="flex min-h-[18rem] flex-col items-center justify-center rounded-[20px] border px-6 py-12 text-center shadow-sm"
      style={{
        backgroundColor: 'var(--color-card)',
        borderColor: 'rgba(22, 163, 74, 0.30)',
      }}
    >
      <div
        className="mb-4 flex h-12 w-12 items-center justify-center rounded-full border text-2xl"
        style={{
          backgroundColor: 'rgba(22, 163, 74, 0.12)',
          borderColor: 'rgba(22, 163, 74, 0.26)',
        }}
        aria-hidden="true"
      >
        🎉
      </div>
      <h2 className="text-lg font-bold" style={{ color: 'var(--color-text)' }}>
        Tudo limpo! Nenhuma ação manual pendente.
      </h2>
      <p className="mt-2 max-w-xl text-sm leading-relaxed" style={{ color: 'var(--color-text-muted)' }}>
        O endpoint de quarentena não retornou notas presas. Quando o robô conseguir integrar uma nota corrigida,
        ela também desaparece automaticamente daqui.
      </p>
    </section>
  );
}

interface QuarentenaErrosPanelProps {
  destinosSelecionados: string[];
}

export default function QuarentenaErrosPanel({ destinosSelecionados }: QuarentenaErrosPanelProps) {
  const [pagina, setPagina] = useState(1);
  const [tamanhoPagina, setTamanhoPagina] = useState(100);
  const colunas = useMemo(() => criarColunas(), []);

  const quarentena = useQuery({
    ...OPERATIONAL_QUERY_POLLING_OPTIONS,
    queryKey: [...QUERY_KEY, pagina, tamanhoPagina, destinosSelecionados],
    queryFn: () => buscarErrosQuarentena(pagina, tamanhoPagina, destinosSelecionados),
    placeholderData: (previousData) => previousData,
    staleTime: 60 * 1000,
    retry: 1,
  });

  const erros = quarentena.data?.content ?? EMPTY_ERROS;
  const totalErros = quarentena.data?.totalElements ?? 0;
  const possuiErros = totalErros > 0;
  const escopoDestinos = useMemo(() => {
    const destinos = Array.from(new Set(erros
      .map((erro) => textoSeguro(erro.destino, ''))
      .filter(Boolean)))
      .sort((a, b) => a.localeCompare(b));
    if (destinos.length > 0) return destinos.join(' / ');
    return destinosSelecionados.length > 0 ? destinosSelecionados.join(' / ') : 'PPG / Vedacit / SELIA';
  }, [destinosSelecionados, erros]);

  function alterarTamanhoPagina(proximoTamanho: number) {
    setTamanhoPagina(proximoTamanho);
  }

  return (
    <div className="w-full space-y-4">
      <section
        className="overflow-hidden rounded-[20px] border shadow-sm"
        style={{
          backgroundColor: 'var(--color-card)',
          borderColor: possuiErros ? 'rgba(249, 115, 22, 0.45)' : 'var(--color-border)',
        }}
      >
        <div
          className="border-b px-5 py-4"
          style={{
            backgroundColor: possuiErros ? 'rgba(249, 115, 22, 0.10)' : 'rgba(22, 163, 74, 0.08)',
            borderColor: possuiErros ? 'rgba(249, 115, 22, 0.25)' : 'rgba(22, 163, 74, 0.20)',
          }}
        >
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div className="flex min-w-0 items-start gap-3">
              <span
                className="mt-0.5 flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border"
                style={{
                  backgroundColor: possuiErros ? 'rgba(249, 115, 22, 0.16)' : 'rgba(22, 163, 74, 0.12)',
                  borderColor: possuiErros ? 'rgba(249, 115, 22, 0.30)' : 'rgba(22, 163, 74, 0.26)',
                  color: possuiErros ? '#c2410c' : 'var(--color-positive-text)',
                }}
              >
                <AlertCircle size={20} aria-hidden="true" />
              </span>
              <div className="min-w-0">
                <p className="text-xs font-bold uppercase tracking-[0.16em]" style={{ color: possuiErros ? '#c2410c' : 'var(--color-positive-text)' }}>
                  {possuiErros ? 'Ação manual requerida' : 'Tudo limpo no momento'}
                </p>
                <h2 className="mt-1 text-xl font-bold" style={{ color: 'var(--color-text)' }}>
                  {possuiErros ? `${formatarNumero(totalErros)} nota(s) em quarentena` : 'Nenhuma nota em quarentena'}
                </h2>
                <p className="mt-1 max-w-3xl text-sm leading-relaxed" style={{ color: 'var(--color-text-muted)' }}>
                  Esses itens já passaram pelas tentativas automáticas e pela repescagem lenta. Corrija o dado de origem
                  ou bloqueio de negócio indicado em “Ação necessária” e aguarde o próximo ciclo do ETL.
                </p>
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <span
                className="inline-flex h-9 items-center gap-2 rounded-lg border px-3 text-xs font-semibold"
                style={{
                  backgroundColor: 'var(--color-card)',
                  borderColor: 'var(--color-border)',
                  color: 'var(--color-text-muted)',
                }}
              >
                <Clock size={14} aria-hidden="true" />
                Atualiza automaticamente
              </span>
              <button
                type="button"
                onClick={() => void quarentena.refetch()}
                disabled={quarentena.isFetching}
                className="inline-flex h-9 items-center rounded-lg border px-3 text-xs font-semibold transition-colors disabled:cursor-not-allowed disabled:opacity-60 hover:border-[var(--color-primary)] focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)]"
                style={{
                  backgroundColor: 'var(--color-card)',
                  borderColor: 'var(--color-border)',
                  color: 'var(--color-text)',
                }}
              >
                {quarentena.isFetching ? 'Atualizando...' : 'Atualizar agora'}
              </button>
            </div>
          </div>
        </div>

        <div className="grid gap-3 px-5 py-4 sm:grid-cols-3">
          <div>
            <p className="text-xs font-medium" style={{ color: 'var(--color-text-muted)' }}>Estado</p>
            <p className="mt-1 text-sm font-bold" style={{ color: possuiErros ? '#c2410c' : 'var(--color-positive-text)' }}>
              {possuiErros ? 'Intervenção humana' : 'Fila limpa'}
            </p>
          </div>
          <div>
            <p className="text-xs font-medium" style={{ color: 'var(--color-text-muted)' }}>Escopo</p>
            <p className="mt-1 text-sm font-bold" style={{ color: 'var(--color-text)' }}>{escopoDestinos}</p>
          </div>
          <div>
            <p className="text-xs font-medium" style={{ color: 'var(--color-text-muted)' }}>Remoção da lista</p>
            <p className="mt-1 text-sm font-bold" style={{ color: 'var(--color-text)' }}>Automática após sucesso</p>
          </div>
        </div>
      </section>

      {quarentena.isError && (
        <MensagemErro
          mensagem={getApiErrorMessage(quarentena.error, 'Erro ao carregar a quarentena de integrações.')}
          tipo={getTipoErro(quarentena.error)}
        />
      )}

      {!quarentena.isLoading && !quarentena.isError && !possuiErros ? (
        <QuarentenaEmptyState />
      ) : (
        <DataTable
          titulo="Notas em quarentena"
          dados={erros}
          colunas={colunas}
          chaveLinha="id"
          isLoading={quarentena.isLoading}
          error={quarentena.error}
          errorFallbackMessage="Erro ao carregar notas em quarentena."
          totalRegistros={totalErros}
          paginaAtual={pagina}
          tamanhoPagina={tamanhoPagina}
          onPaginaChange={setPagina}
          onTamanhoPaginaChange={alterarTamanhoPagina}
          acoesCabecalho={(
            <ExportButton
              nomeArquivo="quarentena-integracoes"
              onExport={() => exportarErrosQuarentenaCsv(destinosSelecionados)}
            />
          )}
        />
      )}
    </div>
  );
}
