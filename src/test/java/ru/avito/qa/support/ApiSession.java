package ru.avito.qa.support;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import ru.avito.qa.client.ApiClient;
import ru.avito.qa.model.RegisteredUser;
import ru.avito.qa.model.UserCredentials;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ApiSession implements AutoCloseable {

    private static final Pattern UUID_PATTERN = Pattern.compile(

            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}"

    );

    private final ApiClient client;
    private final RegisteredUser user;
    private final List<String> createdItemIds = new ArrayList<>();

    private ApiSession(ApiClient client, RegisteredUser user) {

        this.client = client;

        this.user = user;

    }

    @Step("Создание изолированной пользовательской сессии")

    public static ApiSession create(ApiClient client) {

        UserCredentials credentials = TestData.uniqueCredentials();

        Response registration = client.register(credentials.username(), credentials.password());

        ApiAssertions.assertStatus(registration, 201);

        ApiAssertions.assertJson(registration);

        Object rawId = registration.jsonPath().get("id");

        long sellerId = ApiAssertions.numberValue(rawId, "id");

        String returnedUsername = registration.jsonPath().getString("username");


        Assertions.assertTrue(sellerId > 0, "id зарегистрированного пользователя должен быть положительным");

        Assertions.assertEquals(

                credentials.username(),

                returnedUsername,

                "API должно вернуть username созданного пользователя"
        );

        Response authorization = client.authorize(credentials.username(), credentials.password());

        ApiAssertions.assertStatus(authorization, 200);

        ApiAssertions.assertJson(authorization);

        String token = authorization.jsonPath().getString("accessToken");

        String tokenType = authorization.jsonPath().getString("tokenType");

        Assertions.assertNotNull(token, "Ответ авторизации должен содержать accessToken");

        Assertions.assertFalse(token.isBlank(), "accessToken не должен быть пустым");

        Assertions.assertEquals("Bearer", tokenType, "tokenType должен быть Bearer");

        RegisteredUser user = new RegisteredUser(

                sellerId,

                credentials.username(),

                credentials.password(),

                token
        );

        return new ApiSession(client, user);

    }

    @Step("Создание и регистрация объявления для последующей очистки")

    public String createItem(Map<String, ?> payload) {

        Response response = client.createItem(user.accessToken(), payload);

        ApiAssertions.assertStatus(response, 200);

        ApiAssertions.assertJson(response);

        String status = response.jsonPath().getString("status");

        Assertions.assertNotNull(status, "Ответ создания должен содержать поле status");

        Matcher matcher = UUID_PATTERN.matcher(status);

        Assertions.assertTrue(

                matcher.find(),

                () -> "В status отсутствует валидный UUID объявления: " + status
        );

        String itemId = matcher.group();

        createdItemIds.add(itemId);

        return itemId;

    }

    public RegisteredUser user() {

        return user;

    }

    public String token() {

        return user.accessToken();

    }

    public void markDeleted(String itemId) {

        createdItemIds.remove(itemId);

    }

    @Override

    @Step("Очистка объявлений, созданных тестом")

    public void close() {

        List<String> reverseOrder = new ArrayList<>(createdItemIds);

        Collections.reverse(reverseOrder);

        for (String itemId : reverseOrder) {

            try {

                Response response = client.deleteItem(user.accessToken(), itemId);

                if (response.statusCode() != 200 && response.statusCode() != 404) {

                    Allure.addAttachment(

                            "Ошибка очистки объявления " + itemId,

                            "text/plain",

                            "HTTP " + response.statusCode() + "\n" + response.asString()
                    );
                }
            } catch (RuntimeException exception) {

                Allure.addAttachment(

                        "Исключение при очистке объявления " + itemId,

                        "text/plain",

                        exception.toString()
                );
            }
        }
        createdItemIds.clear();
    }
}