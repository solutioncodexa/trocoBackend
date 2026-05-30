package ma.codexa.goldyara.mapper;

import ma.codexa.goldyara.dto.*;
import ma.codexa.goldyara.entity.Order;
import ma.codexa.goldyara.entity.OrderItem;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static ma.codexa.goldyara.mapper.MapperUtils.*;

@Mapper(componentModel = "spring", uses = {CustomerMapper.class}, 
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    OrderMapper INSTANCE = Mappers.getMapper(OrderMapper.class);

    // Mapping automatique pour : customer, paymentMethod
    @Mapping(target = "id", expression = "java(order.getId() != null ? order.getId().toString() : null)")
    @Mapping(target = "items", ignore = true) // Géré dans @AfterMapping
    @Mapping(target = "total", source = "totalAmount")
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToLowercase")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "dateToString")
    @Mapping(target = "promoCode", source = "promoCode")
    @Mapping(target = "discount", source = "discountAmount")
    OrderDTO toDTO(Order order, @Context ProductMapper productMapper);

    default OrderDTO toDTO(Order order) {
        return toDTO(order, Mappers.getMapper(ProductMapper.class));
    }

    @Mapping(target = "id", expression = "java(order.getId() != null ? order.getId().toString() : null)")
    @Mapping(target = "total", source = "totalAmount")
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToLowercase")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "dateToString")
    @Mapping(target = "itemCount", ignore = true)
    @Mapping(target = "previewProductName", ignore = true)
    @Mapping(target = "previewProductImage", ignore = true)
    @Mapping(target = "customer", ignore = true)
    OrderListItemDTO toListItemDTO(Order order);

    List<OrderListItemDTO> toListItemDTOList(List<Order> orders);

    @AfterMapping
    default void mapListItemPreview(@MappingTarget OrderListItemDTO dto, Order order) {
        if (order.getCustomer() != null) {
            dto.setCustomer(new CustomerSummaryDTO(
                    order.getCustomer().getFullName(),
                    order.getCustomer().getPhone(),
                    order.getCustomer().getCity()
            ));
        }
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            dto.setItemCount(0);
            return;
        }
        dto.setItemCount(order.getOrderItems().size());
        var firstItem = order.getOrderItems().get(0);
        if (firstItem.getProduct() != null) {
            dto.setPreviewProductName(firstItem.getProduct().getName());
            dto.setPreviewProductImage(MapperUtils.firstImageUrl(firstItem.getProduct().getImages()));
        }
    }

    @AfterMapping
    default void mapOrderItems(@MappingTarget OrderDTO dto, Order order, @Context ProductMapper productMapper) {
        if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
            List<CartItemDTO> cartItems = order.getOrderItems().stream()
                    .map(item -> {
                        CartItemDTO cartItemDTO = new CartItemDTO();
                        cartItemDTO.setProduct(productMapper.toDetailDTO(item.getProduct()));
                        cartItemDTO.setQuantity(item.getQuantity());
                        cartItemDTO.setSelectedSize(item.getSelectedSize());
                        cartItemDTO.setSelectedGoldType(item.getSelectedGoldType());
                        return cartItemDTO;
                    })
                    .toList();
            dto.setItems(cartItems);
        }
    }

    @Named("statusToLowercase")
    default String statusToLowercase(String status) {
        return MapperUtils.statusToLowercase(status);
    }

    @Named("dateToString")
    default String dateToString(java.time.LocalDateTime dateTime) {
        return MapperUtils.dateToString(dateTime);
    }
}
