import { formatarDataHora } from '../../utils/formatadores';

interface LastUpdatedProps {
  dataExtracao: string | null;
}

export default function LastUpdated({ dataExtracao }: LastUpdatedProps) {
  if (!dataExtracao) return null;

  return (
    <span className="text-xs" style={{ color: 'var(--color-text-subtle)' }}>
      Atualizado em {formatarDataHora(dataExtracao)}
    </span>
  );
}
