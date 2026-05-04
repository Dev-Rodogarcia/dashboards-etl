import { useMemo } from 'react';
import { dataHojeLocal, dataNDiasAtrasLocal, normalizarPeriodo } from '../../utils/dateUtils';

interface DateRangePickerProps {
  dataInicio: string;
  dataFim: string;
  onDataInicioChange: (valor: string) => void;
  onDataFimChange: (valor: string) => void;
  onRangeChange?: (inicio: string, fim: string) => void;
}

// Atalhos corporativos: semana, quinzena, mês, bimestre, trimestre, semestre
const PRESETS = [
  { label: '7d',   dias: 7   },
  { label: '15d',  dias: 15  },
  { label: '30d',  dias: 30  },
  { label: '60d',  dias: 60  },
  { label: '90d',  dias: 90  },
  { label: '180d', dias: 180 },
];

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
  // Detecta qual preset está ativo comparando datas com timezone local
  const presetAtivo = useMemo(() => {
    const hoje = dataHojeLocal();
    for (const { label, dias } of PRESETS) {
      if (dataFim === hoje && dataInicio === dataNDiasAtrasLocal(dias)) return label;
    }
    return null;
  }, [dataInicio, dataFim]);

  function aplicarPreset(dias: number) {
    const p = normalizarPeriodo(dataNDiasAtrasLocal(dias), dataHojeLocal());
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
    <div className="flex flex-wrap items-end gap-4">
      {/* Bloco: campos De / Até */}
      <div className="flex items-end gap-2">
        <div className="flex flex-col gap-1">
          <label
            htmlFor="date-inicio"
            className="text-xs font-medium"
            style={{ color: 'var(--color-text-muted)' }}
          >
            De
          </label>
          <input
            id="date-inicio"
            type="date"
            value={dataInicio}
            onChange={(e) => atualizarInicio(e.target.value)}
            className={inputClass}
            style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
          />
        </div>

        <div className="flex flex-col gap-1">
          <label
            htmlFor="date-fim"
            className="text-xs font-medium"
            style={{ color: 'var(--color-text-muted)' }}
          >
            Até
          </label>
          <input
            id="date-fim"
            type="date"
            value={dataFim}
            onChange={(e) => atualizarFim(e.target.value)}
            className={inputClass}
            style={{ backgroundColor: 'var(--color-card)', borderColor: 'var(--color-border)', color: 'var(--color-text)' }}
          />
        </div>
      </div>

      {/* Separador visual */}
      <div
        className="hidden h-9 w-px self-end sm:block"
        style={{ backgroundColor: 'var(--color-border)' }}
        aria-hidden="true"
      />

      {/* Bloco: atalhos de período */}
      <div className="flex flex-col gap-1">
        <span
          className="text-xs font-medium"
          style={{ color: 'var(--color-text-muted)' }}
        >
          Atalho
        </span>
        <div className="grid grid-cols-3 gap-1 sm:flex sm:flex-wrap">
          {PRESETS.map(({ label, dias }) => {
            const ativo = presetAtivo === label;
            return (
              <button
                key={label}
                type="button"
                onClick={() => aplicarPreset(dias)}
                aria-pressed={ativo}
                className="cursor-pointer rounded-lg border px-2.5 py-2 text-xs font-semibold
                           transition-all duration-150 active:scale-[0.97]
                           focus:outline-none focus:ring-2 focus:ring-[color-mix(in_srgb,var(--color-primary)_30%,transparent)]"
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
