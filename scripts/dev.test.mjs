import { ChildProcess } from 'node:child_process';
import { mkdir, mkdtemp, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

import { afterEach, describe, expect, it, vi } from 'vitest';

import {
  createServiceDefinitions,
  loadStartingPorts,
  reserveAvailablePort,
  reserveDevelopmentPorts,
  runDevelopment,
  startServices,
  superviseServices,
} from './dev.mjs';

const temporaryDirectories = [];

/** 用真实事件接口和虚构 PID 模拟子进程；信号与启动函数均注入替身，不操作开发者进程。 */
function createChildren(processIds) {
  return processIds.map((pid) => {
    const child = new ChildProcess();
    return Object.assign(child, {
      exitCode: null,
      kill: vi.fn(),
      pid,
      signalCode: null,
    });
  });
}

/** 在临时目录构造最小配置，验证端口读取和进程内覆写，不改真实仓库的环境配置。 */
async function createRepositoryConfig() {
  const root = await mkdtemp(join(tmpdir(), 'gnilc-auth-dev-launcher-'));
  temporaryDirectories.push(root);
  await Promise.all([
    mkdir(join(root, 'apps/admin'), { recursive: true }),
    mkdir(join(root, 'apps/server/gnilc-bootstrap/src/main/resources'), {
      recursive: true,
    }),
  ]);
  await Promise.all([
    writeFile(
      join(root, 'apps/admin/.env.development'),
      'VITE_PORT=5088\n',
      'utf8',
    ),
    writeFile(
      join(
        root,
        'apps/server/gnilc-bootstrap/src/main/resources/application.yml',
      ),
      'server:\n  port: 3888\n  servlet:\n    context-path: /api\n',
      'utf8',
    ),
  ]);
  return root;
}

afterEach(async () => {
  await Promise.all(
    temporaryDirectories
      .splice(0)
      .map((directory) => rm(directory, { force: true, recursive: true })),
  );
});

describe('development launcher configuration', () => {
  it('reads each service starting port from its owning configuration', async () => {
    const root = await createRepositoryConfig();

    await expect(loadStartingPorts(root)).resolves.toEqual({
      admin: 5088,
      server: 3888,
    });
  });

  it('selects the first available port without disturbing occupied ports', async () => {
    const occupiedPorts = new Set([5088, 5089]);
    const release = vi.fn();

    await expect(
      reserveAvailablePort(5088, async (port) =>
        occupiedPorts.has(port) ? null : { port, release },
      ),
    ).resolves.toEqual({ port: 5090, release });
  });

  it('reserves selected ports so services never receive the same port', async () => {
    const heldPorts = new Set([4000]);
    const reservePort = async (port) => {
      if (heldPorts.has(port)) return null;
      heldPorts.add(port);
      return { port, release: () => heldPorts.delete(port) };
    };

    const selection = await reserveDevelopmentPorts(
      { admin: 4000, server: 4000 },
      reservePort,
    );

    expect(selection.ports).toEqual({
      admin: 4002,
      server: 4001,
    });
    Object.values(selection.reservations).forEach(({ release }) => release());
  });

  it('builds process-only service overrides around the selected Server port', () => {
    const environment = { PATH: '/tools' };

    const services = createServiceDefinitions(
      { admin: 5089, server: 3889 },
      environment,
    );

    expect(services).toEqual([
      expect.objectContaining({
        environment: { PATH: '/tools', SERVER_PORT: '3889' },
        name: 'Server',
        port: 3889,
        script: 'dev:server',
      }),
      expect.objectContaining({
        environment: {
          E2E_API_TARGET: 'http://localhost:3889',
          VITE_GLOB_API_URL: 'http://localhost:3889/api',
          PATH: '/tools',
          VITE_PORT: '5089',
        },
        name: 'Admin',
        port: 5089,
        script: 'dev:admin',
      }),
    ]);
    expect(environment).toEqual({ PATH: '/tools' });
  });

  it('starts each configured service in its own process group', () => {
    const services = createServiceDefinitions(
      { admin: 5089, server: 3889 },
      { PATH: '/tools' },
    );
    const children = [{ pid: 11 }, { pid: 12 }];
    const spawnChild = vi
      .fn()
      .mockReturnValueOnce(children[0])
      .mockReturnValueOnce(children[1])
      .mockReturnValueOnce(children[2]);

    const running = startServices(services, {
      platform: 'linux',
      root: '/repo',
      spawnChild,
    });

    expect(spawnChild).toHaveBeenNthCalledWith(
      1,
      'pnpm',
      ['run', 'dev:server'],
      expect.objectContaining({
        cwd: '/repo',
        detached: true,
        env: expect.objectContaining({ SERVER_PORT: '3889' }),
        stdio: 'inherit',
      }),
    );
    expect(spawnChild).toHaveBeenNthCalledWith(
      2,
      'pnpm',
      ['run', 'dev:admin'],
      expect.objectContaining({
        cwd: '/repo',
        detached: true,
        env: expect.objectContaining({
          E2E_API_TARGET: 'http://localhost:3889',
          VITE_GLOB_API_URL: 'http://localhost:3889/api',
          VITE_PORT: '5089',
        }),
        stdio: 'inherit',
      }),
    );
    expect(running.map(({ child }) => child)).toEqual(children);
  });

  it('cleans an already-started child when a later spawn fails', () => {
    const services = createServiceDefinitions(
      { admin: 5089, server: 3889 },
      { PATH: '/tools' },
    );
    const serverChild = { pid: 41 };
    const spawnFailure = new Error('spawn failed');
    const spawnChild = vi
      .fn()
      .mockReturnValueOnce(serverChild)
      .mockImplementationOnce(() => {
        throw spawnFailure;
      });
    const killProcess = vi.fn();

    expect(() =>
      startServices(services, {
        killProcess,
        platform: 'linux',
        root: '/repo',
        spawnChild,
      }),
    ).toThrow(spawnFailure);
    expect(killProcess).toHaveBeenCalledExactlyOnceWith(-41, 'SIGTERM');
  });

  // 故意让启动和清理同时失败，断言两份诊断都保留，而不只是检查最后抛出的消息。
  it('preserves a spawn error together with cleanup errors', () => {
    const services = createServiceDefinitions(
      { admin: 5089, server: 3889 },
      { PATH: '/tools' },
    );
    const serverChild = { pid: 42 };
    const spawnError = new Error('Admin spawn failed');
    const cleanupError = new Error('Server cleanup failed');
    const spawnChild = vi
      .fn()
      .mockReturnValueOnce(serverChild)
      .mockImplementationOnce(() => {
        throw spawnError;
      });
    const killProcess = vi.fn(() => {
      throw cleanupError;
    });

    let thrown;
    try {
      startServices(services, {
        killProcess,
        platform: 'linux',
        root: '/repo',
        spawnChild,
      });
    } catch (error) {
      thrown = error;
    }

    expect(thrown).toBeInstanceOf(AggregateError);
    expect(thrown.errors).toEqual([spawnError, cleanupError]);
    expect(killProcess).toHaveBeenCalledExactlyOnceWith(-42, 'SIGTERM');
  });

  it('forwards termination signals only to child groups it started', async () => {
    const parentProcess = new ChildProcess();
    const children = createChildren([71, 72, 73]);
    const running = children.map((child, index) => ({
      child,
      name: ['Server', 'Admin', 'Worker'][index],
    }));
    const killProcess = vi.fn();

    const completed = superviseServices(running, {
      killProcess,
      parentProcess,
      platform: 'linux',
    });
    parentProcess.emit('SIGINT');

    expect(killProcess.mock.calls).toEqual([
      [-71, 'SIGINT'],
      [-72, 'SIGINT'],
      [-73, 'SIGINT'],
    ]);
    for (const child of children) child.emit('exit', null, 'SIGINT');
    await expect(completed).resolves.toBe(130);
    expect(parentProcess.listenerCount('SIGINT')).toBe(0);
    expect(parentProcess.listenerCount('SIGTERM')).toBe(0);
  });

  it('attempts every child group when the first signal fails', async () => {
    const parentProcess = new ChildProcess();
    const children = createChildren([77, 78, 79]);
    const firstCleanupError = new Error('first child could not be stopped');
    const killProcess = vi.fn((processId) => {
      if (processId === -77) throw firstCleanupError;
    });
    const completed = superviseServices(
      children.map((child) => ({ child })),
      { killProcess, parentProcess, platform: 'linux' },
    );
    const outcome = completed.catch((error) => error);

    parentProcess.emit('SIGTERM');

    expect(killProcess.mock.calls).toEqual([
      [-77, 'SIGTERM'],
      [-78, 'SIGTERM'],
      [-79, 'SIGTERM'],
    ]);
    children[1].emit('exit', null, 'SIGTERM');
    children[2].emit('exit', null, 'SIGTERM');
    await expect(outcome).resolves.toBe(firstCleanupError);
  });

  it('uses scoped Windows taskkill commands for every started child tree', async () => {
    const parentProcess = new ChildProcess();
    const children = createChildren([101, 102]);
    const commandRunner = vi.fn(() => ({ status: 0 }));
    const completed = superviseServices(
      children.map((child) => ({ child })),
      { commandRunner, parentProcess, platform: 'win32' },
    );

    parentProcess.emit('SIGTERM');

    expect(commandRunner.mock.calls).toEqual([
      [
        'taskkill',
        ['/PID', '101', '/T', '/F'],
        { stdio: 'ignore', windowsHide: true },
      ],
      [
        'taskkill',
        ['/PID', '102', '/T', '/F'],
        { stdio: 'ignore', windowsHide: true },
      ],
    ]);
    expect(children[0].kill).not.toHaveBeenCalled();
    expect(children[1].kill).not.toHaveBeenCalled();
    for (const child of children) child.emit('exit', null, 'SIGTERM');
    await expect(completed).resolves.toBe(143);
  });

  it('continues Windows cleanup after a nonzero taskkill status', async () => {
    const parentProcess = new ChildProcess();
    const children = createChildren([103, 104]);
    children[0].kill.mockReturnValue(true);
    const commandRunner = vi
      .fn()
      .mockReturnValueOnce({ status: 5 })
      .mockReturnValueOnce({ status: 0 });
    const completed = superviseServices(
      children.map((child) => ({ child })),
      { commandRunner, parentProcess, platform: 'win32' },
    );
    const outcome = completed.catch((error) => error);

    parentProcess.emit('SIGINT');

    expect(commandRunner).toHaveBeenCalledTimes(2);
    expect(
      commandRunner.mock.calls.map(([, arguments_]) => arguments_[1]),
    ).toEqual(['103', '104']);
    children[1].emit('exit', null, 'SIGINT');
    await expect(outcome).resolves.toMatchObject({
      message: 'taskkill failed for child PID 103 with status 5',
    });
  });

  it('propagates a Windows taskkill runner error', async () => {
    const parentProcess = new ChildProcess();
    const [child] = createChildren([105]);
    child.kill.mockReturnValue(true);
    const runnerError = new Error('taskkill executable unavailable');
    const commandRunner = vi.fn(() => ({ error: runnerError, status: null }));
    const completed = superviseServices([{ child }], {
      commandRunner,
      parentProcess,
      platform: 'win32',
    });

    parentProcess.emit('SIGTERM');

    await expect(completed).rejects.toBe(runnerError);
    expect(commandRunner).toHaveBeenCalledExactlyOnceWith(
      'taskkill',
      ['/PID', '105', '/T', '/F'],
      { stdio: 'ignore', windowsHide: true },
    );
  });

  it('ignores a nonzero taskkill status when the exact child handle has exited', async () => {
    const parentProcess = new ChildProcess();
    const [child] = createChildren([107]);
    const commandRunner = vi.fn(() => ({ status: 128 }));
    const completed = superviseServices([{ child }], {
      commandRunner,
      parentProcess,
      platform: 'win32',
    });
    child.exitCode = 1;

    parentProcess.emit('SIGTERM');

    expect(child.kill).not.toHaveBeenCalled();
    child.emit('exit', 1, null);
    await expect(completed).resolves.toBe(143);
  });

  it.each([
    ['returns false', () => false],
    [
      'throws ESRCH',
      () => {
        throw Object.assign(new Error('process is gone'), { code: 'ESRCH' });
      },
    ],
  ])(
    'treats child.kill(0) that %s as an exited child',
    async (_case, probe) => {
      const parentProcess = new ChildProcess();
      const [child] = createChildren([109]);
      child.kill.mockImplementation(probe);
      const commandRunner = vi.fn(() => ({ status: 1 }));
      const completed = superviseServices([{ child }], {
        commandRunner,
        parentProcess,
        platform: 'win32',
      });

      parentProcess.emit('SIGTERM');

      expect(child.kill).toHaveBeenCalledExactlyOnceWith(0);
      child.emit('exit', null, 'SIGTERM');
      await expect(completed).resolves.toBe(143);
    },
  );

  it('surfaces a Windows child liveness probe error', async () => {
    const parentProcess = new ChildProcess();
    const [child] = createChildren([108]);
    const taskkillError = new Error(
      'taskkill failed for child PID 108 with status 1',
    );
    const probeError = Object.assign(new Error('liveness probe failed'), {
      code: 'EACCES',
    });
    child.kill.mockImplementation(() => {
      throw probeError;
    });
    const commandRunner = vi.fn(() => ({ status: 1 }));
    const completed = superviseServices([{ child }], {
      commandRunner,
      parentProcess,
      platform: 'win32',
    });
    const outcome = completed.catch((error) => error);

    parentProcess.emit('SIGTERM');

    const reported = await outcome;
    expect(reported).toBeInstanceOf(AggregateError);
    expect(reported.errors).toEqual([taskkillError, probeError]);
  });

  it('preserves a spawn error before a Windows taskkill error', () => {
    const services = createServiceDefinitions(
      { admin: 5089, server: 3889 },
      { PATH: 'C:\\tools' },
    );
    const [serverChild] = createChildren([106]);
    serverChild.kill.mockReturnValue(true);
    const spawnError = new Error('Admin spawn failed');
    const spawnChild = vi
      .fn()
      .mockReturnValueOnce(serverChild)
      .mockImplementationOnce(() => {
        throw spawnError;
      });
    const commandRunner = vi.fn(() => ({ status: 1 }));

    let thrown;
    try {
      startServices(services, {
        commandRunner,
        platform: 'win32',
        root: 'C:\\repo',
        spawnChild,
      });
    } catch (error) {
      thrown = error;
    }

    expect(thrown).toBeInstanceOf(AggregateError);
    expect(thrown.errors[0]).toBe(spawnError);
    expect(thrown.errors[1]).toMatchObject({
      message: 'taskkill failed for child PID 106 with status 1',
    });
  });

  it('surfaces a child spawn error after stopping its siblings', async () => {
    const parentProcess = new ChildProcess();
    const [failedChild, siblingChild] = createChildren([91, 92]);
    const spawnError = new Error('pnpm could not be started');
    const killProcess = vi.fn();
    const completed = superviseServices(
      [
        { child: failedChild, name: 'Server' },
        { child: siblingChild, name: 'Admin' },
      ],
      { killProcess, parentProcess, platform: 'linux' },
    );

    failedChild.emit('error', spawnError);

    expect(killProcess).toHaveBeenCalledExactlyOnceWith(-92, 'SIGTERM');
    siblingChild.emit('exit', null, 'SIGTERM');
    await expect(completed).rejects.toBe(spawnError);
    expect(parentProcess.listenerCount('SIGINT')).toBe(0);
    expect(parentProcess.listenerCount('SIGTERM')).toBe(0);
  });

  it('surfaces a spawn error recorded before supervisor handoff', async () => {
    const parentProcess = new ChildProcess();
    const [failedChild, siblingChild] = createChildren([93, 94]);
    const spawnError = new Error('Server spawn failed during startup');
    const startupErrorListener = vi.fn();
    failedChild.once('error', startupErrorListener);
    const killProcess = vi.fn();
    const completed = superviseServices(
      [
        {
          child: failedChild,
          name: 'Server',
          startupError: spawnError,
          startupErrorListener,
        },
        { child: siblingChild, name: 'Admin' },
      ],
      { killProcess, parentProcess, platform: 'linux' },
    );

    expect(killProcess).toHaveBeenCalledExactlyOnceWith(-94, 'SIGTERM');
    siblingChild.emit('exit', null, 'SIGTERM');
    await expect(completed).rejects.toBe(spawnError);
    expect(failedChild.listenerCount('error')).toBe(0);
  });

  it('stops startup immediately when a signal arrives before supervisor handoff', async () => {
    const root = await createRepositoryConfig();
    const parentProcess = new ChildProcess();
    const [serverChild] = createChildren([75]);
    const spawnChild = vi.fn(() => serverChild);
    const killProcess = vi.fn();
    const log = vi.fn();
    const reservePort = async (port) => {
      let releasePromise;
      return {
        port,
        release() {
          releasePromise ??= (async () => {
            if (port === 5088) parentProcess.emit('SIGINT');
            await Promise.resolve();
          })();
          return releasePromise;
        },
      };
    };

    const completed = runDevelopment({
      baseEnvironment: { PATH: '/tools' },
      killProcess,
      log,
      parentProcess,
      platform: 'linux',
      reservePort,
      root,
      spawnChild,
    });

    await vi.waitFor(() =>
      expect(killProcess).toHaveBeenCalledExactlyOnceWith(-75, 'SIGINT'),
    );
    expect(spawnChild).toHaveBeenCalledTimes(1);
    expect(log).toHaveBeenCalledExactlyOnceWith(
      'Server: http://localhost:3888/api',
    );
    serverChild.emit('exit', null, 'SIGINT');
    await expect(completed).resolves.toBe(130);
    expect(parentProcess.listenerCount('SIGINT')).toBe(0);
    expect(parentProcess.listenerCount('SIGTERM')).toBe(0);
  });

  it('surfaces a startup signal forwarding failure instead of concealing it', async () => {
    const root = await createRepositoryConfig();
    const parentProcess = new ChildProcess();
    const [serverChild] = createChildren([76]);
    const spawnChild = vi.fn(() => serverChild);
    const forwardingError = new Error('signal forwarding failed');
    const killProcess = vi.fn(() => {
      throw forwardingError;
    });
    const log = vi.fn(() => parentProcess.emit('SIGINT'));
    const reservePort = async (port) => ({
      port,
      release: async () => {},
    });

    const completed = runDevelopment({
      baseEnvironment: { PATH: '/tools' },
      killProcess,
      log,
      parentProcess,
      platform: 'linux',
      reservePort,
      root,
      spawnChild,
    });
    const rejection = completed.catch((error) => error);
    await vi.waitFor(() => expect(spawnChild).toHaveBeenCalledTimes(1));

    const reportedError = await rejection;
    expect(reportedError).toBeInstanceOf(AggregateError);
    expect(reportedError.errors).toEqual([forwardingError, forwardingError]);
    expect(spawnChild).toHaveBeenCalledTimes(1);
    expect(parentProcess.listenerCount('SIGINT')).toBe(0);
    expect(parentProcess.listenerCount('SIGTERM')).toBe(0);
  });

  it('reports selected ports and supervises the two development services', async () => {
    const root = await createRepositoryConfig();
    const parentProcess = new ChildProcess();
    const children = createChildren([81, 82]);
    const lifecycle = [];
    let childIndex = 0;
    const spawnChild = vi.fn((_command, arguments_) => {
      lifecycle.push(`spawn:${arguments_[1]}`);
      return children[childIndex++];
    });
    const reservePort = async (port) => ({
      port,
      async release() {
        lifecycle.push(`release-start:${port}`);
        await Promise.resolve();
        lifecycle.push(`release-end:${port}`);
      },
    });
    const log = vi.fn();
    const killProcess = vi.fn();

    const completed = runDevelopment({
      baseEnvironment: { PATH: '/tools' },
      killProcess,
      log,
      parentProcess,
      platform: 'linux',
      root,
      reservePort,
      spawnChild,
    });
    await vi.waitFor(() => expect(spawnChild).toHaveBeenCalledTimes(2));

    expect(log.mock.calls.map(([message]) => message)).toEqual([
      'Server: http://localhost:3888/api',
      'Admin: http://localhost:5088',
    ]);
    expect(lifecycle).toEqual([
      'release-start:3888',
      'release-end:3888',
      'spawn:dev:server',
      'release-start:5088',
      'release-end:5088',
      'spawn:dev:admin',
    ]);
    parentProcess.emit('SIGTERM');
    for (const child of children) child.emit('exit', null, 'SIGTERM');
    await expect(completed).resolves.toBe(143);
  });
});
