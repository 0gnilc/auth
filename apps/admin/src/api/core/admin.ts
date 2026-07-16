import type { AxiosResponse, HttpResponse } from '@vben/request';
import type { UserInfo } from '@vben/types';

import type { PageParams, PageResult } from '#/api/types';

import { isEmpty } from '@vben/utils';

import { baseRequestClient, requestClient } from '#/api/request';

export namespace AdminApi {
  export interface Admin extends UserInfo {
    createTime: string;
    id: string;
    roleCodes: string[];
    status?: boolean;
  }

  export interface AdminSession {
    accessToken: string;
    refreshToken: string;
  }
}

const REFRESH_TOKEN_HEADER = 'X-Refresh-Token';

export async function getAdminPage(
  params?: PageParams &
    Partial<Pick<AdminApi.Admin, 'nickname' | 'status' | 'username'>>,
) {
  return requestClient.post<PageResult<AdminApi.Admin>>(
    '/sys/admin/page',
    params,
  );
}

export async function createAdmin(
  admin: Omit<
    AdminApi.Admin,
    'createTime' | 'homePath' | 'id' | 'roleCodes' | 'userId'
  > & {
    homePath?: string;
    password: string;
    roleCodes?: string[];
  },
) {
  return requestClient.post<null>('/sys/admin/create', admin);
}

export async function updateAdmin(
  admin: Partial<Omit<AdminApi.Admin, 'createTime' | 'id' | 'userId'>> &
    Pick<AdminApi.Admin, 'id'> & { password?: null | string },
) {
  const { password, ...profile } = admin;
  const data = isEmpty(password?.trim()) ? profile : admin;

  return requestClient.post<null>('/sys/admin/update', data);
}

export async function updateAdminRoles(id: string, roleCodes: string[]) {
  return requestClient.post<null>('/sys/admin/update-roles', {
    id,
    roleCodes,
  });
}

export async function removeAdmin(id: string) {
  return requestClient.post<null>(
    `/sys/admin/remove/${encodeURIComponent(id)}`,
  );
}

export async function login(username: string, password: string) {
  return requestClient.post<AdminApi.AdminSession>('/sys/admin/login', {
    password,
    username,
  });
}

export async function refresh(refreshToken: string) {
  const response = await baseRequestClient.post<
    AxiosResponse<HttpResponse<AdminApi.AdminSession>>
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

export async function updateProfile(
  profile: Pick<AdminApi.Admin, 'avatar' | 'desc' | 'nickname'>,
) {
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
