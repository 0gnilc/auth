package com.gnilc.auth.authz.rbac.exception.advice;

import com.gnilc.auth.authz.rbac.common.constant.ResponseCode;
import com.gnilc.auth.authz.rbac.common.utils.R;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;



@Order
@Slf4j
@Component("com.gnilc.auth.authz.rbac.exception.advice.DefaultExceptionControllerAdvice")
@RestControllerAdvice
public class DefaultExceptionControllerAdvice {

    @ExceptionHandler(value = RuntimeException.class)
    public ResponseEntity<R<?>> defaultRuntimeExceptionHandler(RuntimeException e, HttpServletRequest request) {
        log.error(e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.error(ResponseCode.ERROR, e.getMessage()));
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<R<?>> defaultExceptionHandler(Exception e, HttpServletRequest request) {
        log.error(e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.error(ResponseCode.ERROR, e.getMessage()));
    }

}
