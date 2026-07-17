import { useState } from 'react';
import type { FormEvent } from 'react';
import { CheckCircle2, ExternalLink, Loader2, Send, X } from 'lucide-react';
import { useCriarCotacaoEsl } from '../../../hooks/queries/useEsl';
import type { EslCotacaoResposta, EslCriarCotacaoRequest } from '../../../types/esl';
import { adicionarDias, dataHojeLocal } from '../../../utils/dateUtils';
import { mapearErrosValidacaoEsl, type EslFormErrors } from '../../../utils/eslFormErrors';
import { formatarData, formatarMoeda } from '../../../utils/formatadores';
import {
  EslErrorPanel,
  EslInput,
  EslSelect,
  EslTextarea,
  ESL_PRIMARY_BUTTON_STYLE,
  ESL_SECONDARY_BUTTON_STYLE,
} from './EslFormControls';

interface EslCotacaoOperacoesPanelProps {
  filialSelecionada: string | null;
  open: boolean;
  onClose: () => void;
}

interface CotacaoFormState {
  documentoCliente: string;
  validade: string;
  referencia: string;
  observacoes: string;
  modal: 'rodo' | 'air';
  tipoCalculo: 'price_table' | 'manual';
  tabelaPreco: string;
  documentoPagador: string;
  documentoRemetente: string;
  documentoDestinatario: string;
  cidadeOrigem: string;
  ufOrigem: string;
  cepOrigem: string;
  cidadeDestino: string;
  ufDestino: string;
  cepDestino: string;
  classificacaoProduto: string;
  valorNotasFiscais: string;
  quantidadeVolumes: string;
  pesoReal: string;
  volumeCubico: string;
}

const CAMPO_ALIASES: Record<string, string[]> = {
  documentoCliente: ['documentoCliente', 'customer', 'cliente', 'cnpj do cliente'],
  documentoPagador: ['documentoPagador', 'payer', 'pagador'],
  documentoRemetente: ['documentoRemetente', 'sender', 'remetente'],
  documentoDestinatario: ['documentoDestinatario', 'recipient', 'destinatario', 'destinatário'],
  cidadeOrigem: ['cidadeOrigem', 'originCity', 'cidade de origem'],
  ufOrigem: ['ufOrigem', 'stateCode', 'uf de origem'],
  cepOrigem: ['cepOrigem', 'originPostalCode', 'cep de origem'],
  cidadeDestino: ['cidadeDestino', 'destinationCity', 'cidade de destino'],
  ufDestino: ['ufDestino', 'uf de destino'],
  cepDestino: ['cepDestino', 'destinationPostalCode', 'cep de destino'],
  classificacaoProduto: ['classificacaoProduto', 'productClassification', 'classificacao', 'classificação'],
  tabelaPreco: ['tabelaPreco', 'customerPriceTable', 'tabela de preco', 'tabela de preço'],
  valorNotasFiscais: ['valorNotasFiscais', 'invoicesValue', 'valor das notas'],
  quantidadeVolumes: ['quantidadeVolumes', 'invoicesVolumes', 'volumes'],
  pesoReal: ['pesoReal', 'realWeight', 'peso real'],
  volumeCubico: ['volumeCubico', 'cubicVolume', 'volume cubico', 'volume cúbico'],
};

function criarEstadoInicial(): CotacaoFormState {
  return {
    documentoCliente: '',
    validade: adicionarDias(dataHojeLocal(), 1),
    referencia: '',
    observacoes: '',
    modal: 'rodo',
    tipoCalculo: 'price_table',
    tabelaPreco: '',
    documentoPagador: '',
    documentoRemetente: '',
    documentoDestinatario: '',
    cidadeOrigem: '',
    ufOrigem: '',
    cepOrigem: '',
    cidadeDestino: '',
    ufDestino: '',
    cepDestino: '',
    classificacaoProduto: '',
    valorNotasFiscais: '',
    quantidadeVolumes: '',
    pesoReal: '',
    volumeCubico: '',
  };
}

function numero(valor: string): number {
  return Number(valor.replace(',', '.'));
}

export default function EslCotacaoOperacoesPanel({ filialSelecionada, open, onClose }: EslCotacaoOperacoesPanelProps) {
  if (!open) {
    return null;
  }

  return <EslCotacaoOperacoesPanelAberto filialSelecionada={filialSelecionada} onClose={onClose} />;
}

