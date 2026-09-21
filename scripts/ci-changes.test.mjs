import { Buffer } from 'node:buffer';

import { describe, expect, it } from 'vitest';

import {
  classifyChanges,
  formatGitHubOutputs,
  parseNameStatus,
  renderClassificationSummary,
} from './ci-changes.mjs';

function modified(...paths) {
  return paths.map((path) => ({ paths: [path], status: 'M' }));
}

function classifyScopes(changes, eventName = 'pull_request') {
  const { reason, ...scopes } = classifyChanges({ changes, eventName });
  return { reason, scopes };
}

function expectedScopes(overrides) {
  return {
    reason: 'classified',
    scopes: {
      admin: false,
      conservative: false,
      docsOnly: false,
      e2e: false,
      server: false,
      serverFull: false,
      tooling: false,
      ...overrides,
    },
  };
}

describe('ci change classification', () => {
  it('routes documentation-only changes away from application jobs', () => {
    expect(
      classifyChanges({
        changes: modified('README.md', 'docs/adr/README.md'),
        eventName: 'pull_request',
      }),
    ).toEqual({
      admin: false,
      conservative: false,
      docsOnly: true,
      e2e: false,
      reason: 'classified',
      server: false,
      serverFull: false,
      tooling: false,
    });
  });

  it.each([
    [
      'Admin production code',
      ['apps/admin/src/views/system/admin/index.vue'],
      { admin: true, e2e: true },
    ],
    [
      'Admin tests',
      ['apps/admin/src/views/system/admin/__tests__/index.test.ts'],
      { admin: true },
    ],
    [
      'shared frontend code',
      ['packages/utils/src/index.ts'],
      { admin: true, e2e: true, tooling: true },
    ],
    [
      'root dependency manifests',
      ['pnpm-lock.yaml'],
      {
        admin: true,
        e2e: true,
        server: true,
        serverFull: true,
        tooling: true,
      },
    ],
  ])('routes %s to its affected application checks', (_name, paths, scopes) => {
    expect(classifyScopes(modified(...paths))).toEqual(expectedScopes(scopes));
  });

  it('does not treat mixed documentation and application changes as docs-only', () => {
    expect(
      classifyScopes(
        modified('docs/test/test-strategy.md', 'apps/admin/src/main.ts'),
      ),
    ).toEqual(expectedScopes({ admin: true, e2e: true }));
  });

  it.each([
    [
      'a Service implementation',
      'apps/server/gnilc-core/src/main/java/com/gnilc/core/admin/service/AdminService.java',
      { server: true },
    ],
    [
      'a Controller',
      'apps/server/gnilc-core/src/main/java/com/gnilc/core/admin/controller/AdminController.java',
      { e2e: true, server: true },
    ],
    [
      'an HTTP response model',
      'apps/server/gnilc-core/src/main/java/com/gnilc/core/admin/entity/vo/AdminVo.java',
      { e2e: true, server: true },
    ],
    [
      'a Mapper',
      'apps/server/gnilc-core/src/main/java/com/gnilc/core/admin/dao/AdminMapper.java',
      { server: true, serverFull: true },
    ],
    [
      'cache behavior',
      'apps/server/gnilc-core/src/main/java/com/gnilc/core/admin/cache/AdminQueryCache.java',
      { server: true, serverFull: true },
    ],
    [
      'Server resources',
      'apps/server/gnilc-core/src/main/resources/mapper/AdminMapper.xml',
      { server: true, serverFull: true },
    ],
    [
      'integration-test resources',
      'apps/server/gnilc-core/src/test/resources/application-test.yml',
      { server: true, serverFull: true },
    ],
    [
      'Java configuration',
      'apps/server/gnilc-core/src/main/java/com/gnilc/core/config/SystemAutoConfiguration.java',
      { server: true, serverFull: true },
    ],
    [
      'Maven configuration',
      'apps/server/gnilc-core/pom.xml',
      { server: true, serverFull: true },
    ],
    [
      'an integration test',
      'apps/server/gnilc-core/src/test/java/com/gnilc/core/admin/dao/AdminMapperIT.java',
      { server: true, serverFull: true },
    ],
    [
      'shared test infrastructure',
      'apps/server/gnilc-common/gnilc-test-support/src/main/java/com/gnilc/test/api/ApiTest.java',
      { server: true, serverFull: true },
    ],
    [
      'application composition',
      'apps/server/gnilc-bootstrap/src/main/java/com/gnilc/core/AuthBootApplication.java',
      { e2e: true, server: true, serverFull: true },
    ],
    [
      'deployment schema',
      'apps/server/deploy/sql/07_rbac_admin.sql',
      { server: true, serverFull: true },
    ],
    [
      'an authorization boundary',
      'apps/server/gnilc-core/src/main/java/com/gnilc/core/authz/AdminRequiredRolePolicy.java',
      { e2e: true, server: true },
    ],
    [
      'Server-owned JavaScript tooling',
      'apps/server/scripts/tunnel.mjs',
      { server: true, tooling: true },
    ],
    [
      'Server build manifest',
      'apps/server/package.json',
      { server: true, serverFull: true, tooling: true },
    ],
  ])('routes %s to the required Server mode', (_name, path, scopes) => {
    expect(classifyScopes(modified(path))).toEqual(expectedScopes(scopes));
  });

  it('escalates an ordinary Server change to full verification on main', () => {
    expect(
      classifyScopes(
        modified(
          'apps/server/gnilc-core/src/main/java/com/gnilc/core/admin/service/AdminService.java',
        ),
        'push',
      ),
    ).toEqual(expectedScopes({ server: true, serverFull: true }));
  });

  it.each([
    ['tests/e2e/admin-smoke.spec.ts'],
    ['playwright.config.ts'],
    ['scripts/e2e-service.mjs'],
  ])('routes E2E infrastructure %s to browser tests', (path) => {
    expect(classifyScopes(modified(path))).toEqual(
      expectedScopes({ e2e: true, tooling: true }),
    );
  });

  it('classifies both sides of a rename', () => {
    expect(
      classifyScopes([
        {
          paths: [
            'apps/server/gnilc-core/src/main/java/com/gnilc/core/admin/service/OldService.java',
            'docs/old-service.md',
          ],
          status: 'R100',
        },
      ]),
    ).toEqual(expectedScopes({ server: true }));
  });

  it('parses modified, deleted, copied, and renamed paths from git name-status output', () => {
    expect(
      parseNameStatus(
        Buffer.from(
          [
            'M',
            'README.md',
            'D',
            'apps/admin/src/old.ts',
            'R100',
            'apps/server/Old.java',
            'apps/server/New.java',
            'C075',
            'packages/old.ts',
            'packages/new.ts',
            '',
          ].join('\0'),
        ),
      ),
    ).toEqual([
      { paths: ['README.md'], status: 'M' },
      { paths: ['apps/admin/src/old.ts'], status: 'D' },
      {
        paths: ['apps/server/Old.java', 'apps/server/New.java'],
        status: 'R100',
      },
      {
        paths: ['packages/old.ts', 'packages/new.ts'],
        status: 'C075',
      },
    ]);
  });

  it.each([
    [
      'unknown paths',
      {
        changes: modified('Dockerfile'),
        eventName: 'pull_request',
      },
      'unclassified paths: Dockerfile',
    ],
    [
      'unknown Server paths',
      {
        changes: modified('apps/server/Dockerfile'),
        eventName: 'pull_request',
      },
      'unclassified paths: apps/server/Dockerfile',
    ],
    [
      'an empty diff',
      { changes: [], eventName: 'pull_request' },
      'no changed files',
    ],
    [
      'an unsupported event',
      { changes: modified('README.md'), eventName: 'workflow_dispatch' },
      'unsupported event: workflow_dispatch',
    ],
    [
      'an initial push without a base SHA',
      {
        beforeSha: '0000000000000000000000000000000000000000',
        changes: modified('README.md'),
        eventName: 'push',
      },
      'push before SHA is unavailable',
    ],
  ])('uses conservative checks for %s', (_name, input, reason) => {
    expect(classifyChanges(input)).toEqual({
      admin: true,
      conservative: true,
      docsOnly: false,
      e2e: true,
      reason,
      server: true,
      serverFull: true,
      tooling: true,
    });
  });

  it('publishes stable GitHub outputs and a human-readable summary', () => {
    const classification = classifyChanges({
      changes: modified(
        'apps/server/gnilc-core/src/main/java/com/gnilc/core/admin/service/AdminService.java',
      ),
      eventName: 'pull_request',
    });

    expect(formatGitHubOutputs(classification)).toBe(
      [
        'admin=false',
        'conservative=false',
        'docs_only=false',
        'e2e=false',
        'server=true',
        'server_full=false',
        'tooling=false',
        'reason=classified',
        '',
      ].join('\n'),
    );
    const summary = renderClassificationSummary(classification);
    expect(summary).toContain('| Server | true |');
    expect(summary).toContain('| Server full verification | false |');
    expect(summary).toContain('**Reason:** classified');
  });
});
