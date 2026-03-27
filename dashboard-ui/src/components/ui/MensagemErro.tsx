import type { TipoErro } from '../../utils/apiError';
import { AlertCircle, Clock, WifiOff, CalendarX } from 'lucide-react';

interface MensagemErroProps {
  mensagem: string;
  tipo?: TipoErro;
}

const config: Record<TipoErro, {
  bg: string;
  border: string;
  text: string;
  Icon: typeof AlertCircle;
}> = {
  periodo: {
    bg: 'bg-amber-50 dark:bg-amber-950/30',
    border: 'border-amber-400',
    text: 'text-amber-700 dark:text-amber-400',
    Icon: CalendarX,
  },
  timeout: {
    bg: 'bg-orange-50 dark:bg-orange-950/30',
    border: 'border-orange-400',
    text: 'text-orange-700 dark:text-orange-400',
    Icon: Clock,
  },
  indisponivel: {
    bg: 'bg-gray-50 dark:bg-gray-900/40',
    border: 'border-gray-400',
    text: 'text-gray-600 dark:text-gray-400',
    Icon: WifiOff,
  },
  erro: {
    bg: 'bg-red-50 dark:bg-red-950/30',
    border: 'border-red-400',
    text: 'text-red-700 dark:text-red-400',
    Icon: AlertCircle,
  },
};

export default function MensagemErro({ mensagem, tipo = 'erro' }: MensagemErroProps) {
  const { bg, border, text, Icon } = config[tipo];

  return (
    <div className={`flex items-start gap-3 rounded-lg border px-4 py-3 ${bg} ${border}`}>
      <Icon size={16} className={`mt-0.5 shrink-0 ${text}`} aria-hidden="true" />
      <p className={`text-sm leading-relaxed ${text}`}>{mensagem}</p>
    </div>
  );
}
