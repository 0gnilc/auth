import { spawn, spawnSync } from 'node:child_process';
import { readFile } from 'node:fs/promises';
import { createServer } from 'node:net';
import { dirname, join, resolve } from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

import { parsePort } from './port.mjs';

const repositoryRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const signalExitCodes = { SIGHUP: 129, SIGINT: 130, SIGTERM: 143 };

function parseEnvPort(content, name, source) {
  for (const line of content.split(/\r?\n/u)) {
    const match = line.match(/^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$/u);
    if (match?.[1] !== name) continue;
    const rawValue = match[2];
    const value =
      rawValue.length >= 2 &&
      ((rawValue.startsWith('"') && rawValue.endsWith('"')) ||
        (rawValue.startsWith("'") && rawValue.endsWith("'")))
        ? rawValue.slice(1, -1)
        : rawValue;
    return parsePort(value, source);
  }
  throw new Error(`Missing ${name} in ${source}`);
}

function parseServerPort(content, source) {
  const lines = content.split(/\r?\n/u);
  const serverIndex = lines.findIndex((line) =>
    /^server:\s*(?:#.*)?$/u.test(line),
  );
  if (serverIndex === -1) {
    throw new Error(`Missing server configuration in ${source}`);
  }
  for (const line of lines.slice(serverIndex + 1)) {
    if (/^\S/u.test(line)) break;
    const match = line.match(/^\s+port:\s*([^\s#]+).*$/u);
    if (match) return parsePort(match[1], source);
  }
  throw new Error(`Missing server.port in ${source}`);
}

async function loadStartingPorts(root = repositoryRoot) {
  const adminPath = join(root, 'apps/admin/.env.development');
  const serverPath = join(
    root,
    'apps/server/gnilc-bootstrap/src/main/resources/application.yml',
  );
  const [adminConfig, serverConfig] = await Promise.all([
    readFile(adminPath, 'utf8'),
    readFile(serverPath, 'utf8'),
  ]);
  return {
    admin: parseEnvPort(adminConfig, 'VITE_PORT', adminPath),
    server: parseServerPort(serverConfig, serverPath),
  };
}

/** 实际监听端口直至显式释放；占用或无权限返回空结果，其他监听错误继续传播。 */
function tryReservePort(port) {
  return new Promise((resolveReservation, reject) => {
    const server = createServer();
    server.unref();
    server.once('error', (error) => {
      if (error.code === 'EADDRINUSE' || error.code === 'EACCES') {
        resolveReservation(null);
      } else {
        reject(error);
      }
    });
    server.listen({ exclusive: true, port }, () => {
      let releasePromise;
      resolveReservation({
        port,
        release() {
          releasePromise ??= new Promise((resolveRelease, rejectRelease) => {
            server.close((error) => {
              if (error) rejectRelease(error);
              else resolveRelease();
            });
          });
          return releasePromise;
        },
      });
    });
  });
}

async function reserveAvailablePort(
  startingPort,
  reservePort = tryReservePort,
) {
  const start = parsePort(startingPort, 'development configuration');
  for (let port = start; port <= 65_535; port += 1) {
    const reservation = await reservePort(port);
    if (reservation) return reservation;
  }
  throw new Error(`No available development port at or above ${start}`);
}

/** 连续持有各服务的预留端口，避免本次启动的服务选到同一端口；不会终止已有监听者。 */
async function reserveDevelopmentPorts(
  startingPorts,
  reservePort = tryReservePort,
) {
  const reservations = {};
  try {
    reservations.server = await reserveAvailablePort(
      startingPorts.server,
      reservePort,
    );
    reservations.admin = await reserveAvailablePort(
      startingPorts.admin,
      reservePort,
    );
    return {
      ports: {
        admin: reservations.admin.port,
        server: reservations.server.port,
      },
      reservations,
    };
  } catch (error) {
    await Promise.allSettled(
      Object.values(reservations).map((reservation) => reservation.release()),
    );
    throw error;
  }
}

/** 仅为本次子进程覆写端口与前端代理，不写回开发配置文件。 */
function createServiceDefinitions(ports, baseEnvironment = process.env) {
  const serverPort = String(ports.server);
  return [
    {
      environment: { ...baseEnvironment, SERVER_PORT: serverPort },
      name: 'Server',
      port: ports.server,
      script: 'dev:server',
    },
    {
      environment: {
        ...baseEnvironment,
        E2E_API_TARGET: `http://localhost:${serverPort}`,
        VITE_GLOB_API_URL: `http://localhost:${serverPort}/api`,
        VITE_PORT: String(ports.admin),
      },
      name: 'Admin',
      port: ports.admin,
      script: 'dev:admin',
    },
  ];
}

function startServices(
  services,
  {
    commandRunner = spawnSync,
    killProcess = process.kill.bind(process),
    platform = process.platform,
    root = repositoryRoot,
    spawnChild = spawn,
  } = {},
) {
  const command = platform === 'win32' ? 'pnpm.cmd' : 'pnpm';
  const runningServices = [];
  try {
    for (const service of services) {
      runningServices.push({
        ...service,
        child: spawnChild(command, ['run', service.script], {
          cwd: root,
          detached: platform !== 'win32',
          env: service.environment,
          shell: platform === 'win32',
          stdio: 'inherit',
        }),
      });
    }
    return runningServices;
  } catch (error) {
    const cleanup = signalServicesBestEffort(
      runningServices,
      'SIGTERM',
      platform,
      killProcess,
      commandRunner,
      error,
    );
    throw cleanup.error;
  }
}

/** 只终止本次持有的子进程组或 Windows 进程树，不按端口或进程名称查杀其他服务。 */
function signalChild(child, signal, platform, killProcess, commandRunner) {
  if (!child.pid) return;
  if (platform === 'win32') {
    const result = commandRunner(
      'taskkill',
      ['/PID', String(child.pid), '/T', '/F'],
      { stdio: 'ignore', windowsHide: true },
    );
    let taskkillError;
    if (result.error) {
      taskkillError = normalizeError(result.error);
    } else if (result.status !== 0) {
      taskkillError = new Error(
        `taskkill failed for child PID ${child.pid} with status ${result.status}`,
      );
    }
    if (taskkillError) {
      const probe = probeChildExit(child);
      if (probe.error) {
        const normalizedProbeError = normalizeError(probe.error);
        throw new AggregateError(
          [taskkillError, normalizedProbeError],
          `${taskkillError.message}; child liveness probe also failed: ${normalizedProbeError.message}`,
          { cause: probe.error },
        );
      }
      if (probe.exited) return;
      throw taskkillError;
    }
    return;
  }
  try {
    killProcess(-child.pid, signal);
  } catch (error) {
    if (error.code !== 'ESRCH') throw error;
  }
}

function hasChildExited(child) {
  if (child.exitCode !== null && child.exitCode !== undefined) return true;
  if (child.signalCode !== null && child.signalCode !== undefined) return true;
  try {
    return child.kill(0) === false;
  } catch (error) {
    if (error.code === 'ESRCH') return true;
    throw error;
  }
}

function probeChildExit(child) {
  try {
    return { exited: hasChildExited(child) };
  } catch (error) {
    return { error };
  }
}

function normalizeError(error) {
  return error instanceof Error ? error : new Error(String(error));
}

/** 同时保留启动/运行失败和清理失败，避免后发生的清理错误覆盖原始原因。 */
function combineOperationAndCleanupErrors(originalError, cleanupErrors) {
  const normalizedCleanupErrors = cleanupErrors.map((error) =>
    normalizeError(error),
  );
  const cleanupSummary = normalizedCleanupErrors
    .map((error) => error.message)
    .join('; ');
  if (originalError !== undefined) {
    const normalizedOriginalError = normalizeError(originalError);
    return normalizedCleanupErrors.length === 0
      ? normalizedOriginalError
      : new AggregateError(
          [normalizedOriginalError, ...normalizedCleanupErrors],
          `${normalizedOriginalError.message}; cleanup also failed: ${cleanupSummary}`,
        );
  }
  if (normalizedCleanupErrors.length === 0) return undefined;
  return normalizedCleanupErrors.length === 1
    ? normalizedCleanupErrors[0]
    : new AggregateError(
        normalizedCleanupErrors,
        `Multiple development services could not be stopped: ${cleanupSummary}`,
      );
}

function signalServicesBestEffort(
  runningServices,
  signal,
  platform,
  killProcess,
  commandRunner,
  originalError,
) {
  const cleanupErrors = [];
  const failedServices = [];
  for (const runningService of runningServices) {
    try {
      signalChild(
        runningService.child,
        signal,
        platform,
        killProcess,
        commandRunner,
      );
    } catch (error) {
      cleanupErrors.push(error);
      failedServices.push(runningService);
    }
  }
  return {
    error: combineOperationAndCleanupErrors(originalError, cleanupErrors),
    failedServices,
  };
}

/** 在服务尚未全部启动时接管终止信号，防止监听器交接前继续启动剩余服务。 */
function installStartupSignalGuard(
  runningServices,
  {
    commandRunner = spawnSync,
    killProcess = process.kill.bind(process),
    parentProcess = process,
    platform = process.platform,
  } = {},
) {
  const listeners = new Map();
  let failure;
  let receivedSignal;
  for (const signal of Object.keys(signalExitCodes)) {
    const listener = () => {
      if (receivedSignal) return;
      receivedSignal = signal;
      failure = signalServicesBestEffort(
        runningServices,
        signal,
        platform,
        killProcess,
        commandRunner,
      ).error;
    };
    listeners.set(signal, listener);
    parentProcess.once(signal, listener);
  }
  return {
    get failure() {
      return failure;
    },
    get signal() {
      return receivedSignal;
    },
    remove() {
      for (const [signal, listener] of listeners) {
        parentProcess.removeListener(signal, listener);
      }
    },
  };
}

function superviseServices(
  runningServices,
  {
    commandRunner = spawnSync,
    killProcess = process.kill.bind(process),
    initialSignal,
    parentProcess = process,
    platform = process.platform,
  } = {},
) {
  return new Promise((resolveCompletion, reject) => {
    if (runningServices.length === 0) {
      resolveCompletion(initialSignal ? signalExitCodes[initialSignal] : 0);
      return;
    }
    const pendingChildren = new Set(runningServices.map(({ child }) => child));
    const childListeners = new Map();
    const parentListeners = new Map();
    let completionCode = initialSignal ? signalExitCodes[initialSignal] : 0;
    let completionError;
    let initialCompletionCode;
    let stopping = Boolean(initialSignal);

    const cleanupListeners = () => {
      for (const [signal, listener] of parentListeners) {
        parentProcess.removeListener(signal, listener);
      }
      for (const [child, listeners] of childListeners) {
        child.removeListener('error', listeners.error);
        child.removeListener('exit', listeners.exit);
      }
    };
    const completeIfFinished = () => {
      if (pendingChildren.size > 0) return;
      cleanupListeners();
      if (completionError) {
        reject(
          completionError instanceof Error
            ? completionError
            : new Error(String(completionError)),
        );
      } else resolveCompletion(completionCode);
    };
    const stop = (signal, exitCode) => {
      if (stopping) return;
      stopping = true;
      completionCode = exitCode;
      const cleanup = signalServicesBestEffort(
        runningServices.filter(({ child }) => pendingChildren.has(child)),
        signal,
        platform,
        killProcess,
        commandRunner,
        completionError,
      );
      completionError = cleanup.error;
      for (const { child } of cleanup.failedServices) {
        pendingChildren.delete(child);
      }
      completeIfFinished();
    };

    for (const signal of Object.keys(signalExitCodes)) {
      const listener = () => stop(signal, signalExitCodes[signal]);
      parentListeners.set(signal, listener);
      parentProcess.once(signal, listener);
    }
    for (const runningService of runningServices) {
      const { child } = runningService;
      if (runningService.startupErrorListener) {
        child.removeListener('error', runningService.startupErrorListener);
      }
      if (runningService.startupError) {
        completionError ??= runningService.startupError;
        pendingChildren.delete(child);
        initialCompletionCode ??= 1;
        continue;
      }
      if (child.exitCode !== null || child.signalCode !== null) {
        pendingChildren.delete(child);
        initialCompletionCode ??=
          typeof child.exitCode === 'number'
            ? child.exitCode
            : (signalExitCodes[child.signalCode] ?? 1);
        continue;
      }
      const listeners = {
        error: (error) => {
          completionError ??= error;
          pendingChildren.delete(child);
          stop('SIGTERM', 1);
          completeIfFinished();
        },
        exit: (code, signal) => {
          pendingChildren.delete(child);
          if (!stopping) {
            const exitCode =
              typeof code === 'number' ? code : (signalExitCodes[signal] ?? 1);
            stop('SIGTERM', exitCode);
          }
          completeIfFinished();
        },
      };
      childListeners.set(child, listeners);
      child.once('error', listeners.error);
      child.once('exit', listeners.exit);
    }
    if (initialCompletionCode !== undefined) {
      stop('SIGTERM', initialCompletionCode);
    }
    completeIfFinished();
  });
}

/** 预留端口、逐服务释放并启动，再交给统一监督器；启动失败时尝试清理本次已创建资源。 */
async function runDevelopment({
  baseEnvironment = process.env,
  commandRunner = spawnSync,
  killProcess = process.kill.bind(process),
  log = console.log,
  parentProcess = process,
  platform = process.platform,
  reservePort = tryReservePort,
  root = repositoryRoot,
  spawnChild = spawn,
} = {}) {
  const startingPorts = await loadStartingPorts(root);
  const selection = await reserveDevelopmentPorts(startingPorts, reservePort);
  const runningServices = [];
  const startupSignalGuard = installStartupSignalGuard(runningServices, {
    commandRunner,
    killProcess,
    parentProcess,
    platform,
  });
  try {
    const services = createServiceDefinitions(selection.ports, baseEnvironment);
    for (const service of services) {
      if (startupSignalGuard.signal) break;
      const key = service.name.toLowerCase();
      // 每个服务即将启动才释放自己的预留端口；尚未启动服务的预留仍保持占用。
      await selection.reservations[key].release();
      if (startupSignalGuard.failure) throw startupSignalGuard.failure;
      if (startupSignalGuard.signal) break;
      const startedServices = startServices([service], {
        commandRunner,
        killProcess,
        platform,
        root,
        spawnChild,
      });
      for (const runningService of startedServices) {
        const startupErrorListener = (error) => {
          runningService.startupError = error;
        };
        runningService.startupErrorListener = startupErrorListener;
        runningService.child.once('error', startupErrorListener);
      }
      runningServices.push(...startedServices);
      const suffix = service.name === 'Server' ? '/api' : '';
      log(`${service.name}: http://localhost:${service.port}${suffix}`);
    }
    if (startupSignalGuard.failure) throw startupSignalGuard.failure;
    if (startupSignalGuard.signal) {
      await Promise.allSettled(
        Object.values(selection.reservations).map((reservation) =>
          reservation.release(),
        ),
      );
    }
    const supervision = superviseServices(runningServices, {
      commandRunner,
      initialSignal: startupSignalGuard.signal,
      killProcess,
      parentProcess,
      platform,
    });
    startupSignalGuard.remove();
    return supervision;
  } catch (error) {
    startupSignalGuard.remove();
    await Promise.allSettled(
      Object.values(selection.reservations).map((reservation) =>
        reservation.release(),
      ),
    );
    for (const { child, startupErrorListener } of runningServices) {
      if (startupErrorListener) {
        child.removeListener('error', startupErrorListener);
      }
    }
    const cleanup = signalServicesBestEffort(
      runningServices,
      'SIGTERM',
      platform,
      killProcess,
      commandRunner,
      error,
    );
    throw cleanup.error;
  }
}

export {
  createServiceDefinitions,
  loadStartingPorts,
  reserveAvailablePort,
  reserveDevelopmentPorts,
  runDevelopment,
  startServices,
  superviseServices,
  tryReservePort,
};

if (
  process.argv[1] &&
  resolve(process.argv[1]) === fileURLToPath(import.meta.url)
) {
  runDevelopment()
    .then((exitCode) => {
      process.exitCode = exitCode;
    })
    .catch((error) => {
      console.error(error.message);
      process.exitCode = 1;
    });
}
