package ma.codexa.troco.mapper;

import ma.codexa.troco.dto.CustomOrderDTO;
import ma.codexa.troco.dto.CustomOrderListItemDTO;
import ma.codexa.troco.entity.CustomOrder;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static ma.codexa.troco.mapper.MapperUtils.*;

@Mapper(componentModel = "spring", uses = {CustomerMapper.class}, 
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CustomOrderMapper {

    CustomOrderMapper INSTANCE = Mappers.getMapper(CustomOrderMapper.class);

    // Mapping automatique pour : description, customer
    @Mapping(target = "id", expression = "java(customOrder.getId() != null ? customOrder.getId().toString() : null)")
    @Mapping(target = "imageUrl", source = "referenceImages", qualifiedByName = "firstImageUrl")
    @Mapping(target = "type", source = "productType", qualifiedByName = "productTypeToLowercase")
    @Mapping(target = "size", source = "size")
    @Mapping(target = "weight", source = "weightEstimation")
    @Mapping(target = "style", source = "style", qualifiedByName = "styleToCategory")
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToFrontend")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "dateToString")
    CustomOrderDTO toDTO(CustomOrder customOrder);

    List<CustomOrderDTO> toDTOList(List<CustomOrder> customOrders);

    @Mapping(target = "id", expression = "java(customOrder.getId() != null ? customOrder.getId().toString() : null)")
    @Mapping(target = "imageUrl", source = "referenceImages", qualifiedByName = "firstImageUrl")
    @Mapping(target = "type", source = "productType", qualifiedByName = "productTypeToLowercase")
    @Mapping(target = "weight", source = "weightEstimation")
    @Mapping(target = "style", source = "style", qualifiedByName = "styleToCategory")
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToFrontend")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "dateToString")
    @Mapping(target = "description", source = "description", qualifiedByName = "truncateDescription")
    @Mapping(target = "customer", source = "customer", qualifiedByName = "customerSummaryForList")
    CustomOrderListItemDTO toListItemDTO(CustomOrder customOrder);

    List<CustomOrderListItemDTO> toListItemDTOList(List<CustomOrder> customOrders);

    // Méthodes de conversion nommées utilisant MapperUtils
    @Named("firstImageUrl")
    default String firstImageUrl(java.util.List<ma.codexa.troco.entity.Image> images) {
        return MapperUtils.firstImageUrl(images);
    }

    @Named("imagesToUrls")
    default java.util.List<String> imagesToUrls(java.util.List<ma.codexa.troco.entity.Image> images) {
        return MapperUtils.imagesToList(images);
    }

    @Named("productTypeToLowercase")
    default String productTypeToLowercase(String productType) {
        return MapperUtils.productTypeToLowercase(productType);
    }

    @Named("styleToCategory")
    default String styleToCategory(String style) {
        return MapperUtils.styleToCategory(style);
    }

    @Named("statusToFrontend")
    default String statusToFrontend(String status) {
        return MapperUtils.statusToFrontend(status);
    }

    @Named("dateToString")
    default String dateToString(java.time.LocalDateTime dateTime) {
        return MapperUtils.dateToString(dateTime);
    }

    @Named("truncateDescription")
    default String truncateDescription(String description) {
        if (description == null) {
            return "";
        }
        if (description.length() <= 160) {
            return description;
        }
        return description.substring(0, 160).trim() + "…";
    }

    @Named("customerSummaryForList")
    default ma.codexa.troco.dto.CustomerSummaryDTO customerSummaryForList(
            ma.codexa.troco.entity.Customer customer) {
        if (customer == null) {
            return null;
        }
        return new ma.codexa.troco.dto.CustomerSummaryDTO(
                customer.getFullName(),
                customer.getPhone(),
                null
        );
    }
}
