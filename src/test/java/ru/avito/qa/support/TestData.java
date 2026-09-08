package ru.avito.qa.support;

import ru.avito.qa.model.UserCredentials;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class TestData {

    private TestData() {

    }

    public static UserCredentials uniqueCredentials() {

        String suffix = UUID.randomUUID().toString().replace("-", "");

        return new UserCredentials(

                "qa_" + suffix,

                "Pwd!" + suffix + "9a"
        );
    }

    public static String uniqueItemName() {

        return "item_" + UUID.randomUUID();
    }

    public static Map<String, Object> validItem(long sellerId) {

        return validItem(sellerId, uniqueItemName());
    }

    public static Map<String, Object> validItem(long sellerId, String itemName) {

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("sellerID", sellerId);

        body.put("name", itemName);

        body.put("price", 9_900);

        body.put("statistics", statistics());

        return body;
    }

    public static Map<String, Object> statistics() {

        Map<String, Object> statistics = new LinkedHashMap<>();

        statistics.put("likes", 21);

        statistics.put("viewCount", 11);

        statistics.put("contacts", 43);

        return statistics;
    }
}