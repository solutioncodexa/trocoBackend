package ma.codexa.goldyara.service;

import lombok.RequiredArgsConstructor;
import ma.codexa.goldyara.dto.ProductVariantDTO;
import ma.codexa.goldyara.dto.request.ProductVariantRequest;
import ma.codexa.goldyara.entity.Product;
import ma.codexa.goldyara.entity.ProductVariant;
import ma.codexa.goldyara.mapper.MapperUtils;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductVariantService {

    private final GoldPriceSettingService goldPriceSettingService;

    public List<ProductVariantDTO> toDtoList(Product product) {
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            return product.getVariants().stream()
                    .sorted(Comparator.comparing(v -> v.getDisplayOrder() != null ? v.getDisplayOrder() : 0))
                    .map(this::toDto)
                    .toList();
        }
        return List.of(syntheticFromProduct(product));
    }

    public ProductVariantDTO syntheticFromProduct(Product product) {
        ProductVariantDTO dto = new ProductVariantDTO();
        dto.setId(null);
        dto.setLabel(formatWeightLabel(product.getWeight()));
        dto.setWeight(product.getWeight());
        dto.setPrice(product.getPrice());
        dto.setOriginalPrice(product.getOriginalPrice());
        dto.setMarginGain(product.getMarginGain() != null ? product.getMarginGain() : 500.0);
        dto.setDisplayOrder(0);
        dto.setIsDefault(true);
        return dto;
    }

    /**
     * Met à jour les variantes en place (entités managed) et synchronise poids/prix racine.
     */
    public void applyVariants(Product product, List<ProductVariantRequest> requests, boolean isPromo) {
        List<ProductVariantRequest> source = (requests != null && !requests.isEmpty())
                ? requests
                : buildSingleRequestFromProduct(product, isPromo);

        Map<Long, ProductVariant> existingById = product.getVariants().stream()
                .filter(v -> v.getId() != null)
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

        Set<Long> keptIds = new HashSet<>();
        int order = 0;
        boolean hasDefault = source.stream().anyMatch(r -> Boolean.TRUE.equals(r.getIsDefault()));

        for (ProductVariantRequest req : source) {
            Long variantId = parseVariantId(req.getId());
            ProductVariant variant;

            if (variantId != null && existingById.containsKey(variantId)) {
                variant = existingById.get(variantId);
                keptIds.add(variantId);
            } else {
                variant = new ProductVariant();
                product.addVariant(variant);
            }

            applyRequestToVariant(variant, req, isPromo, order, hasDefault);
            order++;
        }

        product.getVariants().removeIf(v -> v.getId() != null && !keptIds.contains(v.getId()));

        ensureSingleDefault(product);
        syncProductFromDefaultVariant(product);
    }

    public void syncProductFromDefaultVariant(Product product) {
        ProductVariant def = product.getVariants().stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsDefault()))
                .findFirst()
                .orElse(product.getVariants().isEmpty() ? null : product.getVariants().get(0));

        if (def == null) {
            return;
        }
        product.setWeight(def.getWeight());
        product.setPrice(def.getPrice());
        product.setOriginalPrice(def.getOriginalPrice());
        product.setMarginGain(def.getMarginGain());
    }

    public void recalculateAllVariantPrices(Product product, double pricePerGram, boolean isPromo) {
        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            double marginGain = product.getMarginGain() != null ? product.getMarginGain() : 500.0;
            double newBasePrice = product.getWeight() * pricePerGram + marginGain;
            if (isPromo && product.getOriginalPrice() != null && product.getOriginalPrice() > 0
                    && product.getPrice() != null && product.getPrice() > 0) {
                double discountRatio = product.getPrice() / product.getOriginalPrice();
                product.setOriginalPrice(round2(newBasePrice));
                product.setPrice(round2(newBasePrice * discountRatio));
            } else {
                product.setPrice(round2(newBasePrice));
                product.setOriginalPrice(null);
            }
            return;
        }

        for (ProductVariant v : product.getVariants()) {
            double marginGain = v.getMarginGain() != null ? v.getMarginGain() : 500.0;
            double newBasePrice = v.getWeight() * pricePerGram + marginGain;
            if (isPromo && v.getOriginalPrice() != null && v.getOriginalPrice() > 0
                    && v.getPrice() != null && v.getPrice() > 0) {
                double discountRatio = v.getPrice() / v.getOriginalPrice();
                v.setOriginalPrice(round2(newBasePrice));
                v.setPrice(round2(newBasePrice * discountRatio));
            } else {
                v.setPrice(round2(newBasePrice));
                v.setOriginalPrice(null);
            }
        }
        syncProductFromDefaultVariant(product);
    }

    public void ensureVariantsFromProductFields(Product product) {
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            return;
        }
        ProductVariant variant = new ProductVariant();
        variant.setWeight(product.getWeight());
        variant.setMarginGain(product.getMarginGain() != null ? product.getMarginGain() : 500.0);
        variant.setPrice(product.getPrice());
        variant.setOriginalPrice(product.getOriginalPrice());
        variant.setLabel(formatWeightLabel(product.getWeight()));
        variant.setDisplayOrder(0);
        variant.setIsDefault(true);
        product.addVariant(variant);
    }

    private List<ProductVariantRequest> buildSingleRequestFromProduct(Product product, boolean isPromo) {
        ProductVariantRequest req = new ProductVariantRequest();
        ProductVariant existing = product.getVariants().stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsDefault()))
                .findFirst()
                .orElse(product.getVariants().isEmpty() ? null : product.getVariants().get(0));
        if (existing != null && existing.getId() != null) {
            req.setId(existing.getId().toString());
        }
        req.setWeight(product.getWeight());
        req.setMarginGain(product.getMarginGain());
        req.setPrice(product.getPrice());
        req.setOriginalPrice(isPromo ? product.getOriginalPrice() : null);
        req.setIsDefault(true);
        req.setDisplayOrder(0);
        return List.of(req);
    }

    private void applyRequestToVariant(
            ProductVariant variant,
            ProductVariantRequest req,
            boolean isPromo,
            int order,
            boolean hasDefault) {
        double margin = req.getMarginGain() != null ? req.getMarginGain() : 500.0;
        variant.setMarginGain(margin);
        variant.setWeight(req.getWeight());
        variant.setDisplayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : order);
        variant.setLabel(req.getLabel() != null && !req.getLabel().isBlank()
                ? req.getLabel().trim()
                : formatWeightLabel(req.getWeight()));

        if (isPromo && req.getPrice() != null && req.getPrice() > 0) {
            variant.setPrice(req.getPrice());
            variant.setOriginalPrice(req.getOriginalPrice());
        } else {
            variant.setPrice(goldPriceSettingService.calculatePrice(req.getWeight(), margin));
            variant.setOriginalPrice(null);
        }

        boolean isDefault = hasDefault ? Boolean.TRUE.equals(req.getIsDefault()) : (order == 0);
        variant.setIsDefault(isDefault);
    }

    private Long parseVariantId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void ensureSingleDefault(Product product) {
        List<ProductVariant> variants = product.getVariants();
        if (variants.isEmpty()) {
            return;
        }
        long defaultCount = variants.stream().filter(v -> Boolean.TRUE.equals(v.getIsDefault())).count();
        if (defaultCount != 1) {
            for (int i = 0; i < variants.size(); i++) {
                variants.get(i).setIsDefault(i == 0);
            }
        }
    }

    private ProductVariantDTO toDto(ProductVariant v) {
        ProductVariantDTO dto = new ProductVariantDTO();
        dto.setId(v.getId() != null ? v.getId().toString() : null);
        dto.setLabel(v.getLabel());
        dto.setWeight(v.getWeight());
        dto.setPrice(v.getPrice());
        dto.setOriginalPrice(v.getOriginalPrice());
        dto.setMarginGain(v.getMarginGain());
        dto.setDisplayOrder(v.getDisplayOrder());
        dto.setIsDefault(v.getIsDefault());
        return dto;
    }

    private String formatWeightLabel(Double weight) {
        if (weight == null) {
            return "";
        }
        if (weight == Math.floor(weight)) {
            return String.format("%.0f g", weight);
        }
        double rounded1 = Math.round(weight * 10.0) / 10.0;
        if (rounded1 == weight) {
            return String.format("%.1f g", weight);
        }
        return String.format("%.2f g", weight);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public boolean hasPromoBadge(String badges) {
        if (badges == null || badges.isBlank()) {
            return false;
        }
        List<String> list = MapperUtils.badgesToList(badges);
        return list != null && list.stream().anyMatch(b -> "promo".equalsIgnoreCase(b.trim()));
    }
}
