import { execFileSync } from 'node:child_process';
import { existsSync, readFileSync, realpathSync, writeFileSync } from 'node:fs';
import { dirname, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const repositoryRoot = resolve(
  dirname(fileURLToPath(import.meta.url)),
  '../..',
);

/** 产品版本只接受正式版和递增的 RC；不把构建标识混入产品版本。 */
function parseVersion(version) {
  const match =
    /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-rc\.([1-9]\d*))?$/u.exec(
      version,
    );
  if (
    !match ||
    match
      .slice(1)
      .filter(Boolean)
      .some((part) => !Number.isSafeInteger(Number(part)))
  ) {
    throw new Error(`Invalid product version: ${version}`);
  }
  return [
    ...match.slice(1, 4).map(Number),
    match[4] ? Number(match[4]) : Infinity,
  ];
}

function compareVersions(left, right) {
  const a = parseVersion(left);
  const b = parseVersion(right);
  for (let index = 0; index < a.length; index++) {
    if (a[index] !== b[index]) return a[index] > b[index] ? 1 : -1;
  }
  return 0;
}

function readJson(path) {
  return JSON.parse(readFileSync(path, 'utf8'));
}

function productVersion(root = repositoryRoot) {
  const { version } = readJson(resolve(root, 'package.json'));
  parseVersion(version);
  return version;
}

function workspaceManifests(root) {
  root = realpathSync(root);
  const packages = JSON.parse(
    execFileSync('pnpm', ['list', '--recursive', '--depth=-1', '--json'], {
      cwd: root,
      encoding: 'utf8',
    }),
  );
  return [...new Set([root, ...packages.map((pkg) => pkg.path)])].map(
    (directory) => {
      const path = relative(root, directory);
      if (path.startsWith('..'))
        throw new Error(`Workspace escapes repository: ${directory}`);
      return resolve(directory, 'package.json');
    },
  );
}

/** 遍历 Maven reactor 声明的模块，避免把 target 中生成的 POM 当成源码。 */
function reactorPoms(root) {
  const result = [];
  function visit(path) {
    if (result.includes(path))
      throw new Error(`Duplicate Maven module: ${path}`);
    result.push(path);
    const xml = readFileSync(path, 'utf8').replaceAll(/<!--[\s\S]*?-->/gu, '');
    const modules = xml.match(/<modules>([\s\S]*?)<\/modules>/u)?.[1] ?? '';
    for (const match of modules.matchAll(/<module>([^<]+)<\/module>/gu)) {
      if (!/^[a-z\d-]+$/u.test(match[1]))
        throw new Error(`Unsupported Maven module: ${match[1]}`);
      visit(resolve(dirname(path), match[1], 'pom.xml'));
    }
  }
  visit(resolve(root, 'apps/server/pom.xml'));
  return result;
}

/** 仅更新项目坐标和 com.gnilc 内部依赖，保留第三方依赖与插件版本。 */
function synchronizePom(xml, version, isRoot) {
  parseVersion(version);
  const coordinate = isRoot
    ? /(<artifactId>gnilc-auth-parent<\/artifactId>\s*<version>)[^<]+(<\/version>)/u
    : /(<parent>\s*<groupId>com\.gnilc<\/groupId>\s*<artifactId>[^<]+<\/artifactId>\s*<version>)[^<]+(<\/version>)/u;
  if (!coordinate.test(xml))
    throw new Error('Unrecognized project/parent coordinates in POM');
  return xml
    .replace(coordinate, `$1${version}$2`)
    .replaceAll(/<dependency>[\s\S]*?<\/dependency>/gu, (dependency) => {
      if (!/<groupId>com\.gnilc<\/groupId>/u.test(dependency))
        return dependency;
      return dependency.replace(
        /<version>[^<]+<\/version>/u,
        `<version>\${project.version}</version>`,
      );
    });
}

function versionChanges(root = repositoryRoot, version = productVersion(root)) {
  parseVersion(version);
  const changes = [];
  for (const path of workspaceManifests(root)) {
    const original = readFileSync(path, 'utf8');
    const data = JSON.parse(original);
    if (data.version !== version || data.private !== true) {
      data.version = version;
      data.private = true;
      changes.push({ path, content: `${JSON.stringify(data, null, 2)}\n` });
    }
  }
  const rootPom = resolve(root, 'apps/server/pom.xml');
  for (const path of reactorPoms(root)) {
    const original = readFileSync(path, 'utf8');
    const content = synchronizePom(original, version, path === rootPom);
    if (content !== original) changes.push({ path, content });
  }
  return changes;
}

function checkVersions(root = repositoryRoot) {
  const changes = versionChanges(root);
  if (changes.length > 0)
    throw new Error(
      `Version/private flag drift:\n${changes.map(({ path }) => relative(root, path)).join('\n')}`,
    );
  return productVersion(root);
}

function syncVersions(root = repositoryRoot, version = productVersion(root)) {
  const changes = versionChanges(root, version);
  for (const { path, content } of changes) writeFileSync(path, content);
  return changes.length;
}

function prepareRelease(root, version) {
  const current = checkVersions(root);
  if (compareVersions(version, current) <= 0)
    throw new Error(`Release version must be newer than ${current}`);
  const tags = execFileSync('git', ['tag', '--list', 'v*'], {
    cwd: root,
    encoding: 'utf8',
  })
    .trim()
    .split('\n')
    .filter(Boolean);
  for (const tag of tags) {
    if (compareVersions(version, tag.slice(1)) <= 0)
      throw new Error(`Release must be newer than existing tag ${tag}`);
  }
  const changelogPath = resolve(root, 'CHANGELOG.md');
  const changelog = readFileSync(changelogPath, 'utf8');
  const section = /^## Unreleased\n([\s\S]*?)(?=^## |$(?![\s\S]))/mu.exec(
    changelog,
  );
  if (!section?.[1].trim())
    throw new Error(
      'Add reviewed release notes under CHANGELOG.md: ## Unreleased first',
    );
  const upgradePath = resolve(root, `docs/release/${version}.md`);
  if (!existsSync(upgradePath))
    throw new Error(`Write the upgrade/rollback guide first: ${upgradePath}`);
  const updated = changelog.replace(
    section[0],
    `## Unreleased\n\n## ${version}\n${section[1]}`,
  );
  // 所有输入先校验，再写版本文件；失败不生成半份发布说明。
  syncVersions(root, version);
  writeFileSync(changelogPath, updated);
  console.log(
    `Prepared ${version}. Review changes, refresh the frozen lockfile if needed, and open a release PR.`,
  );
}

export {
  checkVersions,
  compareVersions,
  parseVersion,
  prepareRelease,
  productVersion,
  reactorPoms,
  readJson,
  repositoryRoot,
  synchronizePom,
  syncVersions,
  versionChanges,
};

if (
  process.argv[1] &&
  resolve(process.argv[1]) === fileURLToPath(import.meta.url)
) {
  const [command, flag, value, ...extra] = process.argv.slice(2);
  try {
    if (command === 'check' && !flag)
      console.log(`Product version ${checkVersions()} is consistent.`);
    else if (command === 'sync' && !flag)
      console.log(`Synchronized ${syncVersions()} files.`);
    else if (
      command === 'prepare' &&
      flag === '--version' &&
      value &&
      extra.length === 0
    )
      prepareRelease(repositoryRoot, value);
    else
      throw new Error(
        'Usage: version.mjs check | sync | prepare --version X.Y.Z[-rc.N]',
      );
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}
