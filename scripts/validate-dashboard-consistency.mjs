import { execFileSync } from 'node:child_process';
import { createHmac } from 'node:crypto';
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { ENTITIES } from './dashboard-validation/entities.mjs';

const ROOT_DIR = process.cwd();
const API_ENV_PATH = path.join(ROOT_DIR, 'dashboard-api', '.env');
const REPORTS_DIR = path.join(ROOT_DIR, 'reports');

const DEFAULT_TOLERANCES = {
  count: 0,
  number: 0.01,
  currency: 0.01,
  weight: 0.01,
  percentage: 0.5,
  days: 0.1,
  hours: 0.01,
};

function parseArgs(argv) {
  const args = {};
  for (const raw of argv) {
    if (!raw.startsWith('--')) {
      continue;
    }
    const [key, value = 'true'] = raw.slice(2).split('=', 2);
    args[key] = value;
  }
  return args;
}

function parseDotEnv(filePath) {
  const content = readFileSync(filePath, 'utf8');
  return content
    .split(/\r?\n/u)
    .filter((line) => line.trim() && !line.trim().startsWith('#'))
    .reduce((acc, line) => {
      const separator = line.indexOf('=');
      if (separator < 0) {
        return acc;
      }
      const key = line.slice(0, separator).trim();
      const value = line.slice(separator + 1).trim();
      acc[key] = value;
      return acc;
    }, {});
}

function base64UrlJson(value) {
  return Buffer.from(JSON.stringify(value), 'utf8').toString('base64url');
}

function buildJwt(email, secret) {
  const now = Math.floor(Date.now() / 1000);
  const header = { alg: 'HS256', typ: 'JWT' };
  const payload = { sub: email, iat: now, exp: now + 15 * 60 };
  const signingInput = `${base64UrlJson(header)}.${base64UrlJson(payload)}`;
  const signature = createHmac('sha256', secret)
    .update(signingInput)
    .digest('base64url');
  return `${signingInput}.${signature}`;
}

function sqlConnectionFromEnv(env, overrides) {
  const dbUrl = env.DB_URL ?? '';
  const serverMatch = dbUrl.match(/jdbc:sqlserver:\/\/([^;]+)/u);
  const databaseMatch = dbUrl.match(/databaseName=([^;]+)/u);
  const normalizeSqlcmdServer = (value) => {
    if (!value) {
      return value;
    }
    return value.replace(/:(\d+)$/u, ',$1');
  };

  return {
    server: normalizeSqlcmdServer(overrides.dbServer ?? serverMatch?.[1] ?? 'localhost,1433'),
    database: overrides.dbName ?? databaseMatch?.[1] ?? 'ETL_SISTEMA',
    user: overrides.dbUser ?? env.DB_USER,
    password: overrides.dbPassword ?? env.DB_PASSWORD,
  };
}

function runSqlQuery(connection, query) {
  const args = [
    '-S', connection.server,
    '-d', connection.database,
    '-U', connection.user,
    '-P', connection.password,
    '-C',
    '-w', '65535',
    '-y', '0',
    '-Q', query,
  ];

  const rawOutput = execFileSync('SQLCMD.EXE', args, {
    cwd: ROOT_DIR,
    encoding: 'utf8',
  });
  const output = rawOutput.trim();

  if (!output) {
    return null;
  }

  const firstObject = output.indexOf('{');
  const lastObject = output.lastIndexOf('}');
  const firstArray = output.indexOf('[');
  const lastArray = output.lastIndexOf(']');

  let jsonSlice = output;
  if (firstObject >= 0 && lastObject > firstObject) {
    jsonSlice = output.slice(firstObject, lastObject + 1);
  } else if (firstArray >= 0 && lastArray > firstArray) {
    jsonSlice = output.slice(firstArray, lastArray + 1);
  }

  return JSON.parse(jsonSlice);
}

async function fetchJson(url, options = {}) {
  const response = await fetch(url, options);
  const text = await response.text();

  if (!response.ok) {
    throw new Error(`HTTP ${response.status} em ${url}: ${text}`);
  }

  return text ? JSON.parse(text) : null;
}

