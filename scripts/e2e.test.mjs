import { describe, expect, it, vi } from 'vitest';

import { createE2eEnvironment, runE2e } from './e2e.mjs';

describe('end-to-end test launcher', () => {
  it('runs Playwright on reserved ports without disturbing existing services', async () => {
    const releaseServer = vi.fn().mockResolvedValue(undefined);
    const releaseAdmin = vi.fn().mockResolvedValue(undefined);
    const commandRunner = vi.fn(() => ({ status: 0 }));
    const baseEnvironment = { PATH: '/tools' };

    await expect(
      runE2e({
        args: ['--', '--project=admin'],
        baseEnvironment,
        commandRunner,
        loadPorts: vi.fn().mockResolvedValue({
          admin: 5088,
          server: 3888,
        }),
        platform: 'linux',
        reservePorts: vi.fn().mockResolvedValue({
          ports: { admin: 5089, server: 3889 },
          reservations: {
            admin: { release: releaseAdmin },
            server: { release: releaseServer },
          },
        }),
        root: '/repo',
      }),
    ).resolves.toBe(0);

    expect(releaseServer).toHaveBeenCalledOnce();
    expect(releaseAdmin).toHaveBeenCalledOnce();
    expect(commandRunner).toHaveBeenCalledWith(
      'pnpm',
      ['exec', 'playwright', 'test', '--project=admin'],
      {
        cwd: '/repo',
        env: {
          E2E_ADMIN_PORT: '5089',
          E2E_SERVER_PORT: '3889',
          PATH: '/tools',
        },
        shell: false,
        stdio: 'inherit',
      },
    );
    expect(baseEnvironment).toEqual({ PATH: '/tools' });
  });

  it('derives process-only Playwright port overrides', () => {
    const environment = { PATH: '/tools' };

    expect(
      createE2eEnvironment({ admin: 5089, server: 3889 }, environment),
    ).toEqual({
      E2E_ADMIN_PORT: '5089',
      E2E_SERVER_PORT: '3889',
      PATH: '/tools',
    });
    expect(environment).toEqual({ PATH: '/tools' });
  });
});
