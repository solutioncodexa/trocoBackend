package ma.codexa.troco.tenant;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Active le filtre Hibernate {@code fournisseurFilter} dans les services transactionnels,
 * sauf si {@link TenantContext#isBypass()} (SUPER_ADMIN).
 */
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class TenantHibernateFilterAspect {

    private final TenantSupport tenantSupport;

    @Before("execution(* ma.codexa.troco.service..*(..))")
    public void enableTenantFilter() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            return;
        }
        try {
            tenantSupport.applyHibernateFilter();
        } catch (Exception ignored) {
            // Pas de Session Hibernate liée (méthode non-JPA)
        }
    }
}
