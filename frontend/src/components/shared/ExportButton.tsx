import { useState } from 'react';
import { FileSpreadsheet } from 'lucide-react';

interface ExportButtonProps {
  dados?: Record<string, unknown>[];
  nomeArquivo: string;
  onExport?: () => Promise<void> | void;
  label?: string;
}

export default function ExportButton({ onExport, label = 'Exportar CSV' }: ExportButtonProps) {
  const [exportando, setExportando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const podeExportar = Boolean(onExport);

  async function exportarCsv() {
    if (!podeExportar || exportando) return;

    setErro(null);
    setExportando(true);
    try {
      if (onExport) {
        await onExport();
      }
    } catch (error) {
      console.error('Falha ao exportar CSV', error);
      setErro('Nao foi possivel exportar agora.');
    } finally {
      setExportando(false);
    }
  }

  return (
    <span className="inline-flex flex-col items-end gap-1">
      <button
        type="button"
        onClick={exportarCsv}
        disabled={!podeExportar || exportando}
        className="inline-flex h-9 items-center gap-1.5 rounded-lg border px-3 text-xs font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-50 hover:border-[var(--color-primary)] focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)]"
        style={{
          backgroundColor: 'var(--color-bg)',
          borderColor: 'var(--color-border)',
          color: 'var(--color-text)',
        }}
      >
        <FileSpreadsheet size={14} aria-hidden="true" />
        {exportando ? 'Exportando...' : label}
      </button>
      {erro && (
        <span className="text-[11px]" style={{ color: 'var(--color-danger)' }} role="status">
          {erro}
        </span>
      )}
    </span>
  );
}
