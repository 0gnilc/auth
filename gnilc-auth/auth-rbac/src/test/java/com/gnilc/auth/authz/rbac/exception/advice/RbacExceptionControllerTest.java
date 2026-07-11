package com.gnilc.auth.authz.rbac.exception.advice;

import com.gnilc.auth.authz.rbac.common.utils.R;
import com.gnilc.auth.authz.rbac.exception.IllegalConditionException;
import com.gnilc.auth.authz.rbac.exception.InvalidArgumentException;
import com.gnilc.auth.authz.rbac.exception.UnknownErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class RbacExceptionControllerTest {
    private final CustomExceptionControllerAdvice customAdvice = new CustomExceptionControllerAdvice();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new ThrowingController())
                .setControllerAdvice(customAdvice, new DefaultExceptionControllerAdvice())
                .build();
    }

    @Test
    void businessExceptionsKeepHttp200AndUseIndependentBusinessCodes() throws Exception {
        mockMvc.perform(get("/test/errors/argument"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.error").value("bad argument"));
        mockMvc.perform(get("/test/errors/condition"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10002))
                .andExpect(jsonPath("$.error").value("bad state"));
    }

    @Test
    void transportInputErrorsReturnHttp400WithArgumentBusinessCode() throws Exception {
        mockMvc.perform(post("/test/errors/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.error").value("请求体格式错误"));
        mockMvc.perform(get("/test/errors/id/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.error").value("请求参数格式错误"));
        mockMvc.perform(post("/test/errors/body")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("text"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.error").value("请求内容类型不支持"));
    }

    @Test
    void systemAndFallbackExceptionsReturnHttp500() throws Exception {
        mockMvc.perform(get("/test/errors/unknown"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(10000))
                .andExpect(jsonPath("$.error").value("unknown failure"));
        mockMvc.perform(get("/test/errors/runtime"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(10000))
                .andExpect(jsonPath("$.error").value("runtime failure"));
        mockMvc.perform(get("/test/errors/checked"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(10000))
                .andExpect(jsonPath("$.error").value("checked failure"));
    }

    @Test
    void validationErrorsJoinEveryFieldMessage() throws Exception {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "request");
        bindingResult.addError(new FieldError("request", "name", "name required"));
        bindingResult.addError(new FieldError("request", "code", "code required"));
        MethodParameter methodParameter = new MethodParameter(
                ThrowingController.class.getDeclaredMethod("body", Map.class), 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                methodParameter, bindingResult);

        R<?> response = customAdvice.methodArgumentNotValidExceptionHandler(exception);

        assertThat(response.getCode()).isEqualTo(10001);
        assertThat(response.getError()).isEqualTo("name required\ncode required");
    }

    @RestController
    static class ThrowingController {
        @GetMapping("/test/errors/argument")
        void argument() {
            throw new InvalidArgumentException("bad argument");
        }

        @GetMapping("/test/errors/condition")
        void condition() {
            throw new IllegalConditionException("bad state");
        }

        @GetMapping("/test/errors/unknown")
        void unknown() {
            throw new UnknownErrorException("unknown failure");
        }

        @GetMapping("/test/errors/runtime")
        void runtime() {
            throw new RuntimeException("runtime failure");
        }

        @GetMapping("/test/errors/checked")
        void checked() throws Exception {
            throw new Exception("checked failure");
        }

        @GetMapping("/test/errors/id/{id}")
        Long id(@PathVariable("id") Long id) {
            return id;
        }

        @PostMapping(value = "/test/errors/body", consumes = MediaType.APPLICATION_JSON_VALUE)
        Map<String, Object> body(@RequestBody Map<String, Object> body) {
            return body;
        }
    }
}
