package com.marketplace.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.marketplace.db.LiquibaseRunner;
import com.marketplace.db.DataSourceFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Базовый класс для интеграционных тестов с использованием Testcontainers.
 * Запускает контейнер PostgreSQL один раз и выполняет все миграции Liquibase.
 */
public abstract class DatabaseTestBase {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:15");

    // Testcontainers Container instance
    protected static PostgreSQLContainer<?> postgres;

    protected static DataSource testDataSource;
    protected static Properties testProperties;

    // Параметры для подключения
    private static final String DB_USER = "test_user";
    private static final String DB_PASS = "test_pass";
    private static final String DB_NAME = "test_marketdb";

    @BeforeAll
    static void startContainerAndRunMigrations() {
        //Инициализация и запуск контейнера
        postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE)
                .withDatabaseName(DB_NAME)
                .withUsername(DB_USER)
                .withPassword(DB_PASS);

        postgres.start();

        // Инициализация Properties с динамическими данными от контейнера
        testProperties = new Properties();
        testProperties.setProperty("db.url", postgres.getJdbcUrl());
        testProperties.setProperty("db.user", postgres.getUsername());
        testProperties.setProperty("db.password", postgres.getPassword());
        testProperties.setProperty("db.schema", "catalog");

        // Liquibase-настройки (для LiquibaseRunner)
        testProperties.setProperty("liquibase.changelog", "db/changelog/db.changelog-master.xml");
        testProperties.setProperty("liquibase.changelog.schema", "liquibase");

        // Дополнительная настройка пула (если требуется DataSourceFactory)
        testProperties.setProperty("db.pool.max", "5");
        testProperties.setProperty("db.pool.min", "1");
        testProperties.setProperty("db.pool.timeoutMs", "30000");

        // Создаем DataSource (DataSourceFactory метод createFromProperties(Properties props))
        testDataSource = DataSourceFactory.createFromProperties(testProperties);

        //Создаем схему заранее что бы не было ошибок в билде
        try (Connection conn = testDataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE SCHEMA IF NOT EXISTS liquibase");
            stmt.execute("CREATE SCHEMA IF NOT EXISTS catalog");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create necessary schemas", e);
        }

        // Запускаем миграции Liquibase в тестовой базе данных
        LiquibaseRunner.runMigrations(testDataSource, testProperties);
    }

    @AfterAll
    static void stopContainer() {
        // Остановка контейнера и закрытие DataSource
        if (postgres != null) {
            postgres.stop();
        }
        if (testDataSource instanceof AutoCloseable) {
            try {
                ((AutoCloseable) testDataSource).close();
            } catch (Exception e) {
                // Игнорируем ошибку
            }
        }
    }
}