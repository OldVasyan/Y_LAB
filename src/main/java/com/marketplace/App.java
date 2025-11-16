package com.marketplace;

import com.marketplace.repository.DbProductRepository;
import com.marketplace.repository.ProductRepository;
import com.marketplace.service.AuditService;
import com.marketplace.service.AuthService;
import com.marketplace.service.ProductService;
import com.marketplace.db.DataSourceFactory;
import com.marketplace.db.LiquibaseRunner;

import javax.sql.DataSource;
import java.util.Properties;

public class App {

    public static void main(String[] args) {

        // 1. Загружаем application.properties
        Properties props = new Properties();
        try (var in = App.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) props.load(in);
            else throw new RuntimeException("application.properties not found!");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load application.properties", e);
        }

        // 2. Создаём DataSource из настроек
        DataSource dataSource = DataSourceFactory.createFromProperties("application.properties");

        // 3. Запускаем Liquibase миграции
        LiquibaseRunner.runMigrations(dataSource, props);

        // 4. Читаем schema
        String schema = props.getProperty("db.schema", "catalog");

        // 5. Создаём репозиторий
        ProductRepository repository = new DbProductRepository(dataSource, schema);

        // 6. Сервисы
        AuditService auditService = new AuditService();
        AuthService auth = new AuthService(auditService);
        ProductService productService = new ProductService(repository, auditService);

        // 7. Авторизация
        auth.login("admin", "admin123");
        productService.setCurrentUser(auth.getCurrentUser().orElse("unknown"));

        // 8. Запуск консольного меню
        ConsoleMenu menu = new ConsoleMenu(productService, auth);
        menu.start();

        auth.logout();

        //hikari close
        if (dataSource instanceof AutoCloseable) {
            try {
                ((AutoCloseable) dataSource).close();
            } catch (Exception e) {
                throw new RuntimeException("cannot close dataSource properly");
            }
        }
    }
}
