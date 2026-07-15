package com.gnilc.auth.authz.rbac.provider;

import com.gnilc.auth.authz.context.AccessContext;
import com.gnilc.auth.authz.context.AccessEnvironment;
import com.gnilc.auth.authz.context.AccessTarget;
import com.gnilc.auth.authz.provider.Permission;
import com.gnilc.auth.authz.provider.RequiredPermissionsProvider;
import com.gnilc.auth.authz.rbac.provider.cache.PermissionCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Optional;

/**
 * RBAC 所需权限提供者。
 * <p>
 * 负责把访问目标转换为 core 授权流程需要的所需权限集合。
 */
@Component
@Slf4j
public class RbacRequiredPermissionsProvider implements RequiredPermissionsProvider {
  private final AntPathMatcher matcher = new AntPathMatcher();

  @Autowired
  private PermissionCache cache;

  /**
   * RBAC provider 只参与 Servlet 访问环境。
   */
  @Override
  public boolean supports(AccessContext context) {
    return context != null && AccessEnvironment.SERVLET.equals(context.getEnvironment());
  }

  /**
   * 根据访问目标匹配目标权限，返回访问该目标需要的权限。
   *
   * @param context 访问上下文
   * @return 访问目标所需的权限集合
   */
  @Override
  public List<Permission> provide(AccessContext context) {
    if (!supports(context)) {
      return List.of();
    }
    AccessTarget target = context == null ? null : context.getTarget();
    if (target == null || StringUtils.isBlank(target.getIdentifier())) {
      return List.of();
    }
    String path = target.getIdentifier();
    List<TargetPermission> targetPermissions = cache.loadTargetPermissions();
    targetPermissions = Optional.ofNullable(targetPermissions).orElse(List.of());
    // RBAC 第一版只使用目标标识做路径匹配，目标限定符暂不参与匹配。
    return targetPermissions.stream()
      .filter(targetPermission -> matcher.match(targetPermission.getTargetIdentifier(), path))
      .map(targetPermission -> new Permission(targetPermission.getCode()))
      .distinct()
      .toList();
  }
}
