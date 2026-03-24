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
      {catalogo.map((item) => (
        <label
          key={item.chave}
          className={`flex items-start gap-3 rounded-2xl border px-4 py-3 ${
            valor[item.chave] ? 'border-blue-300 bg-blue-50' : 'border-gray-200 bg-white'
          } ${disabled ? 'opacity-60' : ''}`}
        >
          <input
            type="checkbox"
            checked={Boolean(valor[item.chave])}
            disabled={disabled}
            onChange={(event) =>
              onChange({
                ...valor,
                [item.chave]: event.target.checked,
              })
            }
            className="mt-1 h-4 w-4 rounded border-gray-300 text-[#21478A]"
          />
          <div>
            <div className="text-sm font-semibold text-gray-900">{item.nome}</div>
            <div className="text-xs text-gray-500">{item.descricao}</div>
            {item.rota && <div className="mt-1 text-xs text-gray-400">{item.rota}</div>}
          </div>
        </label>
      ))}
    </div>
  );
}
