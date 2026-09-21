import { beforeEach, describe, expect, it, vi } from 'vitest';

import { confirmDiscardChanges } from './confirm-discard-changes';

const { confirm, translate } = vi.hoisted(() => ({
  confirm: vi.fn(),
  translate: vi.fn((key: string) => key),
}));

vi.mock('element-plus', () => ({
  ElMessageBox: { confirm },
}));

vi.mock('#/locales', () => ({
  $t: translate,
}));

describe('confirmDiscardChanges', () => {
  beforeEach(() => {
    confirm.mockReset();
    translate.mockReset().mockImplementation((key: string) => key);
  });

  it('allows closing without asking when nothing changed', async () => {
    await expect(confirmDiscardChanges(false)).resolves.toBe(true);
    expect(confirm).not.toHaveBeenCalled();
  });

  it('allows closing after discarding changes', async () => {
    confirm.mockResolvedValue(undefined);

    await expect(confirmDiscardChanges(true)).resolves.toBe(true);
    expect(confirm).toHaveBeenCalledOnce();
  });

  it('resolves default text using the translations at invocation time', async () => {
    confirm.mockResolvedValue(undefined);
    await confirmDiscardChanges(true);
    translate.mockImplementation((key) => `updated:${key}`);
    await confirmDiscardChanges(true);
    expect(confirm).toHaveBeenLastCalledWith(
      'updated:unsavedChanges.message',
      'updated:unsavedChanges.title',
      {
        cancelButtonText: 'updated:unsavedChanges.keepEditing',
        confirmButtonText: 'updated:unsavedChanges.discard',
        type: 'warning',
      },
    );
  });

  it.each(['cancel', 'close'])('blocks leaving after %s', async (action) => {
    confirm.mockRejectedValue(action);
    await expect(confirmDiscardChanges(true)).resolves.toBe(false);
    expect(confirm).toHaveBeenCalledOnce();
  });

  it.each([
    [undefined, 'unsavedChanges.discard', 'unsavedChanges.keepEditing'],
    [{}, 'unsavedChanges.discard', 'unsavedChanges.keepEditing'],
    [{ confirmButtonText: 'Leave' }, 'Leave', 'unsavedChanges.keepEditing'],
    [{ cancelButtonText: 'Stay' }, 'unsavedChanges.discard', 'Stay'],
    [{ confirmButtonText: 'Leave', cancelButtonText: 'Stay' }, 'Leave', 'Stay'],
  ])(
    'uses independent button defaults for %j',
    async (options, confirmButtonText, cancelButtonText) => {
      confirm.mockResolvedValue(undefined);
      await expect(confirmDiscardChanges(true, options)).resolves.toBe(true);
      expect(confirm).toHaveBeenCalledWith(
        'unsavedChanges.message',
        'unsavedChanges.title',
        { confirmButtonText, cancelButtonText, type: 'warning' },
      );
    },
  );
});
