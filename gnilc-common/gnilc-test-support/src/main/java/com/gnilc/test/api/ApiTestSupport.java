package com.gnilc.test.api;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

/**
 * 随机端口接口测试的 RestAssured 请求构建基类。
 */
public abstract class ApiTestSupport {
    /**
     * 创建指向本机随机端口和应用 {@code /api} 上下文路径的请求规范。
     *
     * @param port Spring Boot 测试服务器实际监听的端口
     * @return 尚未发送、可继续追加请求头和请求体的请求规范
     */
    protected RequestSpecification requestSpecification(int port) {
        return RestAssured.given()
                .baseUri("http://localhost")
                .port(port)
                .basePath("/api");
    }
}
