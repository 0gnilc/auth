import { resolve } from 'node:path';

import { resolveConfig } from 'vite';
import { afterEach, describe, expect, it, vi } from 'vitest';

const VITE_CONFIG_TIMEOUT_MS = 15_000;

afterEach(() => {
  vi.unstubAllEnvs();
});

// 加载真实 Vite 配置，但不启动监听服务；验证的是启动器覆写值及禁止自动换端口的配置。
describe('development frontend ports', () => {
  it(
    'uses the injected Admin port and fails instead of silently changing it',
    async () => {
      vi.stubEnv('E2E_API_TARGET', 'http://localhost:3999');
      vi.stubEnv('VITE_PORT', '5099');

      const config = await resolveConfig(
        { configFile: resolve('apps/admin/vite.config.ts') },
        'serve',
        'development',
      );

      expect(config.server).toMatchObject({
        port: 5099,
        strictPort: true,
      });
      expect(config.server.proxy?.['/api']).toMatchObject({
        target: 'http://localhost:3999',
      });
    },
    VITE_CONFIG_TIMEOUT_MS,
  );
});
