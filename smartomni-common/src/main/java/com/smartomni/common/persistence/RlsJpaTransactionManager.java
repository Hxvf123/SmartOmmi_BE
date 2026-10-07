package com.smartomni.common.persistence;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.orm.jpa.JpaTransactionManager;

/** JpaTransactionManager initializes the provider dialect during bean initialization. */
public class RlsJpaTransactionManager extends JpaTransactionManager {

    public RlsJpaTransactionManager(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        setJpaDialect(new RlsJpaDialect());
    }
}
