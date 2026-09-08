package ru.avito.qa.tests;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.http.Method;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.avito.qa.client.ApiClient;
import ru.avito.qa.support.ApiAssertions;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@Epic("API микросервиса объявлений")

@Feature("Контроль доступа")

class AccessControlTest {

    private final ApiClient client = new ApiClient();

    static Stream<Arguments> protectedEndpoints() {
        String itemId = UUID.randomUUID().toString();

        return Stream.of(
                Arguments.of(
                        "создание объявления",

                        Method.POST,

                        "/api/1/item",

                        Map.of(

                                "sellerID", 1,

                                "name", "unauthorized-item",

                                "price", 100,

                                "statistics", Map.of(

                                        "likes", 0,

                                        "viewCount", 0,

                                        "contacts", 0
                                )
                        )
                ),
                Arguments.of(

                        "получение объявления",

                        Method.GET,

                        "/api/1/item/" + itemId,

                        null
                ),

                Arguments.of(

                        "получение объявлений продавца",

                        Method.GET,

                        "/api/1/1/item",

                        null
                ),

                Arguments.of(

                        "статистика v1",

                        Method.GET,

                        "/api/1/statistic/" + itemId,

                        null
                ),

                Arguments.of(

                        "статистика v2",

                        Method.GET,

                        "/api/2/statistic/" + itemId,

                        null
                ),

                Arguments.of(

                        "удаление объявления",

                        Method.DELETE,

                        "/api/2/item/" + itemId,

                        null
                )
        );
    }



    @ParameterizedTest(name = "{0}")

    @MethodSource("protectedEndpoints")

    @Story("Bearer-аутентификация")

    @Severity(SeverityLevel.BLOCKER)

    @DisplayName("ACCESS-001: защищённые ручки отклоняют запрос без токена")

    void protectedEndpointRejectsRequestWithoutToken(

            String endpointName,

            Method method,

            String path,

            Object body
    ) {

        Response response = client.request(method, path, null, body);



        ApiAssertions.assertStatus(response, 401);

        ApiAssertions.assertJson(response);
    }



    @Test

    @Story("Bearer-аутентификация")

    @Severity(SeverityLevel.BLOCKER)

    @DisplayName("ACCESS-002: некорректный Bearer-токен отклоняется")

    void protectedEndpointRejectsMalformedToken() {

        Response response = client.getItem("not-a-valid-jwt", UUID.randomUUID().toString());



        ApiAssertions.assertStatus(response, 401);

        ApiAssertions.assertJson(response);
    }
}