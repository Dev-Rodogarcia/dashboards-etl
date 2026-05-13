package com.dashboard.api.service;

import jakarta.persistence.PersistenceException;
import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionException;

import java.sql.SQLException;

final class DatabaseReadFallbackUtils {

    private DatabaseReadFallbackUtils() {
    }

    static boolean isRecoverableReadFailure(Throwable ex) {
        if (ex == null) {
            return false;
        }
        return ex instanceof DataAccessException
                || ex instanceof TransactionException
                || ex instanceof PersistenceException
                || ex instanceof HibernateException
                || ex instanceof SQLException
                || isRecoverableReadFailure(ex.getCause());
    }

    static void logFallback(Logger logger, String contexto, RuntimeException ex) {
        logger.warn("{}; retornando fallback seguro. causa={}", contexto, ex.getMessage());
        logger.debug("Detalhes: {}", contexto, ex);
    }
}
