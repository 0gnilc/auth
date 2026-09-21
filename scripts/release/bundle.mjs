import { execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import {
  cpSync,
  existsSync,
  mkdirSync,
  readdirSync,
  readFileSync,
  writeFileSync,
} from 'node:fs';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import {
  artifactFingerprint,
  git,
  releaseNotes,
  sourceFingerprint,
} from './verify.mjs';
import { checkVersions, readJson, repositoryRoot } from './version.mjs';

function sha256(path) {
  return createHash('sha256').update(readFileSync(path)).digest('hex');
}

function assertBuildIdentity(info, expected) {
  for (const key of ['version', 'revision', 'channel']) {
    if (info[key] !== expected[key])
      throw new Error(
        `Artifact ${key} mismatch: ${info[key]} != ${expected[key]}`,
      );
  }
}

function bundleRelease(local = false) {
  const version = checkVersions();
  const receipt = readJson(resolve(repositoryRoot, '.release/verified.json'));
  if (
    receipt.version !== version ||
    receipt.revision !== git(['rev-parse', 'HEAD']) ||
    receipt.fingerprint !== sourceFingerprint()
  )
    throw new Error('Release verification is stale; run release:verify again');
  if (!local && (!receipt.publishable || git(['status', '--porcelain'])))
    throw new Error('Only a verified clean release build may be published');
  const expected = {
    version,
    revision: receipt.revision,
    channel: receipt.publishable ? 'release' : 'development',
  };
  const admin = resolve(repositoryRoot, 'apps/admin/dist');
  const jar = resolve(
    repositoryRoot,
    `apps/server/gnilc-bootstrap/target/gnilc-bootstrap-${version}-exec.jar`,
  );
  if (
    artifactFingerprint(admin) !== receipt.adminFingerprint ||
    sha256(jar) !== receipt.serverChecksum
  )
    throw new Error('Artifacts changed after verification');
  assertBuildIdentity(readJson(resolve(admin, 'build-info.json')), expected);
  const properties = execFileSync(
    'unzip',
    ['-p', jar, 'META-INF/build-info.properties'],
    { encoding: 'utf8' },
  );
  const serverInfo = Object.fromEntries(
    properties
      .split('\n')
      .filter((line) => line.startsWith('build.'))
      .map((line) => {
        const index = line.indexOf('=');
        return [
          line.slice(6, index),
          line.slice(index + 1).replaceAll(String.raw`\:`, ':'),
        ];
      }),
  );
  assertBuildIdentity(serverInfo, expected);
  const output = resolve(
    repositoryRoot,
    '.release',
    `${version}${local ? '-local' : ''}`,
  );
  if (existsSync(output))
    throw new Error(
      `Bundle already exists: ${output}; inspect it before removing or retrying`,
    );
  mkdirSync(output, { recursive: true });
  const jarName = `gnilc-auth-server-${version}.jar`;
  cpSync(jar, resolve(output, jarName));
  execFileSync(
    'zip',
    ['-q', '-r', resolve(output, `gnilc-auth-admin-${version}.zip`), '.'],
    { cwd: admin },
  );
  const deploy = resolve(repositoryRoot, 'apps/server/deploy');
  execFileSync(
    'zip',
    [
      '-q',
      '-r',
      resolve(output, `gnilc-auth-db-${version}.zip`),
      'sql',
      'migrations',
    ],
    { cwd: deploy },
  );
  const assets = readdirSync(output)
    .toSorted((a, b) => a.localeCompare(b))
    .map((name) => ({ name, sha256: sha256(resolve(output, name)) }));
  const manifest = {
    ...expected,
    publishable: !local && receipt.publishable,
    verifiedAt: receipt.verifiedAt,
    workflowRun: process.env.GITHUB_RUN_ID ?? null,
    database: readJson(resolve(deploy, 'migrations/manifest.json')),
    assets,
  };
  writeFileSync(
    resolve(output, 'release-manifest.json'),
    `${JSON.stringify(manifest, null, 2)}\n`,
  );
  writeFileSync(
    resolve(output, 'RELEASE_NOTES.md'),
    `${releaseNotes(version)}\n`,
  );
  const checksums = readdirSync(output)
    .toSorted((a, b) => a.localeCompare(b))
    .map((name) => `${sha256(resolve(output, name))}  ${name}`)
    .join('\n');
  writeFileSync(resolve(output, 'SHA256SUMS'), `${checksums}\n`);
  console.log(output);
  return output;
}

export { assertBuildIdentity, bundleRelease, sha256 };

if (
  process.argv[1] &&
  resolve(process.argv[1]) === fileURLToPath(import.meta.url)
) {
  try {
    const args = process.argv.slice(2);
    if (args.length > 0 && (args.length !== 1 || args[0] !== '--local'))
      throw new Error('Usage: bundle.mjs [--local]');
    bundleRelease(args[0] === '--local');
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}
