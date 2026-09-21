import { fileURLToPath, URL } from 'node:url';

import Vue from '@vitejs/plugin-vue';
import VueJsx from '@vitejs/plugin-vue-jsx';
import { configDefaults, defineConfig } from 'vitest/config';

export default defineConfig({
  plugins: [Vue(), VueJsx()],
  resolve: {
    alias: {
      '#': fileURLToPath(new URL('apps/admin/src', import.meta.url)),
    },
  },
  test: {
    environment: 'happy-dom',
    environmentOptions: {
      happyDOM: {
        settings: {
          // 禁用资源加载时仍触发成功回调，兼容不依赖真实外部脚本加载的组件测试。
          handleDisabledFileLoadingAsSuccess: true,
        },
      },
    },
    exclude: [
      ...configDefaults.exclude,
      '**/e2e/**',
      '**/dist/**',
      '**/.{idea,git,cache,output,temp}/**',
      '**/node_modules/**',
      '**/{stylelint,eslint}.config.*',
      '**/{oxfmt,oxlint}.config.*',
    ],
  },
});
