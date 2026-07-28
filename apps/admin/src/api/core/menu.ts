import type { RouteRecordStringComponent } from '@vben/types';

import { requestClient } from '#/api/request';

/**
 * 获取当前管理员导航路由树。
 */
export async function getAllMenus() {
  return requestClient.get<RouteRecordStringComponent[]>(
    '/sys/admin/menu/routes',
  );
}
