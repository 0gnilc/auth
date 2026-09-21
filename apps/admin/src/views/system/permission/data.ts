import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridColumns } from '#/adapter/vxe-table';
import type { PermissionApi } from '#/api/system';

import { $t } from '#/locales';

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'code',
      label: $t('systemPermission.form.code'),
    },
    {
      component: 'Input',
      fieldName: 'name',
      label: $t('systemPermission.form.name'),
    },
    {
      component: 'Input',
      fieldName: 'targetIdentifier',
      label: $t('systemPermission.form.targetIdentifier'),
    },
    {
      component: 'Select',
      componentProps: {
        clearable: true,
        options: [
          { label: $t('systemPermission.public'), value: true },
          { label: $t('systemPermission.protected'), value: false },
        ],
      },
      fieldName: 'publicAccess',
      label: $t('systemPermission.filters.publicAccess'),
    },
  ];
}

export function useColumns(): VxeTableGridColumns<PermissionApi.Permission> {
  return [
    {
      field: 'code',
      minWidth: 210,
      title: $t('systemPermission.table.code'),
    },
    {
      field: 'name',
      minWidth: 170,
      title: $t('systemPermission.table.name'),
    },
    {
      field: 'targetQualifier',
      title: $t('systemPermission.table.qualifier'),
      width: 100,
    },
    {
      field: 'targetIdentifier',
      minWidth: 240,
      title: $t('systemPermission.table.target'),
    },
    {
      align: 'center',
      field: 'publicAccess',
      slots: { default: 'access' },
      title: $t('systemPermission.table.access'),
      width: 120,
    },
    {
      align: 'center',
      field: 'builtIn',
      slots: { default: 'type' },
      title: $t('systemPermission.table.type'),
      width: 110,
    },
    {
      align: 'center',
      field: 'operation',
      fixed: 'right',
      slots: { default: 'action' },
      title: $t('rbacCommon.actions'),
      width: 120,
    },
  ];
}
