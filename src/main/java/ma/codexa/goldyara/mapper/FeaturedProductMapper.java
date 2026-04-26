package ma.codexa.goldyara.mapper;

import ma.codexa.goldyara.dto.FeaturedProductDTO;
import ma.codexa.goldyara.entity.FeaturedProduct;
import ma.codexa.goldyara.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class FeaturedProductMapper {

    public FeaturedProductDTO toDTO(FeaturedProduct featuredProduct) {
        if (featuredProduct == null) {
            return null;
        }

        FeaturedProductDTO dto = new FeaturedProductDTO();
        dto.setId(featuredProduct.getId());
        dto.setProductId(featuredProduct.getProduct().getId());
        dto.setSection(featuredProduct.getSection().getValue());
        dto.setTitle(featuredProduct.getTitle());
        dto.setDescription(featuredProduct.getDescription());
        dto.setImageUrl(featuredProduct.getImageUrl());
        dto.setDisplayOrder(featuredProduct.getDisplayOrder());
        dto.setIsActive(featuredProduct.getIsActive());
        dto.setCreatedAt(featuredProduct.getCreatedAt());
        dto.setUpdatedAt(featuredProduct.getUpdatedAt());

        // Map product information
        if (featuredProduct.getProduct() != null) {
            Product product = featuredProduct.getProduct();
            FeaturedProductDTO.ProductInfoDTO productInfo = new FeaturedProductDTO.ProductInfoDTO();
            productInfo.setId(product.getId());
            productInfo.setName(product.getName());
            productInfo.setDescription(product.getDescription());
            productInfo.setPrice(product.getPrice());
            productInfo.setCategory(product.getCategory() != null ? product.getCategory().getName() : null);
            productInfo.setProductType(product.getProductType());
            productInfo.setGoldType(product.getGoldType());
            productInfo.setWeight(product.getWeight());
            productInfo.setIsActive(product.isInStock());

            // Set image URL from product images if available
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                productInfo.setImageUrl(product.getImages().get(0).getUrl());
            }

            dto.setProduct(productInfo);
        }

        return dto;
    }

    public FeaturedProduct toEntity(FeaturedProductDTO dto) {
        if (dto == null) {
            return null;
        }

        FeaturedProduct featuredProduct = new FeaturedProduct();
        featuredProduct.setId(dto.getId());
        featuredProduct.setSection(FeaturedProduct.Section.fromValue(dto.getSection()));
        featuredProduct.setTitle(dto.getTitle());
        featuredProduct.setDescription(dto.getDescription());
        featuredProduct.setImageUrl(dto.getImageUrl());
        featuredProduct.setDisplayOrder(dto.getDisplayOrder());
        featuredProduct.setIsActive(dto.getIsActive());

        // Product should be set separately as it requires fetching from database
        return featuredProduct;
    }
}
