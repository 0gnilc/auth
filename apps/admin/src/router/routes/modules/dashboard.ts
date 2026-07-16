import type { RouteRecordRaw } from 'vue-router';

import { $t } from '#/locales';

const routes: RouteRecordRaw[] = [
  {
    component: () => import('#/views/dashboard/index.vue'),
    meta: {
      affixTab: true,
      authority: ['admin'],
      icon: 'lucide:layout-dashboard',
      order: -1,
      title: $t('page.dashboard.title'),
    },
    name: 'Dashboard',
    path: '/dashboard',
  },
  {
    meta: {
      authority: ['admin'],
      hideInBreadcrumb: true,
      hideInMenu: true,
      hideInTab: true,
      title: $t('page.dashboard.title'),
    },
    name: 'WorkspaceRedirect',
    path: '/workspace',
    redirect: '/dashboard',
  },
];

export default routes;
