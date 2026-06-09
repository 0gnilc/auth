package com.gnilc.authz.example;

import com.gnilc.authz.rbac.common.constant.ResponseCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("localtest")
class RbacExampleApplicationContextIT {
    @Autowired
    private MockMvc mockMvc;

    /**
     * Verifies the example application exposes the permission list API.
     */
    @Test
    void permissionListIsExposedByExampleApplication() throws Exception {
        mockMvc.perform(post("/api/authz/permission/list")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResponseCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.SUCCESS.getMessage()));
    }
}
