package ma.codexa.troco.rental;

import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.exception.BusinessException;
import ma.codexa.troco.common.exception.ResourceNotFoundException;
import ma.codexa.troco.entity.OrderItem;
import ma.codexa.troco.entity.Product;
import ma.codexa.troco.entity.ProductVariant;
import ma.codexa.troco.dto.CartItemDTO;
import ma.codexa.troco.repository.OrderItemRepository;
import ma.codexa.troco.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Location : le stock d'un produit louable n'est jamais décrémenté. La disponibilité d'un jour est
 * « stock − quantités réservées par les commandes actives qui couvrent ce jour » ; une commande annulée ou
 * retournée libère ses dates, et une période terminée libère le stock d'elle-même le lendemain.
 */
@Service
@RequiredArgsConstructor
public class RentalService {

    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    /** Calendrier public : quantité disponible par jour sur [from, to]. */
    @Transactional(readOnly = true)
    public Map<String, Object> availability(Long productId, Long variantId, LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new BusinessException("Période invalide", HttpStatus.BAD_REQUEST);
        }
        if (RentalRules.days(from, to) > RentalRules.MAX_AVAILABILITY_DAYS) {
            throw new BusinessException("Période trop longue (" + RentalRules.MAX_AVAILABILITY_DAYS + " jours maximum)", HttpStatus.BAD_REQUEST);
        }
        Product product = productRepository.findByIdWithVariants(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", productId));
        if (!product.isRentalEnabled()) {
            throw new BusinessException("Ce produit n'est pas disponible à la location", HttpStatus.BAD_REQUEST);
        }
        Long pool = poolVariantId(product, variantId);
        int capacity = capacity(product, pool);
        List<OrderItem> overlaps = orderItemRepository.findActiveRentalOverlaps(productId, from, to);

        List<Map<String, Object>> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            days.add(Map.of("date", d.toString(), "available", Math.max(0, capacity - reserved(overlaps, pool, d))));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("unit", RentalRules.normalizeUnit(product.getRentalUnit()));
        out.put("deposit", product.getRentalDeposit());
        out.put("minUnits", product.getRentalMinUnits());
        out.put("maxUnits", product.getRentalMaxUnits());
        out.put("capacity", capacity);
        out.put("days", days);
        return out;
    }

    /**
     * Valide et applique la location à une ligne de commande : période, durée, disponibilité sur chaque jour,
     * puis prix recalculé côté serveur (tarif × unités × quantité). Verrouille le produit pour sérialiser
     * les réservations concurrentes. À appeler dans la transaction de création de commande.
     */
    public void applyToOrderItem(OrderItem item, Product product, CartItemDTO line) {
        if (!product.isRentalEnabled()) {
            if (line.getRentalStart() != null || line.getRentalEnd() != null) {
                throw new BusinessException("« " + product.getName() + " » n'est pas disponible à la location", HttpStatus.BAD_REQUEST);
            }
            return;
        }
        LocalDate start = line.getRentalStart();
        LocalDate end = line.getRentalEnd();
        if (start == null || end == null) {
            throw new BusinessException("Choisissez les dates de location pour « " + product.getName() + " »", HttpStatus.BAD_REQUEST);
        }
        if (end.isBefore(start)) {
            throw new BusinessException("La date de fin précède la date de début", HttpStatus.BAD_REQUEST);
        }
        if (start.isBefore(LocalDate.now())) {
            throw new BusinessException("La date de début est dépassée", HttpStatus.BAD_REQUEST);
        }
        if (RentalRules.days(start, end) > RentalRules.MAX_BOOKING_DAYS) {
            throw new BusinessException("Durée de location trop longue (" + RentalRules.MAX_BOOKING_DAYS + " jours maximum)", HttpStatus.BAD_REQUEST);
        }
        String unit = RentalRules.normalizeUnit(product.getRentalUnit());
        int units = RentalRules.units(unit, start, end);
        int min = product.getRentalMinUnits() != null ? product.getRentalMinUnits() : 1;
        if (units < min) {
            throw new BusinessException("Durée minimale : " + min + (RentalRules.WEEK.equals(unit) ? " semaine(s)" : " jour(s)"), HttpStatus.BAD_REQUEST);
        }
        Integer max = product.getRentalMaxUnits();
        if (max != null && units > max) {
            throw new BusinessException("Durée maximale : " + max + (RentalRules.WEEK.equals(unit) ? " semaine(s)" : " jour(s)"), HttpStatus.BAD_REQUEST);
        }
        int qty = item.getQuantity() != null ? item.getQuantity() : 1;

        // Verrou sur le produit : deux commandes simultanées sur le dernier exemplaire sont traitées l'une après l'autre.
        Product locked = productRepository.lockById(product.getId()).orElse(product);
        Long pool = poolVariantId(locked, item.getSelectedVariantId());
        int capacity = capacity(locked, pool);
        List<OrderItem> overlaps = orderItemRepository.findActiveRentalOverlaps(locked.getId(), start, end);
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            int free = capacity - reserved(overlaps, pool, d);
            if (free < qty) {
                throw new BusinessException(
                        "« " + product.getName() + " » : " + (free <= 0 ? "indisponible" : "seulement " + free + " disponible(s)")
                                + " le " + d + ". Choisissez d'autres dates.",
                        HttpStatus.CONFLICT);
            }
        }

