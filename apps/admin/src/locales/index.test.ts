import { beforeEach, describe, expect, it, vi } from 'vitest';

const localeRuntime = vi.hoisted(() => ({
  loadLocaleMessages: vi.fn(),
  locale: { value: 'zh-CN' },
  setLocaleMessage: vi.fn(),
}));
const dayjsRuntime = vi.hoisted(() => ({ locale: vi.fn() }));

vi.mock('@vben/locales', () => ({
  $t: (key: string) => key,
  i18n: {
    global: {
      fallbackLocale: { value: 'zh-CN' },
      locale: localeRuntime.locale,
      setLocaleMessage: localeRuntime.setLocaleMessage,
    },
  },
  loadCoreLocaleMessages: vi.fn(async () => ({ common: { ok: 'OK' } })),
  loadLocaleMessages: localeRuntime.loadLocaleMessages,
  loadLocalesMapFromDir: vi.fn(() => ({
    'en-US': async () => ({ default: {} }),
    'zh-CN': async () => ({ default: {} }),
  })),
  setupI18n: vi.fn(),
}));
vi.mock('@vben/preferences', () => ({
  preferences: { app: { locale: 'zh-CN' } },
}));
vi.mock('dayjs', () => ({ default: { locale: dayjsRuntime.locale } }));
vi.mock('element-plus/es/locale/lang/en', () => ({ default: {} }));
vi.mock('element-plus/es/locale/lang/zh-cn', () => ({ default: {} }));

describe('admin locale runtime', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localeRuntime.locale.value = 'zh-CN';
  });

  it('replaces every locale without temporarily switching the active locale', async () => {
    const { applyDynamicMessages } = await import('./index');

    await applyDynamicMessages({
      'en-US': { menu: { title: 'Menu' } },
      'zh-CN': { menu: { title: '菜单' } },
    });

    expect(localeRuntime.setLocaleMessage).toHaveBeenCalledTimes(2);
    expect(localeRuntime.setLocaleMessage).toHaveBeenCalledWith(
      'en-US',
      expect.objectContaining({ menu: { title: 'Menu' } }),
    );
    expect(localeRuntime.setLocaleMessage).toHaveBeenCalledWith(
      'zh-CN',
      expect.objectContaining({ menu: { title: '菜单' } }),
    );
    expect(localeRuntime.locale.value).toBe('zh-CN');
    expect(localeRuntime.loadLocaleMessages).not.toHaveBeenCalled();
    expect(dayjsRuntime.locale).not.toHaveBeenCalled();
  });
});
