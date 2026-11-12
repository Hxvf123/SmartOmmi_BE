package com.smartomni.common.tenant;

import com.smartomni.common.persistence.RlsSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

/**
 * Tra cứu thông tin Tenant (id, subdomain, status, subscription_plan) trực tiếp từ DB,
 * dùng transaction-local RLS context (app.user_role = super_admin) để đọc bảng tenants an toàn.
 */
@Slf4j
@Component
public class TenantPlanResolver {

    private static final String BASE_SELECT =
            "SELECT t.id, t.subdomain, t.status, sp.name AS plan_name " +
            "FROM tenants t " +
            "LEFT JOIN subscription_plans sp ON sp.id = t.plan_id ";

    private static final String QUERY_BY_SUBDOMAIN = BASE_SELECT + "WHERE LOWER(t.subdomain) = LOWER(?)";
    private static final String QUERY_BY_ID = BASE_SELECT + "WHERE t.id = ?";

    private final DataSource dataSource;

    @Autowired
    public TenantPlanResolver(ObjectProvider<DataSource> dataSourceProvider) {
        this.dataSource = dataSourceProvider != null ? dataSourceProvider.getIfAvailable() : null;
    }

    public TenantPlanResolver(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<TenantLookupResult> findTenantBySubdomain(String subdomain) {
        if (dataSource == null || subdomain == null || subdomain.isBlank()) {
            return Optional.empty();
        }
        return querySingle(QUERY_BY_SUBDOMAIN, statement -> statement.setString(1, subdomain.trim()));
    }

    public Optional<TenantLookupResult> findTenantById(Long tenantId) {
        if (dataSource == null || tenantId == null) {
            return Optional.empty();
        }
        return querySingle(QUERY_BY_ID, statement -> statement.setLong(1, tenantId));
    }

    /**
     * Lấy gói cước hiện tại từ TenantContext, fallback sang tra cứu DB theo tenantId nếu chưa nạp.
     */
    public String resolveSubscriptionPlan(Long tenantId) {
        String contextPlan = TenantContext.getSubscriptionPlan();
        if (contextPlan != null && !contextPlan.isBlank()) {
            return normalizePlanName(contextPlan);
        }
        if (tenantId != null) {
            Optional<TenantLookupResult> found = findTenantById(tenantId);
            if (found.isPresent()) {
                return found.get().subscriptionPlan();
            }
        }
        return "FREE";
    }

    public static String normalizePlanName(String rawPlan) {
        if (rawPlan == null || rawPlan.isBlank()) {
            return "FREE";
        }
        return rawPlan.trim().toUpperCase(Locale.ROOT);
    }

    private Optional<TenantLookupResult> querySingle(String sql, StatementBinder binder) {
        try (Connection connection = dataSource.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                RlsSession.set(connection, "app.user_role", "super_admin");
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    binder.bind(statement);
                    try (ResultSet rs = statement.executeQuery()) {
                        Optional<TenantLookupResult> result = Optional.empty();
                        if (rs.next()) {
                            long id = rs.getLong("id");
                            String subdomain = rs.getString("subdomain");
                            String status = rs.getString("status");
                            String planName = normalizePlanName(rs.getString("plan_name"));
                            result = Optional.of(new TenantLookupResult(id, subdomain, status, planName));
                        }
                        connection.commit();
                        return result;
                    }
                }
            } catch (SQLException ex) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    ex.addSuppressed(rollbackEx);
                }
                log.debug("Could not resolve tenant metadata from database: {}", ex.getMessage());
                return Optional.empty();
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException ex) {
            log.debug("Database connection unavailable during tenant lookup: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    public record TenantLookupResult(Long id, String subdomain, String status, String subscriptionPlan) {
        public boolean isLocked() {
            return "locked".equalsIgnoreCase(status);
        }
    }
}