        double rate = unitRate(locked, item.getSelectedVariantId());
        item.setRentalStart(start);
        item.setRentalEnd(end);
        item.setRentalUnits(units);
        item.setRentalDeposit(locked.getRentalDeposit());
        item.setUnitPrice(rate);
        item.setSubtotal(rate * units * qty);
    }

    // ── internes ──────────────────────────────────────────────────────────

    /** Variante qui porte le stock : celle choisie, sinon la variante par défaut (ou la première), sinon aucune. */
    private static Long poolVariantId(Product product, Long requested) {
        List<ProductVariant> variants = product.getVariants();
        if (variants == null || variants.isEmpty()) return null;
        if (requested != null && variants.stream().anyMatch(v -> requested.equals(v.getId()))) return requested;
        return variants.stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsDefault()))
                .findFirst().orElse(variants.get(0)).getId();
    }

    private static int capacity(Product product, Long poolVariantId) {
        if (poolVariantId != null && product.getVariants() != null) {
            return product.getVariants().stream()
                    .filter(v -> poolVariantId.equals(v.getId()))
                    .mapToInt(v -> v.getStock() != null ? v.getStock() : 0)
                    .findFirst().orElse(0);
        }
        return product.getStock() != null ? product.getStock() : 0;
    }

    private static double unitRate(Product product, Long requestedVariantId) {
        Long pool = poolVariantId(product, requestedVariantId);
        if (pool != null && product.getVariants() != null) {
            for (ProductVariant v : product.getVariants()) {
                if (pool.equals(v.getId()) && v.getPrice() != null) return v.getPrice();
            }
        }
        return product.getPrice() != null ? product.getPrice() : 0.0;
    }

    /** Quantité réservée ce jour-là pour le même exemplaire (même variante / même stock). */
    private int reserved(List<OrderItem> overlaps, Long poolVariantId, LocalDate day) {
        int sum = 0;
        for (OrderItem o : overlaps) {
            if (o.getRentalStart() == null || o.getRentalEnd() == null) continue;
            if (day.isBefore(o.getRentalStart()) || day.isAfter(o.getRentalEnd())) continue;
            Long itemPool = o.getSelectedVariantId();
            if (poolVariantId != null && itemPool != null && !Objects.equals(poolVariantId, itemPool)) continue;
            sum += o.getQuantity() != null ? o.getQuantity() : 0;
        }
        return sum;
    }
}