function EslCotacaoOperacoesPanelAberto({ filialSelecionada, onClose }: Pick<EslCotacaoOperacoesPanelProps, 'filialSelecionada' | 'onClose'>) {
  const criarCotacao = useCriarCotacaoEsl();
  const [form, setForm] = useState<CotacaoFormState>(criarEstadoInicial);
  const [erros, setErros] = useState<EslFormErrors>({ geral: null, campos: {} });
  const [recibo, setRecibo] = useState<EslCotacaoResposta | null>(null);

  function atualizarCampo<K extends keyof CotacaoFormState>(campo: K, valor: CotacaoFormState[K]) {
    setForm((atual) => ({ ...atual, [campo]: valor }));
    setErros((atual) => {
      if (!atual.campos[campo]) {
        return atual;
      }
      const campos = { ...atual.campos };
      delete campos[campo];
      return { ...atual, campos };
    });
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const solicitacao: EslCriarCotacaoRequest = {
      documentoCliente: form.documentoCliente.trim(),
      validade: form.validade,
      referencia: form.referencia.trim() || undefined,
      observacoes: form.observacoes.trim() || undefined,
      trechos: [{
        modal: form.modal,
        tipoCalculo: form.tipoCalculo,
        tabelaPreco: form.tipoCalculo === 'price_table' ? form.tabelaPreco.trim() || undefined : undefined,
        documentoPagador: form.documentoPagador.trim(),
        documentoRemetente: form.documentoRemetente.trim(),
        documentoDestinatario: form.documentoDestinatario.trim(),
        cidadeOrigem: { nome: form.cidadeOrigem.trim(), uf: form.ufOrigem.trim().toUpperCase() },
        cepOrigem: form.cepOrigem.trim() || undefined,
        cidadeDestino: { nome: form.cidadeDestino.trim(), uf: form.ufDestino.trim().toUpperCase() },
        cepDestino: form.cepDestino.trim() || undefined,
        classificacaoProduto: form.classificacaoProduto.trim(),
        valorNotasFiscais: numero(form.valorNotasFiscais),
        quantidadeVolumes: numero(form.quantidadeVolumes),
        pesoReal: numero(form.pesoReal),
        volumeCubico: numero(form.volumeCubico),
      }],
    };

    try {
      setErros({ geral: null, campos: {} });
      if (!filialSelecionada) {
        setErros({ geral: 'Selecione exatamente uma filial nos filtros antes de criar a cotação ESL.', campos: {} });
        return;
      }
      setRecibo(await criarCotacao.mutateAsync({ filial: filialSelecionada, solicitacao }));
    } catch (error) {
      setErros(mapearErrosValidacaoEsl(error, CAMPO_ALIASES, 'Não foi possível criar a cotação no ESL.'));
    }
  }

  if (recibo) {
    return (
      <section
        className="mb-6 overflow-hidden rounded-[24px] border shadow-sm"
        style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)' }}
        aria-label="Recibo da cotação ESL"
      >
        <EslInlinePanelHeader
          title="Recibo da cotação ESL"
          subtitle="A cotação foi confirmada pelo ESL e não foi persistida no banco interno."
          onClose={onClose}
        />
        <div className="p-4 sm:p-5">
          <section className="space-y-5 rounded-3xl border p-5" style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)' }}>
          <div className="flex items-start gap-3">
            <CheckCircle2 className="mt-0.5 shrink-0" size={24} style={{ color: '#15803d' }} />
            <div>
              <h3 className="text-lg font-bold" style={{ color: 'var(--color-text)' }}>Cotação criada com sucesso</h3>
              <p className="mt-1 text-sm" style={{ color: 'var(--color-text-subtle)' }}>Guarde o número para referência comercial.</p>
            </div>
          </div>
          <div className="grid gap-3 sm:grid-cols-3">
            <ReciboItem label="Número da Cotação" value={recibo.numeroCotacao?.toLocaleString('pt-BR') ?? 'Não informado'} />
            <ReciboItem label="Validade" value={recibo.validade ? formatarData(recibo.validade) : 'Não informada'} />
            <ReciboItem label="Valor Total" value={formatarMoeda(recibo.valorFreteTotal)} emphasis />
          </div>
          <div className="flex flex-wrap justify-end gap-3">
            {recibo.urlImpressao ? (
              <a
                href={recibo.urlImpressao}
                target="_blank"
                rel="noreferrer"
                className="inline-flex items-center gap-2 rounded-xl border px-4 py-2.5 text-sm font-semibold"
                style={ESL_SECONDARY_BUTTON_STYLE}
              >
                <ExternalLink size={16} />
                Abrir impressão
              </a>
            ) : null}
            <button type="button" onClick={onClose} className="rounded-xl px-4 py-2.5 text-sm font-semibold text-white" style={ESL_PRIMARY_BUTTON_STYLE}>
              Concluir
            </button>
          </div>
          </section>
        </div>
      </section>
    );
  }

  return (
    <section
      className="mb-6 overflow-hidden rounded-[24px] border shadow-sm"
      style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)' }}
      aria-label="Nova cotação ESL"
    >
      <EslInlinePanelHeader
        title="Nova Cotação ESL"
        subtitle={filialSelecionada
          ? `Filial selecionada: ${filialSelecionada}. O cálculo e o recibo são retornados de forma síncrona pelo BFF.`
          : 'Selecione exatamente uma filial nos filtros antes de informar os dados comerciais.'}
        onClose={onClose}
        isBusy={criarCotacao.isPending}
      />
      <form className="space-y-6 p-4 sm:p-5" onSubmit={(event) => void handleSubmit(event)}>
        <EslErrorPanel message={erros.geral} />
        {!filialSelecionada ? (
          <div className="rounded-2xl border px-4 py-3 text-sm" style={{ borderColor: 'var(--color-border)', color: 'var(--color-text-subtle)' }}>
            As operações ESL acompanham a filial selecionada no filtro global.
          </div>
        ) : null}

        <section className="space-y-4 rounded-3xl border p-4 sm:p-5" style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)' }}>
          <div>
            <h3 className="font-bold" style={{ color: 'var(--color-text)' }}>Cliente e validade</h3>
            <p className="mt-1 text-sm" style={{ color: 'var(--color-text-subtle)' }}>Dados gerais da solicitação de cotação.</p>
          </div>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <EslInput label="CNPJ/CPF do cliente" value={form.documentoCliente} onChange={(event) => atualizarCampo('documentoCliente', event.target.value)} error={erros.campos.documentoCliente} required />
            <EslInput label="Validade" type="date" value={form.validade} onChange={(event) => atualizarCampo('validade', event.target.value)} required />
            <EslInput label="Referência" value={form.referencia} onChange={(event) => atualizarCampo('referencia', event.target.value)} maxLength={120} className="xl:col-span-2" />
          </div>
          <EslTextarea label="Observações" value={form.observacoes} onChange={(event) => atualizarCampo('observacoes', event.target.value)} maxLength={2000} />
        </section>

        <section className="space-y-4 rounded-3xl border p-4 sm:p-5" style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)' }}>
          <div>
            <h3 className="font-bold" style={{ color: 'var(--color-text)' }}>Trecho e mercadoria</h3>
            <p className="mt-1 text-sm" style={{ color: 'var(--color-text-subtle)' }}>O primeiro fluxo suporta uma cotação com um trecho.</p>
          </div>

          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <EslSelect label="Modal" value={form.modal} onChange={(event) => atualizarCampo('modal', event.target.value as CotacaoFormState['modal'])} required>
              <option value="rodo">Rodoviário</option>
              <option value="air">Aéreo</option>
            </EslSelect>
            <EslSelect label="Tipo de cálculo" value={form.tipoCalculo} onChange={(event) => atualizarCampo('tipoCalculo', event.target.value as CotacaoFormState['tipoCalculo'])} required>
              <option value="price_table">Tabela de preço</option>
              <option value="manual">Manual</option>
            </EslSelect>
            {form.tipoCalculo === 'price_table' ? <EslInput label="Tabela de preço" value={form.tabelaPreco} onChange={(event) => atualizarCampo('tabelaPreco', event.target.value)} error={erros.campos.tabelaPreco} maxLength={160} className="xl:col-span-2" /> : null}
          </div>

          <div className="grid gap-4 md:grid-cols-3">
            <EslInput label="CNPJ/CPF do pagador" value={form.documentoPagador} onChange={(event) => atualizarCampo('documentoPagador', event.target.value)} error={erros.campos.documentoPagador} required />
            <EslInput label="CNPJ/CPF do remetente" value={form.documentoRemetente} onChange={(event) => atualizarCampo('documentoRemetente', event.target.value)} error={erros.campos.documentoRemetente} required />
            <EslInput label="CNPJ/CPF do destinatário" value={form.documentoDestinatario} onChange={(event) => atualizarCampo('documentoDestinatario', event.target.value)} error={erros.campos.documentoDestinatario} required />
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <div className="grid gap-4 rounded-2xl border p-4 sm:grid-cols-[1fr_84px_130px]" style={{ borderColor: 'var(--color-border)' }}>
              <EslInput label="Cidade de origem" value={form.cidadeOrigem} onChange={(event) => atualizarCampo('cidadeOrigem', event.target.value)} error={erros.campos.cidadeOrigem} required />
              <EslInput label="UF" value={form.ufOrigem} onChange={(event) => atualizarCampo('ufOrigem', event.target.value.toUpperCase())} error={erros.campos.ufOrigem} maxLength={2} required />
              <EslInput label="CEP origem" value={form.cepOrigem} onChange={(event) => atualizarCampo('cepOrigem', event.target.value)} error={erros.campos.cepOrigem} maxLength={12} />
            </div>
            <div className="grid gap-4 rounded-2xl border p-4 sm:grid-cols-[1fr_84px_130px]" style={{ borderColor: 'var(--color-border)' }}>
              <EslInput label="Cidade de destino" value={form.cidadeDestino} onChange={(event) => atualizarCampo('cidadeDestino', event.target.value)} error={erros.campos.cidadeDestino} required />
              <EslInput label="UF" value={form.ufDestino} onChange={(event) => atualizarCampo('ufDestino', event.target.value.toUpperCase())} error={erros.campos.ufDestino} maxLength={2} required />
              <EslInput label="CEP destino" value={form.cepDestino} onChange={(event) => atualizarCampo('cepDestino', event.target.value)} error={erros.campos.cepDestino} maxLength={12} />
            </div>
          </div>

          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
            <EslInput label="Classificação da mercadoria" value={form.classificacaoProduto} onChange={(event) => atualizarCampo('classificacaoProduto', event.target.value)} error={erros.campos.classificacaoProduto} required className="xl:col-span-1" />
            <EslInput label="Valor das NFs" inputMode="decimal" type="number" min="0" step="0.01" value={form.valorNotasFiscais} onChange={(event) => atualizarCampo('valorNotasFiscais', event.target.value)} error={erros.campos.valorNotasFiscais} required />
            <EslInput label="Volumes" type="number" min="1" step="1" value={form.quantidadeVolumes} onChange={(event) => atualizarCampo('quantidadeVolumes', event.target.value)} error={erros.campos.quantidadeVolumes} required />
            <EslInput label="Peso real (kg)" inputMode="decimal" type="number" min="0" step="0.001" value={form.pesoReal} onChange={(event) => atualizarCampo('pesoReal', event.target.value)} error={erros.campos.pesoReal} required />
            <EslInput label="Volume cúbico (m³)" inputMode="decimal" type="number" min="0" step="0.001" value={form.volumeCubico} onChange={(event) => atualizarCampo('volumeCubico', event.target.value)} error={erros.campos.volumeCubico} required />
          </div>
        </section>

        <div className="flex flex-wrap justify-end gap-3 pb-1">
          <button type="button" onClick={onClose} disabled={criarCotacao.isPending} className="rounded-xl border px-4 py-2.5 text-sm font-semibold disabled:opacity-50" style={ESL_SECONDARY_BUTTON_STYLE}>Cancelar</button>
          <button type="submit" disabled={!filialSelecionada || criarCotacao.isPending} className="inline-flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-60" style={ESL_PRIMARY_BUTTON_STYLE}>
            {criarCotacao.isPending ? <Loader2 className="animate-spin" size={16} /> : <Send size={16} />}
            {criarCotacao.isPending ? 'Calculando no ESL...' : 'Criar cotação ESL'}
          </button>
        </div>
      </form>
    </section>
  );
}

