import type { PageParams, PageResult } from '#/api/types';

import { isEmpty } from '@vben/utils';

import { requestClient } from '#/api/request';

export namespace AdminApi {
  /** 可编辑的个人资料；null 清空可选字段，管理更新时省略则保留。 */
  export interface ProfileInput {
    /** 可选头像 URL；null 表示清除。 */
    avatar?: null | string;
    /** 可选管理员简介；null 表示清除。 */
    desc?: null | string;
    /** 管理员显示昵称。 */
    nickname: string;
  }

  /** 管理员身份资料；管理员记录 id 与授权主体 userId 分别定位 sys_admin 和 az_user。 */
  export interface Admin {
    /** 管理员头像 URL；未设置时省略。 */
    avatar?: string;
    /** 记录创建时间，ISO 8601 UTC 时间。 */
    createTime: string;
    /** 管理员简介；未设置时省略。 */
    desc?: string;
    /** 登录后的默认首页路径。 */
    homePath: string;
    /** 管理员记录 ID（sys_admin.id），以十进制字符串传输。 */
    id: string;
    /** 管理员显示昵称。 */
    nickname: string;
    /** 已分配的角色编码列表。 */
    roleCodes: string[];
    /** 管理员账户是否启用；未返回时省略。 */
    status?: boolean;
    /** 管理员授权主体 ID（az_user.id），与管理员记录 ID 不同。 */
    userId: string;
    /** 管理员登录名。 */
    username: string;
  }
}

export async function getAdminPage(
  params?: PageParams &
    Partial<Pick<AdminApi.Admin, 'nickname' | 'status' | 'username'>>,
) {
  return requestClient.post<PageResult<AdminApi.Admin>>(
    '/sys/admin/page',
    params,
  );
}

/** 管理员创建输入，增加初始登录密码，角色另行分配。 */
export async function createAdmin(
  data: {
    /** 新管理员的初始登录密码。 */
    password: string;
  } & AdminApi.ProfileInput &
    Omit<
      AdminApi.Admin,
      'avatar' | 'createTime' | 'desc' | 'id' | 'roleCodes' | 'userId'
    >,
) {
  return requestClient.post<null>('/sys/admin/create', data);
}

/** 指定管理员的资料更新输入；密码为空白、null 或省略时不替换现有密码。 */
export async function updateAdmin(
  data: {
    /** 可选的新登录密码；空白、null 或省略时由请求函数移除，不改密码。 */
    password?: null | string;
  } & Partial<AdminApi.ProfileInput> &
    Partial<
      Omit<
        AdminApi.Admin,
        'avatar' | 'createTime' | 'desc' | 'id' | 'roleCodes' | 'userId'
      >
    > &
    Pick<AdminApi.Admin, 'id'>,
) {
  const { password, ...profile } = data;
  const requestData = isEmpty(password?.trim()) ? profile : data;

  return requestClient.post<null>('/sys/admin/update', requestData);
}

export async function saveAdminRoles(id: string, roleCodes: string[]) {
  return requestClient.post<null>('/sys/admin/roles/save', {
    id,
    roleCodes,
  });
}

export async function removeAdmin(id: string) {
  return requestClient.post<null>(
    `/sys/admin/remove/${encodeURIComponent(id)}`,
  );
}
