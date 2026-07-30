import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridColumns } from '#/adapter/vxe-table';
import type { PermissionApi } from '#/api';

import { $t } from '#/locales';

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'code',
      label: $t('page.systemPermission.form.code'),
    },
    {
      component: 'Input',
      fieldName: 'name',
      label: $t('page.systemPermission.form.name'),
    },
    {
      component: 'Input',
      fieldName: 'targetIdentifier',
      label: $t('page.systemPermission.form.targetIdentifier'),
    },
    {
      component: 'Select',
      componentProps: {
        clearable: true,
        options: [
          { label: $t('page.systemPermission.public'), value: true },
          { label: $t('page.systemPermission.protected'), value: false },
        ],
      },
      fieldName: 'publicAccess',
      label: $t('page.systemPermission.filters.publicAccess'),
    },
  ];
}

export function useColumns(): VxeTableGridColumns<PermissionApi.Permission> {
  return [
    {
      field: 'code',
      minWidth: 210,
      title: $t('page.systemPermission.table.code'),
    },
    {
      field: 'name',
      minWidth: 170,
      title: $t('page.systemPermission.table.name'),
    },
    {
      field: 'targetQualifier',
      title: $t('page.systemPermission.table.qualifier'),
      width: 100,
    },
    {
      field: 'targetIdentifier',
      minWidth: 240,
      title: $t('page.systemPermission.table.target'),
    },
    {
      align: 'center',
      field: 'publicAccess',
      slots: { default: 'access' },
      title: $t('page.systemPermission.table.access'),
      width: 120,
    },
    {
      align: 'center',
      field: 'builtIn',
      slots: { default: 'type' },
      title: $t('page.systemPermission.table.type'),
      width: 110,
    },
    {
      align: 'center',
      field: 'operation',
      fixed: 'right',
      slots: { default: 'action' },
      title: $t('page.rbacCommon.actions'),
      width: 120,
    },
  ];
}
