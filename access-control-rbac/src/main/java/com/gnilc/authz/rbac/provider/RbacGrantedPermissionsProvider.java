package com.gnilc.authz.rbac.provider;

import com.gnilc.authz.context.AccessContext;
import com.gnilc.authz.context.AccessIdentity;
import com.gnilc.authz.provider.GrantedPermissionsProvider;
import com.gnilc.authz.provider.Permission;
import com.gnilc.authz.rbac.provider.cache.PermissionCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * RBAC 已授予权限提供者。
 * <p>
 * 负责把访问身份转换为 core 授权流程需要的已授予权限集合。
 */
@Component
@Slf4j
public class RbacGrantedPermissionsProvider implements GrantedPermissionsProvider {
    @Autowired
    private PermissionCache cache;

    /**
     * 根据访问身份加载用户权限，并合并对所有身份开放的公开访问权限。
     *
     * @param context 访问上下文
     * @return 当前身份已授予的权限集合
     */
    @Override
    public List<Permission> provide(AccessContext context) {
        Long userId = getUserId(context);
        List<Permission> userPermissions = List.of();
        if (userId != null) {
            userPermissions = cache.loadUserPermissions(userId);
        }
        userPermissions = Optional.ofNullable(userPermissions).orElse(List.of());
        List<Permission> publicAccessPermissions = cache.loadPublicAccessPermissions();
        publicAccessPermissions = Optional.ofNullable(publicAccessPermissions).orElse(List.of());
        // 公开访问权限对匿名和已识别身份都生效，因此与用户权限合并后去重。
        return Stream.concat(userPermissions.stream(), publicAccessPermissions.stream())
                .distinct().collect(Collectors.toList());
    }

    /**
     * 从访问身份中解析 RBAC 用户 ID。
     * <p>
     * 非数字身份不会中断授权流程，会被视为匿名或非 RBAC 用户身份。
     */
    private Long getUserId(AccessContext context) {
        if (context == null) {
            return null;
        }
        AccessIdentity identity = context.getIdentity();
        if (identity == null || identity.getIdentifier() == null || identity.getIdentifier().isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(identity.getIdentifier());
        } catch (NumberFormatException e) {
            log.warn("Access identity identifier is not a user id: {}", identity.getIdentifier());
            return null;
        }
    }
}
