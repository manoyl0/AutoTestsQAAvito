package ru.avito.qa.client;

import io.qameta.allure.Step;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.http.ContentType;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import ru.avito.qa.config.TestConfig;

import java.util.Collections;
import java.util.Map;

public final class ApiClient {

    private final AllureRestAssured allureFilter;

    public ApiClient() {

        RestAssured.baseURI = TestConfig.baseUrl();

        RestAssured.config = RestAssured.config()

                .httpClient(HttpClientConfig.httpClientConfig()

                        .setParam("http.connection.timeout", TestConfig.connectTimeoutMs())

                        .setParam("http.socket.timeout", TestConfig.socketTimeoutMs())

                        .setParam("http.connection-manager.timeout", (long) TestConfig.connectTimeoutMs()));

        this.allureFilter = new AllureRestAssured()

                .setRequestAttachmentName("HTTP request")

                .setResponseAttachmentName("HTTP response");

    }

    @Step("Регистрация пользователя {username}")

    public Response register(String username, String password) {

        return publicJsonRequest()

                .body(Map.of(

                        "username", username,

                        "password", password

                ))

                .post("/api/1/register");

    }

    @Step("Регистрация пользователя с произвольным телом")

    public Response register(Map<String, ?> body) {

        return publicJsonRequest()

                .body(body)

                .post("/api/1/register");

    }

    @Step("Авторизация пользователя {username}")

    public Response authorize(String username, String password) {

        return publicJsonRequest()

                .body(Map.of(

                        "username", username,

                        "password", password

                ))

                .post("/api/1/autorize");

    }

    @Step("Авторизация с произвольным телом")

    public Response authorize(Map<String, ?> body) {

        return publicJsonRequest()

                .body(body)

                .post("/api/1/autorize");

    }

    @Step("Создание объявления")

    public Response createItem(String token, Map<String, ?> body) {

        return createItem(token, body, Collections.emptyMap());

    }

    @Step("Создание объявления с дополнительными заголовками")

    public Response createItem(String token, Map<String, ?> body, Map<String, ?> headers) {

        RequestSpecification request = protectedJsonRequest(token).headers(headers);

        return request.body(body).post("/api/1/item");

    }

    @Step("Получение объявления {itemId}")

    public Response getItem(String token, String itemId) {

        return protectedRequest(token)

                .pathParam("id", itemId)

                .get("/api/1/item/{id}");

    }

    @Step("Получение объявлений продавца {sellerId}")

    public Response getSellerItems(String token, long sellerId) {

        return protectedRequest(token)

                .pathParam("sellerId", sellerId)

                .get("/api/1/{sellerId}/item");

    }

    @Step("Получение статистики v1 объявления {itemId}")

    public Response getStatisticV1(String token, String itemId) {

        return protectedRequest(token)

                .pathParam("id", itemId)

                .get("/api/1/statistic/{id}");

    }

    @Step("Получение статистики v2 объявления {itemId}")

    public Response getStatisticV2(String token, String itemId) {

        return protectedRequest(token)

                .pathParam("id", itemId)

                .get("/api/2/statistic/{id}");

    }

    @Step("Удаление объявления {itemId}")

    public Response deleteItem(String token, String itemId) {

        return protectedRequest(token)

                .pathParam("id", itemId)

                .delete("/api/2/item/{id}");

    }

    @Step("Произвольный запрос {method} {path}")

    public Response request(Method method, String path, String token, Object body) {

        RequestSpecification request = protectedRequest(token);

        if (body != null) {

            request.contentType(ContentType.JSON).body(body);

        }

        return request.request(method, path);

    }

    private RequestSpecification publicJsonRequest() {

        return RestAssured.given()

                .filter(allureFilter)

                .accept(ContentType.JSON)

                .contentType(ContentType.JSON);

    }

    private RequestSpecification protectedJsonRequest(String token) {

        return protectedRequest(token).contentType(ContentType.JSON);

    }

    private RequestSpecification protectedRequest(String token) {

        RequestSpecification request = RestAssured.given()

                .filter(allureFilter)

                .accept(ContentType.JSON);

        if (token != null && !token.isBlank()) {

            request.header("Authorization", "Bearer " + token);

        }

        return request;

    }

}