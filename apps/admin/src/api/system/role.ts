import { requestClient } from '#/api/request';

export namespace RoleApi {
  export interface Role {
    builtIn: boolean;
    code: string;
    createTime: string;
    id: string;
    name: string;
    remark?: string;
  }
}

export async function getRoleList(
  params: Partial<Pick<RoleApi.Role, 'builtIn' | 'code' | 'name'>> = {},
) {
  return requestClient.post<RoleApi.Role[]>('/authz/role/list', params);
}

export async function createRole(
  data: Omit<RoleApi.Role, 'builtIn' | 'createTime' | 'id'>,
) {
  return requestClient.post<null>('/authz/role/create', data);
}

export async function updateRole(
  data: Omit<RoleApi.Role, 'builtIn' | 'createTime' | 'id'> &
    Pick<RoleApi.Role, 'id'>,
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
