import { useMemo } from 'react';

interface DateRangePickerProps {
  dataInicio: string;
  dataFim: string;
  onDataInicioChange: (valor: string) => void;
  onDataFimChange: (valor: string) => void;
  onRangeChange?: (inicio: string, fim: string) => void;
}

const PRESETS = [
  { label: '7d', dias: 7 },
  { label: '30d', dias: 30 },
  { label: '90d', dias: 90 },
];

function dataNDiasAtras(dias: number): string {
  const d = new Date();
  d.setDate(d.getDate() - dias);
  return d.toISOString().slice(0, 10);
}

function dataHoje(): string {
  return new Date().toISOString().slice(0, 10);
}

function adicionarDias(data: string, dias: number): string {
  const d = new Date(`${data}T00:00:00`);
  d.setDate(d.getDate() + dias);
  return d.toISOString().slice(0, 10);
}

function normalizarPeriodo(dataInicio: string, dataFim: string) {
  if (!dataInicio || !dataFim) return { dataInicio, dataFim };
  if (dataInicio > dataFim) return { dataInicio: dataFim, dataFim };
  const limiteMaximo = adicionarDias(dataInicio, 90);
  if (limiteMaximo < dataFim) return { dataInicio, dataFim: limiteMaximo };
  return { dataInicio, dataFim };
}

const inputClass =
  'cursor-pointer rounded-lg border px-3 py-2 text-sm shadow-sm transition-all duration-150 ' +
  'hover:border-[var(--color-primary)] focus:border-[var(--color-primary)] ' +
  'focus:outline-none focus:ring-2 focus:ring-[color-mix(in_srgb,var(--color-primary)_20%,transparent)]';

export default function DateRangePicker({
  dataInicio,
  dataFim,
  onDataInicioChange,
  onDataFimChange,
  onRangeChange,
}: DateRangePickerProps) {
  // Detecta qual preset está ativo comparando datas
  const presetAtivo = useMemo(() => {
    const hoje = dataHoje();
    for (const { label, dias } of PRESETS) {
      if (dataFim === hoje && dataInicio === dataNDiasAtras(dias)) return label;
    }
    return null;
  }, [dataInicio, dataFim]);

  function aplicarPreset(dias: number) {
    const p = normalizarPeriodo(dataNDiasAtras(dias), dataHoje());
    if (onRangeChange) {
      onRangeChange(p.dataInicio, p.dataFim);
    } else {
      onDataInicioChange(p.dataInicio);
      onDataFimChange(p.dataFim);
    }
  }

  function atualizarInicio(valor: string) {
    if (onRangeChange) {
      onRangeChange(valor, dataFim);
    } else {
      onDataInicioChange(valor);
    }
  }

  function atualizarFim(valor: string) {
    if (onRangeChange) {
      onRangeChange(dataInicio, valor);
    } else {
      onDataFimChange(valor);
    }
  }

  return (
    <div className="flex flex-wrap items-end gap-3">
      {/* Data início */}
      <div className="flex flex-col gap-1">
        <label className="text-xs font-medium" style={{ color: 'var(--color-text-muted)' }}>
          De
        </label>
        <input
          type="date"
          value={dataInicio}
          onChange={(e) => atualizarInicio(e.target.value)}
          className={inputClass}
          style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
        />
      </div>

      {/* Data fim */}
      <div className="flex flex-col gap-1">
        <label className="text-xs font-medium" style={{ color: 'var(--color-text-muted)' }}>
          Até
        </label>
        <input
          type="date"
          value={dataFim}
          onChange={(e) => atualizarFim(e.target.value)}
          className={inputClass}
          style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
        />
      </div>

      {/* Atalhos */}
      <div className="flex flex-col gap-1">
        <span className="text-xs font-medium" style={{ color: 'var(--color-text-muted)' }}>
          Atalho
        </span>
        <div className="flex gap-1">
          {PRESETS.map(({ label, dias }) => {
            const ativo = presetAtivo === label;
            return (
              <button
                key={label}
                type="button"
                onClick={() => aplicarPreset(dias)}
                className="cursor-pointer rounded-lg border px-3 py-2 text-xs font-semibold
                           transition-all duration-150 active:scale-[0.97]"
                style={
                  ativo
                    ? {
                        backgroundColor: 'var(--color-primary)',
                        borderColor: 'var(--color-primary)',
                        color: 'white',
                      }
                    : {
                        backgroundColor: 'var(--color-bg)',
                        borderColor: 'var(--color-border)',
                        color: 'var(--color-text-muted)',
                      }
                }
                onMouseEnter={(e) => {
                  if (!ativo) {
                    (e.currentTarget as HTMLElement).style.borderColor = 'var(--color-primary)';
                    (e.currentTarget as HTMLElement).style.color = 'var(--color-text)';
                  }
                }}
                onMouseLeave={(e) => {
                  if (!ativo) {
                    (e.currentTarget as HTMLElement).style.borderColor = 'var(--color-border)';
                    (e.currentTarget as HTMLElement).style.color = 'var(--color-text-muted)';
                  }
                }}
              >
                {label}
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}
