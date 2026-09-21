package com.gnilc.common.exception;

import com.gnilc.common.constant.ResponseCode;
import com.gnilc.common.i18n.I18nMessageService;
import com.gnilc.common.i18n.SupportedLocale;
import com.gnilc.common.utils.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 由应用显式导入的 REST 异常出口与请求语言配置。
 *
 * <p>业务错误使用 R.code，HTTP 状态由具体异常分支决定；二者不互相替代。</p>
 */
@Import(I18nMessageService.class)
public class RestExceptionHandlingConfiguration {

    @Bean
    public LocaleResolver localeResolver(
            @Value("${app.i18n.default-locale:en-US}") String defaultLocale) {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(SupportedLocale.locales());
        resolver.setDefaultLocale(SupportedLocale.fromLanguageTagOrDefault(defaultLocale));
        return resolver;
    }

    @Bean
    RestExceptionControllerAdvice restExceptionControllerAdvice(I18nMessageService i18nMessageService) {
        return new RestExceptionControllerAdvice(i18nMessageService);
    }

    /** 保留已知错误的业务文案；对未预期异常记录诊断原因并向调用方返回通用本地化提示。 */
    @RestControllerAdvice
    @Order(Ordered.LOWEST_PRECEDENCE)
    @Conditional(ExplicitImportOnlyCondition.class)
    public static final class RestExceptionControllerAdvice {

        private final Logger log = LoggerFactory.getLogger(RestExceptionControllerAdvice.class);
        private final I18nMessageService i18nMessageService;

        public RestExceptionControllerAdvice(I18nMessageService i18nMessageService) {
            this.i18nMessageService = i18nMessageService;
        }

        /** 返回字段错误集合；该校验日志只记录字段与约束码，不输出用户提交的被拒绝值。 */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public R<?> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
            BindingResult bindingResult = exception.getBindingResult();
            List<FieldError> fieldErrors = bindingResult.getFieldErrors().stream()
                    .map(error -> new FieldError(
                            error.getField(),
                            error.getCode(),
                            error.getDefaultMessage()))
                    .toList();
            String message = fieldErrors.stream()
                    .map(FieldError::getMessage)
                    .filter(value -> value != null && !value.isBlank())
                    .findFirst()
                    .orElse(i18nMessageService.get("validation.argument.invalid"));
            String diagnostics = fieldErrors.stream()
                    .map(error -> error.getField() + ":"
                            + (error.getCode() == null
                            ? "Unknown" : error.getCode()))
                    .collect(Collectors.joining(", "));
            log.warn("Request validation failed: {}", diagnostics);
            return R.error(ResponseCode.ARGUMENT_INVALID.getCode(), message, fieldErrors);
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<R<?>> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
            log.warn("Request body could not be read: {}", exception.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(R.error(ResponseCode.ARGUMENT_INVALID,
                            i18nMessageService.get("validation.body.malformed")));
        }

        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<R<?>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException exception) {
            log.warn("Request parameter has an invalid format: {}", exception.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(R.error(ResponseCode.ARGUMENT_INVALID,
                            i18nMessageService.get("validation.parameter.format.invalid")));
        }

        @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
        public ResponseEntity<R<?>> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException exception) {
            log.warn("Request content type is not supported: {}", exception.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(R.error(ResponseCode.ARGUMENT_INVALID,
                            i18nMessageService.get("validation.media.type.unsupported")));
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

        @ExceptionHandler(AuthenticationFailedException.class)
        public R<?> handleAuthenticationFailed(AuthenticationFailedException exception) {
            log.warn("Authentication failed: {}", exception.getMessage());
            return R.error(ResponseCode.AUTHENTICATION_FAILED, exception.getMessage());
        }

        /** 会话缺失或失效使用 HTTP 401，供客户端已有的刷新或重新认证流程识别。 */
        @ExceptionHandler(UnauthorizedException.class)
        public ResponseEntity<R<?>> handleUnauthorized(UnauthorizedException exception) {
            log.warn("Unauthorized request: {}", exception.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(R.error(ResponseCode.UNAUTHORIZED, exception.getMessage()));
        }

        @ExceptionHandler(UnknownErrorException.class)
        public ResponseEntity<R<?>> handleUnknownError(UnknownErrorException exception) {
            log.error("Application error", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(R.error(ResponseCode.ERROR, exception.getMessage()));
        }

        @ExceptionHandler(NoResourceFoundException.class)
        public ResponseEntity<R<?>> handleNoResourceFoundException(NoResourceFoundException exception) {
            log.warn("No resource found: {}", exception.getResourcePath());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(R.error(ResponseCode.NO_RESOURCE_FOUND,
                            exception.getMessage()));
        }

        /** 意外失败保留服务端异常链，响应使用通用文案，不将实现异常消息直接暴露给客户端。 */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<R<?>> handleUnexpectedException(Exception exception) {
            log.error("Unhandled exception", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(R.error(ResponseCode.ERROR,
                            i18nMessageService.get("common.unexpected.error")));
        }
    }

    /**
     * 阻止组件扫描自行启用异常处理器；配置中的 {@link Bean} 方法是显式注册入口。
     */
    static final class ExplicitImportOnlyCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return false;
        }
    }
}
