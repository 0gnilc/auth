package com.gnilc.authz.rbac.context;

import com.gnilc.authz.context.AccessIdentity;
import com.gnilc.authz.context.AccessIdentityResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * RBAC 组合访问身份解析器。
 * <p>
 * 多个认证来源通过委托接入，但最终都归一为 RBAC 全局用户 ID。
 *
 * @param <T> 执行环境对象类型
 */
@Slf4j
public class CompositeAccessIdentityResolver<T> implements AccessIdentityResolver<T> {
    private final List<AccessIdentityResolverDelegate<T>> delegates;

    /**
     * 创建组合访问身份解析器。
     *
     * @param delegates 访问身份解析委托
     */
    public CompositeAccessIdentityResolver(List<AccessIdentityResolverDelegate<T>> delegates) {
        this.delegates = CollectionUtils.isEmpty(delegates) ? List.of() : List.copyOf(delegates);
    }

    @Override
    public AccessIdentity resolve(T source) {
        for (AccessIdentityResolverDelegate<T> delegate : delegates) {
            if (delegate.supports(source)) {
                return normalize(delegate.resolve(source));
            }
        }
        return anonymousIdentity();
    }

    /**
     * 将委托结果规整为 RBAC 可消费的身份事实。
     */
    private AccessIdentity normalize(AccessIdentity identity) {
        if (identity == null || identity.getIdentifier() == null || identity.getIdentifier().isBlank()) {
            return anonymousIdentity();
        }
        String identifier = identity.getIdentifier().trim();
        if (!isNumeric(identifier)) {
            log.warn("RBAC access identity identifier is not a global user_id: {}", identity.getIdentifier());
            return anonymousIdentity();
        }
        if (identifier.equals(identity.getIdentifier())) {
            return identity;
        }
        return new AccessIdentity(identifier, identity.getAttributes());
    }

    private boolean isNumeric(String identifier) {
        for (int i = 0; i < identifier.length(); i++) {
            if (!Character.isDigit(identifier.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private AccessIdentity anonymousIdentity() {
        return new AccessIdentity(null, Map.of("anonymous", true));
    }
}
