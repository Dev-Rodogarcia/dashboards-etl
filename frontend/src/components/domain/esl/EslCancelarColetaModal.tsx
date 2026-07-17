import { useState } from 'react';
import type { FormEvent } from 'react';
import { AlertTriangle, Loader2, XCircle } from 'lucide-react';
import { useCancelarColetaEsl } from '../../../hooks/queries/useEsl';
import type { EslColetaListagemItem, EslMotivoCancelamento } from '../../../types/esl';
import { mapearErrosValidacaoEsl, type EslFormErrors } from '../../../utils/eslFormErrors';
import EslModalFrame from './EslModalFrame';
import {
  EslErrorPanel,
  EslSelect,
  EslTextarea,
  ESL_SECONDARY_BUTTON_STYLE,
} from './EslFormControls';

interface EslCancelarColetaModalProps {
  filial: string;
  coleta: EslColetaListagemItem | null;
  onClose: () => void;
}

interface EslCancelarColetaModalAbertoProps {
  filial: string;
  coleta: EslColetaListagemItem;
  onClose: () => void;
}

const MOTIVOS: Array<{ valor: EslMotivoCancelamento; label: string }> = [
  { valor: 'DESISTENCIA', label: 'Desistência' },
  { valor: 'VOLUME_INCORRETO', label: 'Volume incorreto' },
  { valor: 'DUPLICIDADE', label: 'Duplicidade' },
  { valor: 'OUTROS', label: 'Outros / erro de cadastro' },
];

export default function EslCancelarColetaModal({ filial, coleta, onClose }: EslCancelarColetaModalProps) {
  if (!coleta) {
    return null;
  }

  return <EslCancelarColetaModalAberto key={coleta.coletaId} filial={filial} coleta={coleta} onClose={onClose} />;
}

function EslCancelarColetaModalAberto({ filial, coleta, onClose }: EslCancelarColetaModalAbertoProps) {
  const cancelarColeta = useCancelarColetaEsl();
  const [motivo, setMotivo] = useState<EslMotivoCancelamento>('DESISTENCIA');
  const [detalheOutros, setDetalheOutros] = useState('');
  const [erros, setErros] = useState<EslFormErrors>({ geral: null, campos: {} });

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (motivo === 'OUTROS' && !detalheOutros.trim()) {
      setErros({ geral: null, campos: { detalheOutros: 'Descreva o motivo para confirmar o cancelamento.' } });
      return;
    }

    try {
      setErros({ geral: null, campos: {} });
      await cancelarColeta.mutateAsync({ filial, eslId: coleta.coletaId, solicitacao: { motivo } });
      onClose();
    } catch (error) {
      setErros(mapearErrosValidacaoEsl(error, { motivo: ['motivo', 'cancellationReason', 'cancelamento'] }, 'Não foi possível cancelar a coleta no ESL.'));
    }
  }

  return (
    <EslModalFrame
      title={`Cancelar Coleta ESL #${coleta.numeroColeta?.toLocaleString('pt-BR') ?? coleta.coletaId}`}
      subtitle="O cancelamento é enviado ao ESL pelo ID remoto da coleta e não pode ser desfeito neste painel."
      onClose={onClose}
      isBusy={cancelarColeta.isPending}
      maxWidthClassName="max-w-xl"
    >
      <form className="space-y-5" onSubmit={(event) => void handleSubmit(event)}>
        <div className="flex items-start gap-3 rounded-2xl border px-4 py-3 text-sm" style={{ borderColor: 'rgba(234, 88, 12, 0.5)', backgroundColor: 'rgba(234, 88, 12, 0.08)', color: 'var(--color-text)' }}>
          <AlertTriangle className="mt-0.5 shrink-0" size={18} style={{ color: '#c2410c' }} />
          <p>Confirme o motivo canônico antes de cancelar. A tabela diária será atualizada pela consulta remota após a confirmação.</p>
        </div>
        <EslErrorPanel message={erros.geral} />
        <EslSelect label="Motivo do cancelamento" value={motivo} onChange={(event) => setMotivo(event.target.value as EslMotivoCancelamento)} error={erros.campos.motivo} required>
          {MOTIVOS.map((item) => <option key={item.valor} value={item.valor}>{item.label}</option>)}
        </EslSelect>
        {motivo === 'OUTROS' ? (
          <EslTextarea
            label="Detalhe de outros"
            value={detalheOutros}
            onChange={(event) => {
              setDetalheOutros(event.target.value);
              setErros((atual) => ({ ...atual, campos: { ...atual.campos, detalheOutros: '' } }));
            }}
            error={erros.campos.detalheOutros}
            hint="O ESL recebe o motivo canônico “Outros”; este detalhe serve para conferência na confirmação atual e não é persistido pelo Dashboard."
            maxLength={500}
            required
          />
        ) : null}
        <div className="flex flex-wrap justify-end gap-3">
          <button type="button" onClick={onClose} disabled={cancelarColeta.isPending} className="rounded-xl border px-4 py-2.5 text-sm font-semibold disabled:opacity-50" style={ESL_SECONDARY_BUTTON_STYLE}>Voltar</button>
          <button type="submit" disabled={cancelarColeta.isPending} className="inline-flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-60" style={{ backgroundColor: '#dc2626' }}>
            {cancelarColeta.isPending ? <Loader2 className="animate-spin" size={16} /> : <XCircle size={16} />}
            {cancelarColeta.isPending ? 'Cancelando...' : 'Confirmar cancelamento'}
          </button>
        </div>
      </form>
    </EslModalFrame>
  );
}