function EslInlinePanelHeader({
  title,
  subtitle,
  onClose,
  isBusy = false,
}: {
  title: string;
  subtitle: string;
  onClose: () => void;
  isBusy?: boolean;
}) {
  return (
    <header className="flex flex-wrap items-start justify-between gap-3 border-b px-4 py-4 sm:px-5" style={{ borderColor: 'var(--color-border)' }}>
      <div>
        <h2 className="text-base font-bold" style={{ color: 'var(--color-text)' }}>{title}</h2>
        <p className="mt-1 max-w-3xl text-sm" style={{ color: 'var(--color-text-subtle)' }}>{subtitle}</p>
      </div>
      <button
        type="button"
        onClick={onClose}
        disabled={isBusy}
        className="inline-flex h-9 items-center gap-1.5 rounded-lg border px-3 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-50"
        style={ESL_SECONDARY_BUTTON_STYLE}
      >
        <X size={15} aria-hidden="true" />
        Fechar
      </button>
    </header>
  );
}

function ReciboItem({ label, value, emphasis = false }: { label: string; value: string; emphasis?: boolean }) {
  return (
    <div className="rounded-2xl border px-4 py-3" style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)' }}>
      <p className="text-xs font-semibold uppercase tracking-wide" style={{ color: 'var(--color-text-subtle)' }}>{label}</p>
      <p className="mt-1 text-lg font-bold" style={{ color: emphasis ? '#15803d' : 'var(--color-text)' }}>{value}</p>
    </div>
  );
}
