import process from 'node:process';

import { defineConfig, viteCssLayerPlugin } from '@vben/vite-config';

import ElementPlus from 'unplugin-element-plus/vite';

export default defineConfig(async () => {
  const runtimePort = Number(process.env.VITE_PORT);
  return {
    application: {},
    vite: {
      plugins: [
        // element-plus 的 css 包进 @layer el，使 Tailwind 工具类可覆盖组件样式
        viteCssLayerPlugin({ layerName: 'el', packageName: 'element-plus' }),
        ElementPlus({ format: 'esm' }),
      ],
      server: {
        ...(Number.isInteger(runtimePort) && runtimePort > 0
          ? { port: runtimePort }
          : {}),
        proxy: {
          '/api': {
            changeOrigin: true,
            target: process.env.E2E_API_TARGET,
            ws: true,
          },
        },
        // 启动器已选定并注入端口；被其他进程抢占时应失败，不能静默换端口使代理与测试访问错位。
        strictPort: true,
      },
    },
  };
});
