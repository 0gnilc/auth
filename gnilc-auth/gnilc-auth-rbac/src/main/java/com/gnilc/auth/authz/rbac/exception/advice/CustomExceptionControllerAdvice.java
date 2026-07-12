package com.gnilc.auth.authz.rbac.exception.advice;

import com.gnilc.common.constant.ResponseCode;
import com.gnilc.common.utils.R;
import com.gnilc.common.exception.IllegalConditionException;
import com.gnilc.common.exception.InvalidArgumentException;
import com.gnilc.common.exception.UnknownErrorException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@Slf4j
@Component("com.gnilc.auth.authz.rbac.exception.advice.CustomExceptionControllerAdvice")
@RestControllerAdvice
@Order(Integer.MAX_VALUE - 2)
public class CustomExceptionControllerAdvice {

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public R<?> methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        BindingResult result = e.getBindingResult();
        List<String> messages = result.getFieldErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).toList();
        String s = StringUtils.join(messages, "\n");
        log.error(s, e);
        return R.error(ResponseCode.ARGUMENT_INVALID, s);
    }

    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public ResponseEntity<R<?>> httpMessageNotReadableExceptionHandler(HttpMessageNotReadableException e) {
        log.error(e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(R.error(ResponseCode.ARGUMENT_INVALID, "请求体格式错误"));
    }

    @ExceptionHandler(value = MethodArgumentTypeMismatchException.class)
    public ResponseEntity<R<?>> methodArgumentTypeMismatchExceptionHandler(MethodArgumentTypeMismatchException e) {
        log.error(e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(R.error(ResponseCode.ARGUMENT_INVALID, "请求参数格式错误"));
    }

    @ExceptionHandler(value = HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<R<?>> httpMediaTypeNotSupportedExceptionHandler(HttpMediaTypeNotSupportedException e) {
        log.error(e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(R.error(ResponseCode.ARGUMENT_INVALID, "请求内容类型不支持"));
    }

    @ExceptionHandler(value = InvalidArgumentException.class)
    public R<?> invalidArgumentExceptionHandler(InvalidArgumentException e) {
        log.error(e.getMessage(), e);
        return R.error(ResponseCode.ARGUMENT_INVALID, e.getMessage());
    }

    @ExceptionHandler(value = IllegalConditionException.class)
    public R<?> illegalConditionExceptionHandler(IllegalConditionException e) {
        log.error(e.getMessage(), e);
        return R.error(ResponseCode.ILLEGAL_CONDITION, e.getMessage());
    }

    @ExceptionHandler(value = UnknownErrorException.class)
    public ResponseEntity<R<?>> unknownErrorExceptionHandler(UnknownErrorException e) {
        log.error(e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.error(ResponseCode.ERROR, e.getMessage()));
    }
}
