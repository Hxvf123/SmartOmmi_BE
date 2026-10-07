package com.smartomni.common.persistence;

import com.smartomni.common.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.transaction.TransactionDefinition;

import java.sql.SQLException;
import java.util.Locale;

/** Initialize context before the first query on every new JPA transaction. */
public class RlsJpaDialect extends HibernateJpaDialect {

    @Override
    public Object beginTransaction(EntityManager entityManager, TransactionDefinition definition)
            throws SQLException {
        Object transactionData = super.beginTransaction(entityManager, definition);
        try {
            entityManager.unwrap(Session.class).doWork(connection -> {
                Long tenantId = TenantContext.getTenantId();
                String role = TenantContext.getCurrentRole();
                RlsSession.set(connection, "app.tenant_id", tenantId == null ? "" : tenantId.toString());
                RlsSession.set(connection, "app.user_role", role == null ? "" : role.toLowerCase(Locale.ROOT));
                RlsSession.set(connection, "app.auth_email", "");
                RlsSession.set(connection, "app.auth_user_id", "");
                RlsSession.set(connection, "app.reset_token", "");
            });
            return transactionData;
        } catch (RuntimeException exception) {
            entityManager.getTransaction().rollback();
            cleanupTransaction(transactionData);
            throw exception;
        }
    }
}
