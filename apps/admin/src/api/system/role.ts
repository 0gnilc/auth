import { requestClient } from '#/api/request';

export namespace RoleApi {
  /** RBAC 角色资料；内置身份独立于管理员的角色分配关系。 */
  export interface Role {
    /** 是否为系统维护的内置授权资源；不由角色绑定关系决定。 */
    builtIn: boolean;
    /** 角色唯一编码，用于身份授权绑定。 */
    code: string;
    /** 记录创建时间，ISO 8601 UTC 时间。 */
    createTime: string;
    /** 记录 ID，以十进制字符串传输。 */
    id: string;
    /** 角色显示名称。 */
    name: string;
    /** 内部操作备注；未填写时为 null。 */
    remark: null | string;
  }

  /** 角色创建或更新输入；内置身份等只读字段由服务端维护。 */
  export type Input = Partial<Pick<Role, 'remark'>> &
    Pick<Role, 'code' | 'name'>;
}

export async function getRoleList(
  params: Partial<Pick<RoleApi.Role, 'builtIn' | 'code' | 'name'>> = {},
) {
  return requestClient.post<RoleApi.Role[]>('/authz/role/list', params);
}

export async function createRole(data: RoleApi.Input) {
  return requestClient.post<null>('/authz/role/create', data);
}

export async function updateRole(
  data: Pick<RoleApi.Role, 'id'> & RoleApi.Input,
) {
  return requestClient.post<null>('/authz/role/update', data);
}

export async function removeRole(id: string) {
  return requestClient.post<null>(
    `/authz/role/remove/${encodeURIComponent(id)}`,
  );
}

export async function getRolePermissionIds(roleId: string) {
  return requestClient.post<string[]>(
    `/authz/role-permission/list/${encodeURIComponent(roleId)}`,
  );
}

export async function saveRolePermissions(
  roleId: string,
  permissionIds: string[],
) {
  return requestClient.post<null>('/authz/role-permission/save', {
    permissionIds,
    roleId,
  });
}

export async function getRoleMenuIds(roleId: string) {
  return requestClient.post<string[]>(
    `/authz/role-menu/list/${encodeURIComponent(roleId)}`,
  );
}

export async function saveRoleMenus(roleId: string, menuIds: string[]) {
  return requestClient.post<null>('/authz/role-menu/save', {
    menuIds,
    roleId,
  });
}
