package ma.codexa.troco.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

@Component
public class TenantSupport {

    @PersistenceContext
    private EntityManager entityManager;

    /** Active / désactive le filtre Hibernate selon {@link TenantContext}. */
    public void applyHibernateFilter() {
        Session session = entityManager.unwrap(Session.class);
        if (TenantContext.isBypass()) {
            if (session.getEnabledFilter(TenantScoped.FILTER_NAME) != null) {
                session.disableFilter(TenantScoped.FILTER_NAME);
            }
            return;
        }
        Long fournisseurId = TenantContext.getFournisseurId();
        // Sans tenant résolu : filtre impossible → aucune fuite cross-boutique.
        long effectiveId = fournisseurId != null ? fournisseurId : -1L;
        session.enableFilter(TenantScoped.FILTER_NAME)
                .setParameter(TenantScoped.PARAM_NAME, effectiveId);
    }

    public Long currentFournisseurIdOrNull() {
        return TenantContext.getFournisseurId();
    }

    public Long requireFournisseurId() {
        return TenantContext.requireFournisseurId();
    }
}
