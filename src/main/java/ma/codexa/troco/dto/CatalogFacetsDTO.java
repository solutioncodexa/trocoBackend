package ma.codexa.troco.dto;

import java.util.List;

public record CatalogFacetsDTO(
        List<FacetBucket> categories,
        List<FacetBucket> sizes,
        Double priceMin,
        Double priceMax,
        long totalProducts
) {
    public record FacetBucket(String value, String label, long count) {}
}
