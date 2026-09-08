package ru.avito.qa.tests;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.avito.qa.client.ApiClient;
import ru.avito.qa.config.TestConfig;
import ru.avito.qa.model.UserCredentials;
import ru.avito.qa.support.ApiAssertions;
import ru.avito.qa.support.TestData;

import java.util.Map;



@Epic("API микросервиса объявлений")

@Feature("Регистрация и авторизация")

class RegistrationAuthorizationTest {

    private final ApiClient client = new ApiClient();

    @Test

    @Story("Регистрация")

    @Severity(SeverityLevel.BLOCKER)

    @DisplayName("AUTH-001: регистрация уникального пользователя")

    void registersUniqueUser() {

        UserCredentials credentials = TestData.uniqueCredentials();

        Response response = client.register(credentials.username(), credentials.password());

        ApiAssertions.assertStatus(response, 201);
        ApiAssertions.assertJson(response);

        long id = ApiAssertions.numberValue(response.jsonPath().get("id"), "id");
        String username = response.jsonPath().getString("username");

        Assertions.assertAll(

                () -> Assertions.assertTrue(id > 0, "id пользователя должен быть положительным"),
                () -> Assertions.assertEquals(
                        credentials.username(),
                        username,
                        "Ответ должен содержать отправленный username"
                )
        );
    }

    @Test

    @Story("Авторизация")

    @Severity(SeverityLevel.BLOCKER)

    @DisplayName("AUTH-002: авторизация зарегистрированного пользователя")

    void authorizesRegisteredUser() {

        UserCredentials credentials = TestData.uniqueCredentials();
        ApiAssertions.assertStatus(
                client.register(credentials.username(), credentials.password()),
                201
        );

        Response response = client.authorize(credentials.username(), credentials.password());

        ApiAssertions.assertStatus(response, 200);
        ApiAssertions.assertJson(response);

        String accessToken = response.jsonPath().getString("accessToken");
        String tokenType = response.jsonPath().getString("tokenType");

        Assertions.assertAll(

                () -> Assertions.assertNotNull(accessToken, "accessToken должен присутствовать"),
                () -> Assertions.assertFalse(
                        accessToken == null || accessToken.isBlank(),
                        "accessToken не должен быть пустым"
                ),
                () -> Assertions.assertTrue(
                        "Bearer".equalsIgnoreCase(tokenType),
                        "tokenType должен быть Bearer, получено: " + tokenType
                )
        );
    }

    @Test

    @Story("Регистрация")

    @Severity(SeverityLevel.CRITICAL)

    @DisplayName("AUTH-003: повторная регистрация существующего username отклоняется")

    void rejectsDuplicateUsername() {

        UserCredentials credentials = TestData.uniqueCredentials();

        ApiAssertions.assertStatus(

                client.register(credentials.username(), credentials.password()),
                201
        );

        Response duplicateResponse = client.register(credentials.username(), credentials.password());

        ApiAssertions.assertStatus(duplicateResponse, 409);
        ApiAssertions.assertJson(duplicateResponse);

    }

    @Test

    @Story("Регистрация")

    @Severity(SeverityLevel.CRITICAL)

    @DisplayName("AUTH-004: регистрация без username отклоняется")

    void rejectsRegistrationWithoutUsername() {

        Response response = client.register(Map.of("password", "Password!123"));

        ApiAssertions.assertStatus(response, 400);
        ApiAssertions.assertJson(response);
    }

    @Test

    @Story("Авторизация")

    @Severity(SeverityLevel.BLOCKER)

    @DisplayName("AUTH-005: неверный пароль не позволяет авторизоваться")

    void rejectsWrongPassword() {

        UserCredentials credentials = TestData.uniqueCredentials();
        ApiAssertions.assertStatus(
                client.register(credentials.username(), credentials.password()),
                201
        );

        Response response = client.authorize(credentials.username(), credentials.password() + "_wrong");

        ApiAssertions.assertStatus(response, 401);
        ApiAssertions.assertJson(response);
        Assertions.assertNull(

                response.jsonPath().get("accessToken"),
                "При неверном пароле accessToken не должен возвращаться"
        );
    }

    @Test

    @Story("Авторизация")

    @Severity(SeverityLevel.CRITICAL)

    @DisplayName("AUTH-006: неполный запрос авторизации отклоняется")

    void rejectsIncompleteAuthorizationRequest() {

        Response response = client.authorize(Map.of("username", TestData.uniqueCredentials().username()));

        ApiAssertions.assertStatus(response, 400);
        ApiAssertions.assertJson(response);
        Assertions.assertNull(

                response.jsonPath().get("accessToken"),
                "При некорректном запросе accessToken не должен возвращаться"
        );
    }



    @Test

    @Tag("performance")

    @Story("Производительность")

    @Severity(SeverityLevel.NORMAL)

    @DisplayName("NFR-001: регистрация укладывается в настроенный предел времени")

    void registrationRespondsWithinConfiguredLimit() {

        UserCredentials credentials = TestData.uniqueCredentials();

        Response response = client.register(credentials.username(), credentials.password());

        ApiAssertions.assertStatus(response, 201);
        Assertions.assertTrue(

                response.time() <= TestConfig.performanceMaxMs(),
                () -> "Регистрация заняла " + response.time()
                        + " мс при допустимом значении "
                        + TestConfig.performanceMaxMs() + " мс"
        );
    }
}