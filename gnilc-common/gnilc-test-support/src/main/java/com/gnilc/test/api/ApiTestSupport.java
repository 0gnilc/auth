package com.gnilc.test.api;

import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * 为随机端口接口测试统一配置并复位 RestAssured 全局状态。
 */
public abstract class ApiTestSupport {
    /** 当前测试应用实际监听的随机端口。 */
    @LocalServerPort
    protected int port;

    /** 在每个测试方法前绑定当前应用地址，并开启失败时的请求响应日志。 */
    @BeforeEach
    final void configureRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    /** 在每个测试方法后清除 RestAssured 静态配置，避免上下文间相互污染。 */
    @AfterEach
    final void resetRestAssured() {
        RestAssured.reset();
    }
}
