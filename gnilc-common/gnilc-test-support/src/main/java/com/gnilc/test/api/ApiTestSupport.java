package com.gnilc.test.api;

import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.web.server.LocalServerPort;

public abstract class ApiTestSupport {
    @LocalServerPort
    protected int port;

    @BeforeEach
    final void configureRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @AfterEach
    final void resetRestAssured() {
        RestAssured.reset();
    }
}
