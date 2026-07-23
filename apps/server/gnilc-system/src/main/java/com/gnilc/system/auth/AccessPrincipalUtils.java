package com.gnilc.system.auth;

import com.gnilc.auth.authn.context.AccessPrincipal;
import com.gnilc.auth.authn.servlet.context.DefaultAccessPrincipalHolder;
import com.gnilc.common.base.Preconditions;
import com.gnilc.common.i18n.I18nMessageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 当前访问主体工具。
 */
@Component
public class AccessPrincipalUtils {
    private final I18nMessageService messages;

    public AccessPrincipalUtils(I18nMessageService messages) {
        this.messages = messages;
    }

    /**
     * 获取当前认证用户 ID。
     */
    public Long getUserId() {
        AccessPrincipal principal = DefaultAccessPrincipalHolder.getPrincipal();
        Preconditions.checkArgument(principal != null,
                messages.get("system.auth.session.invalid"));
        String identifier = principal.getIdentifier();
        Preconditions.checkArgument(StringUtils.isNotBlank(identifier),
                messages.get("system.auth.session.invalid"));
        return Long.valueOf(identifier);
    }
}