async function ensureApiUp(apiBaseUrl) {
  const url = new URL('/actuator/health/readiness', apiBaseUrl);
  return fetchJson(url);
}

async function fetchFrontendOverview(apiBaseUrl, token, entity, period) {
  const url = new URL(entity.apiPath, apiBaseUrl);
  url.searchParams.set('dataInicio', period.dataInicio);
  url.searchParams.set('dataFim', period.dataFim);

  return fetchJson(url, {
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: 'application/json',
    },
  });
}

function resolveApiUserEmail(connection, preferredEmail) {
  if (preferredEmail) {
    return preferredEmail;
  }

  const query = `
SET NOCOUNT ON;
SELECT TOP 1 email
FROM acesso.usuarios
WHERE ativo = 1
ORDER BY id
FOR JSON PATH, WITHOUT_ARRAY_WRAPPER;
`.trim();

  const result = runSqlQuery(connection, query);
  if (!result?.email) {
    throw new Error('Não foi possível localizar um usuário ativo para gerar o JWT técnico.');
  }
  return result.email;
}

function resolveTolerance(metric) {
  return metric.toleranceAbs ?? DEFAULT_TOLERANCES[metric.type] ?? 0;
}

function toNumber(value) {
  if (value === null || value === undefined || value === '') {
    return null;
  }

  const numeric = Number(value);
  return Number.isFinite(numeric) ? numeric : null;
}

function round(value, decimals = 4) {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return null;
  }
  return Number(value.toFixed(decimals));
}

function formatNumber(value, fractionDigits = 2) {
  if (value === null || value === undefined) {
    return '-';
  }
  return new Intl.NumberFormat('pt-BR', {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  }).format(value);
}

function formatMetricValue(type, value) {
  if (value === null || value === undefined || value === '') {
    return '-';
  }

  switch (type) {
    case 'currency':
      return new Intl.NumberFormat('pt-BR', {
        style: 'currency',
        currency: 'BRL',
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      }).format(Number(value));
    case 'percentage':
      return `${formatNumber(Number(value), 2)}%`;
    case 'count':
      return new Intl.NumberFormat('pt-BR', {
        maximumFractionDigits: 0,
      }).format(Number(value));
    default:
      return formatNumber(Number(value), 2);
  }
}

function formatDiff(value, type) {
  if (value === null || value === undefined) {
    return '-';
  }

  const sign = value > 0 ? '+' : '';
  if (type === 'currency') {
    return `${sign}${formatMetricValue('currency', value)}`;
  }
  if (type === 'percentage') {
    return `${sign}${formatNumber(value, 2)} p.p.`;
  }
  if (type === 'count') {
    return `${sign}${formatMetricValue('count', value)}`;
  }
  return `${sign}${formatNumber(value, 2)}`;
}

function formatDiffPct(value) {
  if (value === null || value === undefined || !Number.isFinite(value)) {
    return '-';
  }
  const sign = value > 0 ? '+' : '';
  return `${sign}${formatNumber(value, 2)}%`;
}

function normalizeTimestamp(value) {
  if (!value) {
    return null;
  }
  const timestamp = new Date(value);
  if (Number.isNaN(timestamp.getTime())) {
    return null;
  }
  return timestamp;
}

function formatTimestamp(value) {
  const timestamp = normalizeTimestamp(value);
  if (!timestamp) {
    return '-';
  }
  return new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'short',
    timeStyle: 'medium',
    hour12: false,
  }).format(timestamp);
}

function diffMinutes(a, b) {
  const left = normalizeTimestamp(a);
  const right = normalizeTimestamp(b);
  if (!left || !right) {
    return null;
  }
  return round((right.getTime() - left.getTime()) / 60000, 2);
}

