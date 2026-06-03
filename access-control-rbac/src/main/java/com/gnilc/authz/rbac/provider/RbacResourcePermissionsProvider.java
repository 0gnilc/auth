package com.gnilc.authz.rbac.provider;

import com.gnilc.authz.provider.Permission;
import com.gnilc.authz.provider.ResourcePermissionsProvider;
import com.gnilc.authz.rbac.cache.PermissionCache;
import com.gnilc.authz.rbac.cache.ResourcePermission;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@Slf4j
public class RbacResourcePermissionsProvider implements ResourcePermissionsProvider, InitializingBean {
    private final AntPathMatcher matcher = new AntPathMatcher();

    @Autowired
    private PermissionCache cache;
    @Autowired(required = false)
    private ServletContext servletContext;

    private String contextPath;

    @Override
    public List<Permission> provide() {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        String path = request.getRequestURI().replace(contextPath, "");
        List<ResourcePermission> rps = cache.loadAllResourcePermissions();
        rps = Optional.ofNullable(rps).orElse(List.of());
        return rps.stream().filter(rp -> matcher.match(rp.resource(), path))
                .map(rp -> new Permission(rp.symbol()))
                .toList();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (servletContext != null) {
            contextPath = servletContext.getContextPath();
        } else {
            contextPath = "";
        }
    }
}
