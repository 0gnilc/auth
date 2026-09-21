import { execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { readdirSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import {
  compareVersions,
  productVersion,
  readJson,
  repositoryRoot,
} from './version.mjs';

function gh(args) {
  return execFileSync('gh', args, { cwd: repositoryRoot, encoding: 'utf8' });
}

function checksum(path) {
  return createHash('sha256').update(readFileSync(path)).digest('hex');
}

function validateReleaseBundle(directory, version, revision) {
  const manifest = readJson(resolve(directory, 'release-manifest.json'));

  if (
    manifest.publishable !== true ||
    manifest.channel !== 'release' ||
    manifest.version !== version ||
    manifest.revision !== revision
  )
    throw new Error(
      'Downloaded bundle does not match the verified release commit',
    );
  const expectedFiles = new Set([
    `gnilc-auth-admin-${version}.zip`,
    `gnilc-auth-db-${version}.zip`,
    `gnilc-auth-server-${version}.jar`,
    'release-manifest.json',
    'RELEASE_NOTES.md',
    'SHA256SUMS',
  ]);
  const files = readdirSync(directory).toSorted((a, b) => a.localeCompare(b));
  if (
    files.length !== expectedFiles.size ||
    files.some((file) => !expectedFiles.has(file))
  )
    throw new Error('Unexpected or missing release assets');
  const checked = new Set();
  for (const line of readFileSync(resolve(directory, 'SHA256SUMS'), 'utf8')
    .trim()
    .split('\n')) {
    const match = /^([a-f\d]{64}) {2}([\w.-]+)$/u.exec(line);
    if (
      !match ||
      match[2] === 'SHA256SUMS' ||
      !expectedFiles.has(match[2]) ||
      checked.has(match[2])
    )
      throw new Error('Invalid release checksum list');
    if (checksum(resolve(directory, match[2])) !== match[1])
      throw new Error(`Asset checksum mismatch: ${match[2]}`);
    checked.add(match[2]);
  }
  if (checked.size !== files.length - 1)
    throw new Error('Incomplete release checksums');
  return files;
}

/** 上传完成后按 GitHub 返回的摘要复核附件全集，再允许草稿发布。 */
function validateUploadedAssets(assets, directory, files) {
  if (assets.length !== files.length)
    throw new Error('Remote draft has unexpected or missing assets');
  for (const name of files) {
    const asset = assets.find((item) => item.name === name);
    if (
      !asset ||
      asset.state !== 'uploaded' ||
      asset.digest !== `sha256:${checksum(resolve(directory, name))}`
    ) {
      throw new Error(`Remote draft asset was not verified: ${name}`);
    }
  }
}

function publishRelease() {
  if (
    process.env.GITHUB_ACTIONS !== 'true' ||
    process.env.GITHUB_REF !== 'refs/heads/main'
  )
    throw new Error(
      'Publication is only allowed through the manually dispatched main workflow',
    );
  const repo = process.env.GH_REPO;
  if (!/^[\w.-]+\/[\w.-]+$/u.test(repo ?? ''))
    throw new Error('GH_REPO must identify the release repository');
  // GitHub 的设置查询需要 Administration:read，默认 workflow token 不具备。
  // 管理员启用不可变发布后，在受保护的 release environment 中记录确认。
  if (process.env.RELEASE_IMMUTABILITY_CONFIRMED !== 'true') {
    throw new Error(
      'Enable immutable releases and confirm the release environment configuration first',
    );
  }
  const version = productVersion();
  const directory = resolve(repositoryRoot, '.release', version);
  const head = execFileSync('git', ['rev-parse', 'HEAD'], {
    encoding: 'utf8',
  }).trim();
  if (head !== process.env.RELEASE_REVISION)
    throw new Error('Unexpected publication checkout');
  const files = validateReleaseBundle(directory, version, head);
  const tag = `v${version}`;
  const releases = JSON.parse(
    gh(['api', `repos/${repo}/releases`, '--paginate', '--slurp']),
  ).flat();
  for (const release of releases.filter((item) => !item.draft)) {
    if (
      release.tag_name.startsWith('v') &&
      compareVersions(version, release.tag_name.slice(1)) <= 0
    )
      throw new Error(
        `Release must be newer than ${release.tag_name}; published releases cannot be overwritten`,
      );
  }
  let draft = releases.find((release) => release.tag_name === tag);
  const refs = execFileSync(
    'git',
    [
      'ls-remote',
      '--tags',
      'origin',
      `refs/tags/${tag}`,
      `refs/tags/${tag}^{}`,
    ],
    { encoding: 'utf8' },
  ).trim();
  if (refs) {
    const lines = refs.split('\n');
    const commit = (
      lines.find((line) => line.endsWith('^{}')) ?? lines[0]
    ).split(/\s/u)[0];
    if (commit !== head)
      throw new Error(`Tag ${tag} already points to another commit`);
  }
  if (draft && (!draft.draft || draft.target_commitish !== head))
    throw new Error('Existing release is not a draft for this exact commit');
  if (!draft) {
    gh([
      'release',
      'create',
      tag,
      '--repo',
      repo,
      '--target',
      head,
      '--draft',
      '--title',
      `Gnilc Auth ${version}`,
      '--notes-file',
      resolve(directory, 'RELEASE_NOTES.md'),
      ...(version.includes('-rc.') ? ['--prerelease'] : []),
    ]);
    draft = JSON.parse(
      gh(['api', `repos/${repo}/releases`, '--paginate', '--slurp']),
    )
      .flat()
      .find(
        (release) =>
          release.tag_name === tag &&
          release.draft &&
          release.target_commitish === head,
      );
    if (!draft) throw new Error('Created draft could not be verified');
  }
  // 失败重试只补传缺失产物；已上传内容必须逐项匹配，不使用 --clobber。
  for (const name of files) {
    const existing = draft.assets.find((asset) => asset.name === name);
    if (existing) {
      if (existing.digest !== `sha256:${checksum(resolve(directory, name))}`)
        throw new Error(
          `Draft asset differs: ${name}; inspect the failed release before retrying`,
        );
    } else
      gh(['release', 'upload', tag, resolve(directory, name), '--repo', repo]);
  }
  const uploaded = JSON.parse(
    gh(['api', `repos/${repo}/releases/${draft.id}`]),
  );
  validateUploadedAssets(uploaded.assets, directory, files);
  gh([
    'release',
    'edit',
    tag,
    '--repo',
    repo,
    '--draft=false',
    `--prerelease=${version.includes('-rc.')}`,
    `--latest=${!version.includes('-rc.')}`,
  ]);
  console.log(`Published ${repo} ${tag} at ${head}`);
}

export { validateReleaseBundle, validateUploadedAssets };

if (
  process.argv[1] &&
  resolve(process.argv[1]) === fileURLToPath(import.meta.url)
) {
  try {
    publishRelease();
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}
