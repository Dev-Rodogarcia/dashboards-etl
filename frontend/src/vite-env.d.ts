/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_DASHBOARD_BUILD_ID?: string;
  readonly VITE_API_BASE_URL?: string;
  readonly VITE_API_REQUEST_TIMEOUT_MS?: string;
  readonly VITE_API_DOWNLOAD_TIMEOUT_MS?: string;
  readonly VITE_AUTH_REQUEST_TIMEOUT_MS?: string;
  readonly VITE_HOME_COMUNICADOS_API_ENABLED?: string;
  readonly VITE_ACESSO_USUARIO_SUPREMO_PAPEL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
