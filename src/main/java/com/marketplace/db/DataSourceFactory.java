package com.marketplace.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class DataSourceFactory {

    private DataSourceFactory() {}

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

        String url = props.getProperty("db.url");
        String user = props.getProperty("db.user");
        String pass = props.getProperty("db.password");

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(pass);

        // конфигурация
        cfg.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.max", "5")));
        cfg.setMinimumIdle(Integer.parseInt(props.getProperty("db.pool.min", "1")));
        cfg.setConnectionTimeout(Long.parseLong(props.getProperty("db.pool.timeoutMs", "30000")));
        cfg.setPoolName("y_lab_pool");

        return new HikariDataSource(cfg);
    }
}
