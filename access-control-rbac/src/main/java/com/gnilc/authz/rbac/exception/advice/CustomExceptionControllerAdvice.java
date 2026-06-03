package com.gnilc.authz.rbac.exception.advice;

import com.gnilc.authz.rbac.common.constant.ResponseCode;
import com.gnilc.authz.rbac.common.utils.R;
import com.gnilc.authz.rbac.exception.IllegalConditionException;
import com.gnilc.authz.rbac.exception.InvalidArgumentException;
import com.gnilc.authz.rbac.exception.UnknownErrorException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@Component("com.gnilc.authz.rbac.exception.advice.CustomExceptionControllerAdvice")
@RestControllerAdvice
@Order(Integer.MAX_VALUE - 2)
public class CustomExceptionControllerAdvice {

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public R<?> methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        BindingResult result = e.getBindingResult();
        List<String> messages = result.getFieldErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).toList();
        String s = StringUtils.join(messages, "\n");
        log.error(s, e);
        return R.error(ResponseCode.ARGUMENT_INVALID.getCode(), s);
    }

    @ExceptionHandler(value = InvalidArgumentException.class)
    public R<?> invalidArgumentExceptionHandler(InvalidArgumentException e) {
        log.error(e.getMessage(), e);
        return R.error(ResponseCode.ARGUMENT_INVALID.getCode(), e.getMessage());
    }

    @ExceptionHandler(value = IllegalConditionException.class)
    public R<?> illegalConditionExceptionHandler(IllegalConditionException e) {
        log.error(e.getMessage(), e);
        return R.error(ResponseCode.ILLEGAL_CONDITION.getCode(), e.getMessage());
    }

    @ExceptionHandler(value = UnknownErrorException.class)
    public R<?> unknownErrorExceptionHandler(UnknownErrorException e) {
        log.error(e.getMessage(), e);
        return R.error(ResponseCode.ERROR.getCode(), e.getMessage());
    }
}
