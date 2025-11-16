package com.marketplace.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class DataSourceFactory {

    private DataSourceFactory() {}

    /**
     * Создает DataSource, загружая настройки из файла на classpath.
     */
    public static DataSource createFromProperties(String propertiesPath) {
        Properties props = new Properties();
        try (InputStream in = DataSourceFactory.class.getClassLoader().getResourceAsStream(propertiesPath)) {
            if (in == null) {
                throw new IllegalStateException("Cannot find properties on classpath: " + propertiesPath);
            }
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load properties: " + propertiesPath, e);
        }

        // Вызываем перегруженый метод
        return createFromProperties(props);
    }

    /**
     * Перегруженный метод Создает DataSource, используя уже загруженные Properties.
     * Подходит для Testcontainers, который динамически генерирует URL, User, Pass.
     */
    public static DataSource createFromProperties(Properties props) {
        String url = props.getProperty("db.url");
        String user = props.getProperty("db.user");
        String pass = props.getProperty("db.password");

        if (url == null || user == null || pass == null) {
            throw new IllegalArgumentException("Missing required DB connection properties (url, user, password)");
        }

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(pass);

        // конфигурация пула
        // Если Testcontainers не передает эти настройки, будут использоваться значения по умолчанию
        cfg.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.max", "5")));
        cfg.setMinimumIdle(Integer.parseInt(props.getProperty("db.pool.min", "1")));
        cfg.setConnectionTimeout(Long.parseLong(props.getProperty("db.pool.timeoutMs", "30000")));

        // В тестах лучше использовать другое имя пула или динамическое (например, "test_pool")
        cfg.setPoolName(props.getProperty("db.pool.name", "y_lab_pool"));

        return new HikariDataSource(cfg);
    }
}