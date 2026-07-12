package com.gnilc.auth.authz.servlet.context;

import com.gnilc.auth.authz.context.AbstractAccessContextAdapter;
import com.gnilc.auth.authz.context.AccessEnvironment;

/**
 * 默认 Servlet 访问上下文适配器。
 * <p>
 * Servlet 对象不会进入 AccessContext；这里只抽取授权判断需要的访问环境、身份和目标事实。
 */
public class DefaultServletAccessContextAdapter extends AbstractAccessContextAdapter<ServletRequestContext>
        implements ServletAccessContextAdapter {

    /**
     * 创建默认 Servlet 访问上下文适配器。
     *
     * @param identityResolver Servlet 访问身份解析器
     * @param targetResolver   Servlet 访问目标解析器
     */
    public DefaultServletAccessContextAdapter(ServletAccessIdentityResolver identityResolver,
                                              ServletAccessTargetResolver targetResolver) {
        super(context -> AccessEnvironment.SERVLET, identityResolver, targetResolver);
    }
}
