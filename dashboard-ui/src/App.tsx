import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AutenticacaoProvider, useAutenticacao } from './contexts/AutenticacaoContext';
import { FiltroProvider } from './contexts/FiltroContext';
import RotaProtegida from './components/layout/RotaProtegida';
import LayoutPainel from './components/layout/LayoutPainel';
import LoginPage from './pages/LoginPage';
import AcessoNegadoPage from './pages/AcessoNegadoPage';
import AlterarSenhaPage from './pages/AlterarSenhaPage';
import ColetasPage from './pages/ColetasPage';
import ManifestosPage from './pages/ManifestosPage';
import FretesPage from './pages/FretesPage';
import TrackingPage from './pages/TrackingPage';
import FaturasPage from './pages/FaturasPage';
import FaturasPorClientePage from './pages/FaturasPorClientePage';
import ContasAPagarPage from './pages/ContasAPagarPage';
import CotacoesPage from './pages/CotacoesPage';
import ExecutivoPage from './pages/ExecutivoPage';
import EtlSaudePage from './pages/EtlSaudePage';
import AdminSetoresPage from './pages/AdminSetoresPage';
import AdminUsuariosPage from './pages/AdminUsuariosPage';
import { firstAccessibleRoute } from './utils/accessControl';

const queryClient = new QueryClient();

function TelaCarregamento() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-100">
      <div className="h-8 w-8 animate-spin rounded-full border-2 border-[#21478A] border-t-transparent" />
    </div>
  );
}

function RedirecionamentoInicial() {
  const { usuario, carregandoSessao } = useAutenticacao();

  if (carregandoSessao) {
    return <TelaCarregamento />;
  }

  return <Navigate to={usuario?.token ? firstAccessibleRoute(usuario) : '/login'} replace />;
}

function RotaLogin() {
  const { usuario, carregandoSessao } = useAutenticacao();

  if (carregandoSessao) {
    return <TelaCarregamento />;
  }

  if (usuario?.token) {
    return <Navigate to={firstAccessibleRoute(usuario)} replace />;
  }

  return <LoginPage />;
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <AutenticacaoProvider>
          <FiltroProvider>
            <Routes>
              <Route path="/login" element={<RotaLogin />} />
              <Route path="/acesso-negado" element={<AcessoNegadoPage />} />

              <Route element={<RotaProtegida allowPasswordChange />}>
                <Route path="/alterar-senha" element={<AlterarSenhaPage />} />
              </Route>

              <Route element={<RotaProtegida />}>
                <Route element={<LayoutPainel />}>
                  <Route path="/" element={<RedirecionamentoInicial />} />

                  <Route element={<RotaProtegida permissao="coletas" />}>
                    <Route path="/coletas" element={<ColetasPage />} />
                  </Route>

                  <Route element={<RotaProtegida permissao="manifestos" />}>
                    <Route path="/manifestos" element={<ManifestosPage />} />
                  </Route>

                  <Route element={<RotaProtegida permissao="fretes" />}>
                    <Route path="/fretes" element={<FretesPage />} />
                  </Route>

                  <Route element={<RotaProtegida permissao="tracking" />}>
                    <Route path="/tracking" element={<TrackingPage />} />
                  </Route>

                  <Route element={<RotaProtegida permissao="faturas" />}>
                    <Route path="/faturas" element={<FaturasPage />} />
                  </Route>

                  <Route element={<RotaProtegida permissao="faturasPorCliente" />}>
                    <Route path="/faturas-por-cliente" element={<FaturasPorClientePage />} />
                  </Route>

                  <Route element={<RotaProtegida permissao="contasAPagar" />}>
                    <Route path="/contas-a-pagar" element={<ContasAPagarPage />} />
                  </Route>

                  <Route element={<RotaProtegida permissao="cotacoes" />}>
                    <Route path="/cotacoes" element={<CotacoesPage />} />
                  </Route>

                  <Route element={<RotaProtegida permissao="executivo" />}>
                    <Route path="/executivo" element={<ExecutivoPage />} />
                  </Route>

                  <Route element={<RotaProtegida permissao="etlSaude" />}>
                    <Route path="/etl-saude" element={<EtlSaudePage />} />
                  </Route>

                  <Route element={<RotaProtegida adminOnly />}>
                    <Route path="/admin" element={<Navigate to="/admin/setores" replace />} />
                    <Route path="/admin/setores" element={<AdminSetoresPage />} />
                    <Route path="/admin/usuarios" element={<AdminUsuariosPage />} />
                  </Route>
                </Route>
              </Route>

              <Route path="*" element={<RedirecionamentoInicial />} />
            </Routes>
          </FiltroProvider>
        </AutenticacaoProvider>
      </BrowserRouter>
    </QueryClientProvider>
  );
}
