package ru.avito.qa.config;

public final class TestConfig {



    private static final String DEFAULT_BASE_URL = "https://qa-internship.avito.com";

    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 5_000;

    private static final int DEFAULT_SOCKET_TIMEOUT_MS = 10_000;

    private static final long DEFAULT_PERFORMANCE_MAX_MS = 5_000L;



    private TestConfig() {

    }



    public static String baseUrl() {

        return readString("baseUrl", "BASE_URL", DEFAULT_BASE_URL);

    }



    public static int connectTimeoutMs() {

        return readInt("connectTimeoutMs", "CONNECT_TIMEOUT_MS", DEFAULT_CONNECT_TIMEOUT_MS);

    }



    public static int socketTimeoutMs() {

        return readInt("socketTimeoutMs", "SOCKET_TIMEOUT_MS", DEFAULT_SOCKET_TIMEOUT_MS);

    }



    public static long performanceMaxMs() {

        return readLong("performanceMaxMs", "PERFORMANCE_MAX_MS", DEFAULT_PERFORMANCE_MAX_MS);

    }



    private static String readString(String propertyName, String environmentName, String defaultValue) {

        String systemValue = System.getProperty(propertyName);

        if (systemValue != null && !systemValue.isBlank()) {

            return systemValue;

        }



        String environmentValue = System.getenv(environmentName);

        if (environmentValue != null && !environmentValue.isBlank()) {

            return environmentValue;

        }



        return defaultValue;

    }



    private static int readInt(String propertyName, String environmentName, int defaultValue) {

        String value = readString(propertyName, environmentName, String.valueOf(defaultValue));

        try {

            return Integer.parseInt(value);

        } catch (NumberFormatException exception) {

            throw new IllegalArgumentException(

                    "Настройка " + propertyName + " должна быть целым числом, получено: " + value,

                    exception

            );

        }

    }



    private static long readLong(String propertyName, String environmentName, long defaultValue) {

        String value = readString(propertyName, environmentName, String.valueOf(defaultValue));

        try {

            return Long.parseLong(value);

        } catch (NumberFormatException exception) {

            throw new IllegalArgumentException(

                    "Настройка " + propertyName + " должна быть целым числом, получено: " + value,

                    exception

            );

        }

    }

}