function inferPossibleCauses(entity, metricResult, updateDiagnostic, frontendRow) {
  if (metricResult.ok) {
    return [];
  }

  const causes = new Set();

  if (metricResult.backendValue === null || metricResult.frontendValue === null) {
    causes.add('valor nulo ou ausente em uma das fontes');
  }

  if (metricResult.type === 'percentage' && Math.abs(metricResult.diffAbs ?? 0) <= 0.5) {
    causes.add('arredondamento percentual');
  }

  if ((metricResult.type === 'currency' || metricResult.type === 'weight' || metricResult.type === 'days' || metricResult.type === 'hours')
    && Math.abs(metricResult.diffAbs ?? 0) <= 0.1) {
    causes.add('arredondamento na agregação');
  }

  if (entity.key === 'tracking' && metricResult.id === 'previsao_vencida') {
    causes.add('SQL compara por data; API compara com data e hora atuais');
  }

  if (entity.key === 'fretes' && metricResult.id === 'fretes_previsao_vencida') {
    causes.add('métrica sensível ao relógio atual do servidor');
  }

  if (entity.key === 'faturas' && ['valor_faturado', 'valor_recebido', 'saldo_aberto', 'taxa_adimplencia_pct', 'titulos_em_atraso'].includes(metricResult.id)) {
    causes.add('diferença entre visão financeira GraphQL e conciliação dos títulos');
  }

  if (entity.key === 'faturas' && metricResult.id === 'clientes_ativos' && frontendRow?.hasFinancialData === false) {
    causes.add('API zera os KPIs de Faturas quando não há títulos financeiros, mesmo com base operacional ativa');
  }

  if (entity.key === 'faturas_por_cliente') {
    causes.add('normalização por ID Único ou data da última atualização');
  }

  if (['fretes', 'cotacoes', 'manifestos', 'tracking', 'faturas', 'faturas_por_cliente'].includes(entity.key)) {
    causes.add('truncamento/timezone em filtros de data/hora');
  }

  if (updateDiagnostic?.isDivergent) {
    causes.add('dados do frontend/API aparentam estar defasados em relação ao banco');
  }

  if (Math.abs(metricResult.diffPct ?? 0) > 5) {
    causes.add('possível diferença de filtro de período ou escopo');
  }

  if (metricResult.type !== 'count') {
    causes.add('verificar duplicidade ou regra de agregação');
  }

  return Array.from(causes);
}

function compareMetric(entity, metric, sqlRow, frontendRow, updateDiagnostic) {
  let backendValue = sqlRow?.[metric.sqlKey] ?? null;
  let frontendValue = frontendRow?.[metric.apiKey] ?? null;
  const toleranceAbs = resolveTolerance(metric);
  const rawBackendNumber = toNumber(backendValue);
  const rawFrontendNumber = toNumber(frontendValue);

  if ((backendValue === null || backendValue === undefined || backendValue === '') && rawFrontendNumber === 0) {
    backendValue = 0;
  }
  if ((frontendValue === null || frontendValue === undefined || frontendValue === '') && rawBackendNumber === 0) {
    frontendValue = 0;
  }

  const backendNumber = toNumber(backendValue);
  const frontendNumber = toNumber(frontendValue);

  if (backendNumber === null && frontendNumber === null) {
    return {
      ...metric,
      backendValue,
      frontendValue,
      diffAbs: 0,
      diffPct: 0,
      toleranceAbs,
      ok: true,
      status: 'OK',
      causes: [],
    };
  }

  if (backendNumber === null || frontendNumber === null) {
    const result = {
      ...metric,
      backendValue,
      frontendValue,
      diffAbs: null,
      diffPct: null,
      toleranceAbs,
      ok: false,
      status: 'DIVERGENTE',
    };
    return {
      ...result,
      causes: inferPossibleCauses(entity, result, updateDiagnostic, frontendRow),
    };
  }

  const diffAbs = round(frontendNumber - backendNumber, 4);
  const diffPct = backendNumber === 0
    ? (frontendNumber === 0 ? 0 : null)
    : round((diffAbs / Math.abs(backendNumber)) * 100, 4);
  const ok = Math.abs(diffAbs) <= toleranceAbs;

  const result = {
    ...metric,
    backendValue: backendNumber,
    frontendValue: frontendNumber,
    diffAbs,
    diffPct,
    toleranceAbs,
    ok,
    status: ok ? 'OK' : 'DIVERGENTE',
  };

  return {
    ...result,
      causes: inferPossibleCauses(entity, result, updateDiagnostic, frontendRow),
  };
}

