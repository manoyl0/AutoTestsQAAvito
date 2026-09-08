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
import ru.avito.qa.support.ApiAssertions;
import ru.avito.qa.support.TestData;

import java.util.List;
import java.util.Map;

@Epic("API микросервиса объявлений")

@Feature("Сквозной сценарий")

class E2eTest extends BaseAuthenticatedTest {

    @Test

    @Tag("e2e")

    @Story("Жизненный цикл объявления")

    @Severity(SeverityLevel.BLOCKER)

    @DisplayName("E2E-001: регистрация → авторизация → создание → чтение → статистика → удаление")

    void userCanCompleteFullAdvertisementLifecycle() {

        String itemName = TestData.uniqueItemName();

        String itemId = session.createItem(

                TestData.validItem(session.user().id(), itemName)
        );

        Response itemResponse = client.getItem(session.token(), itemId);
        ApiAssertions.assertStatus(itemResponse, 200);
        ApiAssertions.assertJson(itemResponse);



        List<Map<String, Object>> items = ApiAssertions.assertObjectArray(itemResponse);
        Assertions.assertEquals(1, items.size(), "По UUID должно вернуться одно объявление");

        Map<String, Object> item = items.get(0);
        Assertions.assertAll(
                () -> Assertions.assertEquals(
                        itemId,
                        String.valueOf(ApiAssertions.firstPresent(item, "id", "itemId"))
                ),
                () -> Assertions.assertEquals(itemName, item.get("name")),
                () -> Assertions.assertEquals(
                        session.user().id(),
                        ApiAssertions.numberValue(
                                ApiAssertions.firstPresent(item, "sellerID", "sellerId"),
                                "sellerID"
                        )
                )
        );

        Response sellerItemsResponse = client.getSellerItems(
                session.token(),
                session.user().id()
        );

        ApiAssertions.assertStatus(sellerItemsResponse, 200);
        ApiAssertions.assertJson(sellerItemsResponse);

        List<Map<String, Object>> sellerItems = ApiAssertions.assertObjectArray(sellerItemsResponse);

        Map<String, Object> itemFromSellerList = ApiAssertions.findById(sellerItems, itemId);
        Assertions.assertEquals(
                itemName,
                itemFromSellerList.get("name"),
                "Объявление в списке продавца должно иметь исходное имя"
        );

        ApiAssertions.assertStatistics(

                client.getStatisticV1(session.token(), itemId),
                21,
                11,
                43
        );

        ApiAssertions.assertStatistics(

                client.getStatisticV2(session.token(), itemId),
                21,
                11,
                43
        );

        Response deleteResponse = client.deleteItem(session.token(), itemId);

        ApiAssertions.assertStatus(deleteResponse, 200);

        ApiAssertions.assertJson(deleteResponse);

        session.markDeleted(itemId);

        Response deletedItemResponse = client.getItem(session.token(), itemId);

        ApiAssertions.assertStatus(deletedItemResponse, 404);

        ApiAssertions.assertJson(deletedItemResponse);
    }
}