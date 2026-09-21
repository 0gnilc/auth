import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridColumns } from '#/adapter/vxe-table';
import type { RoleApi } from '#/api/system';

import { $t } from '#/locales';

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'code',
      label: $t('systemRole.form.code'),
    },
    {
      component: 'Input',
      fieldName: 'name',
      label: $t('systemRole.form.name'),
    },
    {
      component: 'Select',
      componentProps: {
        clearable: true,
        options: [
          { label: $t('rbacCommon.builtIn'), value: true },
          { label: $t('rbacCommon.custom'), value: false },
        ],
      },
      fieldName: 'builtIn',
      label: $t('systemRole.table.type'),
    },
  ];
}

export function useColumns(): VxeTableGridColumns<RoleApi.Role> {
  return [
    {
      field: 'code',
      minWidth: 170,
      title: $t('systemRole.table.code'),
    },
    {
      field: 'name',
      minWidth: 170,
      title: $t('systemRole.table.name'),
    },
    {
      field: 'remark',
      minWidth: 220,
      title: $t('systemRole.table.remark'),
    },
    {
      align: 'center',
      field: 'builtIn',
      slots: { default: 'type' },
      title: $t('systemRole.table.type'),
      width: 110,
    },
    {
      field: 'createTime',
      formatter: 'formatDateTime',
      title: $t('systemRole.table.createTime'),
      width: 180,
    },
    {
      align: 'center',
      field: 'operation',
      fixed: 'right',
      slots: { default: 'action' },
      title: $t('rbacCommon.actions'),
      width: 280,
    },
  ];
}
