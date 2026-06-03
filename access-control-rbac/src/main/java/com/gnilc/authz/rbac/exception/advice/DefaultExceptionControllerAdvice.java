package com.gnilc.authz.rbac.exception.advice;


import com.gnilc.authz.rbac.common.constant.ResponseCode;
import com.gnilc.authz.rbac.common.utils.R;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;



@Order
@Slf4j
@Component("com.gnilc.authz.rbac.exception.advice.DefaultExceptionControllerAdvice")
@RestControllerAdvice
public class DefaultExceptionControllerAdvice {

    @ExceptionHandler(value = RuntimeException.class)
    public R<?> defaultRuntimeExceptionHandler(RuntimeException e, HttpServletRequest request) {
        log.error(e.getMessage(), e);
        return R.error(ResponseCode.ERROR.getCode(), e.getMessage());
    }

    @ExceptionHandler(value = Exception.class)
    public R<?> defaultExceptionHandler(Exception e, HttpServletRequest request) {
        log.error(e.getMessage(), e);
        return R.error(ResponseCode.ERROR.getCode(), e.getMessage());
    }

}
