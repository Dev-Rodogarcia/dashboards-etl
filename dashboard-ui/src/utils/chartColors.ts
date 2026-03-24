export const CORES = {
  primaria: '#21478A',
  secundaria: '#7c3aed',
  sucesso: '#10b981',
  perigo: '#ef4444',
  aviso: '#f59e0b',
  alerta: '#f59e0b',
  info: '#06b6d4',
  cinza: '#6b7280',
  cinzaClaro: '#d1d5db',
} as const;

export const PALETA_SERIES = [
  '#21478A',
  '#f59e0b',
  '#10b981',
  '#ef4444',
  '#7c3aed',
  '#06b6d4',
  '#ec4899',
  '#84cc16',
  '#f97316',
  '#8b5cf6',
] as const;

export const CORES_STATUS: Record<string, string> = {
  pendente: '#f59e0b',
  'em trânsito': '#21478A',
  finalizado: '#10b981',
  encerrado: '#10b981',
  cancelada: '#ef4444',
  cancelado: '#ef4444',
  coletada: '#06b6d4',
  manifestada: '#7c3aed',
  'em tratativa': '#f97316',
  'em entrega': '#3b82f6',
  'em armazém': '#8b5cf6',
  'em transferência': '#6366f1',
  manifestado: '#7c3aed',
  aguardando: '#d1d5db',
};
