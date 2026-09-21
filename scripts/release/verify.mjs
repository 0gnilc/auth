import { execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import {
  existsSync,
  mkdirSync,
  readdirSync,
  readFileSync,
  rmSync,
  writeFileSync,
} from 'node:fs';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { checkDatabaseManifest, checkHistoricalDatabase } from './database.mjs';
import { checkVersions, repositoryRoot } from './version.mjs';

function git(args, root = repositoryRoot) {
  return execFileSync('git', args, { cwd: root, encoding: 'utf8' }).trim();
}

/** 包括未提交文件的内容快照；构建后若源码发生变化，验收凭据立即失效。 */
function sourceFingerprint(root = repositoryRoot) {
  const files = git(
    ['ls-files', '--cached', '--others', '--exclude-standard', '-z'],
    root,
  )
    .split('\0')
    .filter(Boolean);
  const hash = createHash('sha256');
  for (const file of [...new Set(files)].toSorted((a, b) =>
    a.localeCompare(b),
  )) {
    hash.update(file).update('\0');
    const path = resolve(root, file);
    hash.update(existsSync(path) ? readFileSync(path) : '<deleted>');
    hash.update('\0');
  }
  return hash.digest('hex');
}

/** 固定已通过验收的构建字节，拒绝验收后替换/重新构建的产物。 */
function artifactFingerprint(path) {
  const hash = createHash('sha256');
  function visit(directory) {
    for (const entry of readdirSync(directory, {
      withFileTypes: true,
    }).toSorted((a, b) => a.name.localeCompare(b.name))) {
      const item = resolve(directory, entry.name);
      if (entry.isSymbolicLink())
        throw new Error(`Unexpected artifact symlink: ${item}`);
      if (entry.isDirectory()) visit(item);
      else
        hash
          .update(item.slice(path.length))
          .update('\0')
          .update(readFileSync(item));
    }
  }
  visit(path);
  return hash.digest('hex');
}

function releaseNotes(version, root = repositoryRoot) {
  const changelog = readFileSync(resolve(root, 'CHANGELOG.md'), 'utf8');
  const marker = `## ${version}\n`;
  const start = changelog.indexOf(marker);
  if (start === -1)
    throw new Error(`CHANGELOG.md has no release entry for ${version}`);
  const content = changelog
    .slice(start + marker.length)
    .split('\n## ')[0]
    .trim();
  if (!content || /TODO|TBD|待填写/u.test(content))
    throw new Error('Release notes must be complete');
  const guide = readFileSync(
    resolve(root, `docs/release/${version}.md`),
    'utf8',
  );
  for (const heading of ['升级来源', '数据库', '停机与会话', '回滚']) {
    if (!guide.includes(`## ${heading}`))
      throw new Error(`Upgrade guide must document ${heading}`);
  }
  if (/TODO|TBD|待填写/u.test(guide))
    throw new Error('Upgrade guide must be complete');
  return `${content}\n\n${guide}`;
}

function run(command, args, env) {
  execFileSync(command, args, { cwd: repositoryRoot, env, stdio: 'inherit' });
}

function verifyRelease(local = false) {
  const version = checkVersions();
  releaseNotes(version);
  checkDatabaseManifest();
  checkHistoricalDatabase();
  rmSync(resolve(repositoryRoot, '.release/verified.json'), { force: true });
  if (!local && git(['status', '--porcelain']))
    throw new Error(
      'Release verification requires a clean checkout; use --local for a non-publishable rehearsal',
    );
  const revision = git(['rev-parse', 'HEAD']);
  const fingerprint = sourceFingerprint();
  const environment = {
    ...process.env,
    GNILC_BUILD_CHANNEL: local ? 'development' : 'release',
    GNILC_BUILD_REVISION: revision,
    SOURCE_DATE_EPOCH: git(['show', '-s', '--format=%ct', 'HEAD']),
  };
  run('pnpm', ['test:workspace'], process.env);
  run('pnpm', ['check:type'], process.env);
  run('pnpm', ['test:e2e'], process.env);
  run('pnpm', ['build:admin'], environment);
  run(
    'mvn',
    [
      '--batch-mode',
      '--no-transfer-progress',
      '-f',
      'apps/server/pom.xml',
      `-Dbuild.revision=${revision}`,
      `-Dbuild.channel=${environment.GNILC_BUILD_CHANNEL}`,
      'clean',
      'verify',
    ],
    environment,
  );
  if (sourceFingerprint() !== fingerprint)
    throw new Error('Source changed during release verification');
  const directory = resolve(repositoryRoot, '.release');
  mkdirSync(directory, { recursive: true });
  // 仅成功完成所有测试后写凭据，发布打包会再次核对源码与构建标识。
  const adminFingerprint = artifactFingerprint(
    resolve(repositoryRoot, 'apps/admin/dist'),
  );
  const serverChecksum = createHash('sha256')
    .update(
      readFileSync(
        resolve(
          repositoryRoot,
          `apps/server/gnilc-bootstrap/target/gnilc-bootstrap-${version}-exec.jar`,
        ),
      ),
    )
    .digest('hex');
  writeFileSync(
    resolve(directory, 'verified.json'),
    `${JSON.stringify({ version, revision, fingerprint, adminFingerprint, serverChecksum, publishable: !local, verifiedAt: new Date().toISOString() }, null, 2)}\n`,
  );
  console.log(
    `Verified ${version}${local ? ' (local rehearsal; publication disabled)' : ''}`,
  );
}

export {
  artifactFingerprint,
  git,
  releaseNotes,
  sourceFingerprint,
  verifyRelease,
};

if (
  process.argv[1] &&
  resolve(process.argv[1]) === fileURLToPath(import.meta.url)
) {
  try {
    const args = process.argv.slice(2);
    if (args.length > 0 && (args.length !== 1 || args[0] !== '--local'))
      throw new Error('Usage: verify.mjs [--local]');
    verifyRelease(args[0] === '--local');
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}
