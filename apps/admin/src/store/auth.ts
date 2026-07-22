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
} from '#/api';
import { $t } from '#/locales';

export const useAuthStore = defineStore('auth', () => {
  const accessStore = useAccessStore();
  const userStore = useUserStore();
  const router = useRouter();

  const loginLoading = ref(false);

  /**
   * 异步处理登录操作
   * Asynchronously handle the login process
   * @param params 登录表单数据
   */
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

        // 获取用户信息并存储到 accessStore 中
        const [userInfoResult, accessCodes] = await Promise.all([
          getUserInfo(),
          getMenuAccessCodes(),
        ]);

        userInfo = userInfoResult;

        userStore.setUserInfo(userInfo);
        accessStore.setAccessCodes(accessCodes);

        const { loadDynamicMessages } = await import('#/locales/dynamic');
        await loadDynamicMessages();

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
      // 不做任何处理
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

  async function getUserInfo() {
    const userInfo = await getAdminUserInfo();
    userStore.setUserInfo(userInfo);
    return userInfo;
  }

  async function resetSessionToLogin() {
    await resetSessionState();
    await router.replace(LOGIN_PATH);
  }

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
