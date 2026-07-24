package ma.codexa.troco.mapper;

import lombok.RequiredArgsConstructor;
import ma.codexa.troco.dto.ProductDetailDTO;
import ma.codexa.troco.dto.ProductListItemDTO;
import ma.codexa.troco.entity.Product;
import ma.codexa.troco.service.ProductVariantService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductDtoMapper {

    private final ProductMapper productMapper;
    private final ProductVariantService productVariantService;

    public ProductDetailDTO toDetailDTO(Product product) {
        ProductDetailDTO dto = productMapper.toDetailDTO(product);
        dto.setVariants(productVariantService.toDtoList(product));
        return dto;
    }

    public List<ProductDetailDTO> toDetailDTOList(List<Product> products) {
        return products.stream().map(this::toDetailDTO).toList();
    }

    public ProductListItemDTO toListItemDTO(Product product) {
        return productMapper.toListItemDTO(product);
    }

    public List<ProductListItemDTO> toListItemDTOList(List<Product> products) {
        return productMapper.toListItemDTOList(products);
    }
}
