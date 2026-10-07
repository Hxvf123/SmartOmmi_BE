package com.smartomni.common.persistence;

import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Transaction-local settings; never put tenant state in pool connection-init SQL. */
public final class RlsSession {

    private RlsSession() {
    }

    public static void set(Connection connection, String name, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select set_config(?, ?, true)")) {
            statement.setString(1, name);
            statement.setString(2, value == null ? "" : value);
            statement.execute();
        }
    }

    public static void set(EntityManager entityManager, String name, String value) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("RLS settings require an active transaction");
        }
        entityManager.unwrap(Session.class).doWork(connection -> set(connection, name, value));
    }
}
