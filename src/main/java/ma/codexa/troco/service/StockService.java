package ma.codexa.troco.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.codexa.troco.common.PageResponse;
import ma.codexa.troco.common.exception.BusinessException;
import ma.codexa.troco.common.exception.ResourceNotFoundException;
import ma.codexa.troco.dto.*;
import ma.codexa.troco.dto.request.BulkSafetyStockRequest;
import ma.codexa.troco.dto.request.StockAdjustRequest;
import ma.codexa.troco.dto.request.StockVariantPatchRequest;
import ma.codexa.troco.entity.*;
import ma.codexa.troco.repository.ProductRepository;
import ma.codexa.troco.repository.ProductVariantRepository;
import ma.codexa.troco.repository.StockMovementRepository;
import ma.codexa.troco.repository.StockSettingsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final StockSettingsRepository stockSettingsRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLog;

    /**
     * Lecture seule : settings existants ou defaults en mémoire (pas d'INSERT).
     * Évite « cannot execute INSERT in a read-only transaction » depuis /stats/dashboard.
     */
    @Transactional(readOnly = true)
    public StockSettings resolveSettings() {
        Long fid = ma.codexa.troco.tenant.TenantContext.getFournisseurId();
        if (fid != null) {
            return stockSettingsRepository.findFirstByFournisseurId(fid).orElseGet(() -> {
                StockSettings s = newStockDefaults();
                s.setFournisseurId(fid);
                return s;
            });
        }
        return stockSettingsRepository.findAll().stream().findFirst().orElseGet(StockService::newStockDefaults);
    }

    /** Persiste les settings s'ils n'existent pas (chemins en écriture uniquement). */
    @Transactional
    public StockSettings getOrCreateSettings() {
        Long fid = ma.codexa.troco.tenant.TenantContext.getFournisseurId();
        if (fid != null) {
            return stockSettingsRepository.findFirstByFournisseurId(fid).orElseGet(() -> {
                StockSettings s = newStockDefaults();
                s.setFournisseurId(fid);
                return stockSettingsRepository.save(s);
            });
        }
        return stockSettingsRepository.findAll().stream().findFirst().orElseGet(() ->
                stockSettingsRepository.save(newStockDefaults()));
    }

    private static StockSettings newStockDefaults() {
        StockSettings s = new StockSettings();
        s.setDefaultSafetyStock(10);
        s.setExpiryAlertDays(30);
        s.setAlertsEnabled(true);
        s.setLowStockAlertsEnabled(true);
        s.setExpiryAlertsEnabled(true);
        return s;
    }

    @Transactional(readOnly = true)
    public StockSettingsDTO getSettings() {
        return toSettingsDto(resolveSettings());
    }

    @Transactional
    public StockSettingsDTO updateSettings(StockSettingsDTO dto) {
        StockSettings s = getOrCreateSettings();
        if (dto.getDefaultSafetyStock() != null) {
            if (dto.getDefaultSafetyStock() < 0) {
                throw new BusinessException("Le seuil d'alerte doit être ≥ 0", HttpStatus.BAD_REQUEST);
            }
            s.setDefaultSafetyStock(dto.getDefaultSafetyStock());
        }
        if (dto.getExpiryAlertDays() != null) {
            if (dto.getExpiryAlertDays() < 1) {
                throw new BusinessException("Les jours d'alerte péremption doivent être ≥ 1", HttpStatus.BAD_REQUEST);
            }
            s.setExpiryAlertDays(dto.getExpiryAlertDays());
        }
        if (dto.getAlertsEnabled() != null) {
            s.setAlertsEnabled(dto.getAlertsEnabled());
        }
        if (dto.getLowStockAlertsEnabled() != null) {
            s.setLowStockAlertsEnabled(dto.getLowStockAlertsEnabled());
        }
        if (dto.getExpiryAlertsEnabled() != null) {
            s.setExpiryAlertsEnabled(dto.getExpiryAlertsEnabled());
        }
        return toSettingsDto(stockSettingsRepository.save(s));
    }

    public int effectiveSafetyStock(ProductVariant variant, StockSettings settings) {
        if (variant.getSafetyStock() != null) {
            return variant.getSafetyStock();
        }
        return settings.getDefaultSafetyStock() != null ? settings.getDefaultSafetyStock() : 10;
    }

    @Transactional(readOnly = true)
    public StockOverviewDTO getOverview() {
        StockSettings settings = resolveSettings();
        List<StockVariantRowDTO> rows = listVariantRows(null);
        long low = rows.stream().filter(r -> "LOW".equals(r.getStatus())).count();
        long out = rows.stream().filter(r -> "OUT".equals(r.getStatus())).count();
        LocalDate limit = LocalDate.now().plusDays(settings.getExpiryAlertDays());
        long expiringSoon = rows.stream()
                .filter(r -> r.getExpiryDate() != null && !r.getExpiryDate().isAfter(limit))
                .count();
        double value = rows.stream()
                .mapToDouble(r -> (r.getPrice() != null ? r.getPrice() : 0) * (r.getStock() != null ? r.getStock() : 0))
                .sum();
        List<StockVariantRowDTO> alerts = rows.stream()
                .filter(r -> !"OK".equals(r.getStatus()))
                .sorted(Comparator.comparing(StockVariantRowDTO::getStatus))
                .limit(50)
                .toList();
        return StockOverviewDTO.builder()
                .lowStockCount(low)
                .outOfStockCount(out)
                .expiringSoonCount(expiringSoon)
                .stockValue(Math.round(value * 100.0) / 100.0)
                .defaultSafetyStock(settings.getDefaultSafetyStock())
                .expiryAlertDays(settings.getExpiryAlertDays())
                .alerts(alerts)
                .build();
    }

    @Transactional(readOnly = true)
    public List<StockVariantRowDTO> listVariantRows(String filter) {
        return listVariantRows(filter, 0, Integer.MAX_VALUE).getContent();
    }

    /** Liste paginée des variantes stock. */
    @Transactional(readOnly = true)
    public PageResponse<StockVariantRowDTO> listVariantRows(String filter, int page, int size) {
        StockSettings settings = resolveSettings();
        LocalDate expiryLimit = LocalDate.now().plusDays(settings.getExpiryAlertDays());
        String f = filter != null ? filter.trim().toLowerCase(Locale.ROOT) : "all";
        List<StockVariantRowDTO> result = new ArrayList<>();
        for (ProductVariant v : productVariantRepository.findAllActiveWithProduct()) {
            StockVariantRowDTO row = toRow(v, settings, expiryLimit);
            if (matchesFilter(row, f, expiryLimit)) {
                result.add(row);
            }
        }
        int safeSize = Math.min(Math.max(size, 1), 200);
        int safePage = Math.max(page, 0);
        int from = Math.min(safePage * safeSize, result.size());
        int to = Math.min(from + safeSize, result.size());
        return PageResponse.of(result.subList(from, to), safePage, safeSize, result.size());
    }

    private boolean matchesFilter(StockVariantRowDTO row, String filter, LocalDate expiryLimit) {
        return switch (filter) {
            case "out", "rupture" -> "OUT".equals(row.getStatus());
            case "low", "alerte" -> "LOW".equals(row.getStatus());
            case "expiring", "expire" -> row.getExpiryDate() != null && !row.getExpiryDate().isAfter(expiryLimit);
            case "alert" -> !"OK".equals(row.getStatus());
            default -> true;
        };
    }

    @Transactional(readOnly = true)
    public Page<StockMovementDTO> listMovements(Long variantId, String type, Pageable pageable) {
        String t = (type != null && !type.isBlank()) ? type.trim().toUpperCase(Locale.ROOT) : null;
        if ("ALL".equals(t)) {
            t = null;
        }
        Page<StockMovement> page;
        if (variantId != null && t != null) {
            page = stockMovementRepository.findByVariantIdAndTypeOrderByCreatedAtDesc(variantId, t, pageable);
        } else if (variantId != null) {
            page = stockMovementRepository.findByVariantIdOrderByCreatedAtDesc(variantId, pageable);
        } else if (t != null) {
            page = stockMovementRepository.findByTypeOrderByCreatedAtDesc(t, pageable);
        } else {
            page = stockMovementRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return page.map(this::toMovementDto);
    }

    @Transactional
    public StockVariantRowDTO adjust(StockAdjustRequest request, String createdBy) {
        ProductVariant variant = productVariantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Variante", request.getVariantId()));
        String type = request.getType().trim().toUpperCase(Locale.ROOT);
        int before = variant.getStock() != null ? variant.getStock() : 0;
        int after;
        int qty = request.getQuantity();
        String movementType;

        switch (type) {
            case "IN" -> {
                if (qty <= 0) {
                    throw new BusinessException("La quantité d'entrée doit être > 0", HttpStatus.BAD_REQUEST);
                }
                after = before + qty;
                movementType = "IN";
                variant.setLastRestockedAt(LocalDateTime.now());
            }
            case "OUT" -> {
                if (qty <= 0) {
                    throw new BusinessException("La quantité de sortie doit être > 0", HttpStatus.BAD_REQUEST);
                }
                if (before < qty) {
                    throw new BusinessException("Stock insuffisant (disponible: " + before + ")", HttpStatus.BAD_REQUEST);
                }
                after = before - qty;
                movementType = "OUT";
            }
            case "ADJUST" -> {
                after = qty;
                movementType = "ADJUST";
                if (after > before) {
                    variant.setLastRestockedAt(LocalDateTime.now());
                }
            }
            default -> throw new BusinessException("Type de mouvement invalide: " + type, HttpStatus.BAD_REQUEST);
        }

        variant.setStock(after);
        productVariantRepository.save(variant);
        syncProductStock(variant);

        int moved = Math.abs(after - before);
        stockMovementRepository.save(StockMovement.builder()
                .variantId(variant.getId())
                .productId(variant.getProduct() != null ? variant.getProduct().getId() : null)
                .type(movementType)
                .quantity(moved)
                .stockBefore(before)
                .stockAfter(after)
                .reason(request.getReason())
                .createdBy(createdBy)
                .build());

        String productName = variant.getProduct() != null ? variant.getProduct().getName() : "variante";
        auditLog.record(AuditLogService.Action.STOCK_ADJUST, "STOCK", String.valueOf(variant.getId()),
                movementType + " x" + moved + " sur " + productName
                        + (request.getReason() != null ? " — " + request.getReason() : ""));

        maybeAlert(variant);
        StockSettings settings = getOrCreateSettings();
        return toRow(variant, settings, LocalDate.now().plusDays(settings.getExpiryAlertDays()));
    }

    @Transactional
    public StockVariantRowDTO patchVariant(Long variantId, StockVariantPatchRequest request) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variante", variantId));
        if (Boolean.TRUE.equals(request.getClearSafetyStock())) {
            variant.setSafetyStock(null);
        } else if (request.getSafetyStock() != null) {
            if (request.getSafetyStock() < 0) {
                throw new BusinessException("Le seuil d'alerte doit être ≥ 0", HttpStatus.BAD_REQUEST);
            }
            variant.setSafetyStock(request.getSafetyStock());
        }
        if (request.getReorderQty() != null) {
            variant.setReorderQty(request.getReorderQty());
        }
        if (Boolean.TRUE.equals(request.getClearExpiryDate())) {
            variant.setExpiryDate(null);
        } else if (request.getExpiryDate() != null) {
            variant.setExpiryDate(request.getExpiryDate());
        }
        productVariantRepository.save(variant);
        maybeAlert(variant);
        StockSettings settings = getOrCreateSettings();
        return toRow(variant, settings, LocalDate.now().plusDays(settings.getExpiryAlertDays()));
    }

    @Transactional
    public int bulkSafety(BulkSafetyStockRequest request) {
        if (Boolean.TRUE.equals(request.getClearOverrides())) {
            // Via produit (TenantScoped) — jamais findAll() brut cross-tenant.
            List<ProductVariant> all = productVariantRepository.findAllActiveWithProduct();
            int n = 0;
            for (ProductVariant v : all) {
                if (v.getSafetyStock() != null) {
                    v.setSafetyStock(null);
                    n++;
                }
            }
            productVariantRepository.saveAll(all);
            return n;
        }
        if (request.getSafetyStock() == null) {
            throw new BusinessException("Indiquez safetyStock ou clearOverrides", HttpStatus.BAD_REQUEST);
        }
        if (request.getSafetyStock() < 0) {
            throw new BusinessException("Le seuil d'alerte doit être ≥ 0", HttpStatus.BAD_REQUEST);
        }
        List<ProductVariant> all = productVariantRepository.findAllActiveWithProduct();
        for (ProductVariant v : all) {
            v.setSafetyStock(request.getSafetyStock());
        }
        productVariantRepository.saveAll(all);
        return all.size();
    }

    @Transactional
    public void consumeForOrder(Order order) {
        if (order.getOrderItems() == null) {
            return;
        }
        for (OrderItem item : order.getOrderItems()) {
            ProductVariant variant = resolveVariant(item);
            int qty = item.getQuantity() != null ? item.getQuantity() : 0;
            if (qty <= 0) {
                continue;
            }
            if (variant != null) {
                int before = variant.getStock() != null ? variant.getStock() : 0;
                if (before < qty) {
                    String label = variant.getLabel() != null ? variant.getLabel() : String.valueOf(variant.getId());
                    throw new BusinessException(
                            "Stock insuffisant pour « " + label + " » (disponible: " + before + ", demandé: " + qty + ")",
                            HttpStatus.BAD_REQUEST);
                }
                int after = before - qty;
                variant.setStock(after);
                productVariantRepository.save(variant);
                syncProductStock(variant);
                stockMovementRepository.save(StockMovement.builder()
                        .variantId(variant.getId())
                        .productId(variant.getProduct() != null ? variant.getProduct().getId() : item.getProduct().getId())
                        .type("OUT")
                        .quantity(qty)
                        .stockBefore(before)
                        .stockAfter(after)
                        .reason("Commande " + order.getOrderNumber())
                        .orderId(order.getId())
                        .createdBy("system")
                        .build());
                maybeAlert(variant);
            } else if (item.getProduct() != null) {
                Product product = item.getProduct();
                int before = product.getStock() != null ? product.getStock() : 0;
                if (before < qty) {
                    throw new BusinessException(
                            "Stock insuffisant pour « " + product.getName() + " » (disponible: " + before + ")",
                            HttpStatus.BAD_REQUEST);
                }
                int after = before - qty;
                product.setStock(after);
                productRepository.save(product);
                stockMovementRepository.save(StockMovement.builder()
                        .variantId(null)
                        .productId(product.getId())
                        .type("OUT")
                        .quantity(qty)
                        .stockBefore(before)
                        .stockAfter(after)
                        .reason("Commande " + order.getOrderNumber())
                        .orderId(order.getId())
                        .createdBy("system")
                        .build());
            }
        }
    }

    @Transactional
    public void restoreForOrder(Order order) {
        if (order.getId() == null) {
            return;
        }
        if (stockMovementRepository.existsByOrderIdAndType(order.getId(), "RESTORE")) {
            log.info("Stock already restored for orderId={}", order.getId());
            return;
        }
        if (!stockMovementRepository.existsByOrderIdAndType(order.getId(), "OUT")) {
            log.info("No OUT movements to restore for orderId={}", order.getId());
            return;
        }
        if (order.getOrderItems() == null) {
            return;
        }
        for (OrderItem item : order.getOrderItems()) {
            int qty = item.getQuantity() != null ? item.getQuantity() : 0;
            if (qty <= 0) {
                continue;
            }
            ProductVariant variant = resolveVariant(item);
            if (variant != null) {
                int before = variant.getStock() != null ? variant.getStock() : 0;
                int after = before + qty;
                variant.setStock(after);
                productVariantRepository.save(variant);
                syncProductStock(variant);
                stockMovementRepository.save(StockMovement.builder()
                        .variantId(variant.getId())
                        .productId(variant.getProduct() != null ? variant.getProduct().getId() : item.getProduct().getId())
                        .type("RESTORE")
                        .quantity(qty)
                        .stockBefore(before)
                        .stockAfter(after)
                        .reason("Annulation commande " + order.getOrderNumber())
                        .orderId(order.getId())
                        .createdBy("system")
                        .build());
            } else if (item.getProduct() != null) {
                Product product = item.getProduct();
                int before = product.getStock() != null ? product.getStock() : 0;
                int after = before + qty;
                product.setStock(after);
                productRepository.save(product);
                stockMovementRepository.save(StockMovement.builder()
                        .variantId(null)
                        .productId(product.getId())
                        .type("RESTORE")
                        .quantity(qty)
                        .stockBefore(before)
                        .stockAfter(after)
                        .reason("Annulation commande " + order.getOrderNumber())
                        .orderId(order.getId())
                        .createdBy("system")
                        .build());
            }
        }
    }

    private ProductVariant resolveVariant(OrderItem item) {
        if (item.getSelectedVariantId() != null) {
            return productVariantRepository.findById(item.getSelectedVariantId()).orElse(null);
        }
        Product product = item.getProduct();
        if (product == null || product.getId() == null) {
            return null;
        }
        List<ProductVariant> variants = productVariantRepository.findByProductIdOrderByDisplayOrderAsc(product.getId());
        if (variants.isEmpty()) {
            return null;
        }
        return variants.stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsDefault()))
                .findFirst()
                .orElse(variants.get(0));
    }

    private void syncProductStock(ProductVariant variant) {
        if (variant.getProduct() == null) {
            return;
        }
        Product product = variant.getProduct();
        if (Boolean.TRUE.equals(variant.getIsDefault()) || product.getVariants() == null || product.getVariants().size() <= 1) {
            product.setStock(variant.getStock());
            productRepository.save(product);
        } else {
            int sum = product.getVariants().stream()
                    .mapToInt(v -> v.getStock() != null ? v.getStock() : 0)
                    .sum();
            product.setStock(sum);
            productRepository.save(product);
        }
    }

    private void maybeAlert(ProductVariant variant) {
        StockSettings settings = getOrCreateSettings();
        if (!Boolean.TRUE.equals(settings.getAlertsEnabled())) {
            return;
        }
        int stock = variant.getStock() != null ? variant.getStock() : 0;
        int safety = effectiveSafetyStock(variant, settings);
        String productName = variant.getProduct() != null ? variant.getProduct().getName() : "Produit";
        String label = variant.getLabel() != null ? variant.getLabel() : "";

        if (Boolean.TRUE.equals(settings.getLowStockAlertsEnabled())) {
            if (stock == 0) {
                notificationService.notifyStockAlert(
                        "OUT_OF_STOCK",
                        "Rupture de stock",
                        String.format("%s — %s : stock à 0", productName, label),
                        variant.getId());
            } else if (stock <= safety) {
                notificationService.notifyStockAlert(
                        "LOW_STOCK",
                        "Stock bas",
                        String.format("%s — %s : stock %d (seuil %d)", productName, label, stock, safety),
                        variant.getId());
            }
        }
        if (Boolean.TRUE.equals(settings.getExpiryAlertsEnabled())
                && variant.getExpiryDate() != null
                && !variant.getExpiryDate().isAfter(LocalDate.now().plusDays(settings.getExpiryAlertDays()))) {
            notificationService.notifyStockAlert(
                    "EXPIRING_SOON",
                    "Péremption proche",
                    String.format("%s — %s expire le %s", productName, label, variant.getExpiryDate()),
                    variant.getId());
        }
    }

    private StockVariantRowDTO toRow(ProductVariant v, StockSettings settings, LocalDate expiryLimit) {
        int stock = v.getStock() != null ? v.getStock() : 0;
        int effective = effectiveSafetyStock(v, settings);
        boolean usesDefault = v.getSafetyStock() == null;
        String status;
        if (stock <= 0) {
            status = "OUT";
        } else if (v.getExpiryDate() != null && !v.getExpiryDate().isAfter(expiryLimit)) {
            status = stock <= effective ? "LOW" : "EXPIRING";
        } else if (stock <= effective) {
            status = "LOW";
        } else {
            status = "OK";
        }
        return StockVariantRowDTO.builder()
                .variantId(v.getId())
                .productId(v.getProduct() != null ? v.getProduct().getId() : null)
                .productName(v.getProduct() != null ? v.getProduct().getName() : null)
                .variantLabel(v.getLabel())
                .sku(v.getSku())
                .stock(stock)
                .safetyStock(v.getSafetyStock())
                .effectiveSafetyStock(effective)
                .usesDefaultSafety(usesDefault)
                .reorderQty(v.getReorderQty())
                .expiryDate(v.getExpiryDate())
                .lastRestockedAt(v.getLastRestockedAt())
                .price(v.getPrice())
                .status(status)
                .build();
    }

    private StockMovementDTO toMovementDto(StockMovement m) {
        String productName = null;
        String variantLabel = null;
        if (m.getVariantId() != null) {
            ProductVariant v = productVariantRepository.findById(m.getVariantId()).orElse(null);
            if (v != null) {
                variantLabel = v.getLabel();
            }
        }
        if (m.getProductId() != null) {
            productName = productRepository.findById(m.getProductId()).map(Product::getName).orElse(null);
        }
        return StockMovementDTO.builder()
                .id(m.getId())
                .variantId(m.getVariantId())
                .productId(m.getProductId())
                .productName(productName)
                .variantLabel(variantLabel)
                .type(m.getType())
                .quantity(m.getQuantity())
                .stockBefore(m.getStockBefore())
                .stockAfter(m.getStockAfter())
                .reason(m.getReason())
                .orderId(m.getOrderId())
                .createdBy(m.getCreatedBy())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private StockSettingsDTO toSettingsDto(StockSettings s) {
        return new StockSettingsDTO(
                s.getDefaultSafetyStock(),
                s.getExpiryAlertDays(),
                s.getAlertsEnabled(),
                s.getLowStockAlertsEnabled(),
                s.getExpiryAlertsEnabled());
    }

    @Transactional(readOnly = true)
    public long countLowStock() {
        return listVariantRows("low").size();
    }

    @Transactional(readOnly = true)
    public long countOutOfStock() {
        return listVariantRows("out").size();
    }
}
