package com.gnilc.authz.provider;

import com.google.common.base.Preconditions;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DelegatingResourcePermissionsProvider implements ResourcePermissionsProvider {
    public static final String BEAN_NAME = "delegatingResourcePermissionsProvider";

    private final Set<ResourcePermissionsProvider> rps;

    public DelegatingResourcePermissionsProvider(Set<ResourcePermissionsProvider> rps) {
        Preconditions.checkArgument(!CollectionUtils.isEmpty(rps), "rps is Empty!");
        this.rps = rps;
    }

    @Override
    public List<Permission> provide() {
        return rps.stream().flatMap(rp -> rp.provide().stream()).collect(Collectors.toList());
    }
}