function buildUpdateDiagnostic(entity, sqlRow, frontendRow) {
  const mapping = entity.updateMapping;
  if (!mapping?.sqlKey || !mapping?.apiKey) {
    return null;
  }

  const backendValue = sqlRow?.[mapping.sqlKey] ?? null;
  const frontendValue = frontendRow?.[mapping.apiKey] ?? null;
  const lagMinutes = diffMinutes(backendValue, frontendValue);
  const isDivergent = mapping.comparable && lagMinutes !== null && Math.abs(lagMinutes) > 1;

  return {
    comparable: Boolean(mapping.comparable),
    backendValue,
    frontendValue,
    lagMinutes,
    isDivergent,
  };
}

function summarize(resultsByEntity) {
  const metrics = resultsByEntity.flatMap((entity) => entity.metrics);
  const ok = metrics.filter((metric) => metric.ok).length;
  const divergent = metrics.length - ok;
  const criticalAlerts = metrics.filter((metric) => {
    if (metric.ok) {
      return false;
    }
    if (metric.critical) {
      return true;
    }
    return Math.abs(metric.diffPct ?? 0) > 5;
  });

  return {
    totalMetrics: metrics.length,
    ok,
    divergent,
    consistencyPct: metrics.length === 0 ? 0 : round((ok / metrics.length) * 100, 2),
    criticalAlerts,
  };
}

function renderEntitySection(entityResult) {
  const lines = [];
  lines.push(`## ${entityResult.label}`);
  lines.push('');
  lines.push(`Fonte frontend: \`${entityResult.apiPath}\` -> cards da tela \`${entityResult.uiPath}\`.`);
  if (entityResult.updateDiagnostic?.backendValue || entityResult.updateDiagnostic?.frontendValue) {
    const diag = entityResult.updateDiagnostic;
    const lagText = diag.lagMinutes === null ? 'n/d' : `${diag.lagMinutes > 0 ? '+' : ''}${formatNumber(diag.lagMinutes, 2)} min`;
    const comparability = diag.comparable ? 'comparável' : 'diagnóstico';
    lines.push(`Atualização (${comparability}): SQL ${formatTimestamp(diag.backendValue)} | Frontend ${formatTimestamp(diag.frontendValue)} | defasagem ${lagText}`);
  }
  lines.push('');
  lines.push('| Métrica | Card/Tela | Backend | Frontend | Diferença | Dif % | Status | Possíveis causas |');
  lines.push('| --- | --- | --- | --- | --- | --- | --- | --- |');

  for (const metric of entityResult.metrics) {
    const status = metric.ok ? '✅ OK' : '⚠️ Divergente';
    const causes = metric.causes.length > 0 ? metric.causes.join('; ') : '-';
    lines.push(`| ${metric.label} | ${metric.uiLabel} | ${formatMetricValue(metric.type, metric.backendValue)} | ${formatMetricValue(metric.type, metric.frontendValue)} | ${formatDiff(metric.diffAbs, metric.type)} | ${formatDiffPct(metric.diffPct)} | ${status} | ${causes} |`);
  }

  lines.push('');
  return lines.join('\n');
}

function renderSummarySection(summary) {
  const lines = [];
  lines.push('## Resumo Geral');
  lines.push('');
  lines.push(`- Total de métricas validadas: ${summary.totalMetrics}`);
  lines.push(`- Total OK: ${summary.ok}`);
  lines.push(`- Total divergentes: ${summary.divergent}`);
  lines.push(`- Consistência geral: ${formatNumber(summary.consistencyPct, 2)}%`);
  lines.push('');
  lines.push('## Alertas Críticos');
  lines.push('');

  if (summary.criticalAlerts.length === 0) {
    lines.push('- Nenhuma divergência crítica encontrada.');
    return lines.join('\n');
  }

  for (const alert of summary.criticalAlerts) {
    lines.push(`- ${alert.label}: backend ${formatMetricValue(alert.type, alert.backendValue)} | frontend ${formatMetricValue(alert.type, alert.frontendValue)} | dif ${formatDiff(alert.diffAbs, alert.type)} (${formatDiffPct(alert.diffPct)})`);
  }

  return lines.join('\n');
}

