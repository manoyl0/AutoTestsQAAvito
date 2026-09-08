package ru.avito.qa.tests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import ru.avito.qa.client.ApiClient;
import ru.avito.qa.support.ApiSession;

abstract class BaseAuthenticatedTest {

    protected ApiClient client;
    protected ApiSession session;

    @BeforeEach

    void createAuthenticatedSession() {

        client = new ApiClient();

        session = ApiSession.create(client);
    }

    @AfterEach

    void cleanCreatedResources() {

        if (session != null) {

            session.close();
        }
    }
}