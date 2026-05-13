import fs from 'node:fs';
import path from 'node:path';

const envDir = path.resolve(process.cwd(), '..');
const envFiles = ['.env', '.env.local', '.env.production', '.env.production.local'];
const localHosts = new Set(['localhost', '127.0.0.1', '0.0.0.0', '::1', '[::1]']);

function parseEnvLine(line) {
  const trimmed = line.trim();
  if (!trimmed || trimmed.startsWith('#') || !trimmed.includes('=')) {
    return null;
  }

  const separatorIndex = trimmed.indexOf('=');
  const key = trimmed.slice(0, separatorIndex).trim();
  let value = trimmed.slice(separatorIndex + 1).trim();
  if (
    (value.startsWith('"') && value.endsWith('"'))
    || (value.startsWith("'") && value.endsWith("'"))
  ) {
    value = value.slice(1, -1);
  }
  return [key, value];
}

function readViteEnv() {
  const values = {};
  for (const file of envFiles) {
    const filePath = path.join(envDir, file);
    if (!fs.existsSync(filePath)) {
      continue;
    }

    const lines = fs.readFileSync(filePath, 'utf8').split(/\r?\n/);
    for (const line of lines) {
      const parsed = parseEnvLine(line);
      if (parsed) {
        values[parsed[0]] = parsed[1];
      }
    }
  }

  return { ...values, ...process.env };
}

function fail(message) {
  console.error(`[ERRO] ${message}`);
  process.exit(1);
}

const env = readViteEnv();
const apiBaseUrl = String(env.VITE_API_BASE_URL ?? '').trim();

if (!apiBaseUrl) {
  fail('VITE_API_BASE_URL precisa estar definido para gerar o build de produção do frontend.');
}

let parsedUrl;
try {
  parsedUrl = new URL(apiBaseUrl);
} catch {
  fail(`VITE_API_BASE_URL inválido: ${apiBaseUrl}`);
}

if (parsedUrl.protocol !== 'https:') {
  fail('VITE_API_BASE_URL de produção precisa usar HTTPS.');
}

if (localHosts.has(parsedUrl.hostname)) {
  fail('VITE_API_BASE_URL de produção não pode apontar para localhost/127.0.0.1/0.0.0.0.');
}

console.log(`[OK] Build de produção usará API em ${parsedUrl.origin}`);