function renderMarkdownReport(context, entityResults, summary) {
  const header = [
    '# Validação de Consistência dos Dashboards',
    '',
    `- Período: ${context.period.dataInicio} a ${context.period.dataFim}`,
    `- Data de execução: ${context.executionTimestamp}`,
    `- Fonte frontend: API local consumida pela UI em ${context.apiBaseUrl}`,
    `- Banco: ${context.connection.database} em ${context.connection.server}`,
    `- Usuário técnico avaliado: ${context.apiUserEmail}`,
    '',
  ].join('\n');

  const sections = entityResults.map(renderEntitySection).join('\n');
  const summarySection = renderSummarySection(summary);

  return `${header}${sections}\n${summarySection}\n`;
}

function buildArtifactBaseName(period) {
  return `validacao-dashboard-${period.dataInicio}_${period.dataFim}`;
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const env = parseDotEnv(API_ENV_PATH);
  const connection = sqlConnectionFromEnv(env, args);
  const period = {
    dataInicio: args.dataInicio ?? '2026-03-01',
    dataFim: args.dataFim ?? '2026-03-31',
  };
  const apiBaseUrl = args.apiBaseUrl ?? 'http://localhost:5010';
  const apiUserEmail = resolveApiUserEmail(connection, args.apiUserEmail);
  const jwtSecret = args.jwtSecret ?? env.JWT_SECRET;

  if (!connection.user || !connection.password) {
    throw new Error('Credenciais de banco não encontradas em dashboard-api/.env.');
  }

  if (!jwtSecret) {
    throw new Error('JWT_SECRET não encontrado em dashboard-api/.env.');
  }

  await ensureApiUp(apiBaseUrl);
  const token = buildJwt(apiUserEmail, jwtSecret);

  const entityResults = [];
  for (const entity of ENTITIES) {
    const sqlRow = runSqlQuery(connection, entity.sql(period));
    const frontendRow = await fetchFrontendOverview(apiBaseUrl, token, entity, period);
    const updateDiagnostic = buildUpdateDiagnostic(entity, sqlRow, frontendRow);
    const metrics = entity.metrics.map((metric) => compareMetric(entity, metric, sqlRow, frontendRow, updateDiagnostic));

    entityResults.push({
      key: entity.key,
      label: entity.label,
      apiPath: entity.apiPath,
      uiPath: entity.uiPath,
      sqlRow,
      frontendRow,
      updateDiagnostic,
      metrics,
    });
  }

  const summary = summarize(entityResults);
  const executionTimestamp = new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'short',
    timeStyle: 'medium',
    hour12: false,
  }).format(new Date());

  const context = {
    apiBaseUrl,
    apiUserEmail,
    connection,
    executionTimestamp,
    period,
  };

  const markdown = renderMarkdownReport(context, entityResults, summary);
  const artifactBaseName = buildArtifactBaseName(period);
  const markdownPath = path.join(REPORTS_DIR, `${artifactBaseName}.md`);
  const jsonPath = path.join(REPORTS_DIR, `${artifactBaseName}.json`);

  mkdirSync(REPORTS_DIR, { recursive: true });
  writeFileSync(markdownPath, markdown, 'utf8');
  writeFileSync(jsonPath, JSON.stringify({ context, summary, entities: entityResults }, null, 2), 'utf8');

  console.log(JSON.stringify({
    summary,
    markdownPath,
    jsonPath,
  }, null, 2));
}

main().catch((error) => {
  console.error(error instanceof Error ? error.stack ?? error.message : String(error));
  process.exitCode = 1;
});
