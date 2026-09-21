import { requestClient } from '#/api/request';

export namespace MenuApi {
  /** 导航徽标类型：dot 为圆点，normal 为普通文本徽标。 */
  export const BadgeTypes = ['dot', 'normal'] as const;
  /** 导航徽标使用的主题样式标识。 */
  export const BadgeVariants = [
    'default',
    'destructive',
    'primary',
    'success',
    'warning',
  ] as const;
  /** 菜单类型协议值：catalog 目录，menu 页面，embedded 内嵌页面，link 外链，button 操作按钮。 */
  export const MenuTypes = [
    'catalog',
    'menu',
    'embedded',
    'link',
    'button',
  ] as const;

  /** 管理员维护的导航及按钮菜单树节点；不同菜单种类使用对应导航字段。 */
  export interface Menu {
    /** 前端访问控制码；按钮权限据此控制展示，未配置时为 null 或省略。 */
    accessCode?: null | string;
    /** 激活导航时高亮的菜单路径；未配置时为 null 或省略。 */
    activePath?: null | string;
    /** 是否固定该页面的标签页。 */
    affixTab: boolean;
    /** 固定标签页的排序值；未配置时为 null 或省略。 */
    affixTabOrder?: null | number;
    /** 静态徽标内容；未配置时为 null 或省略。 */
    badge?: null | string;
    /** 徽标类型：圆点或普通徽标；未配置时为 null 或省略。 */
    badgeType?: (typeof BadgeTypes)[number] | null;
    /** 徽标配色变体；未配置时为 null 或省略。 */
    badgeVariants?: (typeof BadgeVariants)[number] | null;
    /** 是否为系统维护的内置授权资源；不由角色绑定关系决定。 */
    builtIn: boolean;
    /** 子菜单列表；叶子节点为空数组。 */
    children: Menu[];
    /** 内部页面的组件映射标识；不适用或未配置时为 null 或省略。 */
    component?: null | string;
    /** 记录创建时间，ISO 8601 UTC 时间。 */
    createTime: string;
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
    /** 菜单图标标识；未配置时为 null 或省略。 */
    icon?: null | string;
    /** 记录 ID，以十进制字符串传输。 */
    id: string;
    /** 内嵌页面地址；非内嵌菜单或未配置时为 null 或省略。 */
    iframeSrc?: null | string;
    /** 是否缓存页面状态。 */
    keepAlive: boolean;
    /** 外部链接地址；非外链菜单或未配置时为 null 或省略。 */
    link?: null | string;
    /** 同一路由可打开的标签页数量上限；未配置时为 null 或省略。 */
    maxNumOfOpenTab?: null | number;
    /** 唯一的导航路由名称。 */
    name: string;
    /** 是否脱离 Admin 基础布局展示。 */
    noBasicLayout: boolean;
    /** 是否在新窗口打开目标。 */
    openInNewWindow: boolean;
    /** 同层导航菜单排序值。 */
    order: number;
    /** 导航路由路径；不适用或未配置时为 null 或省略。 */
    path?: null | string;
    /** 父菜单 ID，字符串 0 表示根节点。 */
    pid: string;
    /** 菜单预设查询参数文本；未配置时为 null 或省略。 */
    query?: null | string;
    /** 导航重定向目标；未配置时为 null 或省略。 */
    redirect?: null | string;
    /** 菜单是否启用；禁用不自动删除已有角色授予关系。 */
    status: boolean;
    /** 菜单显示标题或动态消息键。 */
    title: string;
    /** 菜单种类：目录、内部页面、内嵌页面、外链或按钮。 */
    type: (typeof MenuTypes)[number];
    /** 最后修改时间，ISO 8601 UTC 时间；尚未修改时为 null。 */
    updateTime: null | string;
  }
}

export async function getMenuTree() {
  return requestClient.post<MenuApi.Menu[]>('/authz/menu/tree');
}

export async function createMenu(
  data: Omit<
    MenuApi.Menu,
    'builtIn' | 'children' | 'createTime' | 'id' | 'updateTime'
  >,
) {
  return requestClient.post<null>('/authz/menu/create', data);
}

/**
 * 完整更新菜单。调用方必须提交除只读字段外的全部菜单字段；省略字段不表示保留原值。
 */
export async function updateMenu(
  data: Omit<
    MenuApi.Menu,
    'builtIn' | 'children' | 'createTime' | 'id' | 'updateTime'
  > &
    Pick<MenuApi.Menu, 'id'>,
) {
  return requestClient.post<null>('/authz/menu/update', data);
}

export async function removeMenu(id: string) {
  return requestClient.post<null>(
    `/authz/menu/remove/${encodeURIComponent(id)}`,
  );
}
