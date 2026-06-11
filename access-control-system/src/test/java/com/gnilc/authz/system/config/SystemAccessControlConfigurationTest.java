package com.gnilc.authz.system.config;

import com.gnilc.authz.context.AccessIdentity;
import com.gnilc.authz.denied.AccessDeniedHandler;
import com.gnilc.authz.rbac.context.AccessIdentityResolverDelegate;
import com.gnilc.authz.web.context.FilterDeniedContext;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemAccessControlConfigurationTest {

    /**
     * 系统模块通过 Header 委托把网关透传身份转换成全局 user_id。
     */
    @Test
    void headerDelegateResolvesGlobalUserId() {
        AccessIdentityResolverDelegate<HttpServletRequest> delegate = new SystemAccessControlConfiguration()
                .systemAccessIdentityResolverDelegate();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Access-User-Id")).thenReturn(" 1001 ");

        AccessIdentity identity = delegate.resolve(request);

        assertThat(delegate.supports(request)).isTrue();
        assertThat(identity.getIdentifier()).isEqualTo("1001");
        assertThat(identity.getAttributes()).containsEntry("header", "X-Access-User-Id");
    }

    /**
     * Header 缺失或空白时，该委托不处理请求，由组合解析器回退匿名身份。
     */
    @Test
    void headerDelegateDoesNotSupportBlankHeader() {
        AccessIdentityResolverDelegate<HttpServletRequest> delegate = new SystemAccessControlConfiguration()
                .systemAccessIdentityResolverDelegate();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Access-User-Id")).thenReturn(" ");

        assertThat(delegate.supports(request)).isFalse();
    }

    /**
     * 拒绝处理器保持系统模块原有的 403 JSON 响应格式。
     */
    @Test
    void deniedHandlerWritesForbiddenJsonResponse() throws IOException {
        AccessDeniedHandler<FilterDeniedContext> handler = new SystemAccessControlConfiguration()
                .systemAccessDeniedHandler();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterDeniedContext deniedContext = new FilterDeniedContext(null, response, null);

        handler.handle(null, deniedContext);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        assertThat(response.getContentAsString()).isEqualTo("{\"code\":403,\"message\":\"access denied\",\"data\":null}");
    }
}
