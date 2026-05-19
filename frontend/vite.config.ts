import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

const DEV_FRONTEND_PORT = 5174
const PROD_FRONTEND_PORT = 5173
const LOCAL_DEV_HOSTS = ['localhost', '127.0.0.1']

function normalizarBuildId(value: string): string {
  return value.replace(/[^A-Za-z0-9_-]/g, '').slice(0, 40) || 'dev'
}

function resolverBuildId(command: string): string {
  if (process.env.VITE_DASHBOARD_BUILD_ID) {
    return normalizarBuildId(process.env.VITE_DASHBOARD_BUILD_ID)
  }

  if (command === 'build') {
    return normalizarBuildId(new Date().toISOString())
  }

  return 'dev'
}

export default defineConfig(({ command, mode }) => {
  const isNpmDev = process.env.npm_lifecycle_event === 'dev'

  if (command === 'serve' && isNpmDev && mode !== 'development') {
    throw new Error('Vite dev deve rodar com --mode development para carregar .env.development.')
  }

  const buildId = resolverBuildId(command)

  return {
    envDir: '..',
    plugins: [react(), tailwindcss()],
    build: {
      sourcemap: false,
      rolldownOptions: {
        output: {
          entryFileNames: `assets/[name]-${buildId}-[hash].js`,
          chunkFileNames: `assets/[name]-${buildId}-[hash].js`,
          assetFileNames: `assets/[name]-${buildId}-[hash][extname]`,
        },
      },
    },
    server: {
      host: '127.0.0.1',
      port: DEV_FRONTEND_PORT,
      strictPort: true,
      allowedHosts: LOCAL_DEV_HOSTS,
    },
    preview: {
      host: '127.0.0.1',
      port: PROD_FRONTEND_PORT,
      strictPort: true,
    },
  }
})
