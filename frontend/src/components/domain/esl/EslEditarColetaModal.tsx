import { useState } from 'react';
import type { FormEvent } from 'react';
import { Loader2, Save } from 'lucide-react';
import { useAtualizarColetaEsl } from '../../../hooks/queries/useEsl';
import type { EslAtualizarColetaRequest, EslColetaListagemItem } from '../../../types/esl';
import { dataHojeLocal } from '../../../utils/dateUtils';
import { mapearErrosValidacaoEsl, type EslFormErrors } from '../../../utils/eslFormErrors';
import EslModalFrame from './EslModalFrame';
import {
  EslErrorPanel,
  EslInput,
  EslTextarea,
  ESL_PRIMARY_BUTTON_STYLE,
  ESL_SECONDARY_BUTTON_STYLE,
} from './EslFormControls';

interface EslEditarColetaModalProps {
  filial: string;
  coleta: EslColetaListagemItem | null;
  onClose: () => void;
}

interface EslEditarColetaModalAbertoProps {
  filial: string;
  coleta: EslColetaListagemItem;
  onClose: () => void;
}

interface EdicaoFormState {
  dataAgendada: string;
  horaInicial: string;
  horaFinal: string;
  emailNotificacao: string;
  telefoneNotificacao: string;
  observacoes: string;
}

const CAMPO_ALIASES: Record<string, string[]> = {
  dataAgendada: ['dataAgendada', 'serviceDate', 'data agendada'],
  horaInicial: ['horaInicial', 'serviceStartHour', 'hora inicial'],
  horaFinal: ['horaFinal', 'serviceEndHour', 'hora final'],
  emailNotificacao: ['emailNotificacao', 'notificationEmail', 'e-mail'],
  telefoneNotificacao: ['telefoneNotificacao', 'notificationPhone', 'telefone'],
  observacoes: ['observacoes', 'comments', 'observação', 'observacao'],
};

function horaParaInput(valor: string | null): string {
  return valor?.slice(0, 5) ?? '';
}

function criarEstadoInicial(coleta: EslColetaListagemItem): EdicaoFormState {
  return {
    dataAgendada: coleta.dataAgendada ?? dataHojeLocal(),
    horaInicial: horaParaInput(coleta.horaInicial),
    horaFinal: horaParaInput(coleta.horaFinal),
    emailNotificacao: '',
    telefoneNotificacao: '',
    observacoes: coleta.observacoes ?? '',
  };
}

export default function EslEditarColetaModal({ filial, coleta, onClose }: EslEditarColetaModalProps) {
  if (!coleta) {
    return null;
  }

  return <EslEditarColetaModalAberto key={coleta.coletaId} filial={filial} coleta={coleta} onClose={onClose} />;
}

function EslEditarColetaModalAberto({ filial, coleta, onClose }: EslEditarColetaModalAbertoProps) {
  const atualizarColeta = useAtualizarColetaEsl();
  const [form, setForm] = useState<EdicaoFormState>(() => criarEstadoInicial(coleta));
  const [erros, setErros] = useState<EslFormErrors>({ geral: null, campos: {} });

  function atualizarCampo<K extends keyof EdicaoFormState>(campo: K, valor: EdicaoFormState[K]) {
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
    const solicitacao: EslAtualizarColetaRequest = {
      dataAgendada: form.dataAgendada || undefined,
      horaInicial: form.horaInicial || undefined,
      horaFinal: form.horaFinal || undefined,
      emailNotificacao: form.emailNotificacao.trim() || undefined,
      telefoneNotificacao: form.telefoneNotificacao.trim() || undefined,
      observacoes: form.observacoes.trim() || undefined,
    };

    try {
      setErros({ geral: null, campos: {} });
      await atualizarColeta.mutateAsync({ filial, eslId: coleta.coletaId, solicitacao });
      onClose();
    } catch (error) {
      setErros(mapearErrosValidacaoEsl(error, CAMPO_ALIASES, 'Não foi possível atualizar a coleta no ESL.'));
    }
  }

  return (
    <EslModalFrame
      title={`Editar Coleta ESL #${coleta.numeroColeta?.toLocaleString('pt-BR') ?? coleta.coletaId}`}
      subtitle="Somente campos de agendamento, notificação e observação são atualizados nesta operação."
      onClose={onClose}
      isBusy={atualizarColeta.isPending}
      maxWidthClassName="max-w-3xl"
    >
      <form className="space-y-5" onSubmit={(event) => void handleSubmit(event)}>
        <EslErrorPanel message={erros.geral} />
        <div className="grid gap-4 md:grid-cols-3">
          <EslInput label="Data agendada" type="date" value={form.dataAgendada} onChange={(event) => atualizarCampo('dataAgendada', event.target.value)} error={erros.campos.dataAgendada} required />
          <EslInput label="Hora inicial" type="time" value={form.horaInicial} onChange={(event) => atualizarCampo('horaInicial', event.target.value)} error={erros.campos.horaInicial} required />
          <EslInput label="Hora final" type="time" value={form.horaFinal} onChange={(event) => atualizarCampo('horaFinal', event.target.value)} error={erros.campos.horaFinal} required />
          <EslInput label="E-mail de notificação" type="email" value={form.emailNotificacao} onChange={(event) => atualizarCampo('emailNotificacao', event.target.value)} error={erros.campos.emailNotificacao} />
          <EslInput label="Telefone de notificação" value={form.telefoneNotificacao} onChange={(event) => atualizarCampo('telefoneNotificacao', event.target.value)} error={erros.campos.telefoneNotificacao} />
        </div>
        <EslTextarea label="Observações" value={form.observacoes} onChange={(event) => atualizarCampo('observacoes', event.target.value)} error={erros.campos.observacoes} maxLength={2000} />
        <div className="flex flex-wrap justify-end gap-3">
          <button type="button" onClick={onClose} disabled={atualizarColeta.isPending} className="rounded-xl border px-4 py-2.5 text-sm font-semibold disabled:opacity-50" style={ESL_SECONDARY_BUTTON_STYLE}>Voltar</button>
          <button type="submit" disabled={atualizarColeta.isPending} className="inline-flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-60" style={ESL_PRIMARY_BUTTON_STYLE}>
            {atualizarColeta.isPending ? <Loader2 className="animate-spin" size={16} /> : <Save size={16} />}
            {atualizarColeta.isPending ? 'Salvando...' : 'Salvar alterações'}
          </button>
        </div>
      </form>
    </EslModalFrame>
  );
}
