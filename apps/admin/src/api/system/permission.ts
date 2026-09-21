import { requestClient } from '#/api/request';

export namespace PermissionApi {
  /** 服务器请求授权权限资料；与前端按钮访问码的展示控制不同。 */
  export interface Permission {
    /** 是否为系统维护的内置授权资源；不由角色绑定关系决定。 */
    builtIn: boolean;
    /** 权限唯一编码。 */
    code: string;
    /** 记录创建时间，ISO 8601 UTC 时间。 */
    createTime: string;
    /** 记录 ID，以十进制字符串传输。 */
    id: string;
    /** 权限显示名称。 */
    name: string;
    /** 是否允许公开访问该目标权限。 */
    publicAccess: boolean;
    /** 内部操作备注；未填写时为 null。 */
    remark: null | string;
    /** 权限目标标识，通常是受保护的请求路径。 */
    targetIdentifier: string;
    /** 访问目标限定符，例如 HTTP 方法；null 表示不限定目标变体。 */
    targetQualifier: null | string;
  }

  /** 权限创建或更新输入，指定目标及可选限定信息。 */
  export type Input = Partial<Pick<Permission, 'remark' | 'targetQualifier'>> &
    Pick<Permission, 'code' | 'name' | 'publicAccess' | 'targetIdentifier'>;
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

export async function createPermission(data: PermissionApi.Input) {
  return requestClient.post<null>('/authz/permission/create', data);
}

export async function updatePermission(
  data: PermissionApi.Input & Pick<PermissionApi.Permission, 'id'>,
) {
  return requestClient.post<null>('/authz/permission/update', data);
}

export async function removePermission(id: string) {
  return requestClient.post<null>(
    `/authz/permission/remove/${encodeURIComponent(id)}`,
  );
}
