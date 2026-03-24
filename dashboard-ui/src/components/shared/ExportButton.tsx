interface ExportButtonProps {
  dados: Record<string, unknown>[];
  nomeArquivo: string;
}

export default function ExportButton({ dados, nomeArquivo }: ExportButtonProps) {
  function exportarCSV() {
    if (dados.length === 0) return;

    const colunas = Object.keys(dados[0]);
    const linhas = dados.map((row) =>
      colunas.map((col) => {
        const valor = row[col];
        const str = valor == null ? '' : String(valor);
        return str.includes(',') || str.includes('"') || str.includes('\n')
          ? `"${str.replace(/"/g, '""')}"`
          : str;
      }).join(',')
    );

    const csv = [colunas.join(','), ...linhas].join('\n');
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${nomeArquivo}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  }

  return (
    <button
      onClick={exportarCSV}
      disabled={dados.length === 0}
      className="px-3 py-1.5 text-xs font-medium border border-gray-300 rounded hover:bg-gray-100 transition-colors disabled:opacity-50"
    >
      Exportar CSV
    </button>
  );
}
