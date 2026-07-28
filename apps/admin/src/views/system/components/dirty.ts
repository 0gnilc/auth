import { ElMessageBox } from 'element-plus';

import { $t } from '#/locales';

/** 存在未保存修改时确认是否关闭 Drawer。 */
export async function confirmDrawerClose(changed: boolean): Promise<boolean> {
  if (!changed) return true;

  try {
    await ElMessageBox.confirm(
      $t('page.rbacCommon.unsavedConfirm'),
      $t('page.rbacCommon.unsavedTitle'),
      {
        cancelButtonText: $t('page.rbacCommon.keepEditing'),
        confirmButtonText: $t('page.rbacCommon.discard'),
        type: 'warning',
      },
    );
    return true;
  } catch {
    return false;
  }
}
