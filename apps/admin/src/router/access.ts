import type { GenerateMenuAndRoutesOptions } from '@vben/types';

import { generateAccessible } from '@vben/access';

const forbiddenComponent = () => import('#/views/_core/fallback/forbidden.vue');

async function generateAccess(options: GenerateMenuAndRoutesOptions) {
  // Admin 固定使用前端静态路由，不再回退到已删除的后端菜单接口。
  return await generateAccessible('frontend', {
    ...options,
    forbiddenComponent,
  });
}

export { generateAccess };
