import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridColumns } from '#/adapter/vxe-table';
import type { MenuApi } from '#/api';

import { $t } from '#/locales';

export type MenuType = MenuApi.Menu['type'];

export interface MenuForm {
  accessCode: null | string;
  activePath: null | string;
  affixTab: boolean;
  affixTabOrder: null | number;
  badge: null | string;
  badgeType: MenuApi.Menu['badgeType'];
  badgeVariants: MenuApi.Menu['badgeVariants'];
  component: null | string;
  fullPathKey: boolean;
  hideChildrenInMenu: boolean;
  hideInBreadcrumb: boolean;
  hideInMenu: boolean;
  hideInTab: boolean;
  icon: null | string;
  id?: string;
  iframeSrc: null | string;
  keepAlive: boolean;
  link: null | string;
  maxNumOfOpenTab: null | number;
  name: string;
  noBasicLayout: boolean;
  openInNewWindow: boolean;
  order: number;
  path: null | string;
  pid: string;
  query: null | string;
  redirect: null | string;
  status: boolean;
  title: string;
  type: MenuType;
}

export function createMenuForm(pid = '0', type: MenuType = 'menu'): MenuForm {
  return {
    accessCode: null,
    activePath: null,
    affixTab: false,
    affixTabOrder: null,
    badge: null,
    badgeType: null,
    badgeVariants: null,
    component: null,
    fullPathKey: true,
    hideChildrenInMenu: false,
    hideInBreadcrumb: false,
    hideInMenu: false,
    hideInTab: false,
    icon: null,
    iframeSrc: null,
    keepAlive: false,
    link: null,
    maxNumOfOpenTab: null,
    name: '',
    noBasicLayout: false,
    openInNewWindow: false,
    order: 999,
    path: null,
    pid,
    query: null,
    redirect: null,
    status: true,
    title: '',
    type,
  };
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'keyword',
      label: $t('page.systemMenu.filters.keyword'),
    },
  ];
}

export function useColumns(): VxeTableGridColumns<MenuApi.Menu> {
  return [
    {
      field: 'title',
      minWidth: 260,
      slots: { default: 'title' },
      title: $t('page.systemMenu.table.title'),
      treeNode: true,
    },
    {
      field: 'name',
      minWidth: 150,
      title: $t('page.systemMenu.table.name'),
    },
    {
      align: 'center',
      field: 'type',
      slots: { default: 'type' },
      title: $t('page.systemMenu.table.type'),
      width: 110,
    },
    {
      field: 'accessCode',
      minWidth: 230,
      slots: { default: 'accessCode' },
      title: $t('page.systemMenu.table.accessCode'),
    },
    {
      field: 'path',
      minWidth: 180,
      title: $t('page.systemMenu.table.path'),
    },
    {
      align: 'center',
      field: 'status',
      slots: { default: 'status' },
      title: $t('page.systemMenu.table.status'),
      width: 100,
    },
    {
      align: 'center',
      field: 'operation',
      fixed: 'right',
      slots: { default: 'action' },
      title: $t('page.rbacCommon.actions'),
      width: 230,
    },
  ];
}
