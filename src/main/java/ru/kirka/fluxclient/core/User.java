package ru.kirka.fluxclient.core;

public record User(String uid, String username, String hwid, String role, String expire, String token) {
}
