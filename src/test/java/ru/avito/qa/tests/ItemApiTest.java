package ru.avito.qa.tests;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.avito.qa.support.ApiAssertions;
import ru.avito.qa.support.ApiSession;
import ru.avito.qa.support.TestData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;



@Epic("API микросервиса объявлений")

@Feature("Объявления и статистика")

class ItemApiTest extends BaseAuthenticatedTest {

    @Test

    @Story("Создание и чтение")

    @Severity(SeverityLevel.BLOCKER)

    @DisplayName("ITEM-001: пользователь создаёт и получает своё объявление")

    void createsAndReadsAdvertisement() {

        String itemName = TestData.uniqueItemName();
        String itemId = session.createItem(TestData.validItem(session.user().id(), itemName));


        Response response = client.getItem(session.token(), itemId);


        ApiAssertions.assertStatus(response, 200);
        ApiAssertions.assertJson(response);



        List<Map<String, Object>> items = ApiAssertions.assertObjectArray(response);
        Assertions.assertEquals(1, items.size(), "По UUID должен возвращаться массив из одного объявления");



        Map<String, Object> item = items.get(0);
        Assertions.assertAll(

                () -> Assertions.assertEquals(
                        itemId,
                        String.valueOf(ApiAssertions.firstPresent(item, "id", "itemId")),
                        "UUID объявления не совпадает"
                ),
                () -> Assertions.assertEquals(itemName, item.get("name"), "name не совпадает"),
                () -> Assertions.assertEquals(
                        9_900L,
                        ApiAssertions.numberValue(item.get("price"), "price"),
                        "price не совпадает"
                ),
                () -> Assertions.assertEquals(
                        session.user().id(),
                        ApiAssertions.numberValue(
                                ApiAssertions.firstPresent(item, "sellerID", "sellerId"),
                                "sellerID"
                        ),
                        "sellerID не совпадает"
                )
        );
    }

    @Test

    @Story("Уникальность UUID")

    @Severity(SeverityLevel.CRITICAL)

    @DisplayName("ITEM-002: одинаковые бизнес-поля не приводят к одинаковому UUID")

    void assignsUniqueIdsToAdvertisementsWithSameFields() {

        Map<String, Object> payload = TestData.validItem(session.user().id(), TestData.uniqueItemName());


        String firstId = session.createItem(payload);

        String secondId = session.createItem(payload);

        Assertions.assertNotEquals(

                firstId,
                secondId,
                "Каждое созданное объявление должно получать уникальный UUID"
        );

    }



    @Test

    @Story("Объявления продавца")

    @Severity(SeverityLevel.CRITICAL)

    @DisplayName("ITEM-003: список продавца содержит все созданные им объявления")

    void returnsAllAdvertisementsOfSeller() {

        String firstName = TestData.uniqueItemName();
        String secondName = TestData.uniqueItemName();
        String firstId = session.createItem(TestData.validItem(session.user().id(), firstName));
        String secondId = session.createItem(TestData.validItem(session.user().id(), secondName));

        Response response = client.getSellerItems(session.token(), session.user().id());

        ApiAssertions.assertStatus(response, 200);
        ApiAssertions.assertJson(response);

        List<Map<String, Object>> items = ApiAssertions.assertObjectArray(response);
        Map<String, Object> firstItem = ApiAssertions.findById(items, firstId);
        Map<String, Object> secondItem = ApiAssertions.findById(items, secondId);

        Assertions.assertAll(
                () -> Assertions.assertEquals(firstName, firstItem.get("name")),
                () -> Assertions.assertEquals(secondName, secondItem.get("name"))
        );
    }



    static Stream<Arguments> invalidPayloads() {

        return Stream.of(

                Arguments.of("пустое имя", InvalidPayload.EMPTY_NAME),
                Arguments.of("отрицательная цена", InvalidPayload.NEGATIVE_PRICE),
                Arguments.of("отсутствует statistics", InvalidPayload.MISSING_STATISTICS),
                Arguments.of("sellerID имеет строковый тип", InvalidPayload.STRING_SELLER_ID)
        );
    }



    @ParameterizedTest(name = "{0}")

    @MethodSource("invalidPayloads")

    @Story("Валидация создания")

    @Severity(SeverityLevel.CRITICAL)

    @DisplayName("ITEM-004—ITEM-007: невалидное объявление отклоняется")

    void rejectsInvalidAdvertisementPayload(String caseName, InvalidPayload invalidPayload) {

        Map<String, Object> payload = new LinkedHashMap<>(
                TestData.validItem(session.user().id(), TestData.uniqueItemName())
        );



        switch (invalidPayload) {

            case EMPTY_NAME -> payload.put("name", "");
            case NEGATIVE_PRICE -> payload.put("price", -1);
            case MISSING_STATISTICS -> payload.remove("statistics");
            case STRING_SELLER_ID -> payload.put("sellerID", String.valueOf(session.user().id()));
        }



        Response response = client.createItem(session.token(), payload);

        ApiAssertions.assertStatus(response, 400);
        ApiAssertions.assertJson(response);
    }



