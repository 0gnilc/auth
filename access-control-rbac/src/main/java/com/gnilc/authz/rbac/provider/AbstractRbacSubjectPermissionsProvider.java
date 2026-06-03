package com.gnilc.authz.rbac.provider;

import com.gnilc.authz.provider.Permission;
import com.gnilc.authz.provider.SubjectPermissionsProvider;
import com.gnilc.authz.rbac.cache.PermissionCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
public abstract class AbstractRbacSubjectPermissionsProvider implements SubjectPermissionsProvider {
    @Autowired
    private PermissionCache cache;

    @Override
    public List<Permission> provide() {
        Long userId = getUserId();
        List<Permission> ups = List.of();
        if (userId != null) {
            ups = cache.loadUserPermissions(userId);
        }
        ups = Optional.ofNullable(ups).orElse(List.of());
        List<Permission> eps = cache.loadExposedPermissions();
        eps = Optional.ofNullable(eps).orElse(List.of());
        return Stream.concat(ups.stream(), eps.stream()).collect(Collectors.toList());
    }

    protected abstract Long getUserId();
}
