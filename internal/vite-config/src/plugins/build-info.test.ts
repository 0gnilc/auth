import fs from 'node:fs';
import fsp from 'node:fs/promises';

import { describe, expect, it, vi } from 'vitest';

import { resolveBuildInfo } from './build-info';
import { viteInjectAppLoadingPlugin } from './inject-app-loading';

// 不依赖 Git 仓库的隔离环境用于覆盖源码归档与容器构建。
const archiveRoot = '/nonexistent-gnilc-source-archive';

describe('build identity', () => {
  it('marks ordinary builds as development independently of product version', () => {
    expect(resolveBuildInfo('1.0.0', archiveRoot, {})).toMatchObject({
      version: '1.0.0',
      channel: 'development',
      revision: 'local',
    });
  });
  it('requires a full revision for releases', () => {
    expect(() =>
      resolveBuildInfo('1.0.0', archiveRoot, {
        GNILC_BUILD_CHANNEL: 'release',
      }),
    ).toThrow('full commit');
    expect(
      resolveBuildInfo('1.0.0', archiveRoot, {
        GNILC_BUILD_CHANNEL: 'release',
        GNILC_BUILD_REVISION: 'a'.repeat(40),
        SOURCE_DATE_EPOCH: '0',
      }),
    ).toMatchObject({ buildTime: '1970-01-01T00:00:00.000Z' });
  });
  it('rejects invalid timestamps instead of substituting the current time', () => {
    expect(() =>
      resolveBuildInfo('1.0.0', archiveRoot, {
        SOURCE_DATE_EPOCH: 'yesterday',
      }),
    ).toThrow('Invalid SOURCE_DATE_EPOCH');
  });
  it('keeps the loading theme key stable across product versions', async () => {
    const exists = vi.spyOn(fs, 'existsSync').mockReturnValue(true);
    const read = vi
      .spyOn(fsp, 'readFile')
      .mockResolvedValue('<div>loading</div>');
    try {
      const render = async (version: string) => {
        const plugin = await viteInjectAppLoadingPlugin(true, {
          VITE_APP_STORAGE_NAMESPACE: 'gnilc-auth-admin-storage-v1',
          VITE_APP_VERSION: version,
        });
        if (
          !plugin ||
          typeof plugin !== 'object' ||
          !('transformIndexHtml' in plugin)
        )
          throw new Error('Missing HTML plugin');
        const hook = plugin.transformIndexHtml;
        if (!hook || typeof hook === 'function' || !hook.handler)
          throw new Error('Missing HTML handler');
        return Reflect.apply(hook.handler, {}, ['<body></body>']);
      };
      const before = await render('1.0.0');
      expect(before).toContain(
        'gnilc-auth-admin-storage-v1-prod-preferences-theme',
      );
      expect(await render('1.0.1')).toBe(before);
    } finally {
      exists.mockRestore();
      read.mockRestore();
    }
  });
  it('requires an explicit storage schema namespace for the loading theme', async () => {
    await expect(viteInjectAppLoadingPlugin(true, {})).rejects.toThrow(
      'VITE_APP_STORAGE_NAMESPACE',
    );
  });
});
