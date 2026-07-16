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

  export interface AdminPageQuery extends PageParams {
    nickname?: string;
    status?: boolean;
    username?: string;
  }

  export type AdminPage = PageResult<Admin>;

  export interface CreateAdmin {
    avatar?: string;
    desc?: string;
    homePath?: string;
    nickname: string;
    password: string;
    roleCodes?: string[];
    status?: boolean;
    username: string;
  }

  export interface UpdateAdmin {
    id: string;
    avatar?: string;
    desc?: string;
    homePath?: string;
    nickname?: string;
    password?: null | string;
    roleCodes?: string[];
    status?: boolean;
    username?: string;
  }

  export interface UpdateCurrentProfile {
    avatar?: string;
    desc?: string;
    nickname: string;
  }

  export interface UpdateCurrentPassword {
    newPassword: string;
    oldPassword: string;
  }

  export interface AdminSession {
    accessToken: string;
    refreshToken: string;
  }
}

const REFRESH_TOKEN_HEADER = 'X-Refresh-Token';

export async function getAdminPage(params?: AdminApi.AdminPageQuery) {
  return requestClient.post<AdminApi.AdminPage>('/sys/admin/page', params);
}

export async function createAdmin(admin: AdminApi.CreateAdmin) {
  return requestClient.post<null>('/sys/admin/create', admin);
}

export async function updateAdmin(admin: AdminApi.UpdateAdmin) {
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

export async function updateAdminUserInfo(
  profile: AdminApi.UpdateCurrentProfile,
) {
  return requestClient.post<null>('/sys/admin/user-info/update', profile);
}

export async function updateAdminPassword(
  passwords: AdminApi.UpdateCurrentPassword,
) {
  return requestClient.post<null>('/sys/admin/password/update', passwords);
}

export async function getRoleCodes() {
  return requestClient.get<string[]>('/sys/admin/role-codes');
}

export async function getMenuAccessCodes() {
  return requestClient.get<string[]>('/sys/admin/menu/access-codes');
}
