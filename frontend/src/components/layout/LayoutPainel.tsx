import { useEffect, useMemo, useState } from 'react';
import { CalendarClock } from 'lucide-react';
import { Outlet } from 'react-router-dom';
import TopNav from './TopNav';

type BuildInfo = {
  buildId?: string;
  builtAt?: string;
  deployedAt?: string;
};

const BUILD_ID_FALLBACK = import.meta.env.VITE_DASHBOARD_BUILD_ID ?? 'dev';

function formatarDataHoraBuild(valor?: string): string | null {
  if (!valor) return null;

  const data = new Date(valor);
  if (Number.isNaN(data.getTime())) return null;

  return new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'short',
    timeStyle: 'short',
    timeZone: 'America/Sao_Paulo',
  }).format(data);
}

function useBuildInfo(): BuildInfo {
  const [buildInfo, setBuildInfo] = useState<BuildInfo>({ buildId: BUILD_ID_FALLBACK });

  useEffect(() => {
    const controller = new AbortController();

    async function carregarBuildInfo() {
      try {
        const resposta = await fetch('/build-info.json', {
          cache: 'no-store',
          signal: controller.signal,
        });

        if (!resposta.ok) return;

        const dados = (await resposta.json()) as BuildInfo;
        setBuildInfo({
          buildId: dados.buildId || BUILD_ID_FALLBACK,
          builtAt: dados.builtAt,
          deployedAt: dados.deployedAt,
        });
      } catch {
        if (!controller.signal.aborted) {
          setBuildInfo({ buildId: BUILD_ID_FALLBACK });
        }
      }
    }

    void carregarBuildInfo();

    return () => controller.abort();
  }, []);

  return buildInfo;
}

function BuildInfoFooter() {
  const buildInfo = useBuildInfo();
  const buildId = (buildInfo.buildId?.trim() || BUILD_ID_FALLBACK).trim();
  const isDevBuild = buildId.toLowerCase() === 'dev';
  const dataDeploy = useMemo(
    () => formatarDataHoraBuild(buildInfo.deployedAt),
    [buildInfo.deployedAt],
  );
  const dataBuild = useMemo(
    () => formatarDataHoraBuild(buildInfo.builtAt),
    [buildInfo.builtAt],
  );
  const dataPrincipal = dataDeploy ?? dataBuild;
  const rotuloPrincipal = dataDeploy ? 'Deploy' : 'Build';

  if (!dataPrincipal && isDevBuild) {
    return null;
  }

  return (
    <div className="flex flex-wrap items-center gap-2 text-[11px]">
      {dataPrincipal ? (
        <span
          className="inline-flex h-7 items-center gap-1.5 rounded-md border px-2.5 font-medium"
          style={{
            borderColor: 'var(--color-border)',
            backgroundColor: 'var(--color-bg)',
            color: 'var(--color-text-muted)',
          }}
          title={`${rotuloPrincipal} registrado em ${dataPrincipal}`}
        >
          <CalendarClock size={13} aria-hidden="true" />
          <span>{rotuloPrincipal}</span>
          <span className="font-semibold" style={{ color: 'var(--color-text)' }}>
            {dataPrincipal}
          </span>
        </span>
      ) : (
        <span
          className="inline-flex h-7 items-center gap-1.5 rounded-md border px-2.5 font-medium"
          style={{
            borderColor: 'var(--color-border)',
            backgroundColor: 'var(--color-bg)',
            color: 'var(--color-text-muted)',
          }}
          title="Build publicado"
        >
          <CalendarClock size={13} aria-hidden="true" />
          <span>Build</span>
        </span>
      )}
      {!isDevBuild && (
        <span
          className="inline-flex h-7 items-center rounded-md border px-2.5 font-mono font-semibold"
          style={{
            borderColor: 'var(--color-border)',
            backgroundColor: 'var(--color-card)',
            color: 'var(--color-text-muted)',
          }}
          title="Identificador do build"
        >
          #{buildId}
        </span>
      )}
    </div>
  );
}

export default function LayoutPainel() {
  return (
    <div className="min-h-screen flex flex-col" style={{ backgroundColor: 'var(--color-bg)' }}>
      <TopNav />
      <main className="flex-1 w-full overflow-auto px-3 py-3 sm:px-5 sm:py-4">
        <Outlet />
      </main>
      <footer className="mt-auto border-t px-4 py-3 sm:px-6" style={{ borderColor: 'var(--color-border)', backgroundColor: 'var(--color-card)' }}>
        <div className="flex w-full flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="min-w-0 space-y-1">
            <div className="flex flex-wrap items-center gap-x-3 gap-y-1.5">
              <p className="text-xs font-semibold tracking-wide" style={{ color: 'var(--color-text-muted)' }}>
                DASHBOARDS
              </p>
              <BuildInfoFooter />
            </div>
            <p className="text-xs" style={{ color: 'var(--color-text-subtle)' }}>
              Painel de indicadores operacionais e logísticos.
            </p>
          </div>
          <div className="flex flex-col gap-1 text-left sm:items-end sm:text-right">
            <p className="text-xs" style={{ color: 'var(--color-text-muted)' }}>
              Desenvolvido por{' '}
              <a
                href="https://www.linkedin.com/in/dev-lucasandrade/"
                target="_blank"
                rel="noopener noreferrer"
                className="font-medium transition-opacity hover:opacity-70"
                style={{ color: 'var(--color-primary)' }}
              >
                @valentelucass
              </a>
            </p>
            <p className="text-xs" style={{ color: 'var(--color-text-subtle)' }}>
              Suporte: lucasmac.dev@gmail.com
            </p>
          </div>
        </div>
      </footer>
    </div>
  );
}
