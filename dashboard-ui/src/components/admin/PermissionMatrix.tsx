import type { PermissionCatalogItem, PermissionMap } from '../../types/access';

interface PermissionMatrixProps {
  catalogo: PermissionCatalogItem[];
  valor: PermissionMap;
  onChange: (proximo: PermissionMap) => void;
  disabled?: boolean;
}

export default function PermissionMatrix({ catalogo, valor, onChange, disabled }: PermissionMatrixProps) {
  return (
    <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
      {catalogo.map((item) => {
        const ativo = Boolean(valor[item.chave]);

        return (
          <label
            key={item.chave}
            className={`flex items-start gap-3 rounded-2xl border px-4 py-3 transition-colors ${disabled ? 'opacity-60' : ''}`}
            style={
              ativo
                ? {
                    backgroundColor: 'color-mix(in srgb, var(--color-primary) 12%, var(--color-card))',
                    borderColor: 'color-mix(in srgb, var(--color-primary) 34%, var(--color-border))',
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
              className="mt-1 h-4 w-4 rounded border-gray-300"
              style={{ accentColor: 'var(--color-primary)' }}
            />
            <div>
              <div className="text-sm font-semibold" style={{ color: 'var(--color-text)' }}>{item.nome}</div>
              <div className="text-xs" style={{ color: 'var(--color-text-subtle)' }}>{item.descricao}</div>
              {item.rota && <div className="mt-1 text-xs" style={{ color: 'var(--color-text-muted)' }}>{item.rota}</div>}
            </div>
          </label>
        );
      })}
    </div>
  );
}
