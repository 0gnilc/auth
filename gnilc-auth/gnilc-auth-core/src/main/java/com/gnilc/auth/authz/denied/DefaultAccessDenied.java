package com.gnilc.auth.authz.denied;

import com.gnilc.auth.authz.context.AccessContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 默认访问拒绝入口。
 * <p>
 * 该 implementation 收集所有 {@link AccessDeniedHandler} 策略，并按 Spring order 顺序调用所有支持
 * 当前拒绝上下文的 handler。没有 handler 或没有支持者时按 no-op 处理。
 */
public class DefaultAccessDenied implements AccessDenied {
    private final List<AccessDeniedHandler> handlers;

    /**
     * 创建默认访问拒绝入口。
     *
     * @param handlers 访问拒绝策略集合，传入 {@code null} 时按空集合处理
     */
    public DefaultAccessDenied(Collection<? extends AccessDeniedHandler> handlers) {
        List<AccessDeniedHandler> orderedHandlers = handlers == null ? List.of() : new ArrayList<>(handlers);
        AnnotationAwareOrderComparator.sort(orderedHandlers);
        this.handlers = List.copyOf(orderedHandlers);
    }

    /**
     * 执行当前拒绝上下文的访问拒绝。
     */
    @Override
    public void denied(AccessContext accessContext, AccessDeniedContext deniedContext) {
        for (AccessDeniedHandler handler : handlers) {
            if (handler.supports(accessContext, deniedContext)) {
                handler.handle(accessContext, deniedContext);
            }
        }
    }
}
