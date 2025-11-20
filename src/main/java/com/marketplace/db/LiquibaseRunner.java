package com.marketplace.db;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.resource.ClassLoaderResourceAccessor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Properties;

public class LiquibaseRunner {

    public static void runMigrations(DataSource dataSource, Properties props) {

        String changelog = props.getProperty("liquibase.changelog");

        if (changelog == null) {
            throw new RuntimeException("Missing property: liquibase.changelog");
        }

        try (Connection conn = dataSource.getConnection()) {

            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(
                            new liquibase.database.jvm.JdbcConnection(conn)
                    );


            database.setLiquibaseSchemaName("liquibase");

            Liquibase liquibase = new Liquibase(
                    changelog,
                    new ClassLoaderResourceAccessor(),
                    database
            );

            liquibase.update(new Contexts(), new LabelExpression());

            System.out.println("Liquibase migrations executed successfully.");

        } catch (Exception e) {
            throw new RuntimeException("Failed to run Liquibase migrations", e);
        }
    }
}
