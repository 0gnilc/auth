import { execFileSync } from 'node:child_process';
import {
  mkdirSync,
  mkdtempSync,
  readFileSync,
  rmSync,
  writeFileSync,
} from 'node:fs';
import { tmpdir } from 'node:os';
import { resolve } from 'node:path';

import { afterEach, describe, expect, it } from 'vitest';

import { assertBuildIdentity } from './bundle.mjs';
import {
  checkVersions,
  compareVersions,
  parseVersion,
  prepareRelease,
  synchronizePom,
  syncVersions,
} from './version.mjs';

const temporaryDirectories = [];
function fixture() {
  const root = mkdtempSync(resolve(tmpdir(), 'auth-version-'));
  temporaryDirectories.push(root);
  mkdirSync(resolve(root, 'apps/admin'), { recursive: true });
  mkdirSync(resolve(root, 'apps/server/core'), { recursive: true });
  writeFileSync(
    resolve(root, 'pnpm-workspace.yaml'),
    'packages:\n  - apps/*\n',
  );
  writeFileSync(
    resolve(root, 'package.json'),
    JSON.stringify({ name: 'fixture', private: true, version: '1.0.0-rc.1' }),
  );
  writeFileSync(
    resolve(root, 'apps/admin/package.json'),
    JSON.stringify({ name: '@fixture/admin', version: '5.7.0' }),
  );
  writeFileSync(
    resolve(root, 'apps/server/pom.xml'),
    '<project><artifactId>gnilc-auth-parent</artifactId><version>1.0.0</version><modules><module>core</module></modules></project>',
  );
  writeFileSync(
    resolve(root, 'apps/server/core/pom.xml'),
    '<project><parent><groupId>com.gnilc</groupId><artifactId>gnilc-auth-parent</artifactId><version>1.0.0</version></parent><dependencies><dependency><groupId>com.gnilc</groupId><artifactId>gnilc-auth-rbac</artifactId><version>1.0.0</version></dependency><dependency><groupId>third.party</groupId><version>5.7.0</version></dependency></dependencies></project>',
  );
  return root;
}

afterEach(() => {
  for (const path of temporaryDirectories.splice(0))
    rmSync(path, { recursive: true, force: true });
});

describe('product version contract', () => {
  it.each([
    '01.0.0',
    '1.0',
    'v1.0.0',
    '1.0.0-rc.0',
    '1.0.0+sha',
    '1.0.0-beta.1',
  ])('rejects unsupported version %s', (version) => {
    expect(() => parseVersion(version)).toThrow('Invalid product version');
  });

  it('orders RCs numerically and below the final release', () => {
    expect(compareVersions('1.0.0-rc.10', '1.0.0-rc.2')).toBe(1);
    expect(compareVersions('1.0.0', '1.0.0-rc.10')).toBe(1);
    expect(compareVersions('1.0.1-rc.1', '1.0.0')).toBe(1);
  });

  it('finds reactor/workspace drift, synchronizes it, and preserves third-party versions', () => {
    const root = fixture();
    expect(() => checkVersions(root)).toThrow('drift');
    expect(syncVersions(root)).toBe(3);
    expect(checkVersions(root)).toBe('1.0.0-rc.1');
    expect(syncVersions(root)).toBe(0);
    const pom = readFileSync(resolve(root, 'apps/server/core/pom.xml'), 'utf8');
    expect(pom).toContain(`<version>\${project.version}</version>`);
    expect(pom).toContain(
      '<groupId>third.party</groupId><version>5.7.0</version>',
    );
    const admin = JSON.parse(
      readFileSync(resolve(root, 'apps/admin/package.json'), 'utf8'),
    );
    expect(admin.private).toBe(true);
    admin.version = '1.0.1';
    writeFileSync(
      resolve(root, 'apps/admin/package.json'),
      JSON.stringify(admin),
    );
    expect(() => checkVersions(root)).toThrow('apps/admin/package.json');
  }, 30_000);

  it('prepares a release only after notes exist and archives Unreleased', () => {
    const root = fixture();
    execFileSync('git', ['init', '--quiet'], { cwd: root });
    syncVersions(root);
    writeFileSync(
      resolve(root, 'CHANGELOG.md'),
      '# Changelog\n\n## Unreleased\n\n- Compatible change.\n\n## 1.0.0-rc.1\n\n- Baseline.\n',
    );
    expect(() => prepareRelease(root, '1.0.0')).toThrow(
      'upgrade/rollback guide',
    );
    expect(checkVersions(root)).toBe('1.0.0-rc.1');
    mkdirSync(resolve(root, 'docs/release'), { recursive: true });
    writeFileSync(resolve(root, 'docs/release/1.0.0.md'), '# Upgrade guide');
    prepareRelease(root, '1.0.0');
    expect(checkVersions(root)).toBe('1.0.0');
    expect(readFileSync(resolve(root, 'CHANGELOG.md'), 'utf8')).toContain(
      '## Unreleased\n\n## 1.0.0\n\n- Compatible change.',
    );
    expect(() => prepareRelease(root, '1.0.0')).toThrow('must be newer');
  }, 30_000);

  it('fails rather than silently skipping an unrecognized parent', () => {
    expect(() => synchronizePom('<project/>', '1.0.0', false)).toThrow(
      'Unrecognized',
    );
  });

  it('rejects mixed front/back artifacts even if the product version matches', () => {
    const expected = {
      version: '1.0.0',
      revision: 'a'.repeat(40),
      channel: 'release',
    };
    expect(() =>
      assertBuildIdentity({ ...expected, revision: 'b'.repeat(40) }, expected),
    ).toThrow('revision');
    expect(() =>
      assertBuildIdentity({ ...expected, channel: 'development' }, expected),
    ).toThrow('channel');
    expect(() => assertBuildIdentity(expected, expected)).not.toThrow();
  });
});
