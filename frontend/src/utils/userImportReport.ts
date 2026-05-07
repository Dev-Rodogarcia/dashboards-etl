import { salvarBlobComoArquivo } from '../api/downloadArquivo';
import type { UserImportResult } from '../types/userImport';

function escapeCsvCell(value: string | number | null | undefined): string {
  const text = value == null ? '' : String(value);
  const escaped = text.replace(/"/g, '""');
  return `"${escaped}"`;
}

export function exportarRelatorioImportacaoCsv(resultado: UserImportResult): void {
  const linhas = [
    ['categoria', 'linha', 'nome', 'email', 'setor', 'senhaProvisoria', 'motivo', 'tipoErro'],
    ...resultado.listaCriados.map((item) => {
      const credencial = resultado.credenciaisTemporarias.find((cred) => cred.email === item.email);
      return ['criado', '', item.nome, item.email, item.setor, credencial?.senhaProvisoria ?? '', '', ''];
    }),
    ...resultado.listaIgnorados.map((item) => ['ignorado', '', '', item.email, '', '', item.motivo, 'conflito']),
    ...resultado.listaErros.map((item) => ['erro', item.linha, '', item.email ?? '', '', '', item.motivo, item.tipoErro]),
  ];

  const csv = linhas
    .map((linha) => linha.map((valor) => escapeCsvCell(valor)).join(';'))
    .join('\r\n');

  const blob = new Blob([`\ufeff${csv}`], { type: 'text/csv;charset=utf-8' });
  salvarBlobComoArquivo(blob, 'usuarios-importacao-relatorio.csv');
}
