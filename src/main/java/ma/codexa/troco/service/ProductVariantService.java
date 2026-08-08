package ma.codexa.troco.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.codexa.troco.dto.ProductVariantDTO;
import ma.codexa.troco.dto.VariantAttributeDTO;
import ma.codexa.troco.dto.request.ProductVariantRequest;
import ma.codexa.troco.entity.Product;
import ma.codexa.troco.entity.ProductVariant;
import ma.codexa.troco.mapper.MapperUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ProductVariantService {

    private final ObjectMapper objectMapper = new ObjectMapper();

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
        dto.setLabel(product.getSku() != null ? product.getSku() : "Standard");
        dto.setAttributeName(null);
        dto.setAttributeValue(null);
        dto.setPrice(product.getPrice());
        dto.setOriginalPrice(product.getOriginalPrice());
        dto.setStock(product.getStock());
        dto.setSku(product.getSku());
        dto.setDisplayOrder(0);
        dto.setIsDefault(true);
        dto.setWeight(product.getWeight());
        dto.setMarginGain(product.getMarginGain());
        return dto;
    }

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
        product.setPrice(def.getPrice());
        product.setOriginalPrice(def.getOriginalPrice());
        if (def.getStock() != null) {
            product.setStock(def.getStock());
        }
        if (def.getWeight() != null) {
            product.setWeight(def.getWeight());
        }
        if (def.getMarginGain() != null) {
            product.setMarginGain(def.getMarginGain());
        }
    }

    public void ensureVariantsFromProductFields(Product product) {
        if (product.getVariants() == null) {
            product.setVariants(new java.util.ArrayList<>());
        }
        if (!product.getVariants().isEmpty()) {
            return;
        }
        ProductVariant variant = new ProductVariant();
        variant.setPrice(product.getPrice());
        variant.setOriginalPrice(product.getOriginalPrice());
        variant.setStock(product.getStock() != null ? product.getStock() : 0);
        variant.setSku(product.getSku());
        variant.setLabel("Standard");
        variant.setDisplayOrder(0);
        variant.setIsDefault(true);
        variant.setWeight(product.getWeight());
        variant.setMarginGain(product.getMarginGain());
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
        req.setPrice(product.getPrice());
        req.setOriginalPrice(isPromo ? product.getOriginalPrice() : null);
        req.setStock(product.getStock());
        req.setSku(product.getSku());
        req.setLabel("Standard");
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
        String attrValue = req.getAttributeValue() != null ? req.getAttributeValue().trim() : null;
        String attrName = req.getAttributeName() != null ? req.getAttributeName().trim() : null;

        // Multi-axes : si des attributs détaillés sont fournis, ils priment et sont
        // sérialisés dans attributes_json. Le mono-axe (name/value) reste alimenté par
        // le 1er axe pour compat filtres/vues existantes.
        List<VariantAttributeDTO> cleanedAttrs = cleanAttributes(req.getAttributes());
        if (!cleanedAttrs.isEmpty()) {
            variant.setAttributesJson(writeAttributes(cleanedAttrs));
            attrName = cleanedAttrs.get(0).getName();
            attrValue = cleanedAttrs.get(0).getValue();
        } else {
            variant.setAttributesJson(null);
        }

        variant.setAttributeName(attrName);
        variant.setAttributeValue(attrValue);
        variant.setSku(req.getSku());
        variant.setStock(req.getStock() != null ? req.getStock() : 0);
        if (req.getSafetyStock() != null) {
            variant.setSafetyStock(req.getSafetyStock());
        }
        if (req.getReorderQty() != null) {
            variant.setReorderQty(req.getReorderQty());
        }
        if (req.getExpiryDate() != null) {
            variant.setExpiryDate(req.getExpiryDate());
        }
        variant.setDisplayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : order);
        variant.setWeight(req.getWeight());
        variant.setMarginGain(req.getMarginGain());

        String label = req.getLabel();
        if (label == null || label.isBlank()) {
            if (!cleanedAttrs.isEmpty()) {
                label = cleanedAttrs.stream()
                        .map(VariantAttributeDTO::getValue)
                        .collect(Collectors.joining(" · "));
            } else {
                label = attrValue != null && !attrValue.isBlank() ? attrValue : "Standard";
            }
        }
        variant.setLabel(label.trim());

        if (req.getPrice() == null || req.getPrice() <= 0) {
            throw new IllegalArgumentException("Le prix de la variante est obligatoire");
        }
        variant.setPrice(req.getPrice());
        if (isPromo && req.getOriginalPrice() != null && req.getOriginalPrice() > 0) {
            variant.setOriginalPrice(req.getOriginalPrice());
        } else {
            variant.setOriginalPrice(req.getOriginalPrice());
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
        dto.setAttributeName(v.getAttributeName());
        dto.setAttributeValue(v.getAttributeValue());
        dto.setAttributes(parseAttributes(v));
        dto.setLabel(v.getLabel());
        dto.setPrice(v.getPrice());
        dto.setOriginalPrice(v.getOriginalPrice());
        dto.setStock(v.getStock());
        dto.setSafetyStock(v.getSafetyStock());
        dto.setReorderQty(v.getReorderQty());
        dto.setExpiryDate(v.getExpiryDate());
        dto.setSku(v.getSku());
        dto.setDisplayOrder(v.getDisplayOrder());
        dto.setIsDefault(v.getIsDefault());
        dto.setWeight(v.getWeight());
        dto.setMarginGain(v.getMarginGain());
        return dto;
    }

    private List<VariantAttributeDTO> parseAttributes(ProductVariant v) {
        if (v.getAttributesJson() != null && !v.getAttributesJson().isBlank()) {
            try {
                List<VariantAttributeDTO> list = objectMapper.readValue(
                        v.getAttributesJson(),
                        new TypeReference<List<VariantAttributeDTO>>() {});
                if (list != null && !list.isEmpty()) {
                    return list;
                }
            } catch (Exception ignored) {
                // fallback below
            }
        }
        List<VariantAttributeDTO> fallback = new ArrayList<>();
        if (v.getAttributeName() != null && v.getAttributeValue() != null) {
            fallback.add(new VariantAttributeDTO(v.getAttributeName(), v.getAttributeValue()));
        }
        return fallback;
    }

    private List<VariantAttributeDTO> cleanAttributes(List<VariantAttributeDTO> attrs) {
        if (attrs == null || attrs.isEmpty()) {
            return List.of();
        }
        return attrs.stream()
                .filter(a -> a != null
                        && a.getName() != null && !a.getName().isBlank()
                        && a.getValue() != null && !a.getValue().isBlank())
                .map(a -> new VariantAttributeDTO(a.getName().trim(), a.getValue().trim()))
                .toList();
    }

    private String writeAttributes(List<VariantAttributeDTO> attrs) {
        try {
            return objectMapper.writeValueAsString(attrs);
        } catch (Exception e) {
            return null;
        }
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
