package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import ma.codexa.troco.entity.AbandonedCart;
import ma.codexa.troco.entity.Customer;
import ma.codexa.troco.entity.Order;
import ma.codexa.troco.entity.StoreLead;
import ma.codexa.troco.repository.AbandonedCartRepository;
import ma.codexa.troco.repository.CustomerRepository;
import ma.codexa.troco.repository.LoyaltyAccountRepository;
import ma.codexa.troco.repository.OrderRepository;
import ma.codexa.troco.repository.ProductReviewRepository;
import ma.codexa.troco.repository.StoreLeadRepository;
import ma.codexa.troco.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PrivacyService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final StoreLeadRepository storeLeadRepository;
    private final AbandonedCartRepository abandonedCartRepository;
    private final ProductReviewRepository productReviewRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Map<String, Object> exportSubject(String email, String phone) {
        Long fid = TenantContext.requireFournisseurId();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("fournisseurId", fid);
        out.put("email", email);
        out.put("phone", phone);

        List<Map<String, Object>> customers = new ArrayList<>();
        if (email != null && !email.isBlank()) {
            customerRepository.findByEmail(email.trim()).ifPresent(c -> customers.add(customerMap(c)));
        }
        if (phone != null && !phone.isBlank()) {
            customerRepository.findByPhone(phone.trim()).ifPresent(c -> {
                if (customers.stream().noneMatch(m -> m.get("id").equals(c.getId()))) {
                    customers.add(customerMap(c));
                }
            });
        }
        out.put("customers", customers);

        List<Map<String, Object>> orders = new ArrayList<>();
        for (Order o : orderRepository.findAll()) {
            Customer c = o.getCustomer();
            if (c == null) continue;
            if (matches(c.getEmail(), email) || matches(c.getPhone(), phone)) {
                Map<String, Object> om = new LinkedHashMap<>();
                om.put("orderNumber", o.getOrderNumber());
                om.put("totalAmount", o.getTotalAmount());
                om.put("status", o.getStatus());
                om.put("createdAt", o.getCreatedAt());
                orders.add(om);
            }
        }
        out.put("orders", orders);

        List<Map<String, Object>> leads = new ArrayList<>();
        for (StoreLead l : storeLeadRepository.findAllByOrderByCreatedAtDesc()) {
            if (matches(l.getEmail(), email) || matches(l.getPhone(), phone)) {
                Map<String, Object> lm = new LinkedHashMap<>();
                lm.put("id", l.getId());
                lm.put("fullName", l.getFullName());
                lm.put("email", l.getEmail());
                lm.put("phone", l.getPhone());
                lm.put("createdAt", l.getCreatedAt());
                leads.add(lm);
            }
        }
        out.put("leads", leads);

        List<Map<String, Object>> carts = new ArrayList<>();
        for (AbandonedCart c : abandonedCartRepository.findAllByOrderByUpdatedAtDesc()) {
            if (matches(c.getCustomerEmail(), email) || matches(c.getCustomerPhone(), phone)) {
                Map<String, Object> cm = new LinkedHashMap<>();
                cm.put("id", c.getId());
                cm.put("customerEmail", c.getCustomerEmail());
                cm.put("customerPhone", c.getCustomerPhone());
                cm.put("cartTotal", c.getCartTotal());
                cm.put("createdAt", c.getCreatedAt());
                carts.add(cm);
            }
        }
        out.put("abandonedCarts", carts);

        auditLogService.log(AuditLogService.Action.PRIVACY_EXPORT, AuditLogService.Outcome.SUCCESS,
                email != null ? email : phone, "CNDP subject export");
        return out;
    }

    @Transactional
    public Map<String, Object> eraseSubject(String email, String phone) {
        int anonymized = 0;
        for (Customer c : customerRepository.findAll()) {
            if (matches(c.getEmail(), email) || matches(c.getPhone(), phone)) {
                c.setFullName("Anonymisé");
                c.setEmail(null);
                c.setPhone("anon-" + c.getId());
                c.setAddress("—");
                customerRepository.save(c);
                anonymized++;
            }
        }
        for (StoreLead l : storeLeadRepository.findAll()) {
            if (matches(l.getEmail(), email) || matches(l.getPhone(), phone)) {
                l.setFullName("Anonymisé");
                l.setEmail(null);
                l.setPhone(null);
                l.setMessage(null);
                storeLeadRepository.save(l);
                anonymized++;
            }
        }
        for (AbandonedCart c : abandonedCartRepository.findAll()) {
            if (matches(c.getCustomerEmail(), email) || matches(c.getCustomerPhone(), phone)) {
                c.setCustomerEmail(null);
                c.setCustomerPhone(null);
                c.setCustomerName("Anonymisé");
                c.setCartJson("[]");
                abandonedCartRepository.save(c);
                anonymized++;
            }
        }
        productReviewRepository.findAll().forEach(r -> {
            if (matches(r.getAuthorEmail(), email)) {
                r.setAuthorName("Anonymisé");
                r.setAuthorEmail(null);
                productReviewRepository.save(r);
            }
        });
        if (phone != null && !phone.isBlank()) {
            Long fid = TenantContext.requireFournisseurId();
            loyaltyAccountRepository.findByFournisseurIdAndPhone(fid, phone.replaceAll("[^0-9+]", ""))
                    .ifPresent(loyaltyAccountRepository::delete);
        }

        auditLogService.log(AuditLogService.Action.PRIVACY_ERASE, AuditLogService.Outcome.SUCCESS,
                email != null ? email : phone, "CNDP subject erase count=" + anonymized);
        return Map.of("anonymizedRecords", anonymized);
    }

    private boolean matches(String value, String needle) {
        if (needle == null || needle.isBlank() || value == null) return false;
        return value.trim().equalsIgnoreCase(needle.trim())
                || value.replaceAll("[^0-9+]", "").equalsIgnoreCase(needle.replaceAll("[^0-9+]", "").toLowerCase(Locale.ROOT));
    }

    private Map<String, Object> customerMap(Customer c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("fullName", c.getFullName());
        m.put("email", c.getEmail());
        m.put("phone", c.getPhone());
        m.put("city", c.getCity());
        return m;
    }
}
