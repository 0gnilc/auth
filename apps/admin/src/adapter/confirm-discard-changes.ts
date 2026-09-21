import { ElMessageBox } from 'element-plus';

import { $t } from '#/locales';

export async function confirmDiscardChanges(
  hasUnsavedChanges: boolean,
  options?: {
    cancelButtonText?: string;
    confirmButtonText?: string;
  },
): Promise<boolean> {
  if (!hasUnsavedChanges) return true;

  try {
    await ElMessageBox.confirm(
      $t('unsavedChanges.message'),
      $t('unsavedChanges.title'),
      {
        cancelButtonText:
          options?.cancelButtonText ?? $t('unsavedChanges.keepEditing'),
        confirmButtonText:
          options?.confirmButtonText ?? $t('unsavedChanges.discard'),
        type: 'warning',
      },
    );
    return true;
  } catch {
    return false;
  }
}
