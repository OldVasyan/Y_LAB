package com.marketplace.test;

import com.marketplace.model.Product;
import com.marketplace.repository.DbProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционный тест для DbProductRepository, использующий Testcontainers.
 */
public class DbProductRepositoryIntegrationTest extends DatabaseTestBase {

    private DbProductRepository repository;
    private String schema;

    @BeforeEach
    void setUp() throws SQLException {
        this.schema = testProperties.getProperty("db.schema");
        this.repository = new DbProductRepository(testDataSource, schema);

        // ОЧИСТКА: Удаляем все данные и сбрасываем Sequence
        try (Connection connection = testDataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // Очистка таблицы и сброс внутреннего счетчика
            statement.execute("TRUNCATE " + schema + ".product RESTART IDENTITY");

            // Явный сброс самого Sequence в 1
            statement.execute("ALTER SEQUENCE " + schema + ".product_id_seq RESTART WITH 1");
        }
    }

    // --- TEST 1: Проверка сохранения (INSERT) и выдачи ID через Sequence ---
    @Test
    void testSaveGeneratesIdAndRetrievesCorrectly() throws SQLException {
        Product newProduct = new Product(
                null,
                "Laptop Test",
                "Computers",
                "Dell",
                1500.00,
                Instant.now(),
                Instant.now()
        );

        repository.saveOrUpdate(newProduct);

        //Проверяем, что ID был сгенерирован и присвоен объекту
        Long generatedId = newProduct.getId();
        assertNotNull(generatedId, "ID должен быть сгенерирован Sequence и присвоен объекту");

        // Первый ID после TRUNCATE RESTART IDENTITY должен быть 1.
        assertEquals(1L, generatedId, "Первый ID должен быть 1");

        // Проверяем, что товар можно получить из БД по сгенерированному ID
        Product foundProduct = repository.findById(generatedId); // Ожидаем Product, не Optional
        assertNotNull(foundProduct, "Товар должен быть найден по ID");
        assertEquals("Laptop Test", foundProduct.getName());
    }

    // --- TEST 2: Проверка обновления (UPDATE) через saveOrUpdate ---
    @Test
    void testUpdateProduct() {
        // Сохраняем товар (INSERT, ID=1)
        Product initialProduct = new Product(
                null, "Old Name", "Cat", "Brand", 100.00, Instant.now(), Instant.now());
        repository.saveOrUpdate(initialProduct);

        Long productId = initialProduct.getId(); // Получаем сгенерированный ID=1

        // Готовим объект для обновления (UPDATE)
        String updatedName = "New Updated Name";

        Product productToUpdate = new Product(
                productId,
                updatedName,
                "Updated Cat",
                "Updated Brand",
                200.00,
                initialProduct.getCreatedAt(),
                Instant.now()
        );

        // Act: Вызываем saveOrUpdate, который должен выполнить UPDATE
        repository.saveOrUpdate(productToUpdate);

        // Assert
        // Проверяем, что изменения сохранены в БД
        Product foundProduct = repository.findById(productId);
        assertNotNull(foundProduct, "Обновленный товар должен быть найден");
        assertEquals(updatedName, foundProduct.getName(), "Имя должно быть обновлено");
        assertEquals(200.00, foundProduct.getPrice(), 0.001, "Цена должна быть обновлена");
    }

    // Проверка удаления ---
    @Test
    void testDeleteProduct() {
        // Arrange: Сохраняем товар (ID=1)
        Product productToDelete = new Product(
                null, "To Delete", "Junk", "ABC", 1.00, Instant.now(), Instant.now());
        repository.saveOrUpdate(productToDelete);

        Long productId = productToDelete.getId(); // ID должен быть 1

        // Act
        boolean deleted = repository.deleteById(productId); // Вызываем deleteById

        // Assert
        assertTrue(deleted, "Метод deleteById должен вернуть true");

        Product foundProduct = repository.findById(productId);
        assertNull(foundProduct, "Товар должен быть удален из БД, findById должен вернуть null");
    }
}