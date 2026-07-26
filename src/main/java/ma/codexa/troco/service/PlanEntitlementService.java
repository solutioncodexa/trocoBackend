package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.exception.BusinessException;
import ma.codexa.troco.entity.Fournisseur;
import ma.codexa.troco.entity.Plan;
import ma.codexa.troco.plan.PlanFeatures;
import ma.codexa.troco.repository.FournisseurRepository;
import ma.codexa.troco.repository.OrderRepository;
import ma.codexa.troco.repository.ProductRepository;
import ma.codexa.troco.repository.UserRepository;
import ma.codexa.troco.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlanEntitlementService {

    private final FournisseurRepository fournisseurRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public Plan currentPlan() {
        Long fid = TenantContext.getFournisseurId();
        if (fid == null) {
            throw new BusinessException("Boutique non résolue", HttpStatus.BAD_REQUEST);
        }
        Fournisseur f = fournisseurRepository.findById(fid)
                .orElseThrow(() -> new BusinessException("Fournisseur introuvable", HttpStatus.NOT_FOUND));
        Plan plan = f.getPlan();
        if (plan == null) {
            throw new BusinessException("Aucun plan associé à cette boutique", HttpStatus.PAYMENT_REQUIRED, "PLAN_REQUIRED");
        }
        return plan;
    }

    @Transactional(readOnly = true)
    public PlanFeatures features() {
        Plan p = currentPlan();
        return PlanFeatures.parse(p.getCode(), p.getFeaturesJson());
    }

    @Transactional(readOnly = true)
    public void assertCanCreateProduct() {
        Plan p = currentPlan();
        if (p.getMaxProducts() == null) return;
        long count = productRepository.countActive();
        if (count >= p.getMaxProducts()) {
            throw new BusinessException(
                    "Limite atteinte : votre plan " + p.getName() + " autorise " + p.getMaxProducts()
                            + " produits. Passez au plan supérieur.",
                    HttpStatus.PAYMENT_REQUIRED, "PLAN_PRODUCT_LIMIT");
        }
    }

    @Transactional(readOnly = true)
    public void assertCanCreateStaff() {
        Plan p = currentPlan();
        if (p.getMaxStaff() == null) return;
        Long fid = TenantContext.requireFournisseurId();
        long staff = userRepository.countStoreMembers(fid);
        if (staff >= p.getMaxStaff()) {
            throw new BusinessException(
                    "Limite atteinte : votre plan " + p.getName() + " autorise " + p.getMaxStaff()
                            + " compte(s) (propriétaire inclus). Passez au plan supérieur.",
                    HttpStatus.PAYMENT_REQUIRED, "PLAN_STAFF_LIMIT");
        }
    }

    @Transactional(readOnly = true)
    public void assertCanCreateOrder() {
        Plan p = currentPlan();
        if (p.getMaxOrdersPerMonth() == null) return;
        LocalDateTime from = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime to = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()).atTime(LocalTime.MAX);
        long count = orderRepository.countByCreatedBetween(from, to);
        if (count >= p.getMaxOrdersPerMonth()) {
            throw new BusinessException(
                    "Limite mensuelle atteinte : " + p.getMaxOrdersPerMonth()
                            + " commandes/mois sur le plan " + p.getName() + ".",
                    HttpStatus.PAYMENT_REQUIRED, "PLAN_ORDER_LIMIT");
        }
    }

    @Transactional(readOnly = true)
    public void assertCustomDomainAllowed() {
        Plan p = currentPlan();
        if (!p.isCustomDomain()) {
            throw new BusinessException(
                    "Le domaine personnalisé est disponible à partir du plan Pro.",
                    HttpStatus.PAYMENT_REQUIRED, "PLAN_CUSTOM_DOMAIN");
        }
    }

    @Transactional(readOnly = true)
    public void assertThemeAllowed(String themeKey) {
        PlanFeatures f = features();
        if (!f.themeAllowed(themeKey)) {
            throw new BusinessException(
                    "Ce thème n'est pas inclus dans votre plan (Basic : Classique + Minimal).",
                    HttpStatus.PAYMENT_REQUIRED, "PLAN_THEME");
        }
    }

    @Transactional(readOnly = true)
    public void assertAbandonedCartAllowed() {
        if (!features().abandonedCart()) {
            throw new BusinessException(
                    "Le panier abandonné est disponible à partir du plan Pro.",
                    HttpStatus.PAYMENT_REQUIRED, "PLAN_ABANDONED_CART");
        }
    }

    @Transactional(readOnly = true)
    public void assertWhatsappBusinessAllowed() {
        if (!features().whatsappBusiness()) {
            throw new BusinessException(
                    "WhatsApp Business est disponible à partir du plan Pro.",
                    HttpStatus.PAYMENT_REQUIRED, "PLAN_WHATSAPP");
        }
    }

    @Transactional(readOnly = true)
    public void assertWebhooksAllowed() {
        if (!features().webhooksAllowed()) {
            throw new BusinessException(
                    "Les webhooks sont disponibles à partir du plan Pro.",
                    HttpStatus.PAYMENT_REQUIRED, "PLAN_WEBHOOKS");
        }
    }

    @Transactional(readOnly = true)
    public void assertWebhookEventAllowed(String event) {
        assertWebhooksAllowed();
        if (!features().webhookEventAllowed(event)) {
            throw new BusinessException(
                    "Cet événement webhook nécessite le plan Business.",
                    HttpStatus.PAYMENT_REQUIRED, "PLAN_WEBHOOK_EVENT");
        }
    }

    @Transactional(readOnly = true)
    public void assertAbTestingAllowed() {
        if (!features().abTesting()) {
            throw new BusinessException(
                    "Les tests A/B sont disponibles à partir du plan Pro.",
                    HttpStatus.PAYMENT_REQUIRED, "PLAN_AB_TESTING");
        }
    }

    @Transactional(readOnly = true)
    public void assertApiKeysAllowed() {
        if (!features().apiHeadless()) {
            throw new BusinessException(
                    "L'API headless est disponible à partir du plan Pro.",
                    HttpStatus.PAYMENT_REQUIRED, "PLAN_API_KEYS");
        }
    }

    @Transactional(readOnly = true)
    public void assertLoyaltyAllowed() {
        if (!features().loyalty()) {
            throw new BusinessException(
                    "Le programme fidélité est disponible à partir du plan Pro.",
                    HttpStatus.PAYMENT_REQUIRED, "PLAN_LOYALTY");
        }
    }

    @Transactional(readOnly = true)
    public void assertPixelCountAllowed(int configuredPixelCount) {
        Plan p = currentPlan();
        if (p.getMaxPixels() == null) return;
        if (configuredPixelCount > p.getMaxPixels()) {
            throw new BusinessException(
                    "Votre plan " + p.getName() + " autorise " + p.getMaxPixels() + " pixel(s) marketing.",
                    HttpStatus.PAYMENT_REQUIRED, "PLAN_PIXEL_LIMIT");
        }
    }

    public static Map<String, Object> featuresMap(Plan p) {
        PlanFeatures f = PlanFeatures.parse(p.getCode(), p.getFeaturesJson());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("themes", f.themes());
        m.put("pageBuilder", f.pageBuilder());
        m.put("abTesting", f.abTesting());
        m.put("abandonedCart", f.abandonedCart());
        m.put("abandonedCartAdvanced", f.abandonedCartAdvanced());
        m.put("whatsappBusiness", f.whatsappBusiness());
        m.put("whatsappMultiTemplates", f.whatsappMultiTemplates());
        m.put("webhooks", f.webhooks());
        m.put("blogSeo", f.blogSeo());
        m.put("support", f.support());
        m.put("apiHeadless", f.apiHeadless());
        m.put("loyalty", f.loyalty());
        m.put("multiCurrency", f.multiCurrency());
        return m;
    }
}
