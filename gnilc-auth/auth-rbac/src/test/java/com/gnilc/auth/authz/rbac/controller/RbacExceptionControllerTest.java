package com.gnilc.auth.authz.rbac.controller;

import com.gnilc.auth.authz.rbac.exception.IllegalConditionException;
import com.gnilc.auth.authz.rbac.exception.InvalidArgumentException;
import com.gnilc.auth.authz.rbac.exception.advice.CustomExceptionControllerAdvice;
import com.gnilc.auth.authz.rbac.exception.advice.DefaultExceptionControllerAdvice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RbacExceptionControllerTest {
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new CustomExceptionControllerAdvice(), new DefaultExceptionControllerAdvice())
                .build();
    }

    @Test
    void businessExceptionsRetainBusinessCodes() throws Exception {
        mvc.perform(get("/test/argument"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.error").value("bad argument"));
        mvc.perform(get("/test/condition"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10002));
    }

    @Test
    void malformedJsonAndUnexpectedFailuresUseTransportStatuses() throws Exception {
        mvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10001));
        mvc.perform(get("/test/runtime"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(10000));
    }

    @RestController
    static class ThrowingController {
        @GetMapping("/test/argument")
        void argument() {
            throw new InvalidArgumentException("bad argument");
        }

        @GetMapping("/test/condition")
        void condition() {
            throw new IllegalConditionException("bad state");
        }

        @GetMapping("/test/runtime")
        void runtime() {
            throw new RuntimeException("failed");
        }

        @PostMapping("/test/body")
        void body(@RequestBody Map<String, Object> body) {
        }
    }
}
