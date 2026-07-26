package ma.codexa.troco.mapper;

import ma.codexa.troco.dto.*;
import ma.codexa.troco.entity.Order;
import ma.codexa.troco.entity.OrderItem;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static ma.codexa.troco.mapper.MapperUtils.*;

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
                        if (item.getProduct() != null) {
                            cartItemDTO.setProduct(toLineProduct(item));
                        }
                        cartItemDTO.setQuantity(item.getQuantity());
                        cartItemDTO.setSelectedSize(item.getSelectedSize());
                        cartItemDTO.setSelectedGoldType(item.getSelectedGoldType());
                        cartItemDTO.setCustomLogoUrl(item.getCustomLogoUrl());
                        if (item.getSelectedVariantId() != null) {
                            cartItemDTO.setSelectedVariantId(String.valueOf(item.getSelectedVariantId()));
                        }
                        return cartItemDTO;
                    })
                    .toList();
            dto.setItems(cartItems);
        }
    }

    private static OrderLineProductDTO toLineProduct(OrderItem item) {
        var p = item.getProduct();
        OrderLineProductDTO line = new OrderLineProductDTO();
        line.setId(p.getId() != null ? p.getId().toString() : null);
        line.setName(p.getName());
        Double unit = item.getUnitPrice() != null && item.getUnitPrice() > 0
                ? item.getUnitPrice()
                : p.getDisplayMinPrice();
        line.setPrice(unit);
        String img = MapperUtils.firstImageUrl(p.getImages());
        line.setImages(img != null ? List.of(img) : List.of());
        line.setWeight(item.getSelectedWeight() != null ? item.getSelectedWeight() : p.getWeight());
        line.setSku(p.getSku());
        return line;
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
