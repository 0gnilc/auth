import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridColumns } from '#/adapter/vxe-table';
import type { AdminApi } from '#/api';

import { $t } from '#/locales';

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'username',
      label: $t('page.systemAdmin.form.username'),
    },
    {
      component: 'Input',
      fieldName: 'nickname',
      label: $t('page.systemAdmin.form.nickname'),
    },
    {
      component: 'Select',
      componentProps: {
        clearable: true,
        options: [
          { label: $t('page.rbacCommon.enabled'), value: true },
          { label: $t('page.rbacCommon.disabled'), value: false },
        ],
      },
      fieldName: 'status',
      label: $t('page.systemAdmin.filters.status'),
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
      title: $t('page.systemAdmin.table.username'),
    },
    {
      field: 'nickname',
      minWidth: 150,
      title: $t('page.systemAdmin.table.nickname'),
    },
    {
      field: 'roleCodes',
      minWidth: 220,
      showOverflow: false,
      slots: { default: 'roles' },
      title: $t('page.systemAdmin.table.roles'),
    },
    {
      align: 'center',
      cellRender: {
        attrs: { beforeChange: onStatusChange },
        name: onStatusChange ? 'CellSwitch' : 'CellTag',
      },
      field: 'status',
      title: $t('page.systemAdmin.table.status'),
      width: 100,
    },
    {
      field: 'createTime',
      formatter: 'formatDateTime',
      title: $t('page.systemAdmin.table.createTime'),
      width: 180,
    },
    {
      align: 'center',
      field: 'operation',
      fixed: 'right',
      slots: { default: 'action' },
      title: $t('page.rbacCommon.actions'),
      width: 210,
    },
  ];
}
