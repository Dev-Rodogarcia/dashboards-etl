import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs';
import { dirname, extname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const uiRoot = resolve(scriptDir, '..');
const repoRoot = resolve(uiRoot, '..');

const scanRoots = [
  resolve(uiRoot, 'src'),
  resolve(uiRoot, 'index.html'),
  resolve(uiRoot, 'package.json'),
  resolve(uiRoot, 'scripts'),
  resolve(repoRoot, '.env.example'),
  resolve(repoRoot, '.editorconfig'),
  resolve(repoRoot, '.gitattributes'),
  resolve(repoRoot, '.vscode'),
  resolve(repoRoot, 'README.md'),
  resolve(repoRoot, 'backend', 'src', 'main', 'java', 'com', 'dashboard', 'api', 'controller'),
  resolve(repoRoot, 'backend', 'src', 'main', 'java', 'com', 'dashboard', 'api', 'dto', 'acesso'),
  resolve(repoRoot, 'backend', 'src', 'main', 'java', 'com', 'dashboard', 'api', 'model', 'acesso'),
  resolve(repoRoot, 'backend', 'src', 'main', 'java', 'com', 'dashboard', 'api', 'repository', 'acesso'),
  resolve(repoRoot, 'backend', 'src', 'main', 'java', 'com', 'dashboard', 'api', 'security'),
  resolve(repoRoot, 'backend', 'src', 'main', 'java', 'com', 'dashboard', 'api', 'service', 'acesso'),
  resolve(repoRoot, 'database'),
];

const allowedExtensions = new Set([
  '.bat',
  '.cmd',
  '.css',
  '.html',
  '.java',
  '.js',
  '.json',
  '.md',
  '.mjs',
  '.ps1',
  '.sql',
  '.ts',
  '.tsx',
  '.xml',
  '.yml',
  '.yaml',
]);

const ignoredDirectoryNames = new Set([
  '.git',
  'build',
  'coverage',
  'dist',
  'node_modules',
  'target',
]);

const suspiciousPattern = /(?:\u00C2[\u0080-\u00BF]|\u00C3[\u0080-\u00BF]|\uFFFD)/;
const decoder = new TextDecoder('utf-8', { fatal: true });
const issues = [];

function walk(currentPath) {
  const stats = statSync(currentPath);

  if (stats.isDirectory()) {
    const baseName = currentPath.split(/[\\/]/).pop();
    if (baseName && ignoredDirectoryNames.has(baseName)) {
      return;
    }

    for (const entry of readdirSync(currentPath)) {
      walk(join(currentPath, entry));
    }
    return;
  }

  if (!allowedExtensions.has(extname(currentPath))) {
    return;
  }

  const buffer = readFileSync(currentPath);
  const relativePath = relative(repoRoot, currentPath);

  let text;
  try {
    text = decoder.decode(buffer);
  } catch {
    issues.push(`${relativePath}: invalid UTF-8`);
    return;
  }

  if (!suspiciousPattern.test(text)) {
    return;
  }

  const lines = text.split(/\r?\n/);
  for (let index = 0; index < lines.length; index += 1) {
    if (suspiciousPattern.test(lines[index])) {
      issues.push(`${relativePath}:${index + 1}: suspicious mojibake pattern`);
    }
  }
}

for (const scanRoot of scanRoots) {
  if (!existsSync(scanRoot)) {
    continue;
  }
  walk(scanRoot);
}

if (issues.length > 0) {
  console.error('Encoding check failed.');
  for (const issue of issues) {
    console.error(`- ${issue}`);
  }
  process.exit(1);
}

console.log('Encoding check passed.');
