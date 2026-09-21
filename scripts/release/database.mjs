import { execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { readdirSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { parseVersion, readJson, repositoryRoot } from './version.mjs';

function fileChecksum(path) {
  return createHash('sha256').update(readFileSync(path)).digest('hex');
}

/** 数据库发布清单是兼容性契约；未知来源不能被隐式当作新库。 */
function checkDatabaseManifest(root = repositoryRoot) {
  const deploy = resolve(root, 'apps/server/deploy');
  const manifest = readJson(resolve(deploy, 'migrations/manifest.json'));
  if (!/^baseline-[1-9]\d*$/u.test(manifest.baseline.id))
    throw new Error('Invalid database baseline ID');
  if (!Array.isArray(manifest.supportedUpgradeFrom))
    throw new Error('Declare supportedUpgradeFrom explicitly');
  for (const version of manifest.supportedUpgradeFrom) parseVersion(version);
  const entries = [...manifest.baseline.files, ...manifest.migrations];
  const paths = new Set();
  const ids = new Set([manifest.baseline.id]);
  for (const entry of entries) {
    if (
      !/^(?:sql|migrations)\/[a-zA-Z\d_.-]+\.sql$/u.test(entry.path) ||
      paths.has(entry.path)
    )
      throw new Error(`Invalid/duplicate database path: ${entry.path}`);
    paths.add(entry.path);
    if (fileChecksum(resolve(deploy, entry.path)) !== entry.sha256)
      throw new Error(`Database checksum mismatch: ${entry.path}`);
  }
  let previousId = '';
  for (const migration of manifest.migrations) {
    if (
      !/^\d{14}_[a-z\d_]+$/u.test(migration.id) ||
      ids.has(migration.id) ||
      migration.id <= previousId
    )
      throw new Error(`Invalid/out-of-order migration ID: ${migration.id}`);
    if (!migration.path.startsWith('migrations/') || !migration.description)
      throw new Error(`Incomplete migration: ${migration.id}`);
    ids.add(migration.id);
    previousId = migration.id;
  }
  for (const directory of ['sql', 'migrations']) {
    for (const name of readdirSync(resolve(deploy, directory))) {
      if (name.endsWith('.sql') && !paths.has(`${directory}/${name}`))
        throw new Error(
          `SQL missing from database manifest: ${directory}/${name}`,
        );
    }
  }
  return manifest;
}

/** 已发布基线和迁移只能追加，不能通过改清单校验和掩盖历史 SQL 改动。 */
function checkHistoricalDatabase(root = repositoryRoot) {
  const current = checkDatabaseManifest(root);
  const entries = new Map(
    [...current.baseline.files, ...current.migrations].map((entry) => [
      entry.path,
      entry.sha256,
    ]),
  );
  const tags = execFileSync('git', ['tag', '--list', 'v*'], {
    cwd: root,
    encoding: 'utf8',
  })
    .trim()
    .split('\n')
    .filter(Boolean);
  for (const tag of tags) {
    const manifestPath = 'apps/server/deploy/migrations/manifest.json';
    const files = execFileSync(
      'git',
      ['ls-tree', '--name-only', tag, '--', manifestPath],
      { cwd: root, encoding: 'utf8' },
    );
    if (!files.trim()) continue;
    const historical = JSON.parse(
      execFileSync('git', ['show', `${tag}:${manifestPath}`], {
        cwd: root,
        encoding: 'utf8',
      }),
    );
    if (historical.baseline.id !== current.baseline.id)
      throw new Error(`Published database baseline changed since ${tag}`);
    for (const entry of [
      ...historical.baseline.files,
      ...historical.migrations,
    ]) {
      if (
        entries.get(entry.path) !== `${entry.id ?? 'baseline'}:${entry.sha256}`
      )
        throw new Error(
          `Published SQL changed or disappeared since ${tag}: ${entry.path}`,
        );
    }
  }
}

export { checkDatabaseManifest, checkHistoricalDatabase, fileChecksum };

if (
  process.argv[1] &&
  resolve(process.argv[1]) === fileURLToPath(import.meta.url)
) {
  try {
    const manifest = checkDatabaseManifest();
    checkHistoricalDatabase();
    console.log(
      `Database ${manifest.baseline.id}: ${manifest.baseline.files.length} baseline files, ${manifest.migrations.length} migrations verified.`,
    );
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}
