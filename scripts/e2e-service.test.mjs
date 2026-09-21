import { describe, expect, it, vi } from 'vitest';

import { createServiceDefinition, runService } from './e2e-service.mjs';

const e2eEnvironment = {
  E2E_ADMIN_PORT: '5089',
  E2E_SERVER_PORT: '3889',
  PATH: '/tools',
};

describe('end-to-end service launcher', () => {
  it('builds Admin runtime overrides from the selected E2E ports', () => {
    expect(createServiceDefinition('admin', e2eEnvironment, 'linux')).toEqual({
      args: ['--filter=@app/admin', 'run', 'dev', '--host', '127.0.0.1'],
      command: 'pnpm',
      environment: {
        E2E_ADMIN_PORT: '5089',
        E2E_API_TARGET: 'http://127.0.0.1:3889',
        E2E_SERVER_PORT: '3889',
        PATH: '/tools',
        VITE_GLOB_API_URL: 'http://127.0.0.1:3889/api',
        VITE_PORT: '5089',
      },
      port: 5089,
    });
  });

  it('checks and releases the selected port immediately before startup', async () => {
    const release = vi.fn().mockResolvedValue(undefined);
    const commandRunner = vi.fn(() => ({ status: 0 }));

    await expect(
      runService('admin', {
        baseEnvironment: e2eEnvironment,
        commandRunner,
        platform: 'linux',
        reservePort: vi.fn().mockResolvedValue({ port: 5089, release }),
        root: '/repo',
      }),
    ).resolves.toBe(0);

    expect(release).toHaveBeenCalledOnce();
    expect(commandRunner).toHaveBeenCalledOnce();
  });

  it('rejects an occupied or invalid selected port before spawning', async () => {
    const commandRunner = vi.fn();
    await expect(
      runService('admin', {
        baseEnvironment: e2eEnvironment,
        commandRunner,
        reservePort: vi.fn().mockResolvedValue(null),
      }),
    ).rejects.toThrow('admin E2E port 5089 became unavailable');
    expect(commandRunner).not.toHaveBeenCalled();

    expect(() =>
      createServiceDefinition('admin', {
        ...e2eEnvironment,
        E2E_ADMIN_PORT: '65536',
      }),
    ).toThrow('Invalid port in E2E_ADMIN_PORT: 65536');
  });
});
