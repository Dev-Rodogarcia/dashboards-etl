import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { ThemeProvider } from 'next-themes'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import './index.css'
import App from './App.tsx'
import { AutenticacaoProvider } from './contexts/AutenticacaoContext.tsx'
import { FiltroProvider } from './contexts/FiltroContext.tsx'
import { PageHeaderProvider } from './contexts/PageHeaderContext.tsx'

const dashboardBuildId = import.meta.env.VITE_DASHBOARD_BUILD_ID ?? 'dev';
document.documentElement.dataset.dashboardBuildId = dashboardBuildId;

const BASE_QUERY_STALE_TIME = 5 * 60 * 1000;

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: BASE_QUERY_STALE_TIME,
      refetchOnWindowFocus: false,
    },
  },
});

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeProvider attribute="class" defaultTheme="light" enableSystem={false}>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
          <AutenticacaoProvider>
            <FiltroProvider>
              <PageHeaderProvider>
                <App />
              </PageHeaderProvider>
            </FiltroProvider>
          </AutenticacaoProvider>
        </BrowserRouter>
      </QueryClientProvider>
    </ThemeProvider>
  </StrictMode>,
)
