package ru.avito.qa.support;

import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;

import java.util.List;
import java.util.Map;

public final class ApiAssertions {

    private ApiAssertions() {

    }

    public static void assertStatus(Response response, int expectedStatus) {

        Assertions.assertEquals(

                expectedStatus,

                response.statusCode(),

                () -> "Ожидался HTTP " + expectedStatus

                        + ", получен HTTP " + response.statusCode()

                        + ". Тело ответа: " + response.asString()
        );
    }

    public static void assertJson(Response response) {

        String contentType = response.contentType();

        Assertions.assertNotNull(

                contentType,

                () -> "В ответе отсутствует Content-Type. Тело ответа: " + response.asString()
        );

        Assertions.assertTrue(

                contentType.toLowerCase().contains("application/json"),

                () -> "Ожидался JSON Content-Type, получено: " + contentType

                        + ". Тело ответа: " + response.asString()
        );
    }

    public static List<Map<String, Object>> assertObjectArray(Response response) {

        Object root = response.jsonPath().get();

        Assertions.assertInstanceOf(

                List.class,

                root,

                () -> "Корневой элемент ответа должен быть массивом. Тело: " + response.asString()
        );

        List<?> rawList = (List<?>) root;

        for (Object element : rawList) {

            Assertions.assertInstanceOf(

                    Map.class,

                    element,

                    () -> "Каждый элемент массива должен быть JSON-объектом. Тело: " + response.asString()
            );
        }

        @SuppressWarnings("unchecked")

        List<Map<String, Object>> result = (List<Map<String, Object>>) (List<?>) rawList;

        return result;

    }

    public static long numberValue(Object value, String fieldName) {

        Assertions.assertInstanceOf(

                Number.class,

                value,

                () -> "Поле " + fieldName + " должно быть числом, получено: " + value
        );
        return ((Number) value).longValue();
    }

    public static Object firstPresent(Map<String, Object> object, String... names) {

        for (String name : names) {

            if (object.containsKey(name)) {

                return object.get(name);
            }
        }
        Assertions.fail("Не найдено ни одно из ожидаемых полей: " + String.join(", ", names)

                + ". Объект: " + object);

        return null;
    }

    public static Map<String, Object> findById(List<Map<String, Object>> items, String expectedId) {

        return items.stream()

                .filter(item -> expectedId.equals(String.valueOf(firstPresent(item, "id", "itemId"))))

                .findFirst()

                .orElseThrow(() -> new AssertionError(

                        "Объявление " + expectedId + " отсутствует в ответе: " + items
                ));
    }

    public static void assertStatistics(Response response, long likes, long viewCount, long contacts) {

        assertStatus(response, 200);

        assertJson(response);

        List<Map<String, Object>> result = assertObjectArray(response);

        Assertions.assertFalse(result.isEmpty(), "Массив статистики не должен быть пустым");

        Map<String, Object> statistics = result.get(0);

        Assertions.assertAll(
                () -> Assertions.assertEquals(

                        likes,

                        numberValue(statistics.get("likes"), "likes"),

                        "Некорректное значение likes"

                ),
                () -> Assertions.assertEquals(

                        viewCount,

                        numberValue(statistics.get("viewCount"), "viewCount"),

                        "Некорректное значение viewCount"

                ),
                () -> Assertions.assertEquals(

                        contacts,

                        numberValue(statistics.get("contacts"), "contacts"),

                        "Некорректное значение contacts"

                )
        );
    }
}