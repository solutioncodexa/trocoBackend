package ma.codexa.goldyara.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.codexa.goldyara.common.ApiResponse;
import ma.codexa.goldyara.dto.OrderDTO;
import ma.codexa.goldyara.entity.Order;
import ma.codexa.goldyara.mapper.OrderMapper;
import ma.codexa.goldyara.mapper.ProductMapper;
import ma.codexa.goldyara.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "API de gestion des commandes")
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;

    @Operation(summary = "Récupérer toutes les commandes")
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        List<OrderDTO> orderDTOs = orders.stream()
                .map(order -> orderMapper.toDTO(order, productMapper))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(orderDTOs));
    }

    @Operation(summary = "Récupérer une commande par ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id)
                .map(order -> orderMapper.toDTO(order, productMapper))
                .map(dto -> ResponseEntity.ok(ApiResponse.success(dto)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Commande non trouvée", 404)));
    }

    @Operation(summary = "Récupérer une commande par numéro")
    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrderByOrderNumber(@PathVariable String orderNumber) {
        return orderService.getOrderByOrderNumber(orderNumber)
                .map(order -> orderMapper.toDTO(order, productMapper))
                .map(dto -> ResponseEntity.ok(ApiResponse.success(dto)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Commande non trouvée", 404)));
    }

    @Operation(summary = "Récupérer les commandes par statut")
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getOrdersByStatus(@PathVariable String status) {
        String backendStatus = status.toUpperCase();
        List<Order> orders = orderService.getOrdersByStatus(backendStatus);
        List<OrderDTO> orderDTOs = orders.stream()
                .map(order -> orderMapper.toDTO(order, productMapper))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(orderDTOs));
    }

    @Operation(summary = "Créer une nouvelle commande")
    @PostMapping
    public ResponseEntity<ApiResponse<OrderDTO>> createOrder(@Valid @RequestBody OrderDTO orderDTO) {
        Order createdOrder = orderService.createOrderFromDTO(orderDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(orderMapper.toDTO(createdOrder, productMapper), "Commande créée avec succès"));
    }

    @Operation(summary = "Mettre à jour le statut d'une commande")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderDTO>> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> statusUpdate) {
        String status = statusUpdate.get("status");
        String backendStatus = status.toUpperCase();
        Order updatedOrder = orderService.updateOrderStatus(id, backendStatus);
        return ResponseEntity.ok(ApiResponse.success(orderMapper.toDTO(updatedOrder, productMapper), "Statut mis à jour"));
    }

    @Operation(summary = "Supprimer une commande")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}
