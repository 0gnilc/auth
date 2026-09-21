import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridColumns } from '#/adapter/vxe-table';
import type { AdminApi } from '#/api/system';

import { $t } from '#/locales';

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'username',
      label: $t('systemAdmin.form.username'),
    },
    {
      component: 'Input',
      fieldName: 'nickname',
      label: $t('systemAdmin.form.nickname'),
    },
    {
      component: 'Select',
      componentProps: {
        clearable: true,
        options: [
          { label: $t('rbacCommon.enabled'), value: true },
          { label: $t('rbacCommon.disabled'), value: false },
        ],
      },
      fieldName: 'status',
      label: $t('systemAdmin.filters.status'),
    },
  ];
}

export function useColumns(
  onStatusChange?: (
    newStatus: boolean,
    row: AdminApi.Admin,
  ) => PromiseLike<boolean | undefined>,
): VxeTableGridColumns<AdminApi.Admin> {
  return [
    {
      field: 'username',
      minWidth: 150,
      title: $t('systemAdmin.table.username'),
    },
    {
      field: 'nickname',
      minWidth: 150,
      title: $t('systemAdmin.table.nickname'),
    },
    {
      field: 'roleCodes',
      minWidth: 220,
      showOverflow: false,
      slots: { default: 'roles' },
      title: $t('systemAdmin.table.roles'),
    },
    {
      align: 'center',
      cellRender: {
        attrs: { beforeChange: onStatusChange },
        name: onStatusChange ? 'CellSwitch' : 'CellTag',
      },
      field: 'status',
      title: $t('systemAdmin.table.status'),
      width: 100,
    },
    {
      field: 'createTime',
      formatter: 'formatDateTime',
      title: $t('systemAdmin.table.createTime'),
      width: 180,
    },
    {
      align: 'center',
      field: 'operation',
      fixed: 'right',
      slots: { default: 'action' },
      title: $t('rbacCommon.actions'),
      width: 200,
    },
  ];
}
