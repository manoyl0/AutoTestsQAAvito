package ru.avito.qa.model;

public record RegisteredUser(long id, String username, String password, String accessToken) {
}