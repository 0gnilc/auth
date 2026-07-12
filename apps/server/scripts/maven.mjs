import { spawnSync } from 'node:child_process';
import { existsSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const serverDir = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const commonArgs = ['--batch-mode', '--no-transfer-progress'];

const tasks = {
  build: [[...commonArgs, '-DskipTests', 'package']],
  clean: [[...commonArgs, 'clean']],
  dev: [
    [...commonArgs, '-DskipTests', 'package'],
    [...commonArgs, '-pl', 'gnilc-bootstrap', 'spring-boot:run'],
  ],
  test: [[...commonArgs, 'test']],
  typecheck: [[...commonArgs, '-DskipTests', 'compile']],
  verify: [[...commonArgs, 'verify']],
};

export function getTaskInvocations(task) {
  const invocations = tasks[task];

  if (!invocations) {
    throw new Error(
      `Unknown Maven task "${task}". Expected one of: ${Object.keys(tasks).join(', ')}`,
    );
  }

  return invocations.map((args) => [...args]);
}

export function resolveMavenExecutable({
  fileExists = existsSync,
  platform = process.platform,
  serverDir: projectDir = serverDir,
} = {}) {
  const isWindows = platform === 'win32';
  const wrapper = join(projectDir, isWindows ? 'mvnw.cmd' : 'mvnw');

  if (fileExists(wrapper)) {
    return {
      command: wrapper,
      shell: isWindows,
      source: 'wrapper',
    };
  }

  return {
    command: isWindows ? 'mvn.cmd' : 'mvn',
    shell: isWindows,
    source: 'system',
  };
}

export function runTask(
  task,
  {
    executable = resolveMavenExecutable(),
    spawn = spawnSync,
    stderr = process.stderr,
  } = {},
) {
  for (const args of getTaskInvocations(task)) {
    const result = spawn(executable.command, args, {
      cwd: serverDir,
      env: process.env,
      shell: executable.shell,
      stdio: 'inherit',
    });

    if (result.error) {
      const hint =
        executable.source === 'system'
          ? 'Install Maven or add mvn to PATH, or add Maven Wrapper files to apps/server.'
          : 'Check that the Maven Wrapper can be executed on this platform.';
      stderr.write(`Failed to start Maven: ${result.error.message}\n${hint}\n`);
      return 1;
    }

    if (result.status !== 0) {
      return result.status ?? 1;
    }
  }

  return 0;
}

function main() {
  const task = process.argv[2];

  if (!task) {
    process.stderr.write(
      `Usage: node scripts/maven.mjs <${Object.keys(tasks).join('|')}>\n`,
    );
    return 1;
  }

  try {
    return runTask(task);
  } catch (error) {
    process.stderr.write(`${error.message}\n`);
    return 1;
  }
}

if (
  process.argv[1] &&
  import.meta.url === pathToFileURL(resolve(process.argv[1])).href
) {
  process.exitCode = main();
}
