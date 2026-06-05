package ma.codexa.goldyara.mapper;

import ma.codexa.goldyara.dto.ProductDetailDTO;
import ma.codexa.goldyara.dto.ProductListItemDTO;
import ma.codexa.goldyara.entity.Product;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    ProductMapper INSTANCE = Mappers.getMapper(ProductMapper.class);

    @Mapping(target = "id", expression = "java(product.getId() != null ? product.getId().toString() : null)")
    @Mapping(target = "type", source = "productType", qualifiedByName = "productTypeToLowercase")
    @Mapping(target = "category", source = "style", qualifiedByName = "styleToCategory")
    @Mapping(target = "goldType", source = "goldType", qualifiedByName = "goldTypeToFrontend")
    @Mapping(target = "stockQuantity", source = "stock")
    @Mapping(target = "images", source = "images", qualifiedByName = "imagesToList")
    @Mapping(target = "availableSizes", source = "availableSizes", qualifiedByName = "stringToList")
    @Mapping(target = "badges", source = "badges", qualifiedByName = "badgesToList")
    @Mapping(target = "inStock", expression = "java(product.isInStock())")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "dateToString")
    @Mapping(target = "deleted", source = "deleted")
    @Mapping(target = "showWeight", source = "showWeight")
    ProductDetailDTO toDetailDTO(Product product);

    @Mapping(target = "id", expression = "java(product.getId() != null ? product.getId().toString() : null)")
    @Mapping(target = "type", source = "productType", qualifiedByName = "productTypeToLowercase")
    @Mapping(target = "category", source = "style", qualifiedByName = "styleToCategory")
    @Mapping(target = "goldType", source = "goldType", qualifiedByName = "goldTypeToFrontend")
    @Mapping(target = "stockQuantity", source = "stock")
    @Mapping(target = "images", source = "images", qualifiedByName = "imagesToList")
    @Mapping(target = "badges", source = "badges", qualifiedByName = "badgesToList")
    @Mapping(target = "inStock", expression = "java(product.isInStock())")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "dateToString")
    @Mapping(target = "showWeight", source = "showWeight")
    ProductListItemDTO toListItemDTO(Product product);

    List<ProductListItemDTO> toListItemDTOList(List<Product> products);

    @Mapping(target = "id", expression = "java(dto.getId() != null && !dto.getId().isEmpty() ? Long.parseLong(dto.getId()) : null)")
    @Mapping(target = "productType", source = "type", qualifiedByName = "typeToProductType")
    @Mapping(target = "style", source = "category", qualifiedByName = "categoryToStyle")
    @Mapping(target = "goldType", source = "goldType", qualifiedByName = "goldTypeToBackend")
    @Mapping(target = "stock", source = "stockQuantity")
    @Mapping(target = "availableSizes", source = "availableSizes", qualifiedByName = "listToString")
    @Mapping(target = "badges", source = "badges", qualifiedByName = "badgesToString")
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "showWeight", source = "showWeight")
    Product toEntity(ProductDetailDTO dto);

    List<ProductDetailDTO> toDetailDTOList(List<Product> products);

    @Named("productTypeToLowercase")
    default String productTypeToLowercase(String productType) {
        return MapperUtils.productTypeToLowercase(productType);
    }

    @Named("typeToProductType")
    default String typeToProductType(String type) {
        return MapperUtils.typeToProductType(type);
    }

    @Named("styleToCategory")
    default String styleToCategory(String style) {
        return MapperUtils.styleToCategory(style);
    }

    @Named("categoryToStyle")
    default String categoryToStyle(String category) {
        return MapperUtils.categoryToStyle(category);
    }

    @Named("goldTypeToFrontend")
    default String goldTypeToFrontend(String goldType) {
        return MapperUtils.goldTypeToFrontend(goldType);
    }

    @Named("goldTypeToBackend")
    default String goldTypeToBackend(String goldType) {
        return MapperUtils.goldTypeToBackend(goldType);
    }

    @Named("imagesToList")
    default List<String> imagesToList(List<ma.codexa.goldyara.entity.Image> images) {
        return MapperUtils.imagesToList(images);
    }

    @Named("stringToList")
    default List<String> stringToList(String str) {
        return MapperUtils.stringToList(str);
    }

    @Named("listToString")
    default String listToString(List<String> list) {
        return MapperUtils.listToString(list);
    }

    @Named("badgesToList")
    default List<String> badgesToList(String badges) {
        return MapperUtils.badgesToList(badges);
    }

    @Named("badgesToString")
    default String badgesToString(List<String> badges) {
        return MapperUtils.badgesToString(badges);
    }

    @Named("dateToString")
    default String dateToString(java.time.LocalDateTime dateTime) {
        return MapperUtils.dateToString(dateTime);
    }
}
