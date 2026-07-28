import { beforeEach, describe, expect, it, vi } from 'vitest';

const api = vi.hoisted(() => ({
  getI18nMessageBundle: vi.fn(),
}));
const locales = vi.hoisted(() => ({
  applyDynamicMessages: vi.fn(),
}));

vi.mock('#/api/core/i18n-message', () => api);
vi.mock('./index', () => locales);

describe('dynamic internationalization messages', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.resetModules();
  });

  it('replaces the complete runtime bundle on every explicit reload', async () => {
    const first = { 'zh-CN': { menu: { title: 'First' } } };
    const second = { 'zh-CN': { menu: { title: 'Second' } } };
    api.getI18nMessageBundle
      .mockResolvedValueOnce(first)
      .mockResolvedValueOnce(second);
    const { reloadDynamicMessages } = await import('./dynamic');

    await reloadDynamicMessages();
    await reloadDynamicMessages();

    expect(locales.applyDynamicMessages).toHaveBeenNthCalledWith(1, first);
    expect(locales.applyDynamicMessages).toHaveBeenNthCalledWith(2, second);
  });

  it('allows a failed initial load to retry without blocking the caller', async () => {
    const bundle = { 'zh-CN': { menu: { title: 'Dashboard' } } };
    api.getI18nMessageBundle
      .mockRejectedValueOnce(new Error('network'))
      .mockResolvedValueOnce(bundle);
    const { loadDynamicMessages } = await import('./dynamic');

    await expect(loadDynamicMessages()).resolves.toBe(false);
    await expect(loadDynamicMessages()).resolves.toBe(true);
    await expect(loadDynamicMessages()).resolves.toBe(true);

    expect(api.getI18nMessageBundle).toHaveBeenCalledTimes(2);
    expect(locales.applyDynamicMessages).toHaveBeenCalledWith(bundle);
  });

  it('clears runtime messages and makes the next load request again', async () => {
    const bundle = { 'zh-CN': { menu: { title: 'Dashboard' } } };
    api.getI18nMessageBundle.mockResolvedValue(bundle);
    const { clearDynamicMessages, loadDynamicMessages } =
      await import('./dynamic');

    await loadDynamicMessages();
    await clearDynamicMessages();
    await loadDynamicMessages();

    expect(locales.applyDynamicMessages).toHaveBeenCalledWith({});
    expect(api.getI18nMessageBundle).toHaveBeenCalledTimes(2);
  });
});