    @Test

    @Story("Разграничение доступа")

    @Severity(SeverityLevel.BLOCKER)

    @DisplayName("ACCESS-003: sellerID и X-UserId не позволяют подменить пользователя")

    void rejectsSellerIdSubstitutionAndIgnoresXUserId() {

        ApiSession anotherSession = ApiSession.create(client);

        try {

            Map<String, Object> payload = TestData.validItem(
                    anotherSession.user().id(),
                    TestData.uniqueItemName()
            );



            Response response = client.createItem(

                    session.token(),
                    payload,
                    Map.of("X-UserId", String.valueOf(anotherSession.user().id()))
            );



            ApiAssertions.assertStatus(response, 400);
            ApiAssertions.assertJson(response);

        } finally {
            anotherSession.close();
        }
    }



    @Test

    @Story("Удаление")

    @Severity(SeverityLevel.BLOCKER)

    @DisplayName("ACCESS-004: другой пользователь не может удалить чужое объявление")

    void otherUserCannotDeleteAdvertisement() {

        String itemId = session.createItem(TestData.validItem(session.user().id()));
        ApiSession anotherSession = ApiSession.create(client);

        try {

            Response forbiddenDelete = client.deleteItem(anotherSession.token(), itemId);
            Assertions.assertTrue(

                    forbiddenDelete.statusCode() == 401 || forbiddenDelete.statusCode() == 404,
                    () -> "Удаление чужого объявления должно вернуть 401 или 404, получено HTTP "
                            + forbiddenDelete.statusCode() + ". Тело: " + forbiddenDelete.asString()
            );

            Response ownerRead = client.getItem(session.token(), itemId);
            ApiAssertions.assertStatus(ownerRead, 200);

        } finally {

            anotherSession.close();
        }
    }



    @Test

    @Story("Удаление")

    @Severity(SeverityLevel.BLOCKER)

    @DisplayName("ITEM-010: удалённое объявление больше не доступно")

    void deletesAdvertisement() {

        String itemId = session.createItem(TestData.validItem(session.user().id()));

        Response deleteResponse = client.deleteItem(session.token(), itemId);
        ApiAssertions.assertStatus(deleteResponse, 200);
        ApiAssertions.assertJson(deleteResponse);
        session.markDeleted(itemId);

        Response getResponse = client.getItem(session.token(), itemId);
        ApiAssertions.assertStatus(getResponse, 404);
        ApiAssertions.assertJson(getResponse);
    }



    @Test

    @Story("Статистика")

    @Severity(SeverityLevel.CRITICAL)

    @DisplayName("STAT-001, STAT-002: обе версии возвращают статистику объявления")

    void returnsStatisticsFromBothApiVersions() {

        String itemId = session.createItem(TestData.validItem(session.user().id()));

        Response v1Response = client.getStatisticV1(session.token(), itemId);
        Response v2Response = client.getStatisticV2(session.token(), itemId);

        ApiAssertions.assertStatistics(v1Response, 21, 11, 43);
        ApiAssertions.assertStatistics(v2Response, 21, 11, 43);
    }



    @Test

    @Story("Несуществующие ресурсы")

    @Severity(SeverityLevel.NORMAL)

    @DisplayName("ITEM-008, STAT-003: неизвестный UUID возвращает 404")

    void returnsNotFoundForUnknownResources() {

        String unknownId = UUID.randomUUID().toString();

        List<Response> responses = new ArrayList<>();
        responses.add(client.getItem(session.token(), unknownId));
        responses.add(client.getStatisticV1(session.token(), unknownId));
        responses.add(client.getStatisticV2(session.token(), unknownId));
        responses.add(client.deleteItem(session.token(), unknownId));



        for (Response response : responses) {

            ApiAssertions.assertStatus(response, 404);
            ApiAssertions.assertJson(response);
        }

    }



    @Test

    @Story("Валидация идентификатора")

    @Severity(SeverityLevel.NORMAL)

    @DisplayName("ITEM-009: некорректный формат UUID возвращает 400")

    void rejectsMalformedItemId() {

        Response response = client.getItem(session.token(), "not-a-uuid");

        ApiAssertions.assertStatus(response, 400);
        ApiAssertions.assertJson(response);
    }



    enum InvalidPayload {

        EMPTY_NAME,
        NEGATIVE_PRICE,
        MISSING_STATISTICS,
        STRING_SELLER_ID
    }
}