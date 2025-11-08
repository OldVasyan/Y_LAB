package com.marketplace.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Простой сервис авторизации.
 * Пользователи хранятся в памяти (Map login -> password).
 * Нет регистрации! пользователей добавляем заранее. Есть логирование
 */
public class AuthService {

    private final Map<String, String> users = new HashMap<>();
    private String currentUser;

    private final AuditService auditService;

    public AuthService(AuditService auditService) {
        this.auditService = auditService;

        users.put("admin", "admin123");
        users.put("user", "user123");
    }

    public boolean login(String username, String password) {
        if (users.containsKey(username) && users.get(username).equals(password)) {
            currentUser = username;
            System.out.println("Пользователь " + username + " успешно вошёл в систему.");

            auditService.log(username, "вход в систему");

            return true;
        }
        System.out.println("Неверный логин или пароль.");

        auditService.log(username, "неудачная попытка входа");

        return false;
    }

    public void logout() {
        if (currentUser != null) {
            System.out.println("Пользователь " + currentUser + " вышел из системы.");

            auditService.log(currentUser, "выход из системы");

            currentUser = null;
        } else {
            System.out.println("Нет активной сессии для выхода.");
        }
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public Optional<String> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    public void register(String username, String password) {
        if (users.containsKey(username)) {
            System.out.println("Пользователь уже существует.");
            auditService.log(username, "попытка регистрации уже существующего пользователя");
        } else {
            users.put(username, password);
            System.out.println("Пользователь " + username + " зарегистрирован.");
            auditService.log(username, "регистрация нового пользователя");
        }
    }
}