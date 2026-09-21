import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridColumns } from '#/adapter/vxe-table';
import type { MenuApi } from '#/api/system';

import { $t } from '#/locales';

/** 导航菜单种类，沿用服务端菜单契约。 */
export type MenuType = MenuApi.Menu['type'];

export const menuTypeMessageKeys = {
  button: 'systemMenu.types.button',
  catalog: 'systemMenu.types.catalog',
  embedded: 'systemMenu.types.embedded',
  link: 'systemMenu.types.link',
  menu: 'systemMenu.types.menu',
} as const satisfies Record<MenuType, string>;

export const menuTypeTagTypes = {
  button: 'danger',
  catalog: 'primary',
  embedded: 'success',
  link: 'info',
  menu: 'warning',
} as const satisfies Record<
  MenuType,
  'danger' | 'info' | 'primary' | 'success' | 'warning'
>;

/** 完整菜单编辑表单；省略字段不表示保留原值，未配置值显式保存为 null。 */
export interface MenuForm {
  /** 前端访问控制码；按钮权限据此控制展示，未配置时为 null。 */
  accessCode: null | string;
  /** 激活导航时高亮的菜单路径；未配置时为 null。 */
  activePath: null | string;
  /** 是否固定该页面的标签页。 */
  affixTab: boolean;
  /** 固定标签页的排序值；未配置时为 null。 */
  affixTabOrder: null | number;
  /** 静态徽标内容；未配置时为 null。 */
  badge: null | string;
  /** 徽标类型：圆点或普通徽标；未配置时为 null。 */
  badgeType: MenuApi.Menu['badgeType'];
  /** 徽标配色变体；未配置时为 null。 */
  badgeVariants: MenuApi.Menu['badgeVariants'];
  /** 内部页面的组件映射标识；不适用或未配置时为 null。 */
  component: null | string;
  /** 是否用包含查询参数的完整路径区分标签页。 */
  fullPathKey: boolean;
  /** 是否隐藏导航中的子菜单。 */
  hideChildrenInMenu: boolean;
  /** 是否在面包屑中隐藏。 */
  hideInBreadcrumb: boolean;
  /** 是否在导航菜单中隐藏。 */
  hideInMenu: boolean;
  /** 是否在标签页中隐藏。 */
  hideInTab: boolean;
  /** 菜单图标标识；未配置时为 null。 */
  icon: null | string;
  /** 待编辑菜单 ID；创建时省略。 */
  id?: string;
  /** 内嵌页面地址；非内嵌菜单或未配置时为 null。 */
  iframeSrc: null | string;
  /** 是否缓存页面状态。 */
  keepAlive: boolean;
  /** 外部链接地址；非外链菜单或未配置时为 null。 */
  link: null | string;
  /** 同一路由可打开的标签页数量上限；未配置时为 null。 */
  maxNumOfOpenTab: null | number;
  /** 唯一的导航路由名称。 */
  name: string;
  /** 是否脱离 Admin 基础布局展示。 */
  noBasicLayout: boolean;
  /** 是否在新窗口打开目标。 */
  openInNewWindow: boolean;
  /** 同层导航菜单排序值。 */
  order: number;
  /** 导航路由路径；不适用或未配置时为 null。 */
  path: null | string;
  /** 父菜单 ID，字符串 0 表示根节点。 */
  pid: string;
  /** 菜单预设查询参数文本；未配置时为 null。 */
  query: null | string;
  /** 导航重定向目标；未配置时为 null。 */
  redirect: null | string;
  /** 菜单是否启用；禁用不自动删除已有角色授予关系。 */
  status: boolean;
  /** 菜单显示标题或动态消息键。 */
  title: string;
  /** 菜单种类：目录、内部页面、内嵌页面、外链或按钮。 */
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
      label: $t('systemMenu.filters.keyword'),
    },
  ];
}

export function useColumns(): VxeTableGridColumns<MenuApi.Menu> {
  return [
    {
      field: 'title',
      minWidth: 260,
      slots: { default: 'title' },
      title: $t('systemMenu.table.title'),
      treeNode: true,
    },
    {
      field: 'name',
      minWidth: 150,
      title: $t('systemMenu.table.name'),
    },
    {
      align: 'center',
      field: 'type',
      slots: { default: 'type' },
      title: $t('systemMenu.table.type'),
      width: 110,
    },
    {
      field: 'accessCode',
      minWidth: 230,
      slots: { default: 'accessCode' },
      title: $t('systemMenu.table.accessCode'),
    },
    {
      field: 'path',
      minWidth: 180,
      title: $t('systemMenu.table.path'),
    },
    {
      align: 'center',
      field: 'status',
      slots: { default: 'status' },
      title: $t('systemMenu.table.status'),
      width: 100,
    },
    {
      align: 'center',
      field: 'operation',
      fixed: 'right',
      slots: { default: 'action' },
      title: $t('rbacCommon.actions'),
      width: 190,
    },
  ];
}
