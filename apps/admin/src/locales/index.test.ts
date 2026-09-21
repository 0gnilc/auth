import { loadLocalesMapFromDir } from '@vben/locales';

import dayjs from 'dayjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const localesMap = loadLocalesMapFromDir(
  /\.\/langs\/([^/]+)\/([^/]+)\.json$/,
  import.meta.glob('./langs/*/*.json'),
);
const enMessages = await readLocale('en-US');
const haMessages = await readLocale('ha-NG');
const yoMessages = await readLocale('yo-NG');
const zhMessages = await readLocale('zh-CN');

const localeRuntime = vi.hoisted(() => ({
  loadLocaleMessages: vi.fn(),
  locale: { value: 'zh-CN' },
  setLocaleMessage: vi.fn(),
}));

vi.mock('@vben/locales', async (importOriginal) => {
  const original = await importOriginal<typeof import('@vben/locales')>();
  return {
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
    loadLocalesMapFromDir: original.loadLocalesMapFromDir,
    setupI18n: vi.fn(),
  };
});
vi.mock('@vben/preferences', () => ({
  preferences: { app: { locale: 'zh-CN' } },
}));
vi.mock('element-plus/es/locale/lang/en', () => ({ default: {} }));
vi.mock('element-plus/es/locale/lang/zh-cn', () => ({ default: {} }));

describe('admin locale runtime', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(dayjs, 'locale');
    localeRuntime.locale.value = 'zh-CN';
  });

  it('replaces every locale without temporarily switching the active locale', async () => {
    const { applyDynamicMessages } = await import('./index');

    await applyDynamicMessages({
      'en-US': { menu: { title: 'Menu' } },
      'zh-CN': { menu: { title: '菜单' } },
    });

    expect(localeRuntime.setLocaleMessage).toHaveBeenCalledTimes(4);
    expect(localeRuntime.setLocaleMessage).toHaveBeenCalledWith(
      'en-US',
      expect.objectContaining({ menu: { title: 'Menu' } }),
    );
    expect(localeRuntime.setLocaleMessage).toHaveBeenCalledWith(
      'zh-CN',
      expect.objectContaining({ menu: { title: '菜单' } }),
    );
    expect(localeRuntime.setLocaleMessage).toHaveBeenCalledWith(
      'ha-NG',
      expect.any(Object),
    );
    expect(localeRuntime.setLocaleMessage).toHaveBeenCalledWith(
      'yo-NG',
      expect.any(Object),
    );
    expect(localeRuntime.locale.value).toBe('zh-CN');
    expect(localeRuntime.loadLocaleMessages).not.toHaveBeenCalled();
    expect(dayjs.locale).not.toHaveBeenCalled();
  });

  it('loads business modules as top-level namespaces and keeps static messages above dynamic messages', async () => {
    const { applyDynamicMessages } = await import('./index');

    await applyDynamicMessages({
      'en-US': { systemAdmin: { title: 'Database administrator title' } },
    });

    expect(localeRuntime.setLocaleMessage).toHaveBeenCalledWith(
      'en-US',
      expect.objectContaining({
        authentication: expect.any(Object),
        common: { ok: 'OK' },
        ...enMessages,
      }),
    );
    for (const [locale, messages] of [
      ['ha-NG', haMessages],
      ['yo-NG', yoMessages],
      ['zh-CN', zhMessages],
    ]) {
      expect(localeRuntime.setLocaleMessage).toHaveBeenCalledWith(
        locale,
        expect.objectContaining(messages),
      );
    }
  });

  it('keeps Chinese keys complete and partial locale keys within the English catalog', () => {
    expect(messageKeys(zhMessages)).toEqual(messageKeys(enMessages));
    for (const messages of [haMessages, yoMessages]) {
      expect(messageKeys(enMessages)).toEqual(
        expect.arrayContaining(messageKeys(messages)),
      );
    }
  });
});

function messageKeys(value: unknown, prefix = ''): string[] {
  if (!value || typeof value !== 'object') return [];
  return Object.entries(value)
    .flatMap(([key, child]) => {
      const path = prefix ? `${prefix}.${key}` : key;
      return typeof child === 'object' && child !== null
        ? messageKeys(child, path)
        : [path];
    })
    .toSorted();
}

async function readLocale(locale: string): Promise<Record<string, unknown>> {
  const loader = localesMap[locale];
  if (!loader) return {};
  const messages = await loader();
  return messages.default;
}
