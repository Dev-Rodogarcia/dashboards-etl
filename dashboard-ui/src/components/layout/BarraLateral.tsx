import { NavLink } from 'react-router-dom';
import { usePermissions } from '../../hooks/usePermissions';
import { ADMIN_NAV_ITEMS, DASHBOARD_NAV_ITEMS } from '../../utils/accessControl';

export default function BarraLateral() {
  const { canAccess, isAdminAcesso } = usePermissions();

  const dashboardsVisiveis = DASHBOARD_NAV_ITEMS.filter((item) =>
    item.permission ? canAccess(item.permission) : true,
  );

  return (
    <aside className="min-h-screen w-64 border-r border-gray-200 bg-gray-50">
      <div className="border-b border-gray-200 px-5 py-5">
        <h2 className="text-lg font-bold text-[#21478A]">Dashboards ETL</h2>
        <p className="mt-1 text-xs text-gray-500">Acesso por setor e permissões</p>
      </div>

      <nav className="space-y-6 px-3 py-4">
        <div>
          <div className="px-2 pb-2 text-xs font-semibold uppercase tracking-wide text-gray-400">
            Dashboards
          </div>
          <div className="space-y-1">
            {dashboardsVisiveis.map((item) => (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) =>
                  `block rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                    isActive
                      ? 'bg-[#21478A] text-white'
                      : 'text-gray-700 hover:bg-gray-200'
                  }`
                }
              >
                {item.label}
              </NavLink>
            ))}
          </div>
        </div>

        {isAdminAcesso && (
          <div>
            <div className="px-2 pb-2 text-xs font-semibold uppercase tracking-wide text-gray-400">
              Administração
            </div>
            <div className="space-y-1">
              {ADMIN_NAV_ITEMS.map((item) => (
                <NavLink
                  key={item.path}
                  to={item.path}
                  className={({ isActive }) =>
                    `block rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                      isActive
                        ? 'bg-gray-900 text-white'
                        : 'text-gray-700 hover:bg-gray-200'
                    }`
                  }
                >
                  <div>{item.label}</div>
                  {item.description && (
                    <div className="mt-0.5 text-[11px] opacity-70">{item.description}</div>
                  )}
                </NavLink>
              ))}
            </div>
          </div>
        )}
      </nav>
    </aside>
  );
}
