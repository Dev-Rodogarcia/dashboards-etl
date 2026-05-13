import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import handler from 'serve-handler';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const publicDir = path.resolve(__dirname, '..', 'dist');
const buildInfoPath = path.join(publicDir, 'build-info.json');
const host = process.env.FRONTEND_HOST || '127.0.0.1';
const port = Number(process.env.FRONTEND_PORT || 5173);
const buildId = readBuildId();

const blockedDevPaths = [
  /^\/@vite(?:\/|$)/,
  /^\/@react-refresh(?:\/|$)/,
  /^\/src(?:\/|$)/,
  /^\/node_modules(?:\/|$)/,
  /^\/@fs(?:\/|$)/,
  /^\/vite\.svg$/,
];

function requestPath(url) {
  try {
    return new URL(url, `http://${host}:${port}`).pathname;
  } catch {
    return '/';
  }
}

function readBuildId() {
  try {
    const parsed = JSON.parse(fs.readFileSync(buildInfoPath, 'utf8'));
    return typeof parsed.buildId === 'string' && parsed.buildId.trim()
      ? parsed.buildId.trim()
      : 'unknown';
  } catch {
    return 'unknown';
  }
}

function applySecurityHeaders(request, response) {
  const pathname = requestPath(request.url);
  response.setHeader('X-Content-Type-Options', 'nosniff');
  response.setHeader('X-Dashboard-Build-Id', buildId);
  response.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains; preload');
  response.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');
  response.setHeader('Permissions-Policy', 'camera=(), microphone=(), geolocation=()');
  response.setHeader(
    'Content-Security-Policy',
    "default-src 'self'; script-src 'self'; connect-src 'self' https://api-analytics.rodogarcia.com.br; img-src 'self' data:; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' data: https://fonts.gstatic.com; object-src 'none'; base-uri 'self'; frame-ancestors 'none'",
  );

  if (pathname.startsWith('/assets/')) {
    response.setHeader('Cache-Control', 'public, max-age=31536000, immutable');
  } else {
    response.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, proxy-revalidate');
    response.setHeader('CDN-Cache-Control', 'no-store');
    response.setHeader('Cloudflare-CDN-Cache-Control', 'no-store');
    response.setHeader('Surrogate-Control', 'no-store');
    response.setHeader('Pragma', 'no-cache');
    response.setHeader('Expires', '0');
  }
}

const server = http.createServer((request, response) => {
  const pathname = requestPath(request.url);
  applySecurityHeaders(request, response);

  if (blockedDevPaths.some((pattern) => pattern.test(pathname))) {
    response.statusCode = 404;
    response.setHeader('Content-Type', 'text/plain; charset=utf-8');
    response.end('Not found');
    return;
  }

  return handler(request, response, {
    public: publicDir,
    directoryListing: false,
    rewrites: [
      { source: '/**', destination: '/index.html' },
    ],
  });
});

server.listen(port, host, () => {
  console.log(`[OK] Frontend estatico servido em http://${host}:${port} | build=${buildId}`);
});
