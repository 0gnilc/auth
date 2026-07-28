import { requestClient } from '#/api/request';

export namespace PermissionApi {
  export interface Permission {
    builtIn: boolean;
    code: string;
    createTime: string;
    id: string;
    name: string;
    publicAccess: boolean;
    remark?: string;
    targetIdentifier: string;
    targetQualifier?: string;
  }
}

export async function getPermissionList(
  params: Partial<
    Pick<
      PermissionApi.Permission,
      'code' | 'name' | 'publicAccess' | 'targetIdentifier' | 'targetQualifier'
    >
  > = {},
) {
  return requestClient.post<PermissionApi.Permission[]>(
    '/authz/permission/list',
    params,
  );
}

export async function createPermission(
  data: Omit<PermissionApi.Permission, 'builtIn' | 'createTime' | 'id'>,
) {
  return requestClient.post<null>('/authz/permission/create', data);
}

export async function updatePermission(
  data: Omit<PermissionApi.Permission, 'builtIn' | 'createTime' | 'id'> &
    Pick<PermissionApi.Permission, 'id'>,
) {
  return requestClient.post<null>('/authz/permission/update', data);
}

export async function removePermission(id: string) {
  return requestClient.post<null>(
    `/authz/permission/remove/${encodeURIComponent(id)}`,
  );
}
