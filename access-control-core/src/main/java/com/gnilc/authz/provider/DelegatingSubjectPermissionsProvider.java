package com.gnilc.authz.provider;

import com.google.common.base.Preconditions;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DelegatingSubjectPermissionsProvider implements SubjectPermissionsProvider {
    public static final String BEAN_NAME = "delegatingSubjectPermissionsProvider";

    private final Set<SubjectPermissionsProvider> sps;

    public DelegatingSubjectPermissionsProvider(Set<SubjectPermissionsProvider> sps) {
        Preconditions.checkArgument(!CollectionUtils.isEmpty(sps), "sps is Empty!");
        this.sps = sps;
    }

    @Override
    public List<Permission> provide() {
        return sps.stream().flatMap(vp -> vp.provide().stream()).collect(Collectors.toList());
    }
}
