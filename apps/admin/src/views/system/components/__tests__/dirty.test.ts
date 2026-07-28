import { beforeEach, describe, expect, it, vi } from 'vitest';

import { confirmDrawerClose } from '../dirty';

const { confirm } = vi.hoisted(() => ({ confirm: vi.fn() }));

vi.mock('element-plus', () => ({
  ElMessageBox: { confirm },
}));

vi.mock('#/locales', () => ({
  $t: (key: string) => key,
}));

describe('confirmDrawerClose', () => {
  beforeEach(() => {
    confirm.mockReset();
  });

  it('allows closing without asking when nothing changed', async () => {
    await expect(confirmDrawerClose(false)).resolves.toBe(true);
    expect(confirm).not.toHaveBeenCalled();
  });

  it('allows closing after discarding changes', async () => {
    confirm.mockResolvedValue(undefined);

    await expect(confirmDrawerClose(true)).resolves.toBe(true);
    expect(confirm).toHaveBeenCalledOnce();
  });

  it('keeps the drawer open when discarding is cancelled', async () => {
    confirm.mockRejectedValue(new Error('cancelled'));

    await expect(confirmDrawerClose(true)).resolves.toBe(false);
    expect(confirm).toHaveBeenCalledOnce();
  });
});
