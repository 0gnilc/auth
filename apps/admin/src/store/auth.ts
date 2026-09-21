import type { Recordable, UserInfo } from '@vben/types';

import { ref } from 'vue';
import { useRouter } from 'vue-router';

import { LOGIN_PATH } from '@vben/constants';
import { preferences } from '@vben/preferences';
import { resetAllStores, useAccessStore, useUserStore } from '@vben/stores';

import { ElNotification } from 'element-plus';
import { defineStore } from 'pinia';

import {
  getAdminUserInfo,
  getMenuAccessCodes,
  login as loginAdmin,
  logout as logoutAdmin,
} from '#/api/core';
import { $t } from '#/locales';

export const useAuthStore = defineStore('auth', () => {
  const accessStore = useAccessStore();
  const userStore = useUserStore();
  const router = useRouter();

  const loginLoading = ref(false);

  /** 建立令牌、身份和菜单访问码；初始化身份失败时清理新会话，避免保留不完整权限状态。 */
  async function login(
    params: Recordable<any>,
    onSuccess?: () => Promise<void> | void,
  ) {
    // 异步处理用户登录操作并获取 accessToken
    let userInfo: null | UserInfo = null;
    try {
      loginLoading.value = true;
      const { accessToken, refreshToken } = await loginAdmin(
        params.username,
        params.password,
      );

      // 如果成功获取到 accessToken
      if (accessToken) {
        // 将 accessToken 存储到 accessStore 中
        accessStore.setAccessToken(accessToken);
        accessStore.setRefreshToken(refreshToken);

        try {
          userInfo = await getUserInfo();

          const { loadDynamicMessages } = await import('#/locales/dynamic');
          await loadDynamicMessages();
        } catch (error) {
          await resetSessionState();
          throw error;
        }

        if (accessStore.loginExpired) {
          accessStore.setLoginExpired(false);
        } else {
          onSuccess
            ? await onSuccess?.()
            : await router.push(
                userInfo.homePath || preferences.app.defaultHomePath,
              );
        }

        if (userInfo?.nickname) {
          ElNotification({
            message: `${$t('authentication.loginSuccessDesc')}:${userInfo?.nickname}`,
            title: $t('authentication.loginSuccess'),
            type: 'success',
          });
        }
      }
    } finally {
      loginLoading.value = false;
    }

    return {
      userInfo,
    };
  }

  async function logout(redirect: boolean = true) {
    try {
      if (accessStore.refreshToken) {
        await logoutAdmin(accessStore.refreshToken);
      }
    } catch {
      // 远端注销失败仍要清理本地身份、菜单和动态消息，不能让旧会话继续留在页面。
    }
    await resetSessionState();

    // 回登录页带上当前路由地址
    await router.replace({
      path: LOGIN_PATH,
      query: redirect
        ? {
            redirect: encodeURIComponent(router.currentRoute.value.fullPath),
          }
        : {},
    });
  }

  /** 身份与菜单访问码都加载成功后再发布到 Store，避免二者属于不同的初始化阶段。 */
  async function getUserInfo() {
    const [userInfo, accessCodes] = await Promise.all([
      getAdminUserInfo(),
      getMenuAccessCodes(),
    ]);
    userStore.setUserInfo(userInfo);
    accessStore.setAccessCodes(accessCodes);
    return userInfo;
  }

  async function resetSessionToLogin() {
    await resetSessionState();
    await router.replace(LOGIN_PATH);
  }

  /** 同时清理会话派生状态和动态消息快照，防止下一次登录沿用上一会话内容。 */
  async function resetSessionState() {
    resetAllStores();
    accessStore.setLoginExpired(false);
    const { clearDynamicMessages } = await import('#/locales/dynamic');
    await clearDynamicMessages();
  }

  function $reset() {
    loginLoading.value = false;
  }

  return {
    $reset,
    getUserInfo,
    login,
    loginLoading,
    logout,
    resetSessionToLogin,
  };
});
