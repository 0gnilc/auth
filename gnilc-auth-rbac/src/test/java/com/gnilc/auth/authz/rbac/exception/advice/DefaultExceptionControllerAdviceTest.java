package com.gnilc.auth.authz.rbac.exception.advice;

import com.gnilc.auth.authz.rbac.common.constant.ResponseCode;
import com.gnilc.auth.authz.rbac.common.utils.R;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DefaultExceptionControllerAdviceTest {

    private final DefaultExceptionControllerAdvice advice = new DefaultExceptionControllerAdvice();

    /**
     * Unhandled runtime failures return a 500 transport response with the shared wrapper.
     */
    // TestCaseId: RBAC-EXCEPTION-004
    @Test
    void runtimeExceptionReturnsInternalServerErrorResponseEntity() {
        ResponseEntity<R<?>> response = advice.defaultRuntimeExceptionHandler(
                new RuntimeException("boom"), mock(HttpServletRequest.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ResponseCode.ERROR.getBusinessCode());
        assertThat(response.getBody().getCode()).isNotEqualTo(response.getStatusCode().value());
        assertThat(response.getBody().getData()).isNull();
        assertThat(response.getBody().getError()).isEqualTo("boom");
        assertThat(response.getBody().getMessage()).isEqualTo("boom");
    }

    /**
     * Checked failures also return a 500 transport response with the shared wrapper.
     */
    // TestCaseId: RBAC-EXCEPTION-005
    @Test
    void checkedExceptionReturnsInternalServerErrorResponseEntity() {
        ResponseEntity<R<?>> response = advice.defaultExceptionHandler(
                new Exception("boom"), mock(HttpServletRequest.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ResponseCode.ERROR.getBusinessCode());
        assertThat(response.getBody().getCode()).isNotEqualTo(response.getStatusCode().value());
        assertThat(response.getBody().getData()).isNull();
        assertThat(response.getBody().getError()).isEqualTo("boom");
        assertThat(response.getBody().getMessage()).isEqualTo("boom");
    }
}
