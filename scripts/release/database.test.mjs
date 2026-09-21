import { cpSync, mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

import { checkDatabaseManifest } from './database.mjs';
import { repositoryRoot } from './version.mjs';

describe('database release inventory', () => {
  it('verifies the baseline and rejects altered or unlisted SQL', () => {
    const root = mkdtempSync(resolve(tmpdir(), 'auth-database-manifest-'));
    try {
      mkdirSync(resolve(root, 'apps/server/deploy'), { recursive: true });
      for (const name of ['sql', 'migrations'])
        cpSync(
          resolve(repositoryRoot, 'apps/server/deploy', name),
          resolve(root, 'apps/server/deploy', name),
          { recursive: true },
        );
      expect(checkDatabaseManifest(root).baseline.id).toBe('baseline-1');
      writeFileSync(
        resolve(root, 'apps/server/deploy/migrations/unlisted.sql'),
        'SELECT 1;',
      );
      expect(() => checkDatabaseManifest(root)).toThrow(
        'missing from database manifest',
      );
      rmSync(resolve(root, 'apps/server/deploy/migrations/unlisted.sql'));
      writeFileSync(
        resolve(root, 'apps/server/deploy/sql/01_rbac.sql'),
        'SELECT 2;',
      );
      expect(() => checkDatabaseManifest(root)).toThrow('checksum mismatch');
    } finally {
      rmSync(root, { recursive: true, force: true });
    }
  });
});
