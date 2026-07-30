import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridColumns } from '#/adapter/vxe-table';
import type { RoleApi } from '#/api';

import { $t } from '#/locales';

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'code',
      label: $t('page.systemRole.form.code'),
    },
    {
      component: 'Input',
      fieldName: 'name',
      label: $t('page.systemRole.form.name'),
    },
    {
      component: 'Select',
      componentProps: {
        clearable: true,
        options: [
          { label: $t('page.rbacCommon.builtIn'), value: true },
          { label: $t('page.rbacCommon.custom'), value: false },
        ],
      },
      fieldName: 'builtIn',
      label: $t('page.systemRole.table.type'),
    },
  ];
}

export function useColumns(): VxeTableGridColumns<RoleApi.Role> {
  return [
    {
      field: 'code',
      minWidth: 170,
      title: $t('page.systemRole.table.code'),
    },
    {
      field: 'name',
      minWidth: 170,
      title: $t('page.systemRole.table.name'),
    },
    {
      field: 'remark',
      minWidth: 220,
      title: $t('page.systemRole.table.remark'),
    },
    {
      align: 'center',
      field: 'builtIn',
      slots: { default: 'type' },
      title: $t('page.systemRole.table.type'),
      width: 110,
    },
    {
      field: 'createTime',
      formatter: 'formatDateTime',
      title: $t('page.systemRole.table.createTime'),
      width: 180,
    },
    {
      align: 'center',
      field: 'operation',
      fixed: 'right',
      slots: { default: 'action' },
      title: $t('page.rbacCommon.actions'),
      width: 280,
    },
  ];
}
