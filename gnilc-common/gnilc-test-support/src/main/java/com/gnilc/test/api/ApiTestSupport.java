package com.gnilc.test.api;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

public abstract class ApiTestSupport {
    protected RequestSpecification requestSpecification(int port) {
        return RestAssured.given()
                .baseUri("http://localhost")
                .port(port)
                .basePath("/api");
    }
}
