package com.sistemaventas.database;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class ConexionDBTest {

    @Test
    void obtainsConnectionsFromTheInjectedProvider() throws SQLException {
        Connection expected = (Connection) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, arguments) -> null
        );
        ConexionDB database = new ConexionDB(() -> expected);

        assertSame(expected, database.openConnection());
    }

    @Test
    void preservesProviderFailuresForTheCallerToHandle() {
        SQLException expected = new SQLException("database unavailable");
        ConexionDB database = new ConexionDB(() -> {
            throw expected;
        });

        SQLException actual = assertThrows(SQLException.class, database::openConnection);

        assertSame(expected, actual);
    }
}
