import { requestClient } from '#/api/request';

export namespace MenuApi {
  export const BadgeTypes = ['dot', 'normal'] as const;
  export const BadgeVariants = [
    'default',
    'destructive',
    'primary',
    'success',
    'warning',
  ] as const;
  export const MenuTypes = [
    'catalog',
    'menu',
    'embedded',
    'link',
    'button',
  ] as const;

  export interface Menu {
    accessCode?: null | string;
    activePath?: null | string;
    affixTab: boolean;
    affixTabOrder?: null | number;
    badge?: null | string;
    badgeType?: (typeof BadgeTypes)[number] | null;
    badgeVariants?: (typeof BadgeVariants)[number] | null;
    builtIn: boolean;
    children: Menu[];
    component?: null | string;
    createTime: string;
    fullPathKey: boolean;
    hideChildrenInMenu: boolean;
    hideInBreadcrumb: boolean;
    hideInMenu: boolean;
    hideInTab: boolean;
    icon?: null | string;
    id: string;
    iframeSrc?: null | string;
    keepAlive: boolean;
    link?: null | string;
    maxNumOfOpenTab?: null | number;
    name: string;
    noBasicLayout: boolean;
    openInNewWindow: boolean;
    order: number;
    path?: null | string;
    pid: string;
    query?: null | string;
    redirect?: null | string;
    status: boolean;
    title: string;
    type: (typeof MenuTypes)[number];
    updateTime?: string;
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
