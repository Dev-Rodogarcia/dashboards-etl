import { useState } from 'react';

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
        className="px-3 py-1.5 text-xs font-medium border border-gray-300 rounded hover:bg-gray-100 transition-colors disabled:opacity-50"
      >
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
