import { appendFileSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

function createClassification(overrides = {}) {
  return {
    admin: false,
    conservative: false,
    docsOnly: false,
    e2e: false,
    reason: 'classified',
    server: false,
    serverFull: false,
    tooling: false,
    ...overrides,
  };
}

function createConservativeClassification(reason) {
  return createClassification({
    admin: true,
    conservative: true,
    e2e: true,
    reason,
    server: true,
    serverFull: true,
    tooling: true,
  });
}

function isDocumentationPath(path) {
  return path.endsWith('.md') || path === 'LICENSE';
}

function isTestPath(path) {
  return (
    path.includes('/__tests__/') ||
    path.includes('/src/test/') ||
    /(?:^|\/)\w[^/]*\.(?:spec|test)\.[^/]+$/u.test(path)
  );
}

function isBroadToolingPath(path) {
  return (
    new Set([
      'package.json',
      'pnpm-lock.yaml',
      'pnpm-workspace.yaml',
      'turbo.json',
      'vitest.config.ts',
    ]).has(path) ||
    path.startsWith('.github/workflows/') ||
    (path.startsWith('scripts/') && !isE2eToolingPath(path))
  );
}

function isE2eToolingPath(path) {
  return (
    path.startsWith('tests/e2e/') ||
    path === 'playwright.config.ts' ||
    /^scripts\/e2e(?:-|\.)/u.test(path)
  );
}

function isServerPath(path) {
  return path.startsWith('apps/server/');
}

function isServerToolingPath(path) {
  return (
    path === 'apps/server/package.json' ||
    path.startsWith('apps/server/scripts/')
  );
}

function isRecognizedServerPath(path) {
  return (
    isDocumentationPath(path) ||
    path.startsWith('apps/server/deploy/') ||
    path.endsWith('/pom.xml') ||
    isServerToolingPath(path) ||
    /\/src\/(?:main|test)\/(?:java|resources)\//u.test(path)
  );
}

function requiresFullServerVerification(path) {
  return (
    path.startsWith('apps/server/deploy/') ||
    path.startsWith('apps/server/gnilc-bootstrap/') ||
    path.startsWith('apps/server/gnilc-common/gnilc-test-support/') ||
    path === 'apps/server/package.json' ||
    path === 'apps/server/scripts/maven.mjs' ||
    path.endsWith('/pom.xml') ||
    path.includes('/src/main/resources/') ||
    path.includes('/src/test/resources/') ||
    (path.includes('/src/main/java/') &&
      (path.includes('/config/') || path.includes('/configuration/'))) ||
    path.includes('/dao/') ||
    path.includes('/cache/') ||
    /\/src\/test\/.*IT\.java$/u.test(path)
  );
}

function affectsPublicServerBoundary(path) {
  return (
    !isTestPath(path) &&
    (path.startsWith('apps/server/gnilc-bootstrap/') ||
      path.includes('/controller/') ||
      path.includes('/entity/dto/') ||
      path.includes('/entity/vo/') ||
      path.includes('/exception/') ||
      path.includes('/auth/') ||
      path.includes('/authz/') ||
      path.includes('/session/'))
  );
}

function classifyChanges({ beforeSha, changes, eventName }) {
  if (!['pull_request', 'push'].includes(eventName)) {
    return createConservativeClassification(`unsupported event: ${eventName}`);
  }
  if (
    eventName === 'push' &&
    beforeSha !== undefined &&
    /^0+$/u.test(beforeSha)
  ) {
    return createConservativeClassification('push before SHA is unavailable');
  }
  if (changes.length === 0) {
    return createConservativeClassification('no changed files');
  }

  const paths = changes.flatMap((change) => change.paths);
  const docsOnly =
    paths.length > 0 && paths.every((path) => isDocumentationPath(path));
  if (docsOnly) return createClassification({ docsOnly: true });

  const broadTooling = paths.some((path) => isBroadToolingPath(path));
  if (broadTooling) {
    return createClassification({
      admin: true,
      e2e: true,
      server: true,
      serverFull: true,
      tooling: true,
    });
  }

  const admin = paths.some((path) => path.startsWith('apps/admin/'));
  const sharedFrontend = paths.some(
    (path) => path.startsWith('packages/') || path.startsWith('internal/'),
  );
  const e2e = paths.some(
    (path) =>
      (path.startsWith('apps/admin/src/') && !isTestPath(path)) ||
      ((path.startsWith('packages/') || path.startsWith('internal/')) &&
        !isTestPath(path)),
  );
  const server = paths.some((path) => isServerPath(path));
  const serverFull =
    server &&
    (eventName === 'push' ||
      paths.some((path) => requiresFullServerVerification(path)));
  const serverE2e = paths.some((path) => affectsPublicServerBoundary(path));
  const serverTooling = paths.some((path) => isServerToolingPath(path));
  const e2eTooling = paths.some((path) => isE2eToolingPath(path));
  const recognizedPaths = paths.filter(
    (path) =>
      isDocumentationPath(path) ||
      isBroadToolingPath(path) ||
      isE2eToolingPath(path) ||
      path.startsWith('apps/admin/') ||
      (isServerPath(path) && isRecognizedServerPath(path)) ||
      path.startsWith('packages/') ||
      path.startsWith('internal/'),
  );
  if (recognizedPaths.length !== paths.length) {
    const unclassified = paths.filter(
      (path) => !recognizedPaths.includes(path),
    );
    return createConservativeClassification(
      `unclassified paths: ${[...new Set(unclassified)].join(', ')}`,
    );
  }

  return createClassification({
    admin: admin || sharedFrontend,
    e2e: e2e || serverE2e || e2eTooling,
    server,
    serverFull,
    tooling: sharedFrontend || e2eTooling || serverTooling,
  });
}

function parseNameStatus(buffer) {
  const fields = buffer.toString('utf8').split('\0');
  const changes = [];
  let index = 0;
  while (index < fields.length && fields[index]) {
    const status = fields[index++];
    const pathCount = /^[CR]/u.test(status) ? 2 : 1;
    const paths = fields.slice(index, index + pathCount);
    if (paths.length !== pathCount || paths.some((path) => !path)) {
      throw new Error(`Incomplete git name-status entry for ${status}`);
    }
    changes.push({ paths, status });
    index += pathCount;
  }
  return changes;
}

function sanitizeOutputValue(value) {
  return String(value).replaceAll(/[\r\n]+/gu, ' ');
}

function formatGitHubOutputs(classification) {
  return [
    `admin=${classification.admin}`,
    `conservative=${classification.conservative}`,
    `docs_only=${classification.docsOnly}`,
    `e2e=${classification.e2e}`,
    `server=${classification.server}`,
    `server_full=${classification.serverFull}`,
    `tooling=${classification.tooling}`,
    `reason=${sanitizeOutputValue(classification.reason)}`,
    '',
  ].join('\n');
}

function renderClassificationSummary(classification) {
  const rows = [
    ['Documentation only', classification.docsOnly],
    ['Admin', classification.admin],
    ['Server', classification.server],
    ['Server full verification', classification.serverFull],
    ['E2E', classification.e2e],
    ['Tooling', classification.tooling],
    ['Conservative fallback', classification.conservative],
  ];
  return [
    '## CI change classification',
    '',
    '| Scope | Selected |',
    '| --- | --- |',
    ...rows.map(([label, selected]) => `| ${label} | ${selected} |`),
    '',
    `**Reason:** ${sanitizeOutputValue(classification.reason)}`,
    '',
  ].join('\n');
}

function readOption(args, name) {
  const index = args.indexOf(name);
  return index === -1 ? undefined : args[index + 1];
}

function runCli({
  args = process.argv.slice(2),
  environment = process.env,
} = {}) {
  const eventName = readOption(args, '--event');
  const beforeSha = readOption(args, '--before');
  const uncertainReason = readOption(args, '--uncertain');
  let classification;
  try {
    classification = uncertainReason
      ? createConservativeClassification(uncertainReason)
      : classifyChanges({
          beforeSha,
          changes: parseNameStatus(readFileSync(0)),
          eventName,
        });
  } catch (error) {
    classification = createConservativeClassification(
      `classification failed: ${error.message}`,
    );
  }

  const outputs = formatGitHubOutputs(classification);
  const summary = renderClassificationSummary(classification);
  if (environment.GITHUB_OUTPUT) {
    appendFileSync(environment.GITHUB_OUTPUT, outputs, 'utf8');
  } else {
    process.stdout.write(`${JSON.stringify(classification)}\n`);
  }
  if (environment.GITHUB_STEP_SUMMARY) {
    appendFileSync(environment.GITHUB_STEP_SUMMARY, summary, 'utf8');
  }
  return classification;
}

export {
  classifyChanges,
  formatGitHubOutputs,
  parseNameStatus,
  renderClassificationSummary,
  runCli,
};

if (
  process.argv[1] &&
  resolve(process.argv[1]) === fileURLToPath(import.meta.url)
) {
  runCli();
}
