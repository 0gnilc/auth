package com.gnilc.auth.authz.rbac.provider.cache;

import com.gnilc.auth.authz.rbac.entity.bo.PermissionBo;
import com.gnilc.auth.authz.rbac.provider.TargetPermission;
import com.gnilc.auth.authz.rbac.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 验证数据库加载出的目标权限保留请求方法限定，不能退化为仅按路径授权。 */
class DatabasePermissionCacheLoaderTest {
    private final PermissionService permissionService = mock(PermissionService.class);
    private final DatabasePermissionCacheLoader loader = new DatabasePermissionCacheLoader();

    @BeforeEach
    void injectPermissionService() {
        ReflectionTestUtils.setField(loader, "permissionService", permissionService);
    }

    @Test
    void preservesTargetQualifier() {
        PermissionBo permission = new PermissionBo();
        permission.setTargetIdentifier("/provider-callback/{transactionNo}");
        permission.setTargetQualifier("POST");
        permission.setCode("callback:post");
        when(permissionService.list()).thenReturn(List.of(permission));

        assertThat(loader.loadTargetPermissions())
                .containsExactly(new TargetPermission(
                        "/provider-callback/{transactionNo}", "POST", "callback:post"));
    }
}
