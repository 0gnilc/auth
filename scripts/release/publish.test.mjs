import { createHash } from 'node:crypto';
import {
  mkdtempSync,
  readdirSync,
  readFileSync,
  rmSync,
  writeFileSync,
} from 'node:fs';
import { tmpdir } from 'node:os';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

import { validateReleaseBundle, validateUploadedAssets } from './publish.mjs';

function bundle() {
  const directory = mkdtempSync(resolve(tmpdir(), 'auth-publish-'));
  const version = '1.0.0-rc.1';
  const revision = 'a'.repeat(40);
  for (const name of [
    `gnilc-auth-admin-${version}.zip`,
    `gnilc-auth-server-${version}.jar`,
    `gnilc-auth-db-${version}.zip`,
    'RELEASE_NOTES.md',
  ])
    writeFileSync(resolve(directory, name), `verified-${name}`);
  writeFileSync(
    resolve(directory, 'release-manifest.json'),
    JSON.stringify({
      version,
      revision,
      channel: 'release',
      publishable: true,
    }),
  );
  const checksums = readdirSync(directory)
    .map(
      (name) =>
        `${createHash('sha256')
          .update(readFileSync(resolve(directory, name)))
          .digest('hex')}  ${name}`,
    )
    .join('\n');
  writeFileSync(resolve(directory, 'SHA256SUMS'), `${checksums}\n`);
  return { directory, version, revision };
}

describe('publication preconditions', () => {
  it('accepts a complete bundle and rejects replacement bytes', () => {
    const { directory, version, revision } = bundle();
    try {
      expect(validateReleaseBundle(directory, version, revision)).toHaveLength(
        6,
      );
      writeFileSync(
        resolve(directory, `gnilc-auth-server-${version}.jar`),
        'unverified rebuild',
      );
      expect(() => validateReleaseBundle(directory, version, revision)).toThrow(
        'Asset checksum mismatch',
      );
    } finally {
      rmSync(directory, { recursive: true, force: true });
    }
  });
  it('rejects incomplete or mismatched remote uploads before publication', () => {
    const { directory, version, revision } = bundle();
    try {
      const files = validateReleaseBundle(directory, version, revision);
      const assets = files.map((name) => ({
        name,
        state: 'uploaded',
        digest: `sha256:${createHash('sha256')
          .update(readFileSync(resolve(directory, name)))
          .digest('hex')}`,
      }));
      expect(() =>
        validateUploadedAssets(assets, directory, files),
      ).not.toThrow();
      expect(() =>
        validateUploadedAssets(assets.slice(1), directory, files),
      ).toThrow('missing assets');
      assets[0].digest = `sha256:${'f'.repeat(64)}`;
      expect(() => validateUploadedAssets(assets, directory, files)).toThrow(
        'not verified',
      );
    } finally {
      rmSync(directory, { recursive: true, force: true });
    }
  });
  it('rejects missing checksums, mismatched commits and rehearsal bundles', () => {
    const { directory, version, revision } = bundle();
    try {
      expect(() =>
        validateReleaseBundle(directory, version, 'b'.repeat(40)),
      ).toThrow('verified release commit');
      const checksumPath = resolve(directory, 'SHA256SUMS');
      writeFileSync(
        checksumPath,
        readFileSync(checksumPath, 'utf8').split('\n').slice(1).join('\n'),
      );
      expect(() => validateReleaseBundle(directory, version, revision)).toThrow(
        'Incomplete release checksums',
      );
      writeFileSync(
        resolve(directory, 'release-manifest.json'),
        JSON.stringify({
          version,
          revision,
          channel: 'development',
          publishable: false,
        }),
      );
      expect(() => validateReleaseBundle(directory, version, revision)).toThrow(
        'verified release commit',
      );
    } finally {
      rmSync(directory, { recursive: true, force: true });
    }
  });
});
