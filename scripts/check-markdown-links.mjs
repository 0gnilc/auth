import { execFileSync } from 'node:child_process';
import { existsSync, readFileSync } from 'node:fs';
import { dirname, isAbsolute, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const INLINE_LINK =
  /!?\[[^\]]*\]\((<[^>]+>|[^\s)]+)(?:\s+(?:"[^"]*"|'[^']*'|\([^)]*\)))?\)/gu;

function findBrokenLocalLinks({
  exists = existsSync,
  filePath,
  markdown,
  repositoryRoot = process.cwd(),
}) {
  const broken = [];
  for (const match of markdown.matchAll(INLINE_LINK)) {
    const target = match[1].replaceAll(/^<|>$/gu, '');
    if (/^(?:[a-z][a-z0-9+.-]*:|#|\/\/)/iu.test(target)) continue;
    if (/^[A-Z][A-Z0-9_]+$/u.test(target)) continue;
    const fileTarget = target.split(/[?#]/u, 1)[0];
    if (!fileTarget) continue;
    let decodedTarget;
    try {
      decodedTarget = decodeURI(fileTarget);
    } catch {
      broken.push({ source: filePath, target });
      continue;
    }
    const resolvedTarget = isAbsolute(decodedTarget)
      ? resolve(repositoryRoot, decodedTarget.slice(1))
      : resolve(dirname(filePath), decodedTarget);
    if (!exists(resolvedTarget)) broken.push({ source: filePath, target });
  }
  return broken;
}

function trackedMarkdownFiles() {
  return execFileSync('git', [
    'ls-files',
    '--cached',
    '--others',
    '--exclude-standard',
    '-z',
    '--',
    '*.md',
  ])
    .toString('utf8')
    .split('\0')
    .filter(Boolean);
}

function checkTrackedMarkdownLinks({ repositoryRoot = process.cwd() } = {}) {
  const files = [...new Set(trackedMarkdownFiles())].filter((file) =>
    existsSync(resolve(repositoryRoot, file)),
  );
  const broken = files.flatMap((file) => {
    const filePath = resolve(repositoryRoot, file);
    return findBrokenLocalLinks({
      filePath,
      markdown: readFileSync(filePath, 'utf8'),
      repositoryRoot,
    });
  });
  return { broken, checkedFiles: files.length };
}

export { checkTrackedMarkdownLinks, findBrokenLocalLinks };

if (
  process.argv[1] &&
  resolve(process.argv[1]) === fileURLToPath(import.meta.url)
) {
  const result = checkTrackedMarkdownLinks();
  if (result.broken.length > 0) {
    for (const { source, target } of result.broken) {
      console.error(`${source}: missing local Markdown target ${target}`);
    }
    process.exitCode = 1;
  } else {
    console.log(
      `Checked ${result.checkedFiles} tracked Markdown files; all local links resolve.`,
    );
  }
}
