package ma.codexa.troco.controller;

import lombok.RequiredArgsConstructor;
import ma.codexa.troco.common.ApiResponse;
import ma.codexa.troco.common.PageResponse;
import ma.codexa.troco.dto.OrderCreatedDTO;
import ma.codexa.troco.dto.OrderDTO;
import ma.codexa.troco.dto.ProductListItemDTO;
import ma.codexa.troco.entity.Product;
import ma.codexa.troco.mapper.ProductMapper;
import ma.codexa.troco.service.OrderService;
import ma.codexa.troco.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/headless/v1")
@RequiredArgsConstructor
public class HeadlessApiController {

    private final ProductService productService;
    private final ProductMapper productMapper;
    private final OrderService orderService;

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<PageResponse<ProductListItemDTO>>> products(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        Page<Product> productPage = productService.searchProductsWithFilters(
                keyword, null, null, null, null, null, PageRequest.of(page, Math.min(size, 100)));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                productMapper.toListItemDTOList(productPage.getContent()),
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements())));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<?>> product(@PathVariable Long id) {
        return productService.getProductById(id)
                .<ResponseEntity<ApiResponse<?>>>map(p -> ResponseEntity.ok(
                        ApiResponse.success(productMapper.toListItemDTO(p))))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Not found", 404)));
    }

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<OrderCreatedDTO>> createOrder(@RequestBody OrderDTO orderDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(orderService.createOrderFromDTO(orderDTO)));
    }

    @GetMapping("/orders/{orderNumber}")
    public ResponseEntity<ApiResponse<?>> getOrder(@PathVariable String orderNumber) {
        return orderService.getOrderDtoByOrderNumber(orderNumber)
                .<ResponseEntity<ApiResponse<?>>>map(o -> ResponseEntity.ok(ApiResponse.success(o)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Not found", 404)));
    }
}
