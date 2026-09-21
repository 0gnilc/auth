import type { AxiosResponse, HttpResponse } from '@vben/request';

import type { AdminApi } from '#/api/system/admin';

import { baseRequestClient, requestClient } from '#/api/request';

/** 管理员独立会话的访问及刷新令牌。 */
interface AdminSession {
  /** 当前身份会话的访问令牌。 */
  accessToken: string;
  /** 当前身份会话的刷新令牌，用于刷新或撤销本次会话。 */
  refreshToken: string;
}

const REFRESH_TOKEN_HEADER = 'X-Refresh-Token';

export async function login(username: string, password: string) {
  return requestClient.post<AdminSession>('/sys/admin/login', {
    password,
    username,
  });
}

export async function refresh(refreshToken: string) {
  const response = await baseRequestClient.post<
    AxiosResponse<HttpResponse<AdminSession>>
  >('/sys/admin/refresh', undefined, {
    headers: { [REFRESH_TOKEN_HEADER]: refreshToken },
  });

  return response.data.data;
}

export async function logout(refreshToken: string) {
  await baseRequestClient.post<AxiosResponse<HttpResponse<null>>>(
    '/sys/admin/logout',
    undefined,
    { headers: { [REFRESH_TOKEN_HEADER]: refreshToken } },
  );
}

export async function getAdminUserInfo() {
  return requestClient.get<AdminApi.Admin>('/sys/admin/user-info');
}

export async function updateProfile(profile: AdminApi.ProfileInput) {
  return requestClient.post<null>('/sys/admin/user-info/update', profile);
}

export async function updatePassword(oldPassword: string, newPassword: string) {
  return requestClient.post<null>('/sys/admin/password/update', {
    newPassword,
    oldPassword,
  });
}

export async function getRoleCodes() {
  return requestClient.get<string[]>('/sys/admin/role-codes');
}

export async function getMenuAccessCodes() {
  return requestClient.get<string[]>('/sys/admin/menu/access-codes');
}
