package com.gnilc.common.exception;

import com.gnilc.common.constant.ResponseCode;
import com.gnilc.common.utils.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Objects;

/**
 * Opt-in configuration for the common REST exception handling policy.
 *
 * <p>Applications control activation by explicitly importing this configuration.</p>
 */
public class RestExceptionHandlingConfiguration {

    @Bean
    RestExceptionControllerAdvice restExceptionControllerAdvice() {
        return new RestExceptionControllerAdvice();
    }

    @RestControllerAdvice
    @Order(Ordered.LOWEST_PRECEDENCE)
    final class RestExceptionControllerAdvice {

        private final Logger log = LoggerFactory.getLogger(RestExceptionControllerAdvice.class);
        private static final String UNEXPECTED_ERROR_MESSAGE = "An unexpected error occurred.";

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public R<?> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
            BindingResult bindingResult = exception.getBindingResult();
            String message = bindingResult.getFieldErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .filter(Objects::nonNull)
                    .reduce((left, right) -> left + "\n" + right)
                    .orElse("The request contains invalid fields.");
            log.warn("Request validation failed: {}", message);
            return R.error(ResponseCode.ARGUMENT_INVALID, message);
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<R<?>> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
            log.warn("Request body could not be read: {}", exception.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(R.error(ResponseCode.ARGUMENT_INVALID, "The request body is malformed."));
        }

        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<R<?>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException exception) {
            log.warn("Request parameter has an invalid format: {}", exception.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(R.error(ResponseCode.ARGUMENT_INVALID, "A request parameter has an invalid format."));
        }

        @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
        public ResponseEntity<R<?>> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException exception) {
            log.warn("Request content type is not supported: {}", exception.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(R.error(ResponseCode.ARGUMENT_INVALID, "The request content type is not supported."));
        }

        @ExceptionHandler(InvalidArgumentException.class)
        public R<?> handleInvalidArgument(InvalidArgumentException exception) {
            log.warn("Invalid argument: {}", exception.getMessage());
            return R.error(ResponseCode.ARGUMENT_INVALID, exception.getMessage());
        }

        @ExceptionHandler(IllegalConditionException.class)
        public R<?> handleIllegalCondition(IllegalConditionException exception) {
            log.warn("Illegal condition: {}", exception.getMessage());
            return R.error(ResponseCode.ILLEGAL_CONDITION, exception.getMessage());
        }

        @ExceptionHandler(UnknownErrorException.class)
        public ResponseEntity<R<?>> handleUnknownError(UnknownErrorException exception) {
            log.error("Application error", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(R.error(ResponseCode.ERROR, exception.getMessage()));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<R<?>> handleUnexpectedException(Exception exception) {
            log.error("Unhandled exception", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(R.error(ResponseCode.ERROR, UNEXPECTED_ERROR_MESSAGE));
        }
    }
}
