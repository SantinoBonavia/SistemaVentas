package com.sistemaventas.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

public class ConexionDB {

    private static final ConexionDB DEFAULT = new ConexionDB(() -> DriverManager.getConnection(
            System.getenv("DB_URL"),
            System.getenv("DB_USER"),
            System.getenv("DB_PASSWORD")
    ));

    private final ConnectionProvider connectionProvider;

    public ConexionDB(ConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider);
    }

    public static Connection conectar() throws SQLException {
        return DEFAULT.openConnection();
    }

    public Connection openConnection() throws SQLException {
        return connectionProvider.openConnection();
    }

    @FunctionalInterface
    public interface ConnectionProvider {
        Connection openConnection() throws SQLException;
    }
}
