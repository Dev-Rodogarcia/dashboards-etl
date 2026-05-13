import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

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

export default defineConfig(({ command }) => {
  const buildId = resolverBuildId(command)

  return {
    envDir: '..',
    plugins: [react(), tailwindcss()],
    build: {
      rolldownOptions: {
        output: {
          entryFileNames: `assets/[name]-${buildId}-[hash].js`,
          chunkFileNames: `assets/[name]-${buildId}-[hash].js`,
          assetFileNames: `assets/[name]-${buildId}-[hash][extname]`,
        },
      },
    },
    server: {
      host: true,
      allowedHosts:
      ['analytics.rodogarcia.com.br']
    },
  }
})
