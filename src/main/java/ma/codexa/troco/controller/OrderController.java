package ma.codexa.troco.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.common.PageResponse;
import ma.codexa.troco.dto.OrderCreatedDTO;
import ma.codexa.troco.dto.OrderDTO;
import ma.codexa.troco.dto.OrderListItemDTO;
import ma.codexa.troco.entity.Order;
import ma.codexa.troco.mapper.OrderMapper;
import ma.codexa.troco.mapper.ProductMapper;
import ma.codexa.troco.service.OrderService;
import ma.codexa.troco.security.AppPermissions;
import ma.codexa.troco.security.annotations.RequirePermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Validated
@Tag(name = "Orders", description = "API de gestion des commandes")
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;

    @Operation(summary = "Récupérer les commandes (paginé)")
    @GetMapping
    @RequirePermission(AppPermissions.ORDERS_VIEW)
    public ResponseEntity<ApiResponse<PageResponse<OrderListItemDTO>>> getAllOrders(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        Sort sort = "ASC".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Page<Order> orderPage = orderService.getOrdersPage(status, keyword, PageRequest.of(page, size, sort));
        List<OrderListItemDTO> orderDTOs = orderPage.getContent().stream()
                .map(orderMapper::toListItemDTO)
                .toList();
        PageResponse<OrderListItemDTO> pageResponse = PageResponse.of(
                orderDTOs, orderPage.getNumber(), orderPage.getSize(), orderPage.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    @Operation(summary = "Récupérer une commande par ID")
    @GetMapping("/{id:\\d+}")
    @RequirePermission(AppPermissions.ORDERS_VIEW)
    public ResponseEntity<ApiResponse<OrderDTO>> getOrderById(@PathVariable Long id) {
        return orderService.getOrderDtoById(id)
                .map(dto -> ResponseEntity.ok(ApiResponse.success(dto)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Commande non trouvée", 404)));
    }

    @Operation(summary = "Récupérer une commande par numéro")
    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrderByOrderNumber(@PathVariable String orderNumber) {
        return orderService.getOrderDtoByOrderNumber(orderNumber)
                .map(dto -> ResponseEntity.ok(ApiResponse.success(dto)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Commande non trouvée", 404)));
    }

    @Operation(summary = "Récupérer les commandes par statut")
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<OrderListItemDTO>>> getOrdersByStatus(@PathVariable String status) {
        String backendStatus = status.toUpperCase();
        List<Order> orders = orderService.getOrdersByStatus(backendStatus);
        List<OrderListItemDTO> orderDTOs = orders.stream()
                .map(orderMapper::toListItemDTO)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(orderDTOs));
    }

    @Operation(summary = "Créer une nouvelle commande")
    @PostMapping
    public ResponseEntity<ApiResponse<OrderCreatedDTO>> createOrder(@Valid @RequestBody OrderDTO orderDTO) {
        OrderCreatedDTO created = orderService.createOrderFromDTO(orderDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Commande créée avec succès"));
    }

    @Operation(summary = "Mettre à jour le statut d'une commande")
    @PatchMapping("/{id:\\d+}/status")
    @RequirePermission(AppPermissions.ORDERS_UPDATE)
    public ResponseEntity<ApiResponse<OrderDTO>> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> statusUpdate) {
        String status = statusUpdate.get("status");
        String backendStatus = status.toUpperCase();
        OrderDTO updatedOrder = orderService.updateOrderStatus(id, backendStatus);
        return ResponseEntity.ok(ApiResponse.success(updatedOrder, "Statut mis à jour"));
    }

    @PatchMapping("/{id:\\d+}/tracking")
    @RequirePermission(AppPermissions.ORDERS_UPDATE)
    public ResponseEntity<ApiResponse<OrderDTO>> updateTracking(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.updateTracking(id, body.get("trackingNumber")),
                "Suivi mis à jour"));
    }

    @Operation(summary = "Supprimer une commande")
    @DeleteMapping("/{id:\\d+}")
    @RequirePermission(AppPermissions.ORDERS_UPDATE)
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}
