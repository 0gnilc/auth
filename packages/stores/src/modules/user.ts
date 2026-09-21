import type { BasicUserInfo } from '@vben-core/typings';

import { acceptHMRUpdate, defineStore } from 'pinia';

interface AccessState {
  /**
   * 用户信息
   */
  userInfo: BasicUserInfo | null;
  /**
   * 用户角色
   */
  userRoles: string[];
}

/**
 * @zh_CN 用户信息相关
 */
export const useUserStore = defineStore('core-user', {
  actions: {
    /** 身份更新时同步替换角色代码；清空身份也清空角色，不保留上一会话的前端权限依据。 */
    setUserInfo(userInfo: BasicUserInfo | null) {
      // 设置用户信息
      this.userInfo = userInfo;
      // 设置角色信息
      const roleCodes = userInfo?.roleCodes ?? [];
      this.setUserRoles(roleCodes);
    },
    setUserRoles(roles: string[]) {
      this.userRoles = roles;
    },
  },
  state: (): AccessState => ({
    userInfo: null,
    userRoles: [],
  }),
});

// 解决热更新问题
const hot = import.meta.hot;
if (hot) {
  hot.accept(acceptHMRUpdate(useUserStore, hot));
}
