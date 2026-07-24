import type { PermissionCatalogItem, PermissionMap } from '../../types/access';

interface PermissionMatrixProps {
  catalogo: PermissionCatalogItem[];
  valor: PermissionMap;
  onChange: (proximo: PermissionMap) => void;
  disabled?: boolean;
}

export default function PermissionMatrix({ catalogo, valor, onChange, disabled }: PermissionMatrixProps) {
  return (
    <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 2xl:grid-cols-6">
      {catalogo.map((item) => {
        const ativo = Boolean(valor[item.chave]);

        return (
          <label
            key={item.chave}
            className={`flex min-h-[122px] min-w-0 items-start gap-2.5 rounded-xl border px-3.5 py-3 transition-colors ${disabled ? 'opacity-60' : ''}`}
            style={
              ativo
                ? {
                    backgroundColor: 'var(--color-bg)',
                    borderColor: 'var(--color-primary)',
                  }
                : {
                    backgroundColor: 'var(--color-card)',
                    borderColor: 'var(--color-border)',
                  }
            }
          >
            <input
              type="checkbox"
              checked={ativo}
              disabled={disabled}
              onChange={(event) =>
                onChange({
                  ...valor,
                  [item.chave]: event.target.checked,
                })
              }
              className="mt-1 h-4 w-4 rounded border-[var(--color-border)]"
              style={{ accentColor: 'var(--color-primary)' }}
            />
            <div className="min-w-0">
              <div className="text-sm font-semibold" style={{ color: 'var(--color-text)' }}>{item.nome}</div>
              <div className="mt-0.5 line-clamp-2 text-xs leading-4" title={item.descricao} style={{ color: 'var(--color-text-subtle)' }}>{item.descricao}</div>
              {item.rota && <div className="mt-1 truncate text-xs" title={item.rota} style={{ color: 'var(--color-text-muted)' }}>{item.rota}</div>}
            </div>
          </label>
        );
      })}
    </div>
  );
}
