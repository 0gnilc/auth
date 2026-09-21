import { describe, expect, it, vi } from 'vitest';

const startupRuntime = vi.hoisted(() => ({
  events: [] as string[],
}));

vi.mock('@vben/styles', () => {
  startupRuntime.events.push('styles');
  return {};
});
vi.mock('@vben/styles/ele', () => {
  startupRuntime.events.push('element-styles');
  return {};
});
vi.mock('@vben/preferences', () => ({
  initPreferences: vi.fn(async () => {
    startupRuntime.events.push('preferences');
  }),
}));
vi.mock('@vben/utils', () => ({
  unmountGlobalLoading: vi.fn(() => {
    startupRuntime.events.push('loading-removed');
  }),
}));
vi.mock('../preferences', () => ({ overridesPreferences: {} }));
vi.mock('../bootstrap', () => ({
  bootstrap: vi.fn(async () => {
    startupRuntime.events.push('bootstrap');
  }),
}));

describe('admin startup styles', () => {
  it('loads global styles before the asynchronous application bootstrap', async () => {
    await import('../main');

    await vi.waitFor(() => {
      expect(startupRuntime.events).toContain('loading-removed');
    });

    expect(startupRuntime.events.indexOf('styles')).toBeLessThan(
      startupRuntime.events.indexOf('bootstrap'),
    );
    expect(startupRuntime.events.indexOf('element-styles')).toBeLessThan(
      startupRuntime.events.indexOf('bootstrap'),
    );
  });
});
