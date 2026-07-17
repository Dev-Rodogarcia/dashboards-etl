import { useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import { CheckCircle2, FileCheck2, Loader2, Send, ShieldCheck } from 'lucide-react';
import { useCriarColetaEsl, useValidarNfEsl } from '../../../hooks/queries/useEsl';
import { useNfValidationState, type NfValidationDependencies } from '../../../hooks/useNfValidationState';
import type { EslColetaResposta, EslCriarColetaRequest } from '../../../types/esl';
import { dataHojeLocal } from '../../../utils/dateUtils';
import { getApiErrorMessage } from '../../../utils/apiError';
import { mapearErrosValidacaoEsl, type EslFormErrors } from '../../../utils/eslFormErrors';
import { formatarData, formatarPeso } from '../../../utils/formatadores';
import EslModalFrame from './EslModalFrame';
import {
  EslErrorPanel,
  EslInput,
  EslSelect,
  EslTextarea,
  ESL_PRIMARY_BUTTON_STYLE,
  ESL_SECONDARY_BUTTON_STYLE,
} from './EslFormControls';

interface EslColetaModalProps {
  filial: string;
  open: boolean;
  onClose: () => void;
}

interface ColetaFormState {
  documentoCliente: string;
  referencia: string;
  documentoLocalColeta: string;
  dataAgendada: string;
  horaInicial: string;
  horaFinal: string;
  emailNotificacao: string;
  telefoneNotificacao: string;
  observacoes: string;
  modal: 'rodo' | 'air';
  chaveOrNumeroNf: string;
  valorNotasFiscais: string;
  quantidadeVolumes: string;
  pesoRealNotasFiscais: string;
  documentoRemetente: string;
  documentoDestinatario: string;
  documentoPagador: string;
  altura: string;
  comprimento: string;
  largura: string;
  pesoCubado: string;
  previsaoEntrega: string;
}

const CAMPO_ALIASES: Record<string, string[]> = {
  documentoCliente: ['documentoCliente', 'customer', 'cliente', 'cnpj do cliente'],
  documentoLocalColeta: ['documentoLocalColeta', 'pickupLocation', 'local de coleta', 'filial'],
  dataAgendada: ['dataAgendada', 'serviceDate', 'data agendada'],
  horaInicial: ['horaInicial', 'serviceStartHour', 'hora inicial'],
  horaFinal: ['horaFinal', 'serviceEndHour', 'hora final'],
  emailNotificacao: ['emailNotificacao', 'notificationEmail', 'e-mail'],
  telefoneNotificacao: ['telefoneNotificacao', 'notificationPhone', 'telefone'],
  chaveOrNumeroNf: ['chaveOrNumeroNf', 'invoiceId', 'nota fiscal', 'nota'],
  valorNotasFiscais: ['valorNotasFiscais', 'invoicesValue', 'valor das notas'],
  quantidadeVolumes: ['quantidadeVolumes', 'invoicesVolumes', 'volumes'],
  pesoRealNotasFiscais: ['pesoRealNotasFiscais', 'invoicesRealWeight', 'peso real'],
  documentoRemetente: ['documentoRemetente', 'sender', 'remetente'],
  documentoDestinatario: ['documentoDestinatario', 'recipient', 'destinatario', 'destinatário'],
  documentoPagador: ['documentoPagador', 'payer', 'pagador'],
  altura: ['altura', 'invoicesHeight'],
  comprimento: ['comprimento', 'invoicesLength'],
  largura: ['largura', 'invoicesWidth'],
  pesoCubado: ['pesoCubado', 'invoicesCubedWeight'],
};

function criarEstadoInicial(): ColetaFormState {
  return {
    documentoCliente: '',
    referencia: '',
    documentoLocalColeta: '',
    dataAgendada: dataHojeLocal(),
    horaInicial: '',
    horaFinal: '',
    emailNotificacao: '',
    telefoneNotificacao: '',
    observacoes: '',
    modal: 'rodo',
    chaveOrNumeroNf: '',
    valorNotasFiscais: '',
    quantidadeVolumes: '',
    pesoRealNotasFiscais: '',
    documentoRemetente: '',
    documentoDestinatario: '',
    documentoPagador: '',
    altura: '',
    comprimento: '',
    largura: '',
    pesoCubado: '',
    previsaoEntrega: '',
  };
}

function numero(valor: string): number {
  return Number(valor.replace(',', '.'));
}

function numeroOpcional(valor: string): number | undefined {
  return valor.trim() ? numero(valor) : undefined;
}

function dataHoraOffsetOpcional(valor: string): string | undefined {
  if (!valor) {
    return undefined;
  }

  const data = new Date(valor);
  return Number.isNaN(data.getTime()) ? undefined : data.toISOString();
}

export default function EslColetaModal({ filial, open, onClose }: EslColetaModalProps) {
  if (!open) {
    return null;
  }

  return <EslColetaModalAberto filial={filial} onClose={onClose} />;
}

function EslColetaModalAberto({ filial, onClose }: Pick<EslColetaModalProps, 'filial' | 'onClose'>) {
  const criarColeta = useCriarColetaEsl();
  const validarNf = useValidarNfEsl();
  const [form, setForm] = useState<ColetaFormState>(criarEstadoInicial);
  const [erros, setErros] = useState<EslFormErrors>({ geral: null, campos: {} });
  const [erroNf, setErroNf] = useState<string | null>(null);
  const [recibo, setRecibo] = useState<EslColetaResposta | null>(null);
  const dependenciasNf = useMemo<NfValidationDependencies>(() => ({
    cnpj: form.documentoCliente,
    remetente: form.documentoRemetente,
    filial: form.documentoLocalColeta,
    filialOperacional: filial,
    chaveOrNumeroNf: form.chaveOrNumeroNf,
  }), [filial, form.chaveOrNumeroNf, form.documentoCliente, form.documentoLocalColeta, form.documentoRemetente]);
  const nfValidation = useNfValidationState(dependenciasNf);

  function atualizarCampo<K extends keyof ColetaFormState>(campo: K, valor: ColetaFormState[K]) {
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

  async function handleValidarNf() {
    const chaveOrNumeroNf = form.chaveOrNumeroNf.trim();
    if (!chaveOrNumeroNf) {
      setErroNf('Informe a chave de acesso ou o número da NF antes de validar.');
      return;
    }

    const dependenciasValidadas = { ...dependenciasNf, chaveOrNumeroNf };
    try {
      setErroNf(null);
      const notaFiscal = await validarNf.mutateAsync({ filial, chaveOrNumero: chaveOrNumeroNf });
      nfValidation.registrarNotaFiscalValidada(notaFiscal, dependenciasValidadas);
    } catch (error) {
      nfValidation.invalidarNotaFiscal();
      setErroNf(getApiErrorMessage(error, 'Não foi possível validar a NF no ESL.'));
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!nfValidation.podeSubmeter || !nfValidation.invoiceId) {
      setErroNf('Valide a NF com os dados atuais antes de agendar a coleta.');
      return;
    }

    const solicitacao: EslCriarColetaRequest = {
      documentoCliente: form.documentoCliente.trim(),
      referencia: form.referencia.trim() || undefined,
      documentoLocalColeta: form.documentoLocalColeta.trim(),
      dataAgendada: form.dataAgendada,
      horaInicial: form.horaInicial,
      horaFinal: form.horaFinal,
      emailNotificacao: form.emailNotificacao.trim() || undefined,
      telefoneNotificacao: form.telefoneNotificacao.trim() || undefined,
      observacoes: form.observacoes.trim() || undefined,
      itens: [{
        modal: form.modal,
        valorNotasFiscais: numero(form.valorNotasFiscais),
        quantidadeVolumes: numero(form.quantidadeVolumes),
        pesoRealNotasFiscais: numero(form.pesoRealNotasFiscais),
        documentoRemetente: form.documentoRemetente.trim(),
        documentoDestinatario: form.documentoDestinatario.trim(),
        documentoPagador: form.documentoPagador.trim(),
        invoiceIds: [nfValidation.invoiceId],
        altura: numeroOpcional(form.altura),
        comprimento: numeroOpcional(form.comprimento),
        largura: numeroOpcional(form.largura),
        pesoCubado: numeroOpcional(form.pesoCubado),
        previsaoEntrega: dataHoraOffsetOpcional(form.previsaoEntrega),
      }],
    };

    try {
      setErros({ geral: null, campos: {} });
      setRecibo(await criarColeta.mutateAsync({ filial, solicitacao }));
    } catch (error) {
      setErros(mapearErrosValidacaoEsl(error, CAMPO_ALIASES, 'Não foi possível agendar a coleta no ESL.'));
    }
  }

  if (recibo) {
    return (
      <EslModalFrame
        title="Coleta agendada no ESL"
        subtitle="A tabela diária será atualizada apenas pela consulta remota do ESL."
        onClose={onClose}
        maxWidthClassName="max-w-2xl"
      >
        <section className="space-y-5 rounded-3xl border p-5" style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)' }}>
          <div className="flex items-start gap-3">
            <CheckCircle2 className="mt-0.5 shrink-0" size={24} style={{ color: '#15803d' }} />
            <div>
              <h3 className="text-lg font-bold" style={{ color: 'var(--color-text)' }}>Agendamento confirmado</h3>
              <p className="mt-1 text-sm" style={{ color: 'var(--color-text-subtle)' }}>Coleta #{recibo.numeroColeta?.toLocaleString('pt-BR') ?? recibo.coletaId} em {recibo.dataAgendada ? formatarData(recibo.dataAgendada) : 'data não informada'}.</p>
            </div>
          </div>
          <div className="grid gap-3 sm:grid-cols-3">
            <ReciboItem label="Número" value={recibo.numeroColeta?.toLocaleString('pt-BR') ?? 'Não informado'} />
            <ReciboItem label="Status" value={recibo.status ?? 'Não informado'} />
            <ReciboItem label="Peso" value={recibo.pesoTaxado == null ? 'Não informado' : formatarPeso(recibo.pesoTaxado)} />
          </div>
          <div className="flex justify-end">
            <button type="button" onClick={onClose} className="rounded-xl px-4 py-2.5 text-sm font-semibold text-white" style={ESL_PRIMARY_BUTTON_STYLE}>Concluir</button>
          </div>
        </section>
      </EslModalFrame>
    );
  }

  const isBusy = criarColeta.isPending || validarNf.isPending;
  return (
    <EslModalFrame
      title="Nova Coleta ESL"
      subtitle="A NF deve ser validada antes do agendamento. O invoiceId permanece apenas na memória deste formulário."
      onClose={onClose}
      isBusy={isBusy}
    >
      <form className="space-y-6" onSubmit={(event) => void handleSubmit(event)}>
        <EslErrorPanel message={erros.geral} />

        <section className="space-y-4 rounded-3xl border p-4 sm:p-5" style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)' }}>
          <div>
            <h3 className="font-bold" style={{ color: 'var(--color-text)' }}>Solicitante e agendamento</h3>
            <p className="mt-1 text-sm" style={{ color: 'var(--color-text-subtle)' }}>A data de solicitação é inserida pelo BFF; informe somente a janela de atendimento.</p>
          </div>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <EslInput label="CNPJ/CPF do cliente" value={form.documentoCliente} onChange={(event) => atualizarCampo('documentoCliente', event.target.value)} error={erros.campos.documentoCliente} required />
            <EslInput label="CNPJ/CPF do local de coleta" value={form.documentoLocalColeta} onChange={(event) => atualizarCampo('documentoLocalColeta', event.target.value)} error={erros.campos.documentoLocalColeta} required />
            <EslInput label="Data agendada" type="date" value={form.dataAgendada} onChange={(event) => atualizarCampo('dataAgendada', event.target.value)} error={erros.campos.dataAgendada} required />
            <EslInput label="Referência" value={form.referencia} onChange={(event) => atualizarCampo('referencia', event.target.value)} maxLength={120} />
            <EslInput label="Hora inicial" type="time" value={form.horaInicial} onChange={(event) => atualizarCampo('horaInicial', event.target.value)} error={erros.campos.horaInicial} required />
            <EslInput label="Hora final" type="time" value={form.horaFinal} onChange={(event) => atualizarCampo('horaFinal', event.target.value)} error={erros.campos.horaFinal} required />
            <EslInput label="E-mail de notificação" type="email" value={form.emailNotificacao} onChange={(event) => atualizarCampo('emailNotificacao', event.target.value)} error={erros.campos.emailNotificacao} />
            <EslInput label="Telefone de notificação" value={form.telefoneNotificacao} onChange={(event) => atualizarCampo('telefoneNotificacao', event.target.value)} error={erros.campos.telefoneNotificacao} maxLength={40} />
          </div>
          <EslTextarea label="Observações" value={form.observacoes} onChange={(event) => atualizarCampo('observacoes', event.target.value)} maxLength={2000} />
        </section>

        <section className="space-y-4 rounded-3xl border p-4 sm:p-5" style={{ backgroundColor: 'var(--color-bg)', borderColor: 'var(--color-border)' }}>
          <div>
            <h3 className="font-bold" style={{ color: 'var(--color-text)' }}>Nota fiscal e item da coleta</h3>
            <p className="mt-1 text-sm" style={{ color: 'var(--color-text-subtle)' }}>O formulário cria um item e vincula a NF validada no ESL.</p>
          </div>

          <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_auto] lg:items-end">
            <EslInput label="Chave de acesso ou número da NF" value={form.chaveOrNumeroNf} onChange={(event) => atualizarCampo('chaveOrNumeroNf', event.target.value)} error={erros.campos.chaveOrNumeroNf} required />
            <button type="button" onClick={() => void handleValidarNf()} disabled={validarNf.isPending} className="inline-flex h-[42px] items-center justify-center gap-2 rounded-xl border px-4 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-60" style={ESL_SECONDARY_BUTTON_STYLE}>
              {validarNf.isPending ? <Loader2 className="animate-spin" size={16} /> : <FileCheck2 size={16} />}
              {validarNf.isPending ? 'Validando...' : 'Validar NF'}
            </button>
          </div>
          {erroNf ? <EslErrorPanel message={erroNf} /> : null}
          {nfValidation.status === 'validado' && nfValidation.notaFiscal ? (
            <div className="flex flex-wrap items-center gap-x-5 gap-y-2 rounded-2xl border px-4 py-3 text-sm" style={{ borderColor: 'rgba(21, 128, 61, 0.5)', backgroundColor: 'rgba(21, 128, 61, 0.08)', color: 'var(--color-text)' }}>
              <span className="inline-flex items-center gap-2 font-semibold" style={{ color: '#15803d' }}><ShieldCheck size={17} /> NF validada</span>
              <span>Número: {nfValidation.notaFiscal.numero ?? '—'}</span>
              <span>Série: {nfValidation.notaFiscal.serie ?? '—'}</span>
              <span>ID remoto pronto para o agendamento.</span>
            </div>
          ) : (
            <p className="text-sm" style={{ color: 'var(--color-text-subtle)' }}>Altere qualquer dado vinculado à NF e a validação será exigida novamente.</p>
          )}

          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <EslSelect label="Modal" value={form.modal} onChange={(event) => atualizarCampo('modal', event.target.value as ColetaFormState['modal'])} required>
              <option value="rodo">Rodoviário</option>
              <option value="air">Aéreo</option>
            </EslSelect>
            <EslInput label="Valor das NFs" inputMode="decimal" type="number" min="0" step="0.01" value={form.valorNotasFiscais} onChange={(event) => atualizarCampo('valorNotasFiscais', event.target.value)} error={erros.campos.valorNotasFiscais} required />
            <EslInput label="Volumes" type="number" min="1" step="1" value={form.quantidadeVolumes} onChange={(event) => atualizarCampo('quantidadeVolumes', event.target.value)} error={erros.campos.quantidadeVolumes} required />
            <EslInput label="Peso real (kg)" inputMode="decimal" type="number" min="0" step="0.001" value={form.pesoRealNotasFiscais} onChange={(event) => atualizarCampo('pesoRealNotasFiscais', event.target.value)} error={erros.campos.pesoRealNotasFiscais} required />
            <EslInput label="CNPJ/CPF do remetente" value={form.documentoRemetente} onChange={(event) => atualizarCampo('documentoRemetente', event.target.value)} error={erros.campos.documentoRemetente} required />
            <EslInput label="CNPJ/CPF do destinatário" value={form.documentoDestinatario} onChange={(event) => atualizarCampo('documentoDestinatario', event.target.value)} error={erros.campos.documentoDestinatario} required />
            <EslInput label="CNPJ/CPF do pagador" value={form.documentoPagador} onChange={(event) => atualizarCampo('documentoPagador', event.target.value)} error={erros.campos.documentoPagador} required />
            <EslInput label="Previsão de entrega" type="datetime-local" value={form.previsaoEntrega} onChange={(event) => atualizarCampo('previsaoEntrega', event.target.value)} />
          </div>

          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <EslInput label="Altura (cm)" inputMode="decimal" type="number" min="0" step="0.001" value={form.altura} onChange={(event) => atualizarCampo('altura', event.target.value)} error={erros.campos.altura} />
            <EslInput label="Comprimento (cm)" inputMode="decimal" type="number" min="0" step="0.001" value={form.comprimento} onChange={(event) => atualizarCampo('comprimento', event.target.value)} error={erros.campos.comprimento} />
            <EslInput label="Largura (cm)" inputMode="decimal" type="number" min="0" step="0.001" value={form.largura} onChange={(event) => atualizarCampo('largura', event.target.value)} error={erros.campos.largura} />
            <EslInput label="Peso cubado (kg)" inputMode="decimal" type="number" min="0" step="0.001" value={form.pesoCubado} onChange={(event) => atualizarCampo('pesoCubado', event.target.value)} error={erros.campos.pesoCubado} />
          </div>
        </section>

        <div className="flex flex-wrap justify-end gap-3 pb-1">
          <button type="button" onClick={onClose} disabled={isBusy} className="rounded-xl border px-4 py-2.5 text-sm font-semibold disabled:opacity-50" style={ESL_SECONDARY_BUTTON_STYLE}>Cancelar</button>
          <button type="submit" disabled={!nfValidation.podeSubmeter || criarColeta.isPending} className="inline-flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-60" style={ESL_PRIMARY_BUTTON_STYLE}>
            {criarColeta.isPending ? <Loader2 className="animate-spin" size={16} /> : <Send size={16} />}
            {criarColeta.isPending ? 'Agendando no ESL...' : 'Agendar coleta'}
          </button>
        </div>
      </form>
    </EslModalFrame>
  );
}

function ReciboItem({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border px-4 py-3" style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)' }}>
      <p className="text-xs font-semibold uppercase tracking-wide" style={{ color: 'var(--color-text-subtle)' }}>{label}</p>
      <p className="mt-1 text-lg font-bold" style={{ color: 'var(--color-text)' }}>{value}</p>
    </div>
  );
}
