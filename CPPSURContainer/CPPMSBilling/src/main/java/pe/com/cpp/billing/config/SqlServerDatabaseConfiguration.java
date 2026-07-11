package pe.com.cpp.billing.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SqlServerDatabaseConfiguration {

    @Bean
    DataSource dataSource(DataSourceProperties properties,
            @Value("${billing.database.name:cpp_billing}") String databaseName,
            @Value("${billing.database.auto-create:true}") boolean autoCreate) throws SQLException {
        validateDatabaseName(databaseName);
        if (autoCreate) {
            createDatabaseIfMissing(properties, databaseName);
        }
        return properties.initializeDataSourceBuilder().build();
    }

    private void createDatabaseIfMissing(DataSourceProperties properties, String databaseName) throws SQLException {
        String targetUrl = properties.determineUrl();
        String masterUrl = targetUrl.matches("(?i).*;databaseName=[^;]+.*")
                ? targetUrl.replaceFirst("(?i)(;databaseName=)[^;]+", "$1master")
                : targetUrl + ";databaseName=master";
        try (Connection connection = DriverManager.getConnection(
                masterUrl, properties.determineUsername(), properties.determinePassword());
                Statement statement = connection.createStatement()) {
            statement.execute("IF DB_ID(N'" + databaseName + "') IS NULL CREATE DATABASE [" + databaseName + "]");
        }
    }

    private void validateDatabaseName(String databaseName) {
        if (!databaseName.matches("[A-Za-z][A-Za-z0-9_]{0,62}")) {
            throw new IllegalArgumentException("Nombre de base de datos SQL Server no válido");
        }
    }
}
