package com.gnilc.auth.authz.rbac.exception.advice;

import com.gnilc.auth.authz.rbac.common.constant.ResponseCode;
import com.gnilc.auth.authz.rbac.common.utils.R;
import com.gnilc.auth.authz.rbac.exception.IllegalConditionException;
import com.gnilc.auth.authz.rbac.exception.InvalidArgumentException;
import com.gnilc.auth.authz.rbac.exception.UnknownErrorException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomExceptionControllerAdviceTest {

    private final CustomExceptionControllerAdvice advice = new CustomExceptionControllerAdvice();

    /**
     * Business argument failures stay inside the shared wrapper and let Spring MVC keep HTTP 200.
     */
    // TestCaseId: RBAC-EXCEPTION-001
    @Test
    void invalidArgumentReturnsBusinessFailureBody() {
        R<?> response = advice.invalidArgumentExceptionHandler(new InvalidArgumentException("role code required"));

        assertBusinessFailure(response, ResponseCode.ARGUMENT_INVALID, "role code required");
    }

    /**
     * Illegal business state failures stay inside the shared wrapper and let Spring MVC keep HTTP 200.
     */
    // TestCaseId: RBAC-EXCEPTION-006
    @Test
    void illegalConditionReturnsBusinessFailureBody() {
        R<?> response = advice.illegalConditionExceptionHandler(new IllegalConditionException("role is built in"));

        assertBusinessFailure(response, ResponseCode.ILLEGAL_CONDITION, "role is built in");
    }

    /**
     * Malformed request JSON is a transport failure and carries a real HTTP 400 status.
     */
    // TestCaseId: RBAC-EXCEPTION-002
    @Test
    void malformedJsonReturnsBadRequestResponseEntity() {
        ResponseEntity<R<?>> response = advice.httpMessageNotReadableExceptionHandler(
                new HttpMessageNotReadableException("malformed", null, null));

        assertTransportFailure(response, HttpStatus.BAD_REQUEST, ResponseCode.ARGUMENT_INVALID, "请求体格式错误");
    }

    /**
     * Type mismatch in request arguments is a transport failure and carries a real HTTP 400 status.
     */
    // TestCaseId: RBAC-EXCEPTION-007
    @Test
    void typeMismatchReturnsBadRequestResponseEntity() throws NoSuchMethodException {
        Method method = CustomExceptionControllerAdviceTest.class.getDeclaredMethod("handler", Long.class);
        MethodParameter parameter = new MethodParameter(method, 0);

        ResponseEntity<R<?>> response = advice.methodArgumentTypeMismatchExceptionHandler(
                new MethodArgumentTypeMismatchException("abc", Long.class, "id", parameter, new NumberFormatException("bad")));

        assertTransportFailure(response, HttpStatus.BAD_REQUEST, ResponseCode.ARGUMENT_INVALID, "请求参数格式错误");
    }

    /**
     * Unsupported request media type is a transport failure and carries a real HTTP 400 status.
     */
    // TestCaseId: RBAC-EXCEPTION-003
    @Test
    void unsupportedMediaTypeReturnsBadRequestResponseEntity() {
        ResponseEntity<R<?>> response = advice.httpMediaTypeNotSupportedExceptionHandler(
                new HttpMediaTypeNotSupportedException(MediaType.TEXT_PLAIN, List.of(MediaType.APPLICATION_JSON), HttpMethod.POST));

        assertTransportFailure(response, HttpStatus.BAD_REQUEST, ResponseCode.ARGUMENT_INVALID, "请求内容类型不支持");
    }

    /**
     * Unknown business errors are system failures and carry a real HTTP 500 status.
     */
    // TestCaseId: RBAC-EXCEPTION-008
    @Test
    void unknownErrorReturnsInternalServerErrorResponseEntity() {
        ResponseEntity<R<?>> response = advice.unknownErrorExceptionHandler(new UnknownErrorException("cache unavailable"));

        assertTransportFailure(response, HttpStatus.INTERNAL_SERVER_ERROR, ResponseCode.ERROR, "cache unavailable");
    }

    @SuppressWarnings("unused")
    private void handler(Long id) {
    }

    private void assertBusinessFailure(R<?> response, ResponseCode code, String error) {
        assertThat(response.getCode()).isEqualTo(code.getBusinessCode());
        assertThat(response.getData()).isNull();
        assertThat(response.getError()).isEqualTo(error);
        assertThat(response.getMessage()).isEqualTo(error);
    }

    private void assertTransportFailure(ResponseEntity<R<?>> response, HttpStatus status, ResponseCode code, String error) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isNotEqualTo(status.value());
        assertBusinessFailure(response.getBody(), code, error);
    }
}
