import { spawnSync } from 'node:child_process';
import { dirname, resolve } from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

import { loadStartingPorts, reserveDevelopmentPorts } from './dev.mjs';

const repositoryRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..');

/** 将本轮隔离服务的端口交给 Playwright 的 webServer 配置，不修改应用的默认配置。 */
function createE2eEnvironment(ports, baseEnvironment = process.env) {
  return {
    ...baseEnvironment,
    E2E_ADMIN_PORT: String(ports.admin),
    E2E_SERVER_PORT: String(ports.server),
  };
}

function normalizePlaywrightArgs(args) {
  return args[0] === '--' ? args.slice(1) : args;
}

/** 尝试释放全部预留资源，并将释放失败显式交给调用方。 */
async function releaseReservations(reservations) {
  const results = await Promise.allSettled(
    Object.values(reservations).map((reservation) => reservation.release()),
  );
  const errors = results
    .filter((result) => result.status === 'rejected')
    .map((result) => result.reason);
  if (errors.length > 0) {
    throw new AggregateError(errors, 'Could not release reserved E2E ports');
  }
}

/** 选择一组可用端口后启动 Playwright；各 webServer 启动前仍会再次检查端口竞争。 */
async function runE2e({
  args = process.argv.slice(2),
  baseEnvironment = process.env,
  commandRunner = spawnSync,
  loadPorts = loadStartingPorts,
  platform = process.platform,
  reservePorts = reserveDevelopmentPorts,
  root = repositoryRoot,
} = {}) {
  const startingPorts = await loadPorts(root);
  const selection = await reservePorts(startingPorts);
  await releaseReservations(selection.reservations);

  const command = platform === 'win32' ? 'pnpm.cmd' : 'pnpm';
  const result = commandRunner(
    command,
    ['exec', 'playwright', 'test', ...normalizePlaywrightArgs(args)],
    {
      cwd: root,
      env: createE2eEnvironment(selection.ports, baseEnvironment),
      shell: platform === 'win32',
      stdio: 'inherit',
    },
  );
  if (result.error) throw result.error;
  return result.status ?? 1;
}

export { createE2eEnvironment, runE2e };

if (
  process.argv[1] &&
  resolve(process.argv[1]) === fileURLToPath(import.meta.url)
) {
  runE2e()
    .then((exitCode) => {
      process.exitCode = exitCode;
    })
    .catch((error) => {
      console.error(error.message);
      process.exitCode = 1;
    });
}
