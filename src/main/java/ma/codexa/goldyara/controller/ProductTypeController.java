package ma.codexa.goldyara.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ma.codexa.goldyara.common.ApiResponse;
import ma.codexa.goldyara.dto.ProductTypeDTO;
import ma.codexa.goldyara.entity.ProductType;
import ma.codexa.goldyara.service.ProductTypeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/product-types")
@RequiredArgsConstructor
@Tag(name = "Product Types", description = "API des types de produits")
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class ProductTypeController {

    private final ProductTypeService productTypeService;

    private static ProductTypeDTO toDTO(ProductType t) {
        ProductTypeDTO dto = new ProductTypeDTO();
        dto.setId(t.getId() != null ? t.getId().toString() : null);
        dto.setName(t.getName());
        dto.setCode(t.getCode());
        dto.setRequiresSize(t.getRequiresSize());
        if (t.getSizeOptions() != null && !t.getSizeOptions().isBlank()) {
            dto.setSizeOptions(Arrays.asList(t.getSizeOptions().split("\\s*,\\s*")));
        }
        return dto;
    }

    private static ProductType toEntity(ProductTypeDTO dto) {
        ProductType t = new ProductType();
        if (dto.getId() != null && !dto.getId().isEmpty()) {
            t.setId(Long.parseLong(dto.getId()));
        }
        t.setName(dto.getName());
        t.setCode(dto.getCode());
        t.setRequiresSize(dto.getRequiresSize() != null ? dto.getRequiresSize() : false);
        if (dto.getSizeOptions() != null && !dto.getSizeOptions().isEmpty()) {
            t.setSizeOptions(String.join(",", dto.getSizeOptions()));
        }
        return t;
    }

    @Operation(summary = "Liste des types de produits")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductTypeDTO>>> getAllProductTypes() {
        List<ProductTypeDTO> list = productTypeService.getAllProductTypes().stream()
                .map(ProductTypeController::toDTO)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductTypeDTO> getProductTypeById(@PathVariable Long id) {
        return productTypeService.getProductTypeById(id)
                .map(ProductTypeController::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ProductTypeDTO> getProductTypeByCode(@PathVariable String code) {
        return productTypeService.getProductTypeByCode(code.toUpperCase())
                .map(ProductTypeController::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ProductTypeDTO> createProductType(@RequestBody ProductTypeDTO dto) {
        ProductType entity = toEntity(dto);
        if (entity.getCode() == null || entity.getCode().isBlank()) {
            entity.setCode(dto.getName().toUpperCase().replaceAll("\\s+", "_"));
        } else {
            entity.setCode(entity.getCode().toUpperCase());
        }
        ProductType created = productTypeService.createProductType(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductTypeDTO> updateProductType(@PathVariable Long id, @RequestBody ProductTypeDTO dto) {
        ProductType entity = toEntity(dto);
        entity.setId(id);
        if (entity.getCode() != null) {
            entity.setCode(entity.getCode().toUpperCase());
        }
        ProductType updated = productTypeService.updateProductType(id, entity);
        return ResponseEntity.ok(toDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProductType(@PathVariable Long id) {
        productTypeService.deleteProductType(id);
        return ResponseEntity.noContent().build();
    }
}
