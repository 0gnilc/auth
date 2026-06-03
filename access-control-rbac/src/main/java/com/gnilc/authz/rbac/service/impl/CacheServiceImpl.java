package com.gnilc.authz.rbac.service.impl;

import com.gnilc.authz.rbac.service.CacheService;
import com.gnilc.authz.rbac.service.event.ClearEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class CacheServiceImpl implements CacheService {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Override
    public void clear() {
        publisher.publishEvent(new ClearEvent(this));
    }
}
