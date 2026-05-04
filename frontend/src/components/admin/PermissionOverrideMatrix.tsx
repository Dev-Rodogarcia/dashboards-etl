import type {
  PermissionCatalogItem,
  PermissionMap,
  PermissionOverrideMode,
  PermissionOverrideStateMap,
} from '../../types/access';
import { PAPEL_ADMIN_PLATAFORMA } from '../../utils/accessControl';

interface PermissionOverrideMatrixProps {
  catalogo: PermissionCatalogItem[];
  baseline: PermissionMap;
  papel: string;
  valor: PermissionOverrideStateMap;
  onChange: (proximo: PermissionOverrideStateMap) => void;
  disabled?: boolean;
}

const OPCOES: Array<{ valor: PermissionOverrideMode; label: string }> = [
  { valor: 'inherit', label: 'Herdar' },
  { valor: 'deny', label: 'Negar' },
  { valor: 'grant', label: 'Conceder' },
];

function getToneSurfaceStyle(color: string) {
  return {
    backgroundColor: `color-mix(in srgb, ${color} 12%, var(--color-card))`,
    borderColor: `color-mix(in srgb, ${color} 34%, var(--color-border))`,
  };
}

function getToneBadgeStyle(color: string) {
  return {
    backgroundColor: `color-mix(in srgb, ${color} 14%, var(--color-card))`,
    color: `color-mix(in srgb, ${color} 72%, var(--color-text))`,
  };
}

export default function PermissionOverrideMatrix({
  catalogo,
  baseline,
  papel,
  valor,
  onChange,
  disabled,
}: PermissionOverrideMatrixProps) {
  return (
    <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
      {catalogo.map((item) => {
        const herdado = Boolean(baseline[item.chave]);
        const negado = valor[item.chave] === 'deny';
        const concedido = valor[item.chave] === 'grant';
        const acessoFinal = papel === PAPEL_ADMIN_PLATAFORMA ? true : ((herdado && !negado) || concedido);
        const cardStyle = negado
          ? getToneSurfaceStyle('#ef4444')
          : acessoFinal
            ? getToneSurfaceStyle('#10b981')
            : {
                backgroundColor: 'var(--color-card)',
                borderColor: 'var(--color-border)',
              };

        return (
          <div
            key={item.chave}
            className={`rounded-2xl border px-4 py-3 ${disabled ? 'opacity-60' : ''}`}
            style={cardStyle}
          >
            <div>
              <div className="text-sm font-semibold" style={{ color: 'var(--color-text)' }}>{item.nome}</div>
              <div className="text-xs" style={{ color: 'var(--color-text-subtle)' }}>{item.descricao}</div>
              {item.rota && <div className="mt-1 text-xs" style={{ color: 'var(--color-text-muted)' }}>{item.rota}</div>}
            </div>

            <div className="mt-3 flex flex-wrap gap-2 text-[11px]">
              <span
                className="rounded-full px-2 py-1 font-medium"
                style={
                  herdado
                    ? getToneBadgeStyle('var(--color-primary)')
                    : {
                        backgroundColor: 'var(--color-bg)',
                        color: 'var(--color-text-subtle)',
                      }
                }
              >
                Herdado: {herdado ? 'permitido' : 'negado'}
              </span>
              <span
                className="rounded-full px-2 py-1 font-medium"
                style={acessoFinal ? getToneBadgeStyle('#10b981') : getToneBadgeStyle('#ef4444')}
              >
                Final: {acessoFinal ? 'permitido' : 'negado'}
              </span>
            </div>

            <div className="mt-3 flex flex-wrap gap-2">
              {OPCOES.map((opcao) => {
                const ativo = valor[item.chave] === opcao.valor;
                return (
                  <button
                    key={opcao.valor}
                    type="button"
                    disabled={disabled}
                    onClick={() =>
                      onChange({
                        ...valor,
                        [item.chave]: opcao.valor,
                      })
                    }
                    className="rounded-full border px-3 py-1.5 text-xs font-medium transition-colors disabled:cursor-not-allowed"
                    style={
                      ativo
                        ? {
                            backgroundColor: 'var(--color-primary)',
                            borderColor: 'var(--color-primary)',
                            color: '#ffffff',
                          }
                        : {
                            backgroundColor: 'var(--color-card)',
                            borderColor: 'var(--color-border)',
                            color: 'var(--color-text)',
                          }
                    }
                  >
                    {opcao.label}
                  </button>
                );
              })}
            </div>
          </div>
        );
      })}
    </div>
  );
}
