package com.gnilc.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RestExceptionControllerAdviceTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new RestExceptionControllerAdvice())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void commonExceptionsRetainTheirBusinessCodes() throws Exception {
        mvc.perform(get("/test/argument"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.error").value("Invalid argument."));

        mvc.perform(get("/test/condition"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10002))
                .andExpect(jsonPath("$.error").value("The requested operation is not allowed in the current state."));
    }

    @Test
    void malformedRequestsReturnProfessionalEnglishMessages() throws Exception {
        mvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.error").value("The request body is malformed."));

        mvc.perform(get("/test/number").param("value", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.error").value("A request parameter has an invalid format."));
    }

    @Test
    void unexpectedFailuresDoNotExposeImplementationDetails() throws Exception {
        mvc.perform(get("/test/runtime"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(10000))
                .andExpect(jsonPath("$.error").value("An unexpected error occurred."));
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/test/argument")
        void argument() {
            throw new InvalidArgumentException("Invalid argument.");
        }

        @GetMapping("/test/condition")
        void condition() {
            throw new IllegalConditionException("The requested operation is not allowed in the current state.");
        }

        @GetMapping("/test/runtime")
        void runtime() {
            throw new RuntimeException("database password leaked");
        }

        @PostMapping("/test/body")
        void body(@RequestBody Map<String, Object> body) {
        }

        @GetMapping("/test/number")
        void number(@RequestParam("value") Integer value) {
        }
    }
}
