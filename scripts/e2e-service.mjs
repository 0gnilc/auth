import { spawnSync } from 'node:child_process';
import { dirname, resolve } from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

import { tryReservePort } from './dev.mjs';
import { parsePort } from './port.mjs';

const repositoryRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..');

function readE2ePorts(environment) {
  return {
    admin: parsePort(environment.E2E_ADMIN_PORT, 'E2E_ADMIN_PORT'),
    server: parsePort(environment.E2E_SERVER_PORT, 'E2E_SERVER_PORT'),
  };
}

/** 让 Admin 代理到同一个隔离 Server；Server 由启用测试配置的 E2eServerIT 承载。 */
function createServiceDefinition(
  serviceName,
  baseEnvironment = process.env,
  platform = process.platform,
) {
  const ports = readE2ePorts(baseEnvironment);
  const backendUrl = `http://127.0.0.1:${ports.server}`;
  const pnpm = platform === 'win32' ? 'pnpm.cmd' : 'pnpm';

  if (serviceName === 'server') {
    return {
      args: [
        '--batch-mode',
        '--no-transfer-progress',
        '-f',
        'apps/server/pom.xml',
        '-pl',
        ':gnilc-bootstrap',
        '-am',
        '-Dit.test=E2eServerIT',
        '-Dsurefire.failIfNoSpecifiedTests=false',
        '-Dfailsafe.failIfNoSpecifiedTests=false',
        'verify',
      ],
      command: platform === 'win32' ? 'mvn.cmd' : 'mvn',
      environment: {
        ...baseEnvironment,
        RUN_E2E_SERVER: 'true',
        SERVER_PORT: String(ports.server),
      },
      port: ports.server,
    };
  }
  if (serviceName === 'admin') {
    return {
      args: ['--filter=@app/admin', 'run', 'dev', '--host', '127.0.0.1'],
      command: pnpm,
      environment: {
        ...baseEnvironment,
        E2E_API_TARGET: backendUrl,
        VITE_GLOB_API_URL: `${backendUrl}/api`,
        VITE_PORT: String(ports.admin),
      },
      port: ports.admin,
    };
  }
  throw new Error(`Unknown E2E service: ${serviceName}`);
}

async function runService(
  serviceName,
  {
    baseEnvironment = process.env,
    commandRunner = spawnSync,
    platform = process.platform,
    reservePort = tryReservePort,
    root = repositoryRoot,
  } = {},
) {
  const service = createServiceDefinition(
    serviceName,
    baseEnvironment,
    platform,
  );
  // 总启动器选端口与当前进程启动之间可能发生竞争；占用时失败，不替换端口或影响已有进程。
  const reservation = await reservePort(service.port);
  if (!reservation) {
    throw new Error(
      `${serviceName} E2E port ${service.port} became unavailable before startup`,
    );
  }
  await reservation.release();

  const result = commandRunner(service.command, service.args, {
    cwd: root,
    env: service.environment,
    shell: platform === 'win32',
    stdio: 'inherit',
  });
  if (result.error) throw result.error;
  return result.status ?? 1;
}

export { createServiceDefinition, runService };

if (
  process.argv[1] &&
  resolve(process.argv[1]) === fileURLToPath(import.meta.url)
) {
  runService(process.argv[2])
    .then((exitCode) => {
      process.exitCode = exitCode;
    })
    .catch((error) => {
      console.error(error.message);
      process.exitCode = 1;
    });
}
